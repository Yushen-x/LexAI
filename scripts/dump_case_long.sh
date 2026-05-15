#!/bin/bash
set -a
. /opt/lexai/backend/lexai.env
set +a

QUERY="公司未签劳动合同且拖欠工资两个月，我应当如何主张权利？"

echo "== queryListCase pageSize=5 with long query (raw first 1500 chars) =="
curl -sS -m 15 \
  -X POST https://openapi.delilegal.com/api/qa/v3/search/queryListCase \
  -H "Content-Type: application/json" \
  -H "appid: $DELI_APP_ID" \
  -H "secret: $DELI_SECRET" \
  -d "{\"pageNo\":1,\"pageSize\":5,\"sortField\":\"correlation\",\"sortOrder\":\"desc\",\"condition\":{\"keywordArr\":[\"$QUERY\"]}}" \
  | python3 -c "
import json,sys
raw = sys.stdin.read()
d = json.loads(raw)
items = d.get('body',{}).get('data',[])
print('count:', len(items))
for i, it in enumerate(items):
    print(f'--- item {i} ---')
    print('  type:', type(it).__name__)
    if isinstance(it, dict):
        print('  title:', repr(it.get('title','MISSING'))[:200])
        print('  id:', repr(it.get('id','MISSING'))[:120])
    else:
        print('  raw:', repr(it)[:200])
"
