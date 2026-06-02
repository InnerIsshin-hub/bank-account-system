from __future__ import annotations

from fastapi import FastAPI

from .aml import evaluate_aml
from .kyc import evaluate_ocr_face
from .models import AmlEvaluateRequest, AmlEvaluateResponse, OcrFaceRequest, OcrFaceResponse


app = FastAPI(title="Bank Algorithm Service", version="1.0.0")


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "UP"}


@app.post("/api/aml/evaluate", response_model=AmlEvaluateResponse)
def aml_evaluate(request: AmlEvaluateRequest) -> AmlEvaluateResponse:
    return evaluate_aml(request)


@app.post("/api/kyc/ocr-face", response_model=OcrFaceResponse)
def kyc_ocr_face(request: OcrFaceRequest) -> OcrFaceResponse:
    return evaluate_ocr_face(request)
