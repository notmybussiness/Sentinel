#
# EXP-01: Start Monitoring Stack (WAS Server)
#
# Run this on the WAS server (where Spring Boot runs)
#
# Usage:
#   .\start_monitoring.ps1
#   .\start_monitoring.ps1 -Stop   # Stop the stack
#

param(
    [switch]$Stop
)

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ParentDir = Split-Path -Parent $ScriptDir

Write-Host @"

+====================================================================+
|  EXP-01: Monitoring Stack (WAS Server)                            |
+====================================================================+

"@

Set-Location $ParentDir

if ($Stop) {
    Write-Host "Stopping monitoring stack..."
    docker compose -f docker-compose.monitoring.yml down
    Write-Host "Done."
    exit 0
}

# Start stack
Write-Host "[1/3] Starting monitoring stack..."
docker compose -f docker-compose.monitoring.yml up -d

# Wait for services
Write-Host ""
Write-Host "[2/3] Waiting for services..."

for ($i = 1; $i -le 30; $i++) {
    try {
        $null = Invoke-RestMethod -Uri "http://localhost:8086/ping" -TimeoutSec 1
        Write-Host "  InfluxDB: OK"
        break
    } catch {
        Start-Sleep -Seconds 1
    }
}

for ($i = 1; $i -le 30; $i++) {
    try {
        $null = Invoke-RestMethod -Uri "http://localhost:3001/api/health" -TimeoutSec 1
        Write-Host "  Grafana: OK"
        break
    } catch {
        Start-Sleep -Seconds 1
    }
}

# Get server IP
Write-Host ""
Write-Host "[3/3] Server Info..."

$ips = Get-NetIPAddress -AddressFamily IPv4 |
    Where-Object { $_.InterfaceAlias -notmatch 'Loopback' -and $_.IPAddress -notmatch '^169\.' } |
    Select-Object -ExpandProperty IPAddress

Write-Host @"

+====================================================================+
|  Monitoring Stack Ready!                                          |
+====================================================================+
|
|  Services:
|    - Grafana:    http://localhost:3001  (admin/admin)
|    - InfluxDB:   http://localhost:8086
|    - Prometheus: http://localhost:9090
|
|  For k6 client, use:
|    .\run_k6.ps1 -WasUrl "http://<THIS_SERVER_IP>:8080" \
|                 -InfluxUrl "http://<THIS_SERVER_IP>:8086" \
|                 -Phase baseline
|
|  Detected IPs: $($ips -join ', ')
|
|  To stop: .\start_monitoring.ps1 -Stop
+====================================================================+
"@
