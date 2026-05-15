#!/usr/bin/env bash
# 验证保存超长 content 的合同（之前会触发 Data truncation: Data too long for column 'content'）
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8081/api}"
PAYLOAD_FILE="/tmp/smoke_long_payload.json"

python3 - <<PY > "$PAYLOAD_FILE"
import json
content = ('合同条款示例。' * 10000)[:80000]
print(json.dumps({
    "name": "Smoke 长正文合同",
    "contractType": "采购合同",
    "partyA": "A 公司",
    "partyB": "B 公司",
    "amount": 100000,
    "content": content,
    "status": "DRAFT",
    "source": "AI_GENERATED"
}, ensure_ascii=False))
PY

echo "==> payload size: $(wc -c < "$PAYLOAD_FILE") bytes"
echo "==> POST /contracts"
RESP=$(curl -sS -w '\nHTTP_STATUS:%{http_code}\n' \
  -X POST "$BASE_URL/contracts" \
  -H 'Content-Type: application/json; charset=utf-8' \
  --data-binary "@$PAYLOAD_FILE")

echo "$RESP" | tail -n 5
STATUS=$(echo "$RESP" | grep '^HTTP_STATUS:' | cut -d: -f2 | tr -d '[:space:]')
echo "==> http_status=$STATUS"

if [[ "$STATUS" == "200" || "$STATUS" == "201" ]]; then
  ID=$(echo "$RESP" | python3 -c "
import sys, json
body = sys.stdin.read().split('HTTP_STATUS')[0].strip()
j = json.loads(body)
print((j.get('data') or {}).get('id', ''))
")
  echo "==> created contract id=$ID"
  echo "==> PUT /contracts/$ID/status -> UNDER_REVIEW"
  curl -sS -w '\nHTTP_STATUS:%{http_code}\n' \
    -X PUT "$BASE_URL/contracts/$ID/status?status=UNDER_REVIEW" \
    | tail -n 3
fi
