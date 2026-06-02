#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VENV_DIR="${ALGORITHM_VENV:-/opt/bank-algorithm-venv}"
ALGORITHM_HOST="${ALGORITHM_HOST:-0.0.0.0}"
ALGORITHM_PORT="${ALGORITHM_PORT:-8090}"

if [ ! -x "$VENV_DIR/bin/uvicorn" ]; then
  echo "uvicorn not found at $VENV_DIR/bin/uvicorn" >&2
  echo "Install dependencies with: $VENV_DIR/bin/pip install -r $SCRIPT_DIR/requirements.txt" >&2
  exit 1
fi

cd "$SCRIPT_DIR"
exec "$VENV_DIR/bin/uvicorn" app.main:app --host "$ALGORITHM_HOST" --port "$ALGORITHM_PORT" "$@"
