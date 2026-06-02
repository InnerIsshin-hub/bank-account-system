import base64
from io import BytesIO

from PIL import Image, ImageDraw

from app.kyc import evaluate_ocr_face
from app.models import OcrFaceRequest


def _image_base64(image: Image.Image) -> str:
    buf = BytesIO()
    image.save(buf, format="PNG")
    return base64.b64encode(buf.getvalue()).decode("ascii")


def _sample_id_card() -> Image.Image:
    image = Image.new("RGB", (900, 560), "white")
    draw = ImageDraw.Draw(image)
    draw.rectangle((30, 30, 870, 530), outline="black", width=4)
    draw.text((70, 90), "Name: Zhang Ming", fill="black")
    draw.text((70, 150), "ID: 110101199001010011", fill="black")
    draw.ellipse((650, 100, 790, 240), outline="black", width=5)
    draw.ellipse((690, 150, 705, 165), fill="black")
    draw.ellipse((735, 150, 750, 165), fill="black")
    draw.arc((690, 170, 750, 215), 10, 170, fill="black", width=3)
    return image


def test_kyc_ocr_face_extracts_id_and_face_quality():
    image = _sample_id_card()
    request = OcrFaceRequest(
        image_base64=_image_base64(image),
        expected_id_number="110101199001010011",
        expected_name="Zhang Ming",
    )

    response = evaluate_ocr_face(request)

    assert response.fields["idNumber"] == "110101199001010011"
    assert response.quality["resolutionOk"] is True
    assert response.face["count"] >= 1
    assert response.decision in {"PASS", "CHALLENGE", "MANUAL_REVIEW"}
