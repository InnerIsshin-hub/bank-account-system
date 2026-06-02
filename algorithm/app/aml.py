from __future__ import annotations

from collections import defaultdict, deque
from dataclasses import dataclass
from datetime import timedelta
from decimal import Decimal
from statistics import mean
from typing import Any

from .models import (
    AmlAccountRisk,
    AmlEvaluateRequest,
    AmlEvaluateResponse,
    AmlTransactionAlert,
    RiskAction,
    RiskLevel,
    Transaction,
)


@dataclass(frozen=True)
class AmlConfig:
    high_amount: Decimal = Decimal("50000")
    structuring_floor: Decimal = Decimal("45000")
    structuring_window_hours: int = 24
    rapid_window_minutes: int = 30
    fan_threshold: int = 5
    cycle_depth: int = 4

    @classmethod
    def from_dict(cls, value: dict[str, Any] | None) -> "AmlConfig":
        if not value:
            return cls()
        defaults = cls()
        return cls(
            high_amount=Decimal(str(value.get("high_amount", defaults.high_amount))),
            structuring_floor=Decimal(str(value.get("structuring_floor", defaults.structuring_floor))),
            structuring_window_hours=int(value.get("structuring_window_hours", defaults.structuring_window_hours)),
            rapid_window_minutes=int(value.get("rapid_window_minutes", defaults.rapid_window_minutes)),
            fan_threshold=int(value.get("fan_threshold", defaults.fan_threshold)),
            cycle_depth=int(value.get("cycle_depth", defaults.cycle_depth)),
        )


def evaluate_aml(request: AmlEvaluateRequest) -> AmlEvaluateResponse:
    config = AmlConfig.from_dict(request.config)
    transactions = sorted(request.transactions, key=lambda item: item.timestamp)
    incoming: dict[str, list[Transaction]] = defaultdict(list)
    outgoing: dict[str, list[Transaction]] = defaultdict(list)
    edges: dict[str, set[str]] = defaultdict(set)

    for tx in transactions:
        outgoing[tx.from_account].append(tx)
        incoming[tx.to_account].append(tx)
        edges[tx.from_account].add(tx.to_account)

    focus_accounts = set(request.focus_accounts or [])
    if not focus_accounts:
        focus_accounts = set(incoming) | set(outgoing)

    account_risks = [
        _score_account(account, incoming, outgoing, edges, config)
        for account in sorted(focus_accounts)
    ]
    account_index = {risk.account: risk for risk in account_risks}

    transaction_alerts = [
        _score_transaction(tx, incoming, outgoing, account_index, config)
        for tx in transactions
    ]

    graph_summary = {
        "transactionCount": len(transactions),
        "accountCount": len(set(incoming) | set(outgoing)),
        "edgeCount": sum(len(values) for values in edges.values()),
        "highRiskAccounts": [risk.account for risk in account_risks if risk.level in {"HIGH", "CRITICAL"}],
        "flaggedTransactions": sum(1 for alert in transaction_alerts if alert.action != "PASS"),
    }
    return AmlEvaluateResponse(
        transaction_alerts=transaction_alerts,
        account_risks=account_risks,
        graph_summary=graph_summary,
    )


def _score_account(
    account: str,
    incoming: dict[str, list[Transaction]],
    outgoing: dict[str, list[Transaction]],
    edges: dict[str, set[str]],
    config: AmlConfig,
) -> AmlAccountRisk:
    in_list = incoming.get(account, [])
    out_list = outgoing.get(account, [])
    in_counterparties = {tx.from_account for tx in in_list}
    out_counterparties = {tx.to_account for tx in out_list}
    all_amounts = [float(tx.amount) for tx in in_list + out_list]

    score = 5.0
    reasons: list[str] = []
    features: dict[str, Any] = {
        "incomingCount": len(in_list),
        "outgoingCount": len(out_list),
        "incomingCounterparties": len(in_counterparties),
        "outgoingCounterparties": len(out_counterparties),
        "averageAmount": round(mean(all_amounts), 2) if all_amounts else 0,
    }

    structured = _count_structuring(out_list, config)
    if structured >= 3:
        score += 30 + min(structured * 4, 30)
        reasons.append("multiple outgoing transfers just below reporting threshold")
    features["structuringTransferCount"] = structured

    rapid_matches = _rapid_pass_through(account, in_list, out_list, config)
    if rapid_matches:
        score += 25 + min(rapid_matches * 5, 30)
        reasons.append("rapid pass-through from incoming to outgoing funds")
    features["rapidPassThroughCount"] = rapid_matches

    if len(in_counterparties) >= config.fan_threshold and len(out_counterparties) >= config.fan_threshold:
        score += 25
        reasons.append("fan-in and fan-out pattern")

    cycles = _cycle_count(account, edges, config.cycle_depth)
    if cycles:
        score += 20 + min(cycles * 8, 30)
        reasons.append("cyclic fund movement detected")
    features["cycleCount"] = cycles

    level, action = _decision(score)
    return AmlAccountRisk(
        account=account,
        score=round(score, 2),
        level=level,
        action=action,
        reasons=reasons or ["no material AML pattern detected"],
        features=features,
    )


