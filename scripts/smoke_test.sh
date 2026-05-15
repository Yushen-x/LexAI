#!/bin/bash
# 综合烟测：health / consultation / contract-review，验证 Deli + 混元 + RAG 真实工作
set +e

BASE="http://127.0.0.1:8081/api"

echo "============================="
echo "1. health check"
echo "============================="
curl -sS -m 5 -w "\nHTTP=%{http_code}\n" "$BASE/system/health"
echo

echo "============================="
echo "2. legal/consultation (含 Deli + RAG)"
echo "============================="
curl -sS -m 60 -w "\nHTTP=%{http_code}\n" \
  -X POST "$BASE/legal/consultation" \
  -H "Content-Type: application/json" \
  -d '{"question":"公司未签劳动合同且拖欠工资两个月，我应当如何主张权利？","facts":["已入职6个月","存在微信工作记录","工资曾通过个人转账发放"]}' \
  | python3 -c "import json,sys; d=json.loads(sys.stdin.read().split('HTTP=')[0]); print(json.dumps({'category':d.get('category'),'legalBasisCount':len(d.get('legalBasis',[])),'recommendationsCount':len(d.get('recommendations',[])),'riskAlertsCount':len(d.get('riskAlerts',[])),'retrieval':{'laws':len(d.get('retrievalContext',{}).get('laws',[])),'cases':len(d.get('retrievalContext',{}).get('cases',[])),'knowledge':len(d.get('retrievalContext',{}).get('knowledge',[]))},'sampleLaw': (d.get('retrievalContext',{}).get('laws') or [None])[0],'sampleCase': (d.get('retrievalContext',{}).get('cases') or [None])[0],'sampleKB': (d.get('retrievalContext',{}).get('knowledge') or [None])[0]}, ensure_ascii=False, indent=2))" 2>&1 | head -c 3000
echo
echo

echo "============================="
echo "3. legal/contract-review (Deli law + RAG)"
echo "============================="
curl -sS -m 60 -w "\nHTTP=%{http_code}\n" \
  -X POST "$BASE/legal/contract-review" \
  -H "Content-Type: application/json" \
  -d '{"contractTitle":"软件开发服务合同","contractContent":"本合同由甲方与乙方签订，乙方为甲方提供定制化软件开发服务。合同期限为6个月，总金额为人民币20万元。乙方应在合同签订后3个月内交付初版系统。本合同最终解释权归甲方所有。"}' \
  | python3 -c "import json,sys; d=json.loads(sys.stdin.read().split('HTTP=')[0]); print(json.dumps({'risksCount':len(d.get('risks',[])),'missingCount':len(d.get('missingClauses',[])),'summary': (d.get('summary') or '')[:120],'retrieval':{'laws':len(d.get('retrievalContext',{}).get('laws',[])),'knowledge':len(d.get('retrievalContext',{}).get('knowledge',[]))},'sampleLaw': (d.get('retrievalContext',{}).get('laws') or [None])[0],'sampleKB': (d.get('retrievalContext',{}).get('knowledge') or [None])[0],'firstRisk': d.get('risks',[None])[0]}, ensure_ascii=False, indent=2))" 2>&1 | head -c 3000
echo
