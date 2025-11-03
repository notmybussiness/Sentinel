#!/bin/bash

# Sentinel JMeter - Stock API Test Runner
# Phase 1: Heavy API optimization - Stock price APIs

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

# Run stock test
run_stock() {
    local users=${1:-10}
    local throughput=${2:-600}

    echo ""
    echo "======================================"
    echo "  Phase 1: Stock API Test"
    echo "  Users: $users"
    echo "  Loops: 100"
    echo "  Throughput: $throughput req/min"
    echo "  Timestamp: $TIMESTAMP"
    echo "======================================"
    echo ""

    log_info "Running Stock API Test..."
    log_warning "Note: External API calls (AlphaVantage, Finnhub) may be slow"

    jmeter -n -t test2-stock.jmx \
        -Jhost="$HOST" \
        -Jusers="$users" \
        -Jloops=100 \
        -Jthroughput="$throughput" \
        -l "$RESULTS_DIR/stock-${users}users-${TIMESTAMP}.jtl" \
        -e -o "$RESULTS_DIR/stock-${users}users-${TIMESTAMP}-report"

    log_success "Stock API test completed"
    echo ""
    echo "Results:"
    echo "  JTL:    $RESULTS_DIR/stock-${users}users-${TIMESTAMP}.jtl"
    echo "  Report: $RESULTS_DIR/stock-${users}users-${TIMESTAMP}-report/index.html"
    echo ""
}

# Main script
main() {
    echo "======================================"
    echo "  Sentinel Stock API Test Runner"
    echo "======================================"
    echo ""

    # Check prerequisites
    check_jmeter
    check_backend

    # Parse arguments
    USERS="${1:-10}"
    THROUGHPUT="${2:-600}"

    if [[ "$USERS" == "help" ]] || [[ "$USERS" == "-h" ]] || [[ "$USERS" == "--help" ]]; then
        echo "Usage: $0 [users] [throughput]"
        echo ""
        echo "Arguments:"
        echo "  users       Number of concurrent users (default: 10)"
        echo "  throughput  Requests per minute (default: 600)"
        echo ""
        echo "Examples:"
        echo "  $0           # 10 users, 600 req/min"
        echo "  $0 50        # 50 users, 600 req/min"
        echo "  $0 50 1200   # 50 users, 1200 req/min"
        echo ""
        echo "Environment variables:"
        echo "  HOST=$HOST"
        echo ""
        exit 0
    fi

    run_stock "$USERS" "$THROUGHPUT"

    echo ""
    echo "======================================"
    log_success "Test completed!"
    echo "======================================"
    echo ""
}

# Run main
main "$@"
