#!/usr/bin/env bash
# ============================================================
#  LexAI 一键启动脚本（macOS / Linux 版）
#  用法：chmod +x start.sh && ./start.sh
# ============================================================

set -e

# ---------- Colors ----------
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

# ---------- Constants ----------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="${SCRIPT_DIR}/backend"
FRONTEND_DIR="${SCRIPT_DIR}/frontend"
BACKEND_PORT=8081
FRONTEND_PORT=5173
ENV_FILE="${BACKEND_DIR}/.env"
ENV_EXAMPLE="${BACKEND_DIR}/.env.example"
HAS_ERROR=0
BACKEND_PID=""
FRONTEND_PID=""

# ---------- Cleanup on exit ----------
cleanup() {
    echo ""
    echo -e "${YELLOW}[i] Stopping LexAI services...${NC}"
    if [ -n "$BACKEND_PID" ] && kill -0 "$BACKEND_PID" 2>/dev/null; then
        kill "$BACKEND_PID" 2>/dev/null
        echo -e "${GREEN}[OK] Backend stopped.${NC}"
    fi
    if [ -n "$FRONTEND_PID" ] && kill -0 "$FRONTEND_PID" 2>/dev/null; then
        kill "$FRONTEND_PID" 2>/dev/null
        echo -e "${GREEN}[OK] Frontend stopped.${NC}"
    fi
    echo -e "${GREEN}[OK] LexAI fully stopped.${NC}"
    exit 0
}

trap cleanup SIGINT SIGTERM

# ---------- Title ----------
echo ""
echo -e "${CYAN}${BOLD}"
echo "  +===============================================+"
echo "  |           LexAI - Smart Legal Workbench       |"
echo "  |           Startup Script (macOS/Linux)        |"
echo "  +===============================================+"
echo -e "${NC}"

# ============================================================
# Step 1: Environment Check
# ============================================================
echo -e "${BOLD}[1/5] Checking environment...${NC}"
echo ""

# --- Java ---
if ! command -v java &>/dev/null; then
    echo -e "  ${RED}[X] Java not found. Please install Java 21${NC}"
    echo "      macOS:  brew install openjdk@21"
    echo "      Ubuntu: sudo apt install openjdk-21-jdk"
    echo "      Download: https://adoptium.net/"
    HAS_ERROR=1
else
    JAVA_VER=$(java -version 2>&1 | head -n1)
    echo -e "  ${GREEN}[OK] $JAVA_VER${NC}"
fi

# --- Maven ---
echo -e "  ${GREEN}[OK] Maven: Using bundled Maven Wrapper (mvnw)${NC}"

# --- Node.js ---
if ! command -v node &>/dev/null; then
    echo -e "  ${RED}[X] Node.js not found.${NC}"
    echo "      macOS:  brew install node"
    echo "      Ubuntu: sudo apt install nodejs npm"
    echo "      Download: https://nodejs.org/"
    HAS_ERROR=1
else
    NODE_VER=$(node -v)
    echo -e "  ${GREEN}[OK] Node.js $NODE_VER${NC}"
fi

# --- npm ---
if ! command -v npm &>/dev/null; then
    echo -e "  ${RED}[X] npm not found.${NC}"
    HAS_ERROR=1
else
    NPM_VER=$(npm -v)
    echo -e "  ${GREEN}[OK] npm $NPM_VER${NC}"
fi

echo ""

if [ "$HAS_ERROR" -eq 1 ]; then
    echo -e "  ${RED}WARNING: Missing dependencies. Please install the tools above and retry.${NC}"
    echo ""
    exit 1
fi

echo -e "  ${GREEN}All checks passed!${NC}"
echo ""

# ============================================================
# Step 2: Configure .env file
# ============================================================
echo -e "${BOLD}[2/5] Checking backend .env config...${NC}"

if [ ! -f "$ENV_FILE" ]; then
    if [ -f "$ENV_EXAMPLE" ]; then
        cp "$ENV_EXAMPLE" "$ENV_FILE"
        echo -e "  ${YELLOW}[i] Copied .env.example -> backend/.env${NC}"
        echo "      Edit backend/.env to add API keys for real AI features."
        echo "      Without keys, Mock mode will be used."
    else
        echo -e "  ${YELLOW}[i] No .env.example found. Starting with defaults (Mock mode).${NC}"
    fi
else
    echo -e "  ${GREEN}[OK] Found existing backend/.env${NC}"
fi
echo ""

# ============================================================
# Step 3: Load .env environment variables
# ============================================================
echo -e "${BOLD}[3/5] Loading environment variables...${NC}"

