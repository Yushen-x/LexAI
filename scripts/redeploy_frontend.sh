#!/usr/bin/env bash
set -euo pipefail

FRONTEND_DIR="/opt/lexai/frontend"
TARBALL="/tmp/dist.tgz"

sudo mkdir -p "$FRONTEND_DIR"
sudo rm -rf "$FRONTEND_DIR/dist.new"
sudo mkdir -p "$FRONTEND_DIR/dist.new"
sudo tar -xzf "$TARBALL" -C "$FRONTEND_DIR/dist.new"

# Tarball was created with `tar -C dist .`, so contents are directly under dist.new/
if [[ -d "$FRONTEND_DIR/dist.new/dist" ]] && [[ ! -f "$FRONTEND_DIR/dist.new/index.html" ]]; then
  sudo mv "$FRONTEND_DIR/dist.new/dist"/* "$FRONTEND_DIR/dist.new/"
  sudo rm -rf "$FRONTEND_DIR/dist.new/dist"
fi

if [[ ! -f "$FRONTEND_DIR/dist.new/index.html" ]]; then
  echo "ERROR: index.html missing after extract"
  ls -la "$FRONTEND_DIR/dist.new"
  exit 1
fi

sudo rm -rf "$FRONTEND_DIR/dist.bak"
if [[ -d "$FRONTEND_DIR/dist" ]]; then
  sudo mv "$FRONTEND_DIR/dist" "$FRONTEND_DIR/dist.bak"
fi
sudo mv "$FRONTEND_DIR/dist.new" "$FRONTEND_DIR/dist"
sudo chmod -R a+rX "$FRONTEND_DIR/dist"

echo "==> reload nginx"
sudo nginx -t
sudo systemctl reload nginx

echo "==> verify"
ls "$FRONTEND_DIR/dist" | head
curl -sS -o /dev/null -w "frontend http_code=%{http_code}\n" http://127.0.0.1/
