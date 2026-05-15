#!/bin/bash
# 验证 chat answer + 合同状态变更兼容性
set -e
BASE=http://127.0.0.1:8081/api

echo "== A. 法律咨询 (验证 answer 字段) =="
RESP=$(curl -s -X POST "$BASE/legal/consultation" \
  -H "Content-Type: application/json" \
  -d '{"question":"公司没签劳动合同还拖欠工资，我能拿二倍工资吗？","facts":["入职6个月","无书面合同"],"createFollowUpTask":false}')
echo "$RESP" | python3 -c '
import sys, json
b = json.loads(sys.stdin.read()).get("data", {})
print("category :", b.get("category"))
print("answer   :", (b.get("answer") or "")[:160])
print("rec      :", len(b.get("recommendations") or []))
print("risks    :", len(b.get("riskAlerts") or []))
print("basis    :", len(b.get("legalBasis") or []))
print("conf     :", b.get("confidence"))
'

echo
echo "== B. 合同台账：创建草稿 → 改为提交审查 =="
CREATE=$(curl -s -X POST "$BASE/contracts" \
  -H "Content-Type: application/json" \
  -d '{"name":"smoke合同","contractType":"采购合同","partyA":"甲方A","partyB":"乙方B","amount":5000,"content":"测试正文 ……","source":"AI_DRAFT","status":"DRAFT"}')
ID=$(echo "$CREATE" | python3 -c 'import sys,json;print(json.loads(sys.stdin.read())["data"]["id"])')
echo "  created id = $ID"

UPD=$(curl -s -X PUT "$BASE/contracts/$ID" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"smoke合同\",\"contractType\":\"采购合同\",\"partyA\":\"甲方A\",\"partyB\":\"乙方B\",\"amount\":5000,\"content\":\"修订后的测试正文\",\"source\":\"AI_DRAFT\",\"status\":\"UNDER_REVIEW\"}")
echo "$UPD" | python3 -c '
import sys, json
b = json.loads(sys.stdin.read()).get("data", {})
print("update id     :", b.get("id"))
print("update status :", b.get("status"))
print("update name   :", b.get("name"))
'
