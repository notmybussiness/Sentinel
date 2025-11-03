#!/bin/bash

# Sentinel JMeter Test Runner
# Usage: ./run-tests.sh [test_name] [users]
# Example: ./run-tests.sh baseline 10
#          ./run-tests.sh all

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

# Create results directory
mkdir -p "$RESULTS_DIR"

# Run baseline test
run_baseline() {
    local users=${1:-1}
    log_info "Running Baseline Test with $users users..."

    jmeter -n -t test1-baseline.jmx \
        -Jhost="$HOST" \
        -Jusers="$users" \
        -Jloops=100 \
        -l "$RESULTS_DIR/baseline-${users}users-${TIMESTAMP}.jtl" \
        -e -o "$RESULTS_DIR/baseline-${users}users-${TIMESTAMP}-report"

    log_success "Baseline test completed"
    echo "Report: $RESULTS_DIR/baseline-${users}users-${TIMESTAMP}-report/index.html"
}

# Run stock API test
run_stock() {
    local users=${1:-10}
    log_info "Running Stock API Test with $users users..."

    jmeter -n -t test2-stock.jmx \
        -Jhost="$HOST" \
        -Jusers="$users" \
        -Jloops=100 \
        -Jthroughput=600 \
        -l "$RESULTS_DIR/stock-${users}users-${TIMESTAMP}.jtl" \
        -e -o "$RESULTS_DIR/stock-${users}users-${TIMESTAMP}-report"

    log_success "Stock API test completed"
    echo "Report: $RESULTS_DIR/stock-${users}users-${TIMESTAMP}-report/index.html"
}

# Run SSE test
run_sse() {
    local clients=${1:-10}
    log_info "Running SSE Test with $clients clients for 5 minutes..."

    jmeter -n -t test3-sse.jmx \
        -Jhost="$HOST" \
        -Jclients="$clients" \
        -Jduration=300 \
        -l "$RESULTS_DIR/sse-${clients}clients-${TIMESTAMP}.jtl" \
        -e -o "$RESULTS_DIR/sse-${clients}clients-${TIMESTAMP}-report"

    log_success "SSE test completed"
    echo "Report: $RESULTS_DIR/sse-${clients}clients-${TIMESTAMP}-report/index.html"
}

# Run Long Polling test
run_longpoll() {
    local clients=${1:-10}

    if [ "$clients" -gt 50 ]; then
        log_warning "Long Polling with >50 clients may cause memory issues"
        read -p "Continue? (y/n) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            log_info "Aborted"
            return
        fi
    fi

    log_info "Running Long Polling Test with $clients clients for 5 minutes..."

    jmeter -n -t test5-longpoll.jmx \
        -Jhost="$HOST" \
        -Jclients="$clients" \
        -Jduration=300 \
        -l "$RESULTS_DIR/longpoll-${clients}clients-${TIMESTAMP}.jtl" \
        -e -o "$RESULTS_DIR/longpoll-${clients}clients-${TIMESTAMP}-report"

    log_success "Long Polling test completed"
    echo "Report: $RESULTS_DIR/longpoll-${clients}clients-${TIMESTAMP}-report/index.html"
}

# Run Portfolio test (N+1)
run_portfolio() {
    local users=${1:-10}
    log_info "Running Portfolio Test with $users users..."

    jmeter -n -t test6-portfolio.jmx \
        -Jhost="$HOST" \
        -Jusers="$users" \
        -Jloops=100 \
        -l "$RESULTS_DIR/portfolio-${users}users-${TIMESTAMP}.jtl" \
        -e -o "$RESULTS_DIR/portfolio-${users}users-${TIMESTAMP}-report"

    log_success "Portfolio test completed"
    echo "Report: $RESULTS_DIR/portfolio-${users}users-${TIMESTAMP}-report/index.html"
}

# Run all tests
run_all() {
    log_info "Running all tests sequentially..."

    # Baseline with multiple user counts
    for users in 1 10 50; do
        run_baseline "$users"
        sleep 30  # Stabilization time
    done

    # Stock API
    run_stock 10
    sleep 30

    # SSE
    run_sse 10
    sleep 30

    # Long Polling (only 10 clients for safety)
    run_longpoll 10
    sleep 30

    # Portfolio
    run_portfolio 10
    sleep 30

    log_success "All tests completed!"
    echo "Results directory: $RESULTS_DIR/"
}

# Compare results
compare_results() {
    log_info "Comparing recent results..."

    # Find latest baseline and optimized portfolio tests
    baseline_jtl=$(ls -t "$RESULTS_DIR"/portfolio-*-baseline-*.jtl 2>/dev/null | head -1)
    optimized_jtl=$(ls -t "$RESULTS_DIR"/portfolio-*-optimized-*.jtl 2>/dev/null | head -1)

    if [ -z "$baseline_jtl" ] || [ -z "$optimized_jtl" ]; then
        log_warning "No comparison data available"
        echo "Run portfolio tests with both baseline and optimized code"
        return
    fi

    echo ""
    echo "=== Response Time Comparison ==="
    echo "Baseline:  $(awk -F',' 'NR>1 {sum+=$2; count++} END {print sum/count " ms"}' "$baseline_jtl")"
    echo "Optimized: $(awk -F',' 'NR>1 {sum+=$2; count++} END {print sum/count " ms"}' "$optimized_jtl")"
    echo ""
}

# Main script
main() {
    echo "======================================"
    echo "  Sentinel JMeter Test Runner"
    echo "  Timestamp: $TIMESTAMP"
    echo "======================================"
    echo ""

    # Check prerequisites
    check_jmeter
    check_backend

    # Parse arguments
    TEST_NAME="${1:-help}"
    USERS_OR_CLIENTS="${2:-10}"

    case "$TEST_NAME" in
        baseline)
            run_baseline "$USERS_OR_CLIENTS"
            ;;
        stock)
            run_stock "$USERS_OR_CLIENTS"
            ;;
        sse)
            run_sse "$USERS_OR_CLIENTS"
            ;;
        longpoll)
            run_longpoll "$USERS_OR_CLIENTS"
            ;;
        portfolio)
            run_portfolio "$USERS_OR_CLIENTS"
            ;;
        all)
            run_all
            ;;
        compare)
            compare_results
            ;;
        help|*)
            echo "Usage: $0 [test_name] [users/clients]"
            echo ""
            echo "Available tests:"
            echo "  baseline [users]     - Phase 0: All APIs baseline (default: 1)"
            echo "  stock [users]        - Phase 1: Stock APIs (default: 10)"
            echo "  sse [clients]        - Phase 2: SSE streaming (default: 10)"
            echo "  longpoll [clients]   - Phase 2: Long Polling (default: 10, max 50)"
            echo "  portfolio [users]    - Phase 3: Portfolio N+1 (default: 10)"
            echo "  all                  - Run all tests sequentially"
            echo "  compare              - Compare baseline vs optimized"
            echo ""
            echo "Examples:"
            echo "  $0 baseline 10       # Baseline test with 10 users"
            echo "  $0 sse 50            # SSE test with 50 concurrent clients"
            echo "  $0 all               # Run all tests"
            echo "  $0 compare           # Compare results"
            echo ""
            echo "Environment:"
            echo "  HOST=$HOST (override with: HOST=192.168.0.58 $0 baseline)"
            echo ""
            ;;
    esac

    echo ""
    echo "======================================"
    log_success "Test run complete!"
    echo "======================================"
    echo ""
}

# Run main
main "$@"
