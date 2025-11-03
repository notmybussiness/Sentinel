#!/bin/bash

# Sentinel JMeter - Portfolio Test Runner
# Phase 3: N+1 Query optimization test

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

check_postgresql() {
    log_info "Checking PostgreSQL at $PG_HOST:$PG_PORT..."
    if command -v psql &> /dev/null; then
        if PGPASSWORD=$PG_PASSWORD psql -h $PG_HOST -p $PG_PORT -U $PG_USER -d $PG_DB -c "SELECT 1" > /dev/null 2>&1; then
            log_success "PostgreSQL is UP"
            return 0
        else
            log_warning "PostgreSQL connection failed"
            return 1
        fi
    else
        log_warning "psql not installed, skipping PostgreSQL check"
        return 1
    fi
}

reset_pg_stats() {
    log_info "Resetting PostgreSQL query statistics..."
    if PGPASSWORD=$PG_PASSWORD psql -h $PG_HOST -p $PG_PORT -U $PG_USER -d $PG_DB -c "SELECT pg_stat_statements_reset();" > /dev/null 2>&1; then
        log_success "Statistics reset"
    else
        log_warning "Could not reset statistics (pg_stat_statements may not be enabled)"
    fi
}

get_query_stats() {
    local output_file=$1
    log_info "Collecting query statistics..."
    if PGPASSWORD=$PG_PASSWORD psql -h $PG_HOST -p $PG_PORT -U $PG_USER -d $PG_DB > "$output_file" 2>&1 <<EOF
SELECT
    query,
    calls,
    ROUND(mean_exec_time::numeric, 2) as mean_ms,
    ROUND(total_exec_time::numeric, 2) as total_ms
FROM pg_stat_statements
WHERE query LIKE '%portfolio%' OR query LIKE '%holding%'
ORDER BY calls DESC
LIMIT 20;
EOF
    then
        log_success "Query statistics saved to $output_file"
    else
        log_warning "Could not collect query statistics"
    fi
}

# Create results directory
mkdir -p "$RESULTS_DIR"

# Run portfolio test
run_portfolio() {
    local users=${1:-10}
    local mode=${2:-baseline}

    echo ""
    echo "======================================"
    echo "  Phase 3: Portfolio N+1 Test"
    echo "  Mode: $mode"
    echo "  Users: $users"
    echo "  Loops: 100"
    echo "  Timestamp: $TIMESTAMP"
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
        -l "$RESULTS_DIR/portfolio-${mode}-${users}users-${TIMESTAMP}.jtl" \
        -e -o "$RESULTS_DIR/portfolio-${mode}-${users}users-${TIMESTAMP}-report"

    # Get query statistics after test
    if check_postgresql; then
        get_query_stats "$RESULTS_DIR/portfolio-${mode}-queries-${TIMESTAMP}.txt"
    fi

    log_success "Portfolio test completed"
    echo ""
    echo "Results:"
    echo "  JTL:    $RESULTS_DIR/portfolio-${mode}-${users}users-${TIMESTAMP}.jtl"
    echo "  Report: $RESULTS_DIR/portfolio-${mode}-${users}users-${TIMESTAMP}-report/index.html"
    if [ -f "$RESULTS_DIR/portfolio-${mode}-queries-${TIMESTAMP}.txt" ]; then
        echo "  Queries: $RESULTS_DIR/portfolio-${mode}-queries-${TIMESTAMP}.txt"
        echo ""
        log_info "Top queries:"
        head -15 "$RESULTS_DIR/portfolio-${mode}-queries-${TIMESTAMP}.txt"
    fi
    echo ""
}

# Main script
main() {
    echo "======================================"
    echo "  Sentinel Portfolio Test Runner"
    echo "======================================"
    echo ""

    # Check prerequisites
    check_jmeter
    check_backend
    check_postgresql

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
