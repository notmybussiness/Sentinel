#!/bin/bash

# Sentinel JMeter - SSE Test Runner
# Phase 2: Server-Sent Events streaming performance test

set -e

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
HOST="${HOST:-192.168.0.58}"
RESULTS_DIR="./results"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# PostgreSQL Configuration
PG_HOST="${PG_HOST:-192.168.0.5}"
PG_PORT="${PG_PORT:-5432}"
PG_USER="${PG_USER:-sentinel}"
PG_PASSWORD="${PG_PASSWORD:-sentinel_password}"
PG_DB="${PG_DB:-sentinel}"

# Functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Check prerequisites
check_jmeter() {
    if ! command -v jmeter &> /dev/null; then
        log_error "JMeter not installed"
        exit 1
    fi
    log_success "JMeter found: $(jmeter --version | head -1)"
}

check_backend() {
    log_info "Checking backend at $HOST:8080..."
    if curl -f "http://$HOST:8080/actuator/health" > /dev/null 2>&1; then
        log_success "Backend is UP"
    else
        log_error "Backend is DOWN at $HOST:8080"
        exit 1
    fi
}

# Create results directory
mkdir -p "$RESULTS_DIR"

# Run SSE test
run_sse() {
    local clients=${1:-10}
    local duration=${2:-300}

    echo ""
    echo "======================================"
    echo "  Phase 2: SSE Streaming Test"
    echo "  Concurrent Clients: $clients"
    echo "  Duration: $duration seconds"
    echo "  Timestamp: $TIMESTAMP"
    echo "======================================"
    echo ""

    log_info "Running SSE Test with $clients clients for $duration seconds..."
    log_warning "Long-lived connections will be maintained"
    log_info "Monitoring memory usage is recommended"

    # Start background memory monitoring if possible
    if command -v curl &> /dev/null; then
        (
            while sleep 5; do
                MEM=$(curl -s "http://$HOST:8080/actuator/metrics/jvm.memory.used" 2>/dev/null | grep -o '"value":[0-9]*' | head -1 | cut -d':' -f2)
                if [ -n "$MEM" ]; then
                    MEM_MB=$((MEM / 1024 / 1024))
                    echo "$(date '+%H:%M:%S') - Memory: ${MEM_MB}MB" >> "$RESULTS_DIR/sse-memory-${TIMESTAMP}.log"
                fi
            done
        ) &
        MONITOR_PID=$!
    fi

    jmeter -n -t test3-sse.jmx \
        -Jhost="$HOST" \
        -Jclients="$clients" \
        -Jduration="$duration" \
        -l "$RESULTS_DIR/sse-${clients}clients-${TIMESTAMP}.jtl" \
        -e -o "$RESULTS_DIR/sse-${clients}clients-${TIMESTAMP}-report"

    # Stop memory monitoring
    if [ -n "$MONITOR_PID" ]; then
        kill $MONITOR_PID 2>/dev/null || true
    fi

    log_success "SSE test completed"
    echo ""
    echo "Results:"
    echo "  JTL:    $RESULTS_DIR/sse-${clients}clients-${TIMESTAMP}.jtl"
    echo "  Report: $RESULTS_DIR/sse-${clients}clients-${TIMESTAMP}-report/index.html"
    if [ -f "$RESULTS_DIR/sse-memory-${TIMESTAMP}.log" ]; then
        echo "  Memory: $RESULTS_DIR/sse-memory-${TIMESTAMP}.log"
    fi
    echo ""
}

# Main script
main() {
    echo "======================================"
    echo "  Sentinel SSE Test Runner"
    echo "======================================"
    echo ""

    # Check prerequisites
    check_jmeter
    check_backend

    # Parse arguments
    CLIENTS="${1:-10}"
    DURATION="${2:-300}"

    if [[ "$CLIENTS" == "help" ]] || [[ "$CLIENTS" == "-h" ]] || [[ "$CLIENTS" == "--help" ]]; then
        echo "Usage: $0 [clients] [duration]"
        echo ""
        echo "Arguments:"
        echo "  clients   Number of concurrent SSE clients (default: 10)"
        echo "  duration  Test duration in seconds (default: 300)"
        echo ""
        echo "Examples:"
        echo "  $0           # 10 clients, 5 minutes"
        echo "  $0 50        # 50 clients, 5 minutes"
        echo "  $0 50 600    # 50 clients, 10 minutes"
        echo ""
        echo "Recommended test scenarios:"
        echo "  $0 10 300    # Light load"
        echo "  $0 50 300    # Medium load"
        echo "  $0 100 300   # Heavy load"
        echo "  $0 500 300   # Stress test (WARNING: high memory usage)"
        echo ""
        echo "Environment variables:"
        echo "  HOST=$HOST"
        echo ""
        exit 0
    fi

    run_sse "$CLIENTS" "$DURATION"

    echo ""
    echo "======================================"
    log_success "Test completed!"
    echo "======================================"
    echo ""
}

# Run main
main "$@"
