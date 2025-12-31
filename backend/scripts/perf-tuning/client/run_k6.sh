#!/bin/bash
#
# EXP-01: k6 Client Runner (macOS)
#
# Usage:
#   ./run_k6.sh baseline    # Before index
#   ./run_k6.sh after       # After index
#

set -e

# ============================================================================
# CONFIGURATION - 실제 환경
# ============================================================================
WAS_URL="http://192.168.0.58:8080"
INFLUX_URL="http://192.168.0.58:8086"
GRAFANA_URL="http://192.168.0.58:3001"

# ============================================================================
PHASE="${1:-baseline}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PARENT_DIR="$(dirname "$SCRIPT_DIR")"
RESULTS_DIR="$PARENT_DIR/results"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

echo ""
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║  EXP-01: k6 Client (macOS)                                     ║"
echo "╠════════════════════════════════════════════════════════════════╣"
echo "║  Phase:      $PHASE"
echo "║  WAS:        $WAS_URL"
echo "║  InfluxDB:   $INFLUX_URL"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Ensure results directory
mkdir -p "$RESULTS_DIR"

# 1. Check WAS server
echo "[1/3] Checking WAS server..."
if curl -s --connect-timeout 5 "$WAS_URL/actuator/health" > /dev/null 2>&1; then
    echo "  ✓ WAS: OK"
else
    echo "  ✗ ERROR: Cannot reach WAS at $WAS_URL"
    exit 1
fi

# 2. Check InfluxDB
echo ""
echo "[2/3] Checking InfluxDB..."
if curl -s --connect-timeout 5 "$INFLUX_URL/ping" > /dev/null 2>&1; then
    echo "  ✓ InfluxDB: OK"
else
    echo "  ✗ ERROR: Cannot reach InfluxDB at $INFLUX_URL"
    echo "  → WAS 서버에서 start_monitoring.ps1 실행 필요"
    exit 1
fi

# 3. Generate tokens if needed
TOKENS_FILE="$PARENT_DIR/tokens.csv"
if [ ! -f "$TOKENS_FILE" ]; then
    echo ""
    echo "  Generating tokens..."
    python3 "$PARENT_DIR/generate_tokens.py" --url "$WAS_URL" --output "$TOKENS_FILE"
fi

TOKEN_COUNT=$(($(wc -l < "$TOKENS_FILE") - 1))
echo "  Tokens: $TOKEN_COUNT users"

# 4. Run k6
echo ""
echo "[3/3] Running k6..."
echo ""

LOG_FILE="$RESULTS_DIR/exp01_${PHASE}_${TIMESTAMP}.log"

k6 run \
    --out "influxdb=$INFLUX_URL/k6" \
    -e "WAS_URL=$WAS_URL" \
    -e "INFLUX_URL=$INFLUX_URL" \
    --tag "testid=exp01_${PHASE}_${TIMESTAMP}" \
    --tag "phase=$PHASE" \
    "$PARENT_DIR/exp01_db_indexing.js" 2>&1 | tee "$LOG_FILE"

echo ""
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║  Test Complete!                                                ║"
echo "╠════════════════════════════════════════════════════════════════╣"
echo "║  Log:     $LOG_FILE"
echo "║  Grafana: $GRAFANA_URL/d/exp01-db-indexing"
echo "╚════════════════════════════════════════════════════════════════╝"
