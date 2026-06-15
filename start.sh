#!/usr/bin/env bash
# ============================================================
#  LexAI 一键启动（macOS / Linux）
#  用法：
#    ./start.sh              # 后台启动，完成后退出
#    ./start.sh --foreground # 前台运行，Ctrl+C 停止
#    ./start.sh -f
# ============================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="${SCRIPT_DIR}/backend"
FRONTEND_DIR="${SCRIPT_DIR}/frontend"
RUN_DIR="${SCRIPT_DIR}/.lexai"
BACKEND_PORT=8081
FRONTEND_PORT=5173
ENV_FILE="${BACKEND_DIR}/.env"
ENV_EXAMPLE="${BACKEND_DIR}/.env.example"
FOREGROUND=0
BACKEND_PID=""
FRONTEND_PID=""

if [ "${1:-}" = "--foreground" ] || [ "${1:-}" = "-f" ]; then
    FOREGROUND=1
fi

log_ok()   { echo -e "  ${GREEN}[OK]${NC} $*"; }
log_warn() { echo -e "  ${YELLOW}[i]${NC} $*"; }
log_err()  { echo -e "  ${RED}[X]${NC} $*"; }

port_in_use() {
    lsof -ti ":$1" >/dev/null 2>&1
}

resolve_java_home() {
    local candidates=(
        "/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
        "/usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
    )

    for home in "${candidates[@]}"; do
        if [ -x "${home}/bin/java" ]; then
            export JAVA_HOME="$home"
            export PATH="${JAVA_HOME}/bin:${PATH}"
            return 0
        fi
    done

    if command -v /usr/libexec/java_home >/dev/null 2>&1; then
        local detected
        detected="$(/usr/libexec/java_home -v 21 2>/dev/null || true)"
        if [ -n "$detected" ] && [ -x "${detected}/bin/java" ]; then
            export JAVA_HOME="$detected"
            export PATH="${JAVA_HOME}/bin:${PATH}"
            return 0
        fi
    fi

    if command -v java >/dev/null 2>&1 && java -version 2>&1 | grep -q 'version "21'; then
        return 0
    fi

    return 1
}

mysql_available() {
    local host="${MYSQL_HOST:-127.0.0.1}"
    local port="${MYSQL_PORT:-3306}"
    if command -v nc >/dev/null 2>&1; then
        nc -z "$host" "$port" >/dev/null 2>&1
        return $?
    fi
    (echo >/dev/tcp/"$host"/"$port") >/dev/null 2>&1
}

save_pid() {
    mkdir -p "$RUN_DIR"
    echo "$1" > "${RUN_DIR}/$2.pid"
}

cleanup_foreground() {
    echo ""
    log_warn "Stopping LexAI services..."
    [ -n "$BACKEND_PID" ] && kill "$BACKEND_PID" 2>/dev/null || true
    [ -n "$FRONTEND_PID" ] && kill "$FRONTEND_PID" 2>/dev/null || true
    rm -f "${RUN_DIR}/backend.pid" "${RUN_DIR}/frontend.pid"
    log_ok "LexAI stopped."
    exit 0
}

if [ "$FOREGROUND" -eq 1 ]; then
    trap cleanup_foreground SIGINT SIGTERM
fi

echo ""
echo -e "${CYAN}${BOLD}LexAI - 启动脚本${NC}"
echo ""

# ---------- 已在运行则跳过 ----------
if port_in_use "$BACKEND_PORT" && port_in_use "$FRONTEND_PORT"; then
    log_warn "服务似乎已在运行："
    echo "  Frontend: http://localhost:${FRONTEND_PORT}"
    echo "  Backend:  http://localhost:${BACKEND_PORT}/api"
    echo ""
    echo "如需重启，请先执行: ./stop.sh"
    exit 0
fi

if port_in_use "$BACKEND_PORT"; then
    log_err "端口 ${BACKEND_PORT} 已被占用，请先执行 ./stop.sh"
    exit 1
fi

if port_in_use "$FRONTEND_PORT"; then
    log_err "端口 ${FRONTEND_PORT} 已被占用，请先执行 ./stop.sh"
    exit 1
fi

# ---------- 环境检查 ----------
echo -e "${BOLD}[1/5] 检查运行环境${NC}"

if ! resolve_java_home; then
    log_err "未找到 Java 21。请安装后重试："
    echo "      macOS:  brew install openjdk@21"
    echo "      Ubuntu: sudo apt install openjdk-21-jdk"
    exit 1
fi
log_ok "Java: $(java -version 2>&1 | head -n1)"

if ! command -v node >/dev/null 2>&1 || ! command -v npm >/dev/null 2>&1; then
    log_err "未找到 Node.js / npm，请先安装 Node.js"
    exit 1
fi
log_ok "Node.js $(node -v), npm $(npm -v)"
echo ""

# ---------- .env ----------
echo -e "${BOLD}[2/5] 加载配置${NC}"

if [ ! -f "$ENV_FILE" ] && [ -f "$ENV_EXAMPLE" ]; then
    cp "$ENV_EXAMPLE" "$ENV_FILE"
    log_warn "已从 .env.example 创建 backend/.env"
