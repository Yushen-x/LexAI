@echo off
chcp 65001 >nul 2>&1
setlocal EnableDelayedExpansion

REM ============================================================
REM  LexAI 一键启动脚本（Windows BAT 版）
REM  用法：双击 start.bat，或在项目根目录运行 .\start.bat
REM ============================================================

title LexAI Launcher

REM ---------- 常量 ----------
set "ROOT=%~dp0"
set "BACKEND_DIR=%ROOT%backend"
set "FRONTEND_DIR=%ROOT%frontend"
set "BACKEND_PORT=8081"
set "FRONTEND_PORT=5173"
set "ENV_FILE=%BACKEND_DIR%\.env"
set "ENV_EXAMPLE=%BACKEND_DIR%\.env.example"
set "HAS_ERROR=0"

REM ---------- 标题 ----------
echo.
echo  +===============================================+
echo  ^|           LexAI - Smart Legal Workbench       ^|
echo  ^|           Startup Script (Windows BAT)        ^|
echo  +===============================================+
echo.

REM ============================================================
REM Step 1: Environment Check
REM ============================================================
echo [1/5] Checking environment...
echo.

REM --- Java ---
where java >nul 2>&1
if !ERRORLEVEL! neq 0 (
    echo  [X] Java not found. Please install Java 21
    echo      Download: https://adoptium.net/
    set "HAS_ERROR=1"
) else (
    for /f "tokens=*" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do (
        echo  [OK] %%v
    )
)

REM --- Maven ---
echo  [OK] Maven: Using bundled Maven Wrapper (mvnw)

REM --- Node.js ---
where node >nul 2>&1
if !ERRORLEVEL! neq 0 (
    echo  [X] Node.js not found.
    echo      Download: https://nodejs.org/
    set "HAS_ERROR=1"
) else (
    for /f %%v in ('node -v') do echo  [OK] Node.js %%v
)

REM --- npm ---
where npm >nul 2>&1
if !ERRORLEVEL! neq 0 (
    echo  [X] npm not found.
    set "HAS_ERROR=1"
) else (
    for /f %%v in ('npm -v') do echo  [OK] npm %%v
)

echo.

if "!HAS_ERROR!"=="1" (
    echo  WARNING: Missing dependencies. Please install the tools above and retry.
    echo.
    pause
    exit /b 1
)

echo  All checks passed!
echo.

REM ============================================================
REM Step 2: Configure .env file
REM ============================================================
echo [2/5] Checking backend .env config...

if not exist "%ENV_FILE%" (
    if exist "%ENV_EXAMPLE%" (
        copy "%ENV_EXAMPLE%" "%ENV_FILE%" >nul
        echo  [i] Copied .env.example to backend\.env
        echo      Edit backend\.env to add API keys for real AI features.
    ) else (
        echo  [i] No .env.example found. Starting with defaults.
    )
) else (
    echo  [OK] Found existing backend\.env
)
echo.

REM ============================================================
REM Step 3: Load .env environment variables
REM ============================================================
echo [3/5] Loading environment variables...

if exist "%ENV_FILE%" (
    for /f "usebackq tokens=*" %%L in ("%ENV_FILE%") do (
        call :parse_env_line "%%L"
    )
    echo  [OK] Environment variables loaded.
) else (
    echo  [i] No .env file. Using defaults.
)
echo.

REM ============================================================
REM Step 4: Start Backend
REM ============================================================
echo [4/5] Starting backend (Spring Boot) on port %BACKEND_PORT%...
echo.

set "BOOT_ARGS="
if "!TENCENT_LLM_API_KEY!"=="" (
    if "!DELI_APP_ID!"=="" (
        echo  [i] No API keys detected. Starting in Mock mode.
        set "BOOT_ARGS=-Dspring-boot.run.arguments=--lexai.ai.mode=mock"
    )
)

start "LexAI Backend" /D "%BACKEND_DIR%" cmd /k "mvnw.cmd spring-boot:run !BOOT_ARGS!"

echo  [i] Waiting for backend to start...
set "RETRY=0"
:wait_backend
if !RETRY! geq 60 (
    echo.
    echo  [!] Backend startup timed out. Check the backend window.
    goto start_frontend
)
timeout /t 2 /nobreak >nul
set /a RETRY+=1

where curl >nul 2>&1
if !ERRORLEVEL! equ 0 (
    curl -s -o nul -w "%%{http_code}" http://localhost:%BACKEND_PORT%/api/actuator/health 2>nul | findstr "200" >nul
    if !ERRORLEVEL! equ 0 (
        echo.
        echo  [OK] Backend is up and running!
        echo.
        goto start_frontend
    )
)
<nul set /p="."
goto wait_backend

REM ============================================================
REM Step 5: Start Frontend
REM ============================================================
:start_frontend
echo [5/5] Starting frontend (Vite + Vue 3) on port %FRONTEND_PORT%...
echo.

if not exist "%FRONTEND_DIR%\node_modules" (
    echo  [i] First launch. Installing frontend dependencies...
    start "LexAI npm install" /wait /D "%FRONTEND_DIR%" cmd /c "npm install"
    echo  [OK] Frontend dependencies installed.
)

start "LexAI Frontend" /D "%FRONTEND_DIR%" cmd /k "npm run dev"

echo  [i] Waiting for frontend to start...
timeout /t 5 /nobreak >nul
echo.

REM ============================================================
REM Done
REM ============================================================
echo.
echo  +===============================================+
echo  ^|        LexAI started successfully!            ^|
echo  +-----------------------------------------------+
echo  ^|                                               ^|
echo  ^|  Frontend:  http://localhost:%FRONTEND_PORT%            ^|
echo  ^|  Backend:   http://localhost:%BACKEND_PORT%/api         ^|
echo  ^|  H2 Console:http://localhost:%BACKEND_PORT%/api/h2-console ^|
echo  ^|                                               ^|
echo  ^|  To stop: close the Backend/Frontend windows  ^|
echo  ^|                                               ^|
echo  +===============================================+
echo.

timeout /t 3 /nobreak >nul
start "" "http://localhost:%FRONTEND_PORT%"

echo  Press any key to close this launcher (backend and frontend will keep running)...
pause >nul
exit /b 0

REM ============================================================
REM Subroutine: Parse a single .env line
REM ============================================================
:parse_env_line
set "ELINE=%~1"
REM Skip empty lines
if "!ELINE!"=="" goto :eof
REM Skip comments
if "!ELINE:~0,1!"=="#" goto :eof
REM Remove 'export ' prefix
if "!ELINE:~0,7!"=="export " set "ELINE=!ELINE:~7!"
REM Find the = sign and split
for /f "tokens=1,* delims==" %%A in ("!ELINE!") do (
    set "EKEY=%%A"
    set "EVAL=%%B"
)
REM Trim key
for /f "tokens=*" %%K in ("!EKEY!") do set "EKEY=%%K"
REM Remove single quotes from value
set "EVAL=!EVAL:'=!"
REM Skip if key or val is empty
if "!EKEY!"=="" goto :eof
if "!EVAL!"=="" goto :eof
REM Set the environment variable
set "!EKEY!=!EVAL!"
goto :eof
