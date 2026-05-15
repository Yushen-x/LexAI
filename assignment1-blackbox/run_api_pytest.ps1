$ErrorActionPreference = 'Stop'

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = if ($env:LEXAI_REPO_ROOT) { $env:LEXAI_REPO_ROOT } else { 'D:\Desktop\LexAI' }
$backendDir = Join-Path $repoRoot 'backend'
$outLog = Join-Path $backendDir 'pytest-backend.out.log'
$errLog = Join-Path $backendDir 'pytest-backend.err.log'
$port = 18081
$baseUrl = "http://localhost:$port/api"
$healthUrl = "$baseUrl/system/health"
$mavenWrapperRoot = 'C:\Users\xxy\.m2\wrapper\dists\apache-maven-3.9.11'
$mavenRepoLocal = 'C:\Users\xxy\.m2\repository'

$mavenCmd = $null
if (Test-Path -LiteralPath $mavenWrapperRoot) {
    $mavenCmd = Get-ChildItem -LiteralPath $mavenWrapperRoot -Recurse -Filter 'mvn.cmd' -ErrorAction SilentlyContinue |
        Select-Object -First 1 -ExpandProperty FullName
}
if (-not $mavenCmd) {
    $mavenCmd = Join-Path $backendDir 'mvnw.cmd'
}

if (-not (Test-Path -LiteralPath $backendDir)) {
    throw "LexAI backend directory not found: $backendDir. Set LEXAI_REPO_ROOT if the project moved."
}

if (Test-Path -LiteralPath $outLog) {
    Remove-Item -LiteralPath $outLog -Force
}
if (Test-Path -LiteralPath $errLog) {
    Remove-Item -LiteralPath $errLog -Force
}

$env:LEXAI_AI_MODE = 'mock'
$env:SPRING_DATASOURCE_URL = 'jdbc:h2:mem:lexai-pytest;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH'

$proc = Start-Process -FilePath $mavenCmd `
    -ArgumentList '-q', "-Dmaven.repo.local=$mavenRepoLocal", '-DskipTests', 'spring-boot:run', "-Dspring-boot.run.arguments=--server.port=$port" `
    -WorkingDirectory $backendDir `
    -RedirectStandardOutput $outLog `
    -RedirectStandardError $errLog `
    -PassThru

try {
    $ready = $false
    for ($i = 0; $i -lt 60; $i++) {
        Start-Sleep -Seconds 2
        if ($proc.HasExited) {
            $stderr = if (Test-Path -LiteralPath $errLog) { Get-Content -LiteralPath $errLog -Tail 50 | Out-String } else { '' }
            $stdout = if (Test-Path -LiteralPath $outLog) { Get-Content -LiteralPath $outLog -Tail 50 | Out-String } else { '' }
            throw "Backend process exited early. stdout:`n$stdout`nstderr:`n$stderr"
        }
        try {
            $resp = Invoke-WebRequest -UseBasicParsing $healthUrl
            if ($resp.StatusCode -eq 200) {
                $ready = $true
                break
            }
        } catch {
        }
    }

    if (-not $ready) {
        throw "Backend did not become ready at $healthUrl. Check $outLog and $errLog."
    }

    $env:LEXAI_BASE_URL = $baseUrl
    python -m pytest (Join-Path $repoRoot 'assignment1-blackbox\pytest_api') -q
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}
finally {
    if ($proc -and -not $proc.HasExited) {
        Stop-Process -Id $proc.Id -Force
    }
}
