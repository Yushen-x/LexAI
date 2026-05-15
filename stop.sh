#!/usr/bin/env bash
# ============================================================
#  LexAI 停止脚本（macOS / Linux 版）
#  关闭占用 8081 / 5173 端口的进程
# ============================================================

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo ""
echo -e "${YELLOW}正在停止 LexAI 服务...${NC}"
echo ""

# 停止后端 (8081)
BACKEND_PID=$(lsof -ti :8081 2>/dev/null)
if [ -n "$BACKEND_PID" ]; then
    echo -e "  [i] 正在关闭后端进程 PID: $BACKEND_PID"
    kill $BACKEND_PID 2>/dev/null
    sleep 1
    # 如果还在运行则强制终止
    kill -0 $BACKEND_PID 2>/dev/null && kill -9 $BACKEND_PID 2>/dev/null
    echo -e "  ${GREEN}[✓] 后端服务已停止${NC}"
else
    echo -e "  [i] 后端服务未在运行"
fi

# 停止前端 (5173)
FRONTEND_PID=$(lsof -ti :5173 2>/dev/null)
if [ -n "$FRONTEND_PID" ]; then
    echo -e "  [i] 正在关闭前端进程 PID: $FRONTEND_PID"
    kill $FRONTEND_PID 2>/dev/null
    echo -e "  ${GREEN}[✓] 前端服务已停止${NC}"
else
    echo -e "  [i] 前端服务未在运行"
fi

echo ""
echo -e "${GREEN}[✓] LexAI 服务已完全停止${NC}"
echo ""