fi

if [ -f "$ENV_FILE" ]; then
    set -a
    # shellcheck disable=SC1090
    source "$ENV_FILE"
    set +a
    log_ok "已加载 backend/.env"
else
    log_warn "未找到 backend/.env，使用默认配置"
fi
echo ""

# ---------- 启动参数 ----------
echo -e "${BOLD}[3/5] 确定启动模式${NC}"

SPRING_ARGS=()
DB_MODE="MySQL"

if mysql_available; then
    log_ok "检测到 MySQL (${MYSQL_HOST:-127.0.0.1}:${MYSQL_PORT:-3306})"
else
    SPRING_ARGS+=(--spring.profiles.active=local)
    DB_MODE="H2 内存库（local profile，无需 MySQL）"
    log_warn "MySQL 未运行，自动切换为 H2 内存库模式"
fi

if [ -z "${TENCENT_LLM_API_KEY:-}" ] && [ -z "${DELI_APP_ID:-}" ]; then
    SPRING_ARGS+=(--lexai.ai.mode=mock)
    AI_MODE="Mock"
    log_warn "未配置 AI 密钥，使用 Mock 模式"
else
    AI_MODE="${LEXAI_AI_MODE:-tencent}"
    log_ok "AI 模式: ${AI_MODE}"
fi
echo ""

# ---------- 启动后端 ----------
echo -e "${BOLD}[4/5] 启动后端 (端口 ${BACKEND_PORT})${NC}"

cd "$BACKEND_DIR"
chmod +x mvnw

BOOT_ARG=""
if [ "${#SPRING_ARGS[@]}" -gt 0 ]; then
    joined="$(IFS=,; echo "${SPRING_ARGS[*]}")"
    BOOT_ARG="-Dspring-boot.run.arguments=${joined}"
fi

./mvnw spring-boot:run $BOOT_ARG \
    > "${BACKEND_DIR}/backend.out.log" 2>"${BACKEND_DIR}/backend.err.log" &
BACKEND_PID=$!
save_pid "$BACKEND_PID" backend
cd "$SCRIPT_DIR"

echo -ne "  等待后端就绪"
BACKEND_READY=0
for _ in $(seq 1 90); do
    sleep 2
    if ! kill -0 "$BACKEND_PID" 2>/dev/null; then
        echo ""
        log_err "后端启动失败，请查看日志："
        echo "      ${BACKEND_DIR}/backend.err.log"
        echo "      ${BACKEND_DIR}/backend.out.log"
        rm -f "${RUN_DIR}/backend.pid"
        exit 1
    fi
    HTTP_CODE="$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${BACKEND_PORT}/api/actuator/health" 2>/dev/null || echo "000")"
    if [ "$HTTP_CODE" = "200" ]; then
        BACKEND_READY=1
        echo ""
        log_ok "后端已就绪"
        break
    fi
    echo -n "."
done

if [ "$BACKEND_READY" -eq 0 ]; then
    echo ""
    log_err "后端启动超时，请查看 ${BACKEND_DIR}/backend.out.log"
    exit 1
fi
echo ""

# ---------- 启动前端 ----------
echo -e "${BOLD}[5/5] 启动前端 (端口 ${FRONTEND_PORT})${NC}"

if [ ! -d "${FRONTEND_DIR}/node_modules" ]; then
    log_warn "首次运行，正在安装前端依赖..."
    (cd "$FRONTEND_DIR" && npm install)
    log_ok "前端依赖安装完成"
fi

cd "$FRONTEND_DIR"
npm run dev > "${FRONTEND_DIR}/frontend.out.log" 2>"${FRONTEND_DIR}/frontend.err.log" &
FRONTEND_PID=$!
save_pid "$FRONTEND_PID" frontend
cd "$SCRIPT_DIR"

sleep 3

FRONTEND_URL="http://localhost:${FRONTEND_PORT}"
if ! curl -s -o /dev/null "http://localhost:${FRONTEND_PORT}/" 2>/dev/null; then
    if curl -s -o /dev/null "http://localhost:5174/" 2>/dev/null; then
        FRONTEND_URL="http://localhost:5174"
        log_warn "5173 被占用，前端运行在 5174"
    fi
fi

echo ""
echo -e "${CYAN}${BOLD}LexAI 已启动${NC}"
echo "  Frontend : ${FRONTEND_URL}"
echo "  Backend  : http://localhost:${BACKEND_PORT}/api"
echo "  Database : ${DB_MODE}"
echo "  AI Mode  : ${AI_MODE}"
echo ""
echo "  日志："
echo "    backend  -> ${BACKEND_DIR}/backend.out.log"
echo "    frontend -> ${FRONTEND_DIR}/frontend.out.log"
echo ""
echo "  停止服务：./stop.sh"

if command -v open >/dev/null 2>&1; then
    open "$FRONTEND_URL"
elif command -v xdg-open >/dev/null 2>&1; then
    xdg-open "$FRONTEND_URL"
fi

if [ "$FOREGROUND" -eq 1 ]; then
    echo ""
    log_warn "前台模式：按 Ctrl+C 停止所有服务"
    wait
else
    echo ""
fi
