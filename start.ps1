# ============================================================
#  LexAI 一键启动脚本（PowerShell 版）
#  用法：在项目根目录运行 .\start.ps1
#  注意：若提示脚本执行策略问题，请先执行：
#        Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned
# ============================================================

$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

# ---------- 常量 ----------
$ROOT = $PSScriptRoot
if (-not $ROOT) { $ROOT = Split-Path -Parent $MyInvocation.MyCommand.Path }
$BACKEND_DIR = Join-Path $ROOT "backend"
$FRONTEND_DIR = Join-Path $ROOT "frontend"
$BACKEND_PORT = 8081
$FRONTEND_PORT = 5173
$ENV_FILE = Join-Path $BACKEND_DIR ".env"
$ENV_EXAMPLE = Join-Path $BACKEND_DIR ".env.example"
$HAS_ERROR = $false

# ---------- 标题 ----------
Write-Host ""
Write-Host "  +===============================================+" -ForegroundColor Cyan
Write-Host "  |           LexAI - Smart Legal Workbench       |" -ForegroundColor Cyan
Write-Host "  |           Startup Script (PowerShell)         |" -ForegroundColor Cyan
Write-Host "  +===============================================+" -ForegroundColor Cyan
Write-Host ""

# ============================================================
# Step 1: Environment Check
# ============================================================
Write-Host "[1/5] Checking environment..." -ForegroundColor White
Write-Host ""

# --- Java ---
$javaCmd = Get-Command java -ErrorAction SilentlyContinue
if (-not $javaCmd) {
    Write-Host "  [X] Java not found. Please install Java 21" -ForegroundColor Red
    Write-Host "      Download: https://adoptium.net/"
    $HAS_ERROR = $true
}
else {
    $javaVerOutput = & java -version 2>&1 | Select-Object -First 1
    Write-Host "  [OK] Java: $javaVerOutput" -ForegroundColor Green
}

# --- Maven ---
Write-Host "  [OK] Maven: Using bundled Maven Wrapper (mvnw)" -ForegroundColor Green

# --- Node.js ---
$nodeCmd = Get-Command node -ErrorAction SilentlyContinue
if (-not $nodeCmd) {
    Write-Host "  [X] Node.js not found." -ForegroundColor Red
    Write-Host "      Download: https://nodejs.org/"
    $HAS_ERROR = $true
}
else {
    $nodeVer = & node -v
    Write-Host "  [OK] Node.js: $nodeVer" -ForegroundColor Green
}

# --- npm ---
$npmCmd = Get-Command npm -ErrorAction SilentlyContinue
if (-not $npmCmd) {
    Write-Host "  [X] npm not found." -ForegroundColor Red
    $HAS_ERROR = $true
}
else {
    $npmVer = & npm -v
    Write-Host "  [OK] npm: $npmVer" -ForegroundColor Green
}

Write-Host ""

