# Bank Algorithm Service

Standalone algorithm service for demo risk and KYC workflows.

## Features

- AML/funds risk detection with explainable graph and rule features.
- ID-card OCR parsing with image quality checks.
- Face-region detection and simple liveness scoring from multiple frames.

The service is deterministic and does not train deep-learning models.

## Run

```bash
cd algorithm
./start.sh
```

Health check:

```bash
curl http://localhost:8090/health
```

## Test

```bash
cd algorithm
/opt/bank-algorithm-venv/bin/python -m pytest -q
```

The prepared runtime in this container uses `/opt/bank-algorithm-venv`.
System OCR dependencies are `tesseract-ocr`, `tesseract-ocr-chi-sim`, `libglib2.0-0`, and `libgl1`.
