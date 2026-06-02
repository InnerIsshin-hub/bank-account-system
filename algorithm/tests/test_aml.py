from datetime import datetime, timedelta
from decimal import Decimal

from app.aml import evaluate_aml
from app.models import AmlEvaluateRequest, Transaction


def test_aml_detects_structuring_and_cycle():
    base = datetime(2026, 6, 1, 9, 0, 0)
    request = AmlEvaluateRequest(
        transactions=[
            Transaction(transaction_id="T1", from_account="A", to_account="B", amount=Decimal("48000"), timestamp=base),
            Transaction(transaction_id="T2", from_account="A", to_account="C", amount=Decimal("47000"), timestamp=base + timedelta(minutes=5)),
            Transaction(transaction_id="T3", from_account="A", to_account="D", amount=Decimal("49000"), timestamp=base + timedelta(minutes=10)),
            Transaction(transaction_id="T4", from_account="D", to_account="A", amount=Decimal("46000"), timestamp=base + timedelta(minutes=15)),
        ]
    )

    response = evaluate_aml(request)
    account_a = next(item for item in response.account_risks if item.account == "A")

    assert account_a.action in {"CHALLENGE", "MANUAL_REVIEW", "REJECT"}
    assert account_a.features["structuringTransferCount"] >= 1
    assert account_a.features["cycleCount"] >= 1
    assert response.graph_summary["flaggedTransactions"] >= 1


def test_aml_low_risk_passes():
    request = AmlEvaluateRequest(
        transactions=[
            Transaction(
                transaction_id="T1",
                from_account="A",
                to_account="B",
                amount=Decimal("100"),
                timestamp=datetime(2026, 6, 1, 9, 0, 0),
            )
        ]
    )

    response = evaluate_aml(request)

    assert response.account_risks[0].level == "LOW"
    assert response.transaction_alerts[0].action == "PASS"
