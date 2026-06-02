from __future__ import annotations

import base64
import re
from io import BytesIO
from typing import Any

import cv2
import numpy as np
import pytesseract
from PIL import Image, ImageOps

from .models import OcrFaceRequest, OcrFaceResponse


ID_PATTERN = re.compile(r"\b[1-9]\d{5}(?:18|19|20)\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\d|3[01])\d{3}[\dXx]\b")
NAME_PATTERNS = [
    re.compile(r"(?:Name|NAME|name)[:\s]+([A-Za-z][A-Za-z\s]{1,40})"),
    re.compile(r"(?:姓名)[:\s]*([\u4e00-\u9fff]{2,8})"),
]


def evaluate_ocr_face(request: OcrFaceRequest) -> OcrFaceResponse:
    image = _decode_image(request.image_base64)
    cv_image = cv2.cvtColor(np.array(image.convert("RGB")), cv2.COLOR_RGB2BGR)
    gray = cv2.cvtColor(cv_image, cv2.COLOR_BGR2GRAY)

    quality = _quality(gray)
    ocr_text = _ocr(image)
    fields = _extract_fields(ocr_text)
    face = _detect_face(cv_image)
    face["livenessScore"] = _liveness_score(request.liveness_frames_base64 or [])

    score = 10.0
    reasons: list[str] = []
    if quality["sharpness"] < 80:
        score += 20
        reasons.append("image is blurry")
    if quality["brightness"] < 45 or quality["brightness"] > 220:
        score += 15
        reasons.append("image brightness is outside normal range")
    if fields.get("idNumber"):
        score -= 10
    else:
        score += 25
        reasons.append("id number was not recognized")
    if request.expected_id_number and fields.get("idNumber") != request.expected_id_number:
        score += 35
        reasons.append("id number does not match expected value")
    if request.expected_name and fields.get("name"):
        if _normalize_name(fields["name"]) != _normalize_name(request.expected_name):
            score += 20
            reasons.append("name does not match expected value")
    elif request.expected_name:
        score += 10
        reasons.append("name was not recognized")
    if face["count"] != 1:
        score += 25
        reasons.append("expected exactly one face region")
    if request.liveness_frames_base64 and face["livenessScore"] < 0.15:
        score += 25
        reasons.append("liveness frame difference is too small")

    decision = "PASS"
    if score >= 85:
        decision = "REJECT"
    elif score >= 55:
        decision = "MANUAL_REVIEW"
    elif score >= 35:
        decision = "CHALLENGE"

    return OcrFaceResponse(
        decision=decision,
        score=round(max(score, 0), 2),
        document_type="ID_CARD",
        fields=fields,
        face=face,
        quality=quality,
        reasons=reasons or ["document and face checks passed"],
        ocr_text=ocr_text.strip(),
    )


def _decode_image(image_base64: str) -> Image.Image:
    value = image_base64.split(",", 1)[-1]
    raw = base64.b64decode(value)
    return Image.open(BytesIO(raw)).convert("RGB")


def _quality(gray: np.ndarray) -> dict[str, Any]:
    h, w = gray.shape[:2]
    sharpness = float(cv2.Laplacian(gray, cv2.CV_64F).var())
    brightness = float(gray.mean())
    glare_ratio = float((gray > 245).sum() / gray.size)
    return {
        "width": int(w),
        "height": int(h),
        "sharpness": round(sharpness, 2),
        "brightness": round(brightness, 2),
        "glareRatio": round(glare_ratio, 4),
        "resolutionOk": w >= 480 and h >= 300,
    }


def _ocr(image: Image.Image) -> str:
    gray = ImageOps.grayscale(image)
    scaled = gray.resize((gray.width * 2, gray.height * 2))
    try:
        return pytesseract.image_to_string(scaled, lang="chi_sim+eng", config="--psm 6")
    except pytesseract.TesseractError:
        return pytesseract.image_to_string(scaled, lang="eng", config="--psm 6")


def _extract_fields(text: str) -> dict[str, Any]:
    compact = re.sub(r"\s+", " ", text)
    id_match = ID_PATTERN.search(compact.replace(" ", ""))
    name = None
    for pattern in NAME_PATTERNS:
        match = pattern.search(compact)
        if match:
            name = match.group(1).strip()
            break
    return {
        "name": name,
        "idNumber": id_match.group(0).upper() if id_match else None,
    }


def _detect_face(image: np.ndarray) -> dict[str, Any]:
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    faces: list[tuple[int, int, int, int]] = []
    cascade_path = cv2.data.haarcascades + "haarcascade_frontalface_default.xml"
    classifier = cv2.CascadeClassifier(cascade_path)
    if not classifier.empty():
        detected = classifier.detectMultiScale(gray, scaleFactor=1.1, minNeighbors=4, minSize=(48, 48))
        faces = [tuple(map(int, item)) for item in detected]
    if not faces:
        faces = _fallback_face_regions(gray)
    boxes = [{"x": x, "y": y, "width": w, "height": h} for x, y, w, h in faces]
    return {
        "count": len(boxes),
        "boxes": boxes,
        "method": "haar" if boxes else "none",
    }


def _fallback_face_regions(gray: np.ndarray) -> list[tuple[int, int, int, int]]:
    blurred = cv2.GaussianBlur(gray, (5, 5), 0)
    edges = cv2.Canny(blurred, 50, 150)
    contours, _ = cv2.findContours(edges, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    candidates: list[tuple[int, int, int, int]] = []
    for contour in contours:
        x, y, w, h = cv2.boundingRect(contour)
        area = w * h
        if area < 2500:
            continue
        ratio = w / max(h, 1)
        if 0.55 <= ratio <= 1.45:
            candidates.append((x, y, w, h))
    candidates.sort(key=lambda box: box[2] * box[3], reverse=True)
    return candidates[:1]


def _liveness_score(frames_base64: list[str]) -> float:
    if len(frames_base64) < 2:
        return 0.0
    frames = []
    for item in frames_base64[:5]:
        image = _decode_image(item)
        gray = cv2.cvtColor(np.array(image.convert("RGB")), cv2.COLOR_RGB2GRAY)
        frames.append(cv2.resize(gray, (160, 100)))
    diffs = []
    for left, right in zip(frames, frames[1:]):
        diffs.append(float(np.mean(cv2.absdiff(left, right))) / 255.0)
    return round(float(np.mean(diffs)), 4) if diffs else 0.0


def _normalize_name(value: str) -> str:
    return re.sub(r"\s+", "", value).lower()
