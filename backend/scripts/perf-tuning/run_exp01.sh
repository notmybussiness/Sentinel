#!/bin/bash
#
# EXP-01: DB Indexing Performance Test Runner
#
# Usage:
#   ./run_exp01.sh baseline    # Before index
#   ./run_exp01.sh after       # After index
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESULTS_DIR="$SCRIPT_DIR/results"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
TEST_PHASE="${1:-baseline}"

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║  EXP-01: DB Indexing Performance Test                         ║"
echo "║  Phase: $TEST_PHASE                                              ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# 1. Check prerequisites
echo "[1/5] Checking prerequisites..."

if ! command -v k6 &> /dev/null; then
    echo "  ERROR: k6 is not installed"
    echo "  Install: https://k6.io/docs/get-started/installation/"
    exit 1
fi
echo "  k6: OK"

if ! curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "  ERROR: Backend is not running on localhost:8080"
    exit 1
fi
echo "  Backend: OK"

# 2. Start monitoring stack
echo ""
echo "[2/5] Starting monitoring stack..."

cd "$SCRIPT_DIR"
docker compose -f docker-compose.monitoring.yml up -d

# Wait for services
echo "  Waiting for InfluxDB..."
for i in {1..30}; do
    if curl -s http://localhost:8086/ping > /dev/null 2>&1; then
        echo "  InfluxDB: OK"
        break
    fi
    sleep 1
done

echo "  Grafana: http://localhost:3001 (admin/admin)"

# 3. Generate tokens
echo ""
echo "[3/5] Generating test tokens..."

if [ ! -f "$SCRIPT_DIR/tokens.csv" ]; then
    python "$SCRIPT_DIR/generate_tokens.py" --url http://localhost:8080 --output "$SCRIPT_DIR/tokens.csv"
else
    echo "  Using existing tokens.csv"
fi

TOKEN_COUNT=$(wc -l < "$SCRIPT_DIR/tokens.csv")
echo "  Tokens: $((TOKEN_COUNT - 1)) users"

# 4. Run k6 test with InfluxDB output
echo ""
echo "[4/5] Running k6 test..."
echo "  Output: InfluxDB (localhost:8086/k6)"
echo ""

RESULT_FILE="$RESULTS_DIR/exp01_${TEST_PHASE}_${TIMESTAMP}.json"

k6 run \
    --out influxdb=http://localhost:8086/k6 \
    --tag testid=exp01_${TEST_PHASE}_${TIMESTAMP} \
    "$SCRIPT_DIR/exp01_db_indexing.js" \
    2>&1 | tee "$RESULTS_DIR/exp01_${TEST_PHASE}_${TIMESTAMP}.log"

# 5. Collect results
echo ""
echo "[5/5] Collecting results..."

# Query InfluxDB for summary
curl -s -G 'http://localhost:8086/query' \
    --data-urlencode "db=k6" \
    --data-urlencode "q=SELECT mean(value) FROM http_reqs WHERE time > now() - 10m GROUP BY time(1m)" \
    > "$RESULTS_DIR/exp01_${TEST_PHASE}_${TIMESTAMP}_influx.json"

echo ""
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║  Test Complete!                                                ║"
echo "╠════════════════════════════════════════════════════════════════╣"
echo "║  Results: $RESULTS_DIR"
echo "║  Grafana: http://localhost:3001/d/exp01-db-indexing"
echo "║                                                                ║"
echo "║  Next Steps:                                                   ║"
if [ "$TEST_PHASE" == "baseline" ]; then
echo "║  1. Review baseline metrics in Grafana                        ║"
echo "║  2. Apply DB indexes (see PLAN_PERF_1000TPS.md)               ║"
echo "║  3. Run: ./run_exp01.sh after                                 ║"
else
echo "║  1. Compare 'baseline' vs 'after' in Grafana                  ║"
echo "║  2. Update PLAN_PERF_1000TPS.md with results                  ║"
fi
echo "╚════════════════════════════════════════════════════════════════╝"