def _score_transaction(
    tx: Transaction,
    incoming: dict[str, list[Transaction]],
    outgoing: dict[str, list[Transaction]],
    account_index: dict[str, AmlAccountRisk],
    config: AmlConfig,
) -> AmlTransactionAlert:
    score = 5.0
    reasons: list[str] = []
    features: dict[str, Any] = {
        "amount": float(tx.amount),
        "senderRisk": account_index.get(tx.from_account).score if tx.from_account in account_index else 0,
        "receiverRisk": account_index.get(tx.to_account).score if tx.to_account in account_index else 0,
    }

    if tx.amount >= config.high_amount:
        score += 30
        reasons.append("large transfer amount")
    elif config.structuring_floor <= tx.amount < config.high_amount:
        score += 20
        reasons.append("amount close to reporting threshold")

    before_in = [
        item for item in incoming.get(tx.from_account, [])
        if timedelta(0) <= tx.timestamp - item.timestamp <= timedelta(minutes=config.rapid_window_minutes)
    ]
    if before_in:
        score += 20
        reasons.append("sender recently received funds before sending")

    related_outgoing = [
        item for item in outgoing.get(tx.from_account, [])
        if abs((tx.timestamp - item.timestamp).total_seconds()) <= config.structuring_window_hours * 3600
    ]
    near_threshold = [item for item in related_outgoing if config.structuring_floor <= item.amount < config.high_amount]
    if len(near_threshold) >= 3:
        score += 20
        reasons.append("sender has repeated near-threshold transfers")

    score += min(features["senderRisk"], 80) * 0.25
    score += min(features["receiverRisk"], 80) * 0.15

    level, action = _decision(score)
    return AmlTransactionAlert(
        transaction_id=tx.transaction_id,
        score=round(score, 2),
        level=level,
        action=action,
        reasons=reasons or ["transaction passed AML rules"],
        features=features,
    )


def _count_structuring(transactions: list[Transaction], config: AmlConfig) -> int:
    near = [tx for tx in transactions if config.structuring_floor <= tx.amount < config.high_amount]
    max_count = 0
    for index, tx in enumerate(near):
        window_end = tx.timestamp + timedelta(hours=config.structuring_window_hours)
        max_count = max(max_count, sum(1 for item in near[index:] if item.timestamp <= window_end))
    return max_count


def _rapid_pass_through(
    account: str,
    incoming: list[Transaction],
    outgoing: list[Transaction],
    config: AmlConfig,
) -> int:
    matches = 0
    for in_tx in incoming:
        for out_tx in outgoing:
            if out_tx.timestamp < in_tx.timestamp:
                continue
            if out_tx.timestamp - in_tx.timestamp > timedelta(minutes=config.rapid_window_minutes):
                continue
            ratio = min(in_tx.amount, out_tx.amount) / max(in_tx.amount, out_tx.amount)
            if ratio >= Decimal("0.80") and out_tx.from_account == account:
                matches += 1
                break
    return matches


def _cycle_count(start: str, edges: dict[str, set[str]], max_depth: int) -> int:
    count = 0
    queue: deque[tuple[str, int, set[str]]] = deque([(start, 0, {start})])
    while queue:
        node, depth, visited = queue.popleft()
        if depth >= max_depth:
            continue
        for nxt in edges.get(node, set()):
            if nxt == start and depth >= 1:
                count += 1
                continue
            if nxt not in visited:
                queue.append((nxt, depth + 1, visited | {nxt}))
    return count


def _decision(score: float) -> tuple[RiskLevel, RiskAction]:
    if score >= 90:
        return "CRITICAL", "REJECT"
    if score >= 70:
        return "HIGH", "MANUAL_REVIEW"
    if score >= 40:
        return "MEDIUM", "CHALLENGE"
    return "LOW", "PASS"
