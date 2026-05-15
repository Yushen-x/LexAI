#!/bin/bash
set +e
BASE="http://127.0.0.1:8081/api"

echo "============================="
echo "1. consultation (confidence + citations)"
echo "============================="
curl -sS -m 90 -X POST "$BASE/legal/consultation" \
  -H "Content-Type: application/json" \
  -d '{"question":"公司未签劳动合同且拖欠工资两个月，我应当如何主张权利？","facts":["已入职6个月","存在微信工作记录"]}' \
  | python3 -c "
import json,sys,re
raw = sys.stdin.read()
try:
    d = json.loads(raw)['data']
except Exception as e:
    print('parse error:', e); print(raw[:500]); sys.exit(0)
print('category:', d.get('category'))
print('confidence:', d.get('confidence'))
print('legalBasis count:', len(d.get('legalBasis',[])))
print('recommendations count:', len(d.get('recommendations',[])))
rc = d.get('retrievalContext') or {}
print('retrieval laws/cases/kb:', len(rc.get('laws',[])), len(rc.get('cases',[])), len(rc.get('knowledge',[])))
all_txt = ' '.join((d.get('legalBasis') or []) + (d.get('recommendations') or []) + (d.get('riskAlerts') or []))
cites = re.findall(r'\[[LCK]\d+\]', all_txt)
print('citation markers found:', len(cites), cites[:8])
print('--- first legalBasis ---')
print((d.get('legalBasis') or [''])[0][:280])
"

echo
echo "============================="
echo "2. contract-review (self-check + confidence + citations)"
echo "============================="
curl -sS -m 120 -X POST "$BASE/legal/contract-review" \
  -H "Content-Type: application/json" \
  -d '{"contractTitle":"软件开发服务合同","contractContent":"本合同由甲方与乙方签订，乙方为甲方提供定制化软件开发服务。合同期限为6个月，总金额为人民币20万元。乙方应在合同签订后3个月内交付初版系统。本合同最终解释权归甲方所有。"}' \
  | python3 -c "
import json,sys,re
raw = sys.stdin.read()
try:
    d = json.loads(raw)['data']
except Exception as e:
    print('parse error:', e); print(raw[:500]); sys.exit(0)
print('confidence:', d.get('confidence'))
print('risks count:', len(d.get('risks',[])))
print('missing count:', len(d.get('missingClauses',[])))
print('summary head:', (d.get('summary') or '')[:200])
rc = d.get('retrievalContext') or {}
print('retrieval laws/kb:', len(rc.get('laws',[])), len(rc.get('knowledge',[])))
all_txt = (d.get('summary') or '')
for r in d.get('risks',[]) or []:
    all_txt += ' ' + (r.get('issue') or '') + ' ' + (r.get('suggestion') or '')
cites = re.findall(r'\[[LCK]\d+\]', all_txt)
print('citation markers found:', len(cites), cites[:10])
print('--- first risk ---')
print((d.get('risks') or [None])[0])
"

echo
echo "============================="
echo "3. contract-draft (RAG + confidence)"
echo "============================="
curl -sS -m 90 -X POST "$BASE/legal/contract-draft" \
  -H "Content-Type: application/json" \
  -d '{"contractName":"上海智云科技-张三 劳动合同","contractType":"劳动合同","partyA":"上海智云科技有限公司","partyB":"张三","amount":180000,"duration":"3年（试用期3个月）","requirements":"试用期3个月，月薪1.5万元，包含保密条款与竞业限制条款"}' \
  | python3 -c "
import json,sys
raw = sys.stdin.read()
try:
    d = json.loads(raw)['data']
except Exception as e:
    print('parse error:', e); print(raw[:500]); sys.exit(0)
print('title:', d.get('title'))
print('confidence:', d.get('confidence'))
content = d.get('generatedContent') or ''
print('content length:', len(content))
rc = d.get('retrievalContext') or {}
print('retrieval laws/cases/kb:', len(rc.get('laws',[])), len(rc.get('cases',[])), len(rc.get('knowledge',[])))
print('--- content head ---')
print(content[:500])
print('--- sample law ---')
print((rc.get('laws') or [''])[0][:280])
print('--- sample kb ---')
print((rc.get('knowledge') or [''])[0][:280])
"
