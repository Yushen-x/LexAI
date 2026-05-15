#!/bin/bash
set +e
BASE="http://127.0.0.1:8081/api"

echo "============================="
echo "1. consultation raw"
echo "============================="
curl -sS -m 80 \
  -X POST "$BASE/legal/consultation" \
  -H "Content-Type: application/json" \
  -d '{"question":"公司未签劳动合同且拖欠工资两个月，我应当如何主张权利？","facts":["已入职6个月","存在微信工作记录"]}' \
  | python3 -c "
import json,sys
raw = sys.stdin.read()
try:
    d = json.loads(raw)['data']
except Exception as e:
    print('parse error:', e); print(raw[:500]); sys.exit(0)
print('category:', d.get('category'))
print('legalBasis count:', len(d.get('legalBasis',[])))
print('recommendations count:', len(d.get('recommendations',[])))
print('riskAlerts count:', len(d.get('riskAlerts',[])))
rc = d.get('retrievalContext') or {}
print('retrieval.laws count:', len(rc.get('laws',[])))
print('retrieval.cases count:', len(rc.get('cases',[])))
print('retrieval.knowledge count:', len(rc.get('knowledge',[])))
print()
print('--- sample law (if any) ---')
print((rc.get('laws') or [''])[0][:280])
print('--- sample case (if any) ---')
print((rc.get('cases') or [''])[0][:280])
print('--- sample kb (if any) ---')
print((rc.get('knowledge') or [''])[0][:280])
print('--- first legalBasis ---')
print((d.get('legalBasis') or [''])[0][:280])
"

echo
echo "============================="
echo "2. contract-review raw"
echo "============================="
curl -sS -m 80 \
  -X POST "$BASE/legal/contract-review" \
  -H "Content-Type: application/json" \
  -d '{"contractTitle":"软件开发服务合同","contractContent":"本合同由甲方与乙方签订，乙方为甲方提供定制化软件开发服务。合同期限为6个月，总金额为人民币20万元。乙方应在合同签订后3个月内交付初版系统。本合同最终解释权归甲方所有。"}' \
  | python3 -c "
import json,sys
raw = sys.stdin.read()
try:
    d = json.loads(raw)['data']
except Exception as e:
    print('parse error:', e); print(raw[:500]); sys.exit(0)
print('risks count:', len(d.get('risks',[])))
print('missing count:', len(d.get('missingClauses',[])))
print('summary head:', (d.get('summary') or '')[:150])
rc = d.get('retrievalContext') or {}
print('retrieval.laws count:', len(rc.get('laws',[])))
print('retrieval.knowledge count:', len(rc.get('knowledge',[])))
print('--- sample law ---')
print((rc.get('laws') or [''])[0][:300])
print('--- first risk ---')
print(d.get('risks',[None])[0])
"
