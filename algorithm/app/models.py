from __future__ import annotations

from datetime import datetime
from decimal import Decimal
from typing import Any, Literal

from pydantic import BaseModel, Field


RiskLevel = Literal["LOW", "MEDIUM", "HIGH", "CRITICAL"]
RiskAction = Literal["PASS", "CHALLENGE", "MANUAL_REVIEW", "REJECT"]


class Transaction(BaseModel):
    transaction_id: str = Field(..., min_length=1)
    from_account: str = Field(..., min_length=1)
    to_account: str = Field(..., min_length=1)
    amount: Decimal = Field(..., gt=0)
    timestamp: datetime
    currency: str = "CNY"
    channel: str | None = None
    device_id: str | None = None
    ip: str | None = None
    remark: str | None = None


class AmlEvaluateRequest(BaseModel):
    transactions: list[Transaction]
    focus_accounts: list[str] | None = None
    config: dict[str, Any] | None = None


class AmlTransactionAlert(BaseModel):
    transaction_id: str
    score: float
    level: RiskLevel
    action: RiskAction
    reasons: list[str]
    features: dict[str, Any]


class AmlAccountRisk(BaseModel):
    account: str
    score: float
    level: RiskLevel
    action: RiskAction
    reasons: list[str]
    features: dict[str, Any]


class AmlEvaluateResponse(BaseModel):
    transaction_alerts: list[AmlTransactionAlert]
    account_risks: list[AmlAccountRisk]
    graph_summary: dict[str, Any]


class OcrFaceRequest(BaseModel):
    image_base64: str
    expected_name: str | None = None
    expected_id_number: str | None = None
    liveness_frames_base64: list[str] | None = None


class OcrFaceResponse(BaseModel):
    decision: RiskAction
    score: float
    document_type: str
    fields: dict[str, Any]
    face: dict[str, Any]
    quality: dict[str, Any]
    reasons: list[str]
    ocr_text: str
