#!/bin/bash
# 远端部署脚本：备份 -> 替换 -> 重启 -> 健康检查
set -euo pipefail

BACKEND_DIR=/opt/lexai/backend
FRONTEND_DIR=/opt/lexai/frontend
TS=$(date +%Y%m%d-%H%M%S)

echo "== 1. 备份 =="
sudo mkdir -p "$BACKEND_DIR/backup" "$FRONTEND_DIR/backup"
if [ -f "$BACKEND_DIR/lexai-backend.jar" ]; then
  sudo cp "$BACKEND_DIR/lexai-backend.jar" "$BACKEND_DIR/backup/lexai-backend-$TS.jar"
fi
if [ -d "$FRONTEND_DIR/dist" ]; then
  sudo tar -czf "$FRONTEND_DIR/backup/dist-$TS.tgz" -C "$FRONTEND_DIR" dist
fi

echo "== 2. 替换 backend jar (同时覆盖两种文件名以兼容 systemd unit 配置) =="
sudo install -m 0644 -o ubuntu -g ubuntu /tmp/lexai-backend-0.1.0.jar "$BACKEND_DIR/lexai-backend.jar"
sudo install -m 0644 -o ubuntu -g ubuntu /tmp/lexai-backend-0.1.0.jar "$BACKEND_DIR/lexai-backend-0.1.0.jar"

echo "== 3. 解包 frontend dist (兼容 tar 顶层是 dist/ 或直接是文件的两种打包) =="
sudo rm -rf "$FRONTEND_DIR/dist.new" "$FRONTEND_DIR/_unpack"
sudo mkdir -p "$FRONTEND_DIR/_unpack"
sudo tar -xzf /tmp/lexai-dist.tgz -C "$FRONTEND_DIR/_unpack"
# 如果解出来是 _unpack/dist/index.html 就用 _unpack/dist；否则就是 _unpack/index.html
if [ -f "$FRONTEND_DIR/_unpack/dist/index.html" ]; then
  sudo mv "$FRONTEND_DIR/_unpack/dist" "$FRONTEND_DIR/dist.new"
elif [ -f "$FRONTEND_DIR/_unpack/index.html" ]; then
  sudo mv "$FRONTEND_DIR/_unpack" "$FRONTEND_DIR/dist.new"
else
  echo "ERROR: tar 包里找不到 index.html，部署中止"
  ls -lR "$FRONTEND_DIR/_unpack" | head -n 30
  exit 1
fi
sudo rm -rf "$FRONTEND_DIR/_unpack" "$FRONTEND_DIR/dist.bak" 2>/dev/null || true
[ -d "$FRONTEND_DIR/dist" ] && sudo mv "$FRONTEND_DIR/dist" "$FRONTEND_DIR/dist.bak"
sudo mv "$FRONTEND_DIR/dist.new" "$FRONTEND_DIR/dist"
sudo chown -R www-data:www-data "$FRONTEND_DIR/dist"
sudo chmod -R a+rX "$FRONTEND_DIR/dist"
test -f "$FRONTEND_DIR/dist/index.html" && echo "  index.html OK ($(stat -c %s "$FRONTEND_DIR/dist/index.html") bytes)"

echo "== 4. 重启 lexai-backend =="
sudo systemctl restart lexai-backend
sleep 6

echo "== 5. 健康检查 =="
for i in 1 2 3 4 5; do
  CODE=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8081/api/system/health || echo 000)
  echo "  backend health attempt $i -> HTTP $CODE"
  if [ "$CODE" = "200" ]; then break; fi
  sleep 3
done
WEB=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1/ || echo 000)
echo "  frontend / -> HTTP $WEB"
WEB_API=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1/api/system/health || echo 000)
echo "  nginx /api/system/health -> HTTP $WEB_API"

echo
echo "== 6. service status =="
sudo systemctl --no-pager --full status lexai-backend | head -n 14
echo
echo "== 7. tail logs =="
sudo journalctl -u lexai-backend -n 20 --no-pager | tail -n 18

echo
echo "DONE @ $TS"
