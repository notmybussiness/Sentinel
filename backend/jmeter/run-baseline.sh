#!/bin/bash

# Sentinel JMeter - Baseline Test Runner
# Phase 0: All APIs baseline performance test

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
        echo "Install with:"
        echo "  Mac:     brew install jmeter"
        echo "  Windows: choco install jmeter"
        echo "  Linux:   sudo apt install jmeter"
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
        echo "Please start backend first:"
        echo "  cd .. && ./gradlew bootRun"
        exit 1
    fi
}

check_postgresql() {
    log_info "Checking PostgreSQL at $PG_HOST:$PG_PORT..."
    if command -v psql &> /dev/null; then
        if PGPASSWORD=$PG_PASSWORD psql -h $PG_HOST -p $PG_PORT -U $PG_USER -d $PG_DB -c "SELECT 1" > /dev/null 2>&1; then
            log_success "PostgreSQL is UP"
        else
            log_warning "PostgreSQL connection failed (continuing anyway)"
        fi
    else
        log_warning "psql not installed, skipping PostgreSQL check"
    fi
}

# Create results directory
mkdir -p "$RESULTS_DIR"

# Run baseline test
run_baseline() {
    local users=${1:-1}

    echo ""
    echo "======================================"
    echo "  Phase 0: Baseline Test"
    echo "  Users: $users"
    echo "  Loops: 100"
    echo "  Timestamp: $TIMESTAMP"
    echo "======================================"
    echo ""

    log_info "Running Baseline Test with $users users..."

    jmeter -n -t test1-baseline.jmx \
        -Jhost="$HOST" \
        -Jusers="$users" \
        -Jloops=100 \
        -l "$RESULTS_DIR/baseline-${users}users-${TIMESTAMP}.jtl" \
        -e -o "$RESULTS_DIR/baseline-${users}users-${TIMESTAMP}-report"

    log_success "Baseline test completed"
    echo ""
    echo "Results:"
    echo "  JTL:    $RESULTS_DIR/baseline-${users}users-${TIMESTAMP}.jtl"
    echo "  Report: $RESULTS_DIR/baseline-${users}users-${TIMESTAMP}-report/index.html"
    echo ""
}

# Main script
main() {
    echo "======================================"
    echo "  Sentinel Baseline Test Runner"
    echo "======================================"
    echo ""

    # Check prerequisites
    check_jmeter
    check_backend
    check_postgresql

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
