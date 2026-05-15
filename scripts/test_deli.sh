#!/bin/bash
# 在服务器上验证 Deli API 实际可用性
set -a
. /opt/lexai/backend/lexai.env
set +a

echo "=========================================="
echo "测试 1: queryListLaw 法律法规检索"
echo "=========================================="
curl -sS -m 15 -w "\nHTTP=%{http_code}\n" \
  -X POST https://openapi.delilegal.com/api/qa/v3/search/queryListLaw \
  -H "Content-Type: application/json" \
  -H "appid: $DELI_APP_ID" \
  -H "secret: $DELI_SECRET" \
  -d '{"pageNo":1,"pageSize":3,"sortField":"correlation","sortOrder":"desc","condition":{"keywords":["合同违约金"],"fieldName":"semantic"}}' \
  | head -c 2000
echo
echo

echo "=========================================="
echo "测试 2: queryListCase 案例检索"
echo "=========================================="
curl -sS -m 15 -w "\nHTTP=%{http_code}\n" \
  -X POST https://openapi.delilegal.com/api/qa/v3/search/queryListCase \
  -H "Content-Type: application/json" \
  -H "appid: $DELI_APP_ID" \
  -H "secret: $DELI_SECRET" \
  -d '{"pageNo":1,"pageSize":2,"sortField":"correlation","sortOrder":"desc","condition":{"keywordArr":["劳动合同解除"]}}' \
  | head -c 2000
echo