if ($HAS_ERROR) {
    Write-Host "  WARNING: Missing dependencies. Please install the tools above and retry." -ForegroundColor Red
    Write-Host ""
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host "  All checks passed!" -ForegroundColor Green
Write-Host ""

# ============================================================
# Step 2: Configure .env file
# ============================================================
Write-Host "[2/5] Checking backend .env config..." -ForegroundColor White

if (-not (Test-Path $ENV_FILE)) {
    if (Test-Path $ENV_EXAMPLE) {
        Copy-Item $ENV_EXAMPLE $ENV_FILE
        Write-Host "  [i] Copied .env.example -> backend/.env" -ForegroundColor Yellow
        Write-Host "      Edit backend/.env to add API keys for real AI features."
        Write-Host "      Without keys, Mock mode will be used."
    }
    else {
        Write-Host "  [i] No .env.example found. Starting with defaults (Mock mode)." -ForegroundColor Yellow
    }
}
else {
    Write-Host "  [OK] Found existing backend/.env" -ForegroundColor Green
}
Write-Host ""

# ============================================================
# Step 3: Load .env environment variables
# ============================================================
Write-Host "[3/5] Loading environment variables..." -ForegroundColor White

if (Test-Path $ENV_FILE) {
    $envLines = Get-Content $ENV_FILE
    foreach ($rawLine in $envLines) {
        $line = $rawLine.Trim()
        if ([string]::IsNullOrWhiteSpace($line)) { continue }
        if ($line.StartsWith('#')) { continue }

        # Remove 'export ' prefix if present
        if ($line.StartsWith('export ')) {
            $line = $line.Substring(7)
        }

        $eqIndex = $line.IndexOf('=')
        if ($eqIndex -gt 0) {
            $key = $line.Substring(0, $eqIndex).Trim()
            $val = $line.Substring($eqIndex + 1).Trim()
            # Remove surrounding quotes
            if ($val.Length -ge 2) {
                $first = $val[0]
                $last = $val[$val.Length - 1]
                if (($first -eq "'" -and $last -eq "'") -or ($first -eq '"' -and $last -eq '"')) {
                    $val = $val.Substring(1, $val.Length - 2)
                }
            }
            if ($key -and $val) {
                [System.Environment]::SetEnvironmentVariable($key, $val, 'Process')
            }
        }
    }
    Write-Host "  [OK] Environment variables loaded." -ForegroundColor Green
}
else {
    Write-Host "  [i] No .env file. Using defaults." -ForegroundColor Yellow
}
Write-Host ""

# ============================================================
# Step 4: Start Backend
# ============================================================
Write-Host "[4/5] Starting backend (Spring Boot)..." -ForegroundColor White
Write-Host "       Port: $BACKEND_PORT"
Write-Host ""

# Determine startup mode
$bootArgs = ""
$deliId = [System.Environment]::GetEnvironmentVariable('DELI_APP_ID', 'Process')
$tencentKey = [System.Environment]::GetEnvironmentVariable('TENCENT_LLM_API_KEY', 'Process')
if (-not $tencentKey -and -not $deliId) {
    Write-Host "  [i] No API keys detected. Starting backend in Mock mode." -ForegroundColor Yellow
    $bootArgs = "-Dspring-boot.run.arguments=--lexai.ai.mode=mock"
}

# Start backend in new window
$backendProcess = Start-Process -FilePath "cmd.exe" -ArgumentList "/k", ".\mvnw.cmd spring-boot:run $bootArgs" -WorkingDirectory $BACKEND_DIR -PassThru -WindowStyle Normal
Write-Host "  [i] Backend process started (PID: $($backendProcess.Id))" -ForegroundColor Yellow

# Wait for backend to start
Write-Host -NoNewline "  [i] Waiting for backend to be ready"
$retry = 0
$backendReady = $false
while ($retry -lt 60) {
    Start-Sleep -Seconds 2
    $retry++
    try {
        $healthUrl = "http://localhost:$BACKEND_PORT/api/actuator/health"
        $response = Invoke-WebRequest -Uri $healthUrl -UseBasicParsing -TimeoutSec 2 -ErrorAction Stop
        if ($response.StatusCode -eq 200) {
            $backendReady = $true
            break
        }
    }
    catch {
        # Still starting up, continue waiting
    }
    Write-Host -NoNewline "."
}
Write-Host ""

if ($backendReady) {
    Write-Host "  [OK] Backend is up and running!" -ForegroundColor Green
}
else {
    Write-Host "  [!] Backend startup timed out. Check the backend window for logs." -ForegroundColor Red
}
Write-Host ""

# ============================================================
# Step 5: Start Frontend
# ============================================================
Write-Host "[5/5] Starting frontend (Vite + Vue 3)..." -ForegroundColor White
Write-Host "       Port: $FRONTEND_PORT"
Write-Host ""

# Check node_modules
$nodeModulesPath = Join-Path $FRONTEND_DIR "node_modules"
if (-not (Test-Path $nodeModulesPath)) {
    Write-Host "  [i] First launch detected. Installing frontend dependencies..." -ForegroundColor Yellow
    Start-Process -FilePath "cmd.exe" -ArgumentList "/c", "npm install" -WorkingDirectory $FRONTEND_DIR -PassThru -Wait -WindowStyle Normal | Out-Null
    Write-Host "  [OK] Frontend dependencies installed." -ForegroundColor Green
}

# Start frontend in new window
$frontendProcess = Start-Process -FilePath "cmd.exe" -ArgumentList "/k", "npm run dev" -WorkingDirectory $FRONTEND_DIR -PassThru -WindowStyle Normal
Write-Host "  [i] Frontend process started (PID: $($frontendProcess.Id))" -ForegroundColor Yellow

Start-Sleep -Seconds 5

# ============================================================
# Done
# ============================================================
Write-Host ""
Write-Host "  +===============================================+" -ForegroundColor Cyan
Write-Host "  |        LexAI started successfully!            |" -ForegroundColor Cyan
Write-Host "  +-----------------------------------------------+" -ForegroundColor Cyan
Write-Host "  |                                               |" -ForegroundColor Cyan
Write-Host "  |  Frontend:  http://localhost:$FRONTEND_PORT            |" -ForegroundColor Cyan
Write-Host "  |  Backend:   http://localhost:$BACKEND_PORT/api         |" -ForegroundColor Cyan
Write-Host "  |  H2 Console:http://localhost:$BACKEND_PORT/api/h2-console |" -ForegroundColor Cyan
Write-Host "  |                                               |" -ForegroundColor Cyan
Write-Host "  |  To stop: close the Backend/Frontend windows  |" -ForegroundColor Cyan
Write-Host "  |                                               |" -ForegroundColor Cyan
Write-Host "  +===============================================+" -ForegroundColor Cyan
Write-Host ""

# Open browser
Start-Sleep -Seconds 2
Start-Process "http://localhost:$FRONTEND_PORT"

Write-Host "  Press any key to close this launcher (backend & frontend will keep running)..." -ForegroundColor Yellow
$null = $Host.UI.RawUI.ReadKey('NoEcho,IncludeKeyDown')
