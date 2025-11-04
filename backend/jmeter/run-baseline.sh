#!/bin/bash

# Sentinel JMeter - Baseline Test Runner
# Phase 0: All APIs baseline performance test

set -e

# Load common functions
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common-functions.sh"

# ==================== Run Baseline Test ====================

run_baseline() {
    local users=${1:-1}
    local timestamp=$(date +%Y%m%d_%H%M%S)

    echo ""
    echo "======================================"
    echo "  Phase 0: Baseline Test"
    echo "  Users: $users"
    echo "  Loops: 100"
    echo "  Timestamp: $timestamp"
    echo "======================================"
    echo ""

    log_info "Running Baseline Test with $users users..."

    jmeter -n -t test1-baseline.jmx \
        -Jhost="$HOST" \
        -Jusers="$users" \
        -Jloops=100 \
        -l "$RESULTS_DIR/baseline-${users}users-${timestamp}.jtl" \
        -e -o "$RESULTS_DIR/baseline-${users}users-${timestamp}-report"

    log_success "Baseline test completed"
    echo ""
    echo "Results:"
    echo "  JTL:    $RESULTS_DIR/baseline-${users}users-${timestamp}.jtl"
    echo "  Report: $RESULTS_DIR/baseline-${users}users-${timestamp}-report/index.html"
    echo ""
}

# ==================== Main Script ====================

main() {
    echo ""
    echo "======================================"
    echo "  Sentinel Baseline Test Runner"
    echo "======================================"
    echo ""

    # Check prerequisites
    check_jmeter || exit 1
    check_backend || exit 1
    check_postgresql

    # Setup directory
    setup_results_dir

    # Parse arguments
    USERS="${1:-1}"

    if [[ "$USERS" == "help" ]] || [[ "$USERS" == "-h" ]] || [[ "$USERS" == "--help" ]]; then
        echo "Usage: $0 [users]"
        echo ""
        echo "Arguments:"
        echo "  users    Number of concurrent users (default: 1)"
        echo ""
        echo "Examples:"
        echo "  $0           # 1 user"
        echo "  $0 10        # 10 users"
        echo "  $0 100       # 100 users"
        echo ""
        echo "Environment variables:"
        echo "  HOST=$HOST"
        echo "  PG_HOST=$PG_HOST"
        echo "  PG_PASSWORD=$PG_PASSWORD"
        echo ""
        exit 0
    fi

    run_baseline "$USERS"

    echo ""
    echo "======================================"
    log_success "Test completed!"
    echo "======================================"
    echo ""
}

# Run main
main "$@"
