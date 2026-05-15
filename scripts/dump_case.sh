#!/bin/bash
set -a
. /opt/lexai/backend/lexai.env
set +a

echo "== queryListCase fields for long query =="
curl -sS -m 15 \
  -X POST https://openapi.delilegal.com/api/qa/v3/search/queryListCase \
  -H "Content-Type: application/json" \
  -H "appid: $DELI_APP_ID" \
  -H "secret: $DELI_SECRET" \
  -d '{"pageNo":1,"pageSize":2,"sortField":"correlation","sortOrder":"desc","condition":{"keywordArr":["公司未签劳动合同且拖欠工资两个月，我应当如何主张权利"]}}' \
  | python3 -c "
import json,sys
d = json.loads(sys.stdin.read())
items = d.get('body',{}).get('data',[])
print('total items:', len(items))
for i, it in enumerate(items[:2]):
    print(f'--- item {i} keys ---')
    print(list(it.keys()))
    for k,v in it.items():
        s = str(v)
        if len(s) > 120: s = s[:120]+'...'
        print(f'  {k}: {s}')
    print()
"
