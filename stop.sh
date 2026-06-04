#!/usr/bin/env bash
# ============================================================
#  LexAI 一键停止（macOS / Linux）
#  用法：./stop.sh
# ============================================================

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_DIR="${SCRIPT_DIR}/.lexai"

log_ok()   { echo -e "  ${GREEN}[OK]${NC} $*"; }
log_warn() { echo -e "  ${YELLOW}[i]${NC} $*"; }

kill_pid() {
    local pid="$1"
    [ -z "$pid" ] && return 1
    kill "$pid" 2>/dev/null || return 1
    sleep 1
    kill -0 "$pid" 2>/dev/null && kill -9 "$pid" 2>/dev/null
}

kill_port() {
    local port="$1"
    local label="$2"
    local pids
    pids="$(lsof -ti ":${port}" 2>/dev/null || true)"
    if [ -z "$pids" ]; then
        log_warn "${label} 未在运行 (端口 ${port})"
        return 0
    fi
    echo "  关闭 ${label} (端口 ${port}, PID: ${pids//$'\n'/ })"
    # shellcheck disable=SC2086
    kill $pids 2>/dev/null || true
    sleep 1
    pids="$(lsof -ti ":${port}" 2>/dev/null || true)"
    if [ -n "$pids" ]; then
        # shellcheck disable=SC2086
        kill -9 $pids 2>/dev/null || true
    fi
    log_ok "${label} 已停止"
}

echo ""
echo -e "${YELLOW}正在停止 LexAI 服务...${NC}"
echo ""

# 先按 PID 文件停止（Maven 子进程可能仍占用端口）
if [ -f "${RUN_DIR}/backend.pid" ]; then
    kill_pid "$(cat "${RUN_DIR}/backend.pid")" && log_ok "后端进程已停止 (PID 文件)" || true
fi
if [ -f "${RUN_DIR}/frontend.pid" ]; then
    kill_pid "$(cat "${RUN_DIR}/frontend.pid")" && log_ok "前端进程已停止 (PID 文件)" || true
fi

# 按端口兜底清理
kill_port 8081 "后端"
kill_port 5173 "前端"
kill_port 5174 "前端 (备用端口)"

# 清理 Maven / Vite 残留
pkill -f "spring-boot:run.*lexai-backend" 2>/dev/null || true
pkill -f "vite.*${SCRIPT_DIR}/frontend" 2>/dev/null || true

rm -f "${RUN_DIR}/backend.pid" "${RUN_DIR}/frontend.pid"

echo ""
log_ok "LexAI 服务已完全停止"
echo ""
