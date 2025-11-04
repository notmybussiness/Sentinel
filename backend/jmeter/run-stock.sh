#!/bin/bash

# Sentinel JMeter - Stock API Test Runner
# Phase 1: Heavy API optimization - Stock price APIs

set -e

# Load common functions
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common-functions.sh"

# ==================== Run Stock Test ====================

run_stock() {
    local users=${1:-10}
    local throughput=${2:-600}
    local timestamp=$(date +%Y%m%d_%H%M%S)

    echo ""
    echo "======================================"
    echo "  Phase 1: Stock API Test"
    echo "  Users: $users"
    echo "  Loops: 100"
    echo "  Throughput: $throughput req/min"
    echo "  Timestamp: $timestamp"
    echo "======================================"
    echo ""

    log_info "Running Stock API Test..."
    log_warning "Note: External API calls (AlphaVantage, Finnhub) may be slow"

    jmeter -n -t test2-stock.jmx \
        -Jhost="$HOST" \
        -Jusers="$users" \
        -Jloops=100 \
        -Jthroughput="$throughput" \
        -l "$RESULTS_DIR/stock-${users}users-${timestamp}.jtl" \
        -e -o "$RESULTS_DIR/stock-${users}users-${timestamp}-report"

    log_success "Stock API test completed"
    echo ""
    echo "Results:"
    echo "  JTL:    $RESULTS_DIR/stock-${users}users-${timestamp}.jtl"
    echo "  Report: $RESULTS_DIR/stock-${users}users-${timestamp}-report/index.html"
    echo ""
}

# ==================== Main Script ====================

main() {
    echo ""
    echo "======================================"
    echo "  Sentinel Stock API Test Runner"
    echo "======================================"
    echo ""

    # Check prerequisites
    check_jmeter || exit 1
    check_backend || exit 1

    # Setup directory
    setup_results_dir

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
