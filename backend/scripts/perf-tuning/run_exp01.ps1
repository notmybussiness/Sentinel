#
# EXP-01: DB Indexing Performance Test Runner (Windows)
#
# Usage:
#   .\run_exp01.ps1 baseline    # Before index
#   .\run_exp01.ps1 after       # After index
#

param(
    [Parameter(Position=0)]
    [string]$Phase = "baseline"
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ResultsDir = Join-Path $ScriptDir "results"
$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"

Write-Host @"

+====================================================================+
|  EXP-01: DB Indexing Performance Test                             |
|  Phase: $Phase                                                       |
+====================================================================+

"@

# 1. Check prerequisites
Write-Host "[1/5] Checking prerequisites..."

try {
    $null = k6 version
    Write-Host "  k6: OK"
} catch {
    Write-Host "  ERROR: k6 is not installed"
    Write-Host "  Install: winget install k6"
    exit 1
}

try {
    $health = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -TimeoutSec 5
    Write-Host "  Backend: OK"
} catch {
    Write-Host "  ERROR: Backend is not running on localhost:8080"
    exit 1
}

# 2. Start monitoring stack
Write-Host ""
Write-Host "[2/5] Starting monitoring stack..."

Set-Location $ScriptDir
docker compose -f docker-compose.monitoring.yml up -d

Write-Host "  Waiting for InfluxDB..."
for ($i = 1; $i -le 30; $i++) {
    try {
        $null = Invoke-RestMethod -Uri "http://localhost:8086/ping" -TimeoutSec 1
        Write-Host "  InfluxDB: OK"
        break
    } catch {
        Start-Sleep -Seconds 1
    }
}

Write-Host "  Grafana: http://localhost:3001 (admin/admin)"

# 3. Generate tokens
Write-Host ""
Write-Host "[3/5] Generating test tokens..."

$TokensFile = Join-Path $ScriptDir "tokens.csv"
if (-not (Test-Path $TokensFile)) {
    python "$ScriptDir\generate_tokens.py" --url http://localhost:8080 --output $TokensFile
} else {
    Write-Host "  Using existing tokens.csv"
}

$TokenCount = (Get-Content $TokensFile | Measure-Object -Line).Lines - 1
Write-Host "  Tokens: $TokenCount users"

# 4. Run k6 test
Write-Host ""
Write-Host "[4/5] Running k6 test..."
Write-Host "  Output: InfluxDB (localhost:8086/k6)"
Write-Host ""

$LogFile = Join-Path $ResultsDir "exp01_${Phase}_${Timestamp}.log"

# Ensure results directory exists
if (-not (Test-Path $ResultsDir)) {
    New-Item -ItemType Directory -Path $ResultsDir | Out-Null
}

k6 run `
    --out influxdb=http://localhost:8086/k6 `
    --tag testid=exp01_${Phase}_${Timestamp} `
    "$ScriptDir\exp01_db_indexing.js" 2>&1 | Tee-Object -FilePath $LogFile

# 5. Collect results
Write-Host ""
Write-Host "[5/5] Collecting results..."

$InfluxResult = Join-Path $ResultsDir "exp01_${Phase}_${Timestamp}_influx.json"
try {
    $query = [System.Web.HttpUtility]::UrlEncode("SELECT mean(value) FROM http_reqs WHERE time > now() - 10m GROUP BY time(1m)")
    Invoke-RestMethod -Uri "http://localhost:8086/query?db=k6&q=$query" -OutFile $InfluxResult
} catch {
    Write-Host "  Warning: Could not query InfluxDB"
}

Write-Host @"

+====================================================================+
|  Test Complete!                                                    |
+====================================================================+
|  Results: $ResultsDir
|  Grafana: http://localhost:3001/d/exp01-db-indexing
|
"@

if ($Phase -eq "baseline") {
    Write-Host "|  Next Steps:"
    Write-Host "|  1. Review baseline metrics in Grafana"
    Write-Host "|  2. Apply DB indexes (see PLAN_PERF_1000TPS.md)"
    Write-Host "|  3. Run: .\run_exp01.ps1 after"
} else {
    Write-Host "|  Next Steps:"
    Write-Host "|  1. Compare 'baseline' vs 'after' in Grafana"
    Write-Host "|  2. Update PLAN_PERF_1000TPS.md with results"
}
Write-Host "+====================================================================+"