if [ -f "$ENV_FILE" ]; then
    set -a
    # shellcheck disable=SC1090
    source "$ENV_FILE"
    set +a
    echo -e "  ${GREEN}[OK] Environment variables loaded.${NC}"
else
    echo -e "  ${YELLOW}[i] No .env file. Using defaults.${NC}"
fi
echo ""

# ============================================================
# Step 4: Start Backend
# ============================================================
echo -e "${BOLD}[4/5] Starting backend (Spring Boot) on port $BACKEND_PORT...${NC}"
echo ""

# Determine startup mode
BOOT_ARGS=""
if [ -z "$TENCENT_LLM_API_KEY" ] && [ -z "$DELI_APP_ID" ]; then
    echo -e "  ${YELLOW}[i] No API keys detected. Starting in Mock mode.${NC}"
    BOOT_ARGS='-Dspring-boot.run.arguments=--lexai.ai.mode=mock'
fi

# Start backend in background
cd "$BACKEND_DIR"
chmod +x mvnw
./mvnw spring-boot:run $BOOT_ARGS > "${BACKEND_DIR}/backend.out.log" 2>"${BACKEND_DIR}/backend.err.log" &
BACKEND_PID=$!
cd "$SCRIPT_DIR"

# Wait for backend
echo -ne "  ${YELLOW}[i] Waiting for backend to be ready${NC}"
RETRY=0
BACKEND_READY=0
while [ $RETRY -lt 60 ]; do
    sleep 2
    RETRY=$((RETRY + 1))

    # Check if process is still alive
    if ! kill -0 "$BACKEND_PID" 2>/dev/null; then
        echo ""
        echo -e "  ${RED}[X] Backend process exited. Check logs:${NC}"
        echo "      ${BACKEND_DIR}/backend.err.log"
        echo "      ${BACKEND_DIR}/backend.out.log"
        break
    fi

    # Health check
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${BACKEND_PORT}/api/actuator/health" 2>/dev/null || echo "000")
    if [ "$HTTP_CODE" = "200" ]; then
        BACKEND_READY=1
        echo ""
        echo -e "  ${GREEN}[OK] Backend is up and running!${NC}"
        break
    fi
    echo -n "."
done

if [ $RETRY -ge 60 ] && [ $BACKEND_READY -eq 0 ]; then
    echo ""
    echo -e "  ${RED}[!] Backend startup timed out. Check logs.${NC}"
fi
echo ""

# ============================================================
# Step 5: Start Frontend
# ============================================================
echo -e "${BOLD}[5/5] Starting frontend (Vite + Vue 3) on port $FRONTEND_PORT...${NC}"
echo ""

# Check node_modules
if [ ! -d "${FRONTEND_DIR}/node_modules" ]; then
    echo -e "  ${YELLOW}[i] First launch. Installing frontend dependencies...${NC}"
    cd "$FRONTEND_DIR"
    npm install
    cd "$SCRIPT_DIR"
    echo -e "  ${GREEN}[OK] Frontend dependencies installed.${NC}"
fi

# Start frontend in background
cd "$FRONTEND_DIR"
npm run dev > "${FRONTEND_DIR}/frontend.out.log" 2>"${FRONTEND_DIR}/frontend.err.log" &
FRONTEND_PID=$!
cd "$SCRIPT_DIR"

sleep 5

# ============================================================
# Done
# ============================================================
echo ""
echo -e "${CYAN}${BOLD}"
echo "  +===============================================+"
echo "  |        LexAI started successfully!            |"
echo "  +-----------------------------------------------+"
echo "  |                                               |"
echo "  |  Frontend:  http://localhost:${FRONTEND_PORT}            |"
echo "  |  Backend:   http://localhost:${BACKEND_PORT}/api         |"
echo "  |  H2 Console:http://localhost:${BACKEND_PORT}/api/h2-console |"
echo "  |                                               |"
echo "  |  Press Ctrl+C to stop all services            |"
echo "  |                                               |"
echo "  +===============================================+"
echo -e "${NC}"

# Open browser
if command -v open &>/dev/null; then
    open "http://localhost:${FRONTEND_PORT}"
elif command -v xdg-open &>/dev/null; then
    xdg-open "http://localhost:${FRONTEND_PORT}"
fi

echo -e "${YELLOW}Logs:${NC}"
echo "  Backend: ${BACKEND_DIR}/backend.out.log"
echo "  Frontend: ${FRONTEND_DIR}/frontend.out.log"
echo ""
echo -e "${YELLOW}Press Ctrl+C to stop all services.${NC}"

# Keep running, wait for Ctrl+C
wait
