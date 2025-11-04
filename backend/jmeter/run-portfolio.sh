#!/bin/bash

# Sentinel JMeter - Portfolio Test Runner
# Phase 3: N+1 Query optimization test

set -e

# Load common functions
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common-functions.sh"

# ==================== Run Portfolio Test ====================

run_portfolio() {
    local users=${1:-10}
    local mode=${2:-baseline}
    local timestamp=$(date +%Y%m%d_%H%M%S)

    echo ""
    echo "======================================"
    echo "  Phase 3: Portfolio N+1 Test"
    echo "  Mode: $mode"
    echo "  Users: $users"
    echo "  Loops: 100"
    echo "  Timestamp: $timestamp"
    echo "======================================"
    echo ""

    log_info "Running Portfolio Test..."

    # Reset PostgreSQL stats if available
    if check_postgresql; then
        reset_pg_stats
    fi

    log_warning "Monitoring N+1 query patterns"

    jmeter -n -t test6-portfolio.jmx \
        -Jhost="$HOST" \
        -Jusers="$users" \
        -Jloops=100 \
        -l "$RESULTS_DIR/portfolio-${mode}-${users}users-${timestamp}.jtl" \
        -e -o "$RESULTS_DIR/portfolio-${mode}-${users}users-${timestamp}-report"

    # Get query statistics after test
    if check_postgresql; then
        get_query_stats "$RESULTS_DIR/portfolio-${mode}-queries-${timestamp}.txt"
    fi

    log_success "Portfolio test completed"
    echo ""
    echo "Results:"
    echo "  JTL:    $RESULTS_DIR/portfolio-${mode}-${users}users-${timestamp}.jtl"
    echo "  Report: $RESULTS_DIR/portfolio-${mode}-${users}users-${timestamp}-report/index.html"
    if [ -f "$RESULTS_DIR/portfolio-${mode}-queries-${timestamp}.txt" ]; then
        echo "  Queries: $RESULTS_DIR/portfolio-${mode}-queries-${timestamp}.txt"
        echo ""
        log_info "Top queries:"
        head -15 "$RESULTS_DIR/portfolio-${mode}-queries-${timestamp}.txt"
    fi
    echo ""
}

# ==================== Main Script ====================

main() {
    echo ""
    echo "======================================"
    echo "  Sentinel Portfolio Test Runner"
    echo "======================================"
    echo ""

    # Check prerequisites
    check_jmeter || exit 1
    check_backend || exit 1
    check_postgresql

    # Setup directory
    setup_results_dir

    # Parse arguments
    USERS="${1:-10}"
    MODE="${2:-baseline}"

    if [[ "$USERS" == "help" ]] || [[ "$USERS" == "-h" ]] || [[ "$USERS" == "--help" ]]; then
        echo "Usage: $0 [users] [mode]"
        echo ""
        echo "Arguments:"
        echo "  users   Number of concurrent users (default: 10)"
        echo "  mode    Test mode: baseline or optimized (default: baseline)"
        echo ""
        echo "Examples:"
        echo "  $0                    # 10 users, baseline mode"
        echo "  $0 50                 # 50 users, baseline mode"
        echo "  $0 50 baseline        # 50 users, baseline (N+1 problem)"
        echo "  $0 50 optimized       # 50 users, optimized (JOIN FETCH)"
        echo ""
        echo "Workflow:"
        echo "  1. Run baseline test:"
        echo "     $0 50 baseline"
        echo ""
        echo "  2. Apply JOIN FETCH optimization in code"
        echo ""
        echo "  3. Run optimized test:"
        echo "     $0 50 optimized"
        echo ""
        echo "  4. Compare results:"
        echo "     - Check JTL files for response times"
        echo "     - Check query files for SQL query counts"
        echo ""
        echo "Environment variables:"
        echo "  HOST=$HOST"
        echo "  PG_HOST=$PG_HOST"
        echo "  PG_USER=$PG_USER"
        echo "  PG_PASSWORD=$PG_PASSWORD"
        echo ""
        exit 0
    fi

    run_portfolio "$USERS" "$MODE"

    echo ""
    echo "======================================"
    log_success "Test completed!"
    echo "======================================"
    echo ""
}

# Run main
main "$@"
