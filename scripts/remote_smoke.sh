#!/usr/bin/env bash
set -u

echo "== nginx /api/actuator/health =="
curl -s -w '\nhttp=%{http_code}\n' http://127.0.0.1/api/actuator/health
echo

echo "== nginx /api/contracts =="
curl -s 'http://127.0.0.1/api/contracts?page=0&size=2' | head -c 500
echo
echo

echo "== nginx /api/tasks =="
curl -s 'http://127.0.0.1/api/tasks' | head -c 800
echo
echo

echo "== nginx / (index head) =="
curl -sI http://127.0.0.1/ | head -3

echo "== assets fingerprint in index.html =="
curl -s http://127.0.0.1/ | grep -oE 'assets/[^"]+' | head -5
echo

echo "== backend uptime / pid =="
systemctl show lexai-backend -p ActiveEnterTimestamp -p MainPID --value
echo

echo "== last 20 backend log lines =="
journalctl -u lexai-backend -n 20 --no-pager 2>/dev/null || sudo -n journalctl -u lexai-backend -n 20 --no-pager
