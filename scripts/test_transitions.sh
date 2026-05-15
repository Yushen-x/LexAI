#!/usr/bin/env bash
set -u

echo "== Contract 1 (UNDER_REVIEW) try to COMPLETED (should FAIL) =="
curl -s -X PUT 'http://127.0.0.1:8081/api/contracts/1/status' \
  -H 'Content-Type: application/json' \
  -d '{"status":"COMPLETED"}'
echo

echo "== Contract 1 (UNDER_REVIEW) try to SIGNED without approval (should FAIL) =="
curl -s -X PUT 'http://127.0.0.1:8081/api/contracts/1/status' \
  -H 'Content-Type: application/json' \
  -d '{"status":"SIGNED"}'
echo

echo "== Contract 3 (DRAFT) to UNDER_REVIEW (should OK) =="
curl -s -X PUT 'http://127.0.0.1:8081/api/contracts/3/status' \
  -H 'Content-Type: application/json' \
  -d '{"status":"UNDER_REVIEW"}'
echo

echo "== Contract 2 (SIGNED) to IN_PROGRESS (should OK) =="
curl -s -X PUT 'http://127.0.0.1:8081/api/contracts/2/status' \
  -H 'Content-Type: application/json' \
  -d '{"status":"IN_PROGRESS"}'
echo

echo "== Tasks final =="
curl -s 'http://127.0.0.1:8081/api/tasks' | python3 -m json.tool 2>/dev/null | head -30
echo

echo "== Contracts final (first 2) =="
curl -s 'http://127.0.0.1:8081/api/contracts?page=0&size=3' | python3 -m json.tool 2>/dev/null | head -30
