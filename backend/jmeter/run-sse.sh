#!/bin/bash

# Sentinel JMeter - SSE Test Runner
# Phase 2: Server-Sent Events streaming performance test

set -e

# Load common functions
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common-functions.sh"

# ==================== Run SSE Test ====================

run_sse() {
    local clients=${1:-10}
    local duration=${2:-300}
    local timestamp=$(date +%Y%m%d_%H%M%S)

    echo ""
    echo "======================================"
    echo "  Phase 2: SSE Streaming Test"
    echo "  Concurrent Clients: $clients"
    echo "  Duration: $duration seconds"
    echo "  Timestamp: $timestamp"
    echo "======================================"
    echo ""

    log_info "Running SSE Test with $clients clients for $duration seconds..."
    log_warning "Long-lived connections will be maintained"
    log_info "Monitoring memory usage is recommended"

    # Start background memory monitoring
    if command -v curl &> /dev/null; then
        (
            while sleep 5; do
                MEM=$(curl -s "http://$HOST:8080/actuator/metrics/jvm.memory.used" 2>/dev/null | grep -o '"value":[0-9]*' | head -1 | cut -d':' -f2)
                if [ -n "$MEM" ]; then
                    MEM_MB=$((MEM / 1024 / 1024))
                    echo "$(date '+%H:%M:%S') - Memory: ${MEM_MB}MB" >> "$RESULTS_DIR/sse-memory-${timestamp}.log"
                fi
            done
        ) &
        MONITOR_PID=$!
    fi

    jmeter -n -t test3-sse.jmx \
        -Jhost="$HOST" \
        -Jclients="$clients" \
        -Jduration="$duration" \
        -l "$RESULTS_DIR/sse-${clients}clients-${timestamp}.jtl" \
        -e -o "$RESULTS_DIR/sse-${clients}clients-${timestamp}-report"

    # Stop memory monitoring
    if [ -n "$MONITOR_PID" ]; then
        kill $MONITOR_PID 2>/dev/null || true
    fi

    log_success "SSE test completed"
    echo ""
    echo "Results:"
    echo "  JTL:    $RESULTS_DIR/sse-${clients}clients-${timestamp}.jtl"
    echo "  Report: $RESULTS_DIR/sse-${clients}clients-${timestamp}-report/index.html"
    if [ -f "$RESULTS_DIR/sse-memory-${timestamp}.log" ]; then
        echo "  Memory: $RESULTS_DIR/sse-memory-${timestamp}.log"
    fi
    echo ""
}

# ==================== Main Script ====================

main() {
    echo ""
    echo "======================================"
    echo "  Sentinel SSE Test Runner"
    echo "======================================"
    echo ""

    # Check prerequisites
    check_jmeter || exit 1
    check_backend || exit 1

    # Setup directory
    setup_results_dir

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
