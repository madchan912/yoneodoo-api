#!/usr/bin/env bash
# 운영 DB → 로컬 DB 동기화 (Python 스크립트 래퍼)
# 사용: ./scripts/sync_prod_to_local_db.sh [--env-file /path/to/.env.sync]
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if command -v python3 >/dev/null 2>&1; then
  exec python3 "$SCRIPT_DIR/sync_prod_to_local_db.py" "$@"
fi
if command -v python >/dev/null 2>&1; then
  exec python "$SCRIPT_DIR/sync_prod_to_local_db.py" "$@"
fi
echo "ERROR: python3 또는 python 이 PATH에 필요합니다." >&2
exit 1
