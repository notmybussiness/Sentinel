#!/bin/bash

# Sentinel JMeter - Common Functions
# Shared utilities for all test scripts

# ==================== Configuration ====================

# Network Configuration
export HOST="${HOST:-192.168.0.58}"
export PG_HOST="${PG_HOST:-192.168.0.5}"
export PG_PORT="${PG_PORT:-5432}"
export PG_USER="${PG_USER:-sentinel}"
export PG_PASSWORD="${PG_PASSWORD:-sentinel_password}"
export PG_DB="${PG_DB:-sentinel}"

# Results Directory
export RESULTS_DIR="./results"

# ==================== Colors ====================

export GREEN='\033[0;32m'
export RED='\033[0;31m'
export YELLOW='\033[1;33m'
export BLUE='\033[0;34m'
export NC='\033[0m'

# ==================== Logging Functions ====================

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

# ==================== Health Check ====================

check_jmeter() {
    if ! command -v jmeter &> /dev/null; then
        log_error "JMeter not installed"
        echo "Install with:"
        echo "  Mac:     brew install jmeter"
        echo "  Windows: choco install jmeter"
        echo "  Linux:   sudo apt install jmeter"
        return 1
    fi
    log_success "JMeter found: $(jmeter --version 2>&1 | head -1)"
    return 0
}

check_backend() {
    log_info "Checking backend at $HOST:8080..."
    if curl -f -s "http://$HOST:8080/actuator/health" > /dev/null 2>&1; then
        log_success "Backend is UP"
        return 0
    else
        log_error "Backend is DOWN at $HOST:8080"
        echo "Please start backend first:"
        echo "  cd backend && ./gradlew bootRun"
        return 1
    fi
}

check_postgresql() {
    log_info "Checking PostgreSQL at $PG_HOST:$PG_PORT..."
    if command -v psql &> /dev/null; then
        if PGPASSWORD=$PG_PASSWORD psql -h $PG_HOST -p $PG_PORT -U $PG_USER -d $PG_DB -c "SELECT 1" > /dev/null 2>&1; then
            log_success "PostgreSQL is UP"
            return 0
        else
            log_warning "PostgreSQL connection failed (continuing anyway)"
            return 1
        fi
    else
        log_warning "psql not installed, skipping PostgreSQL check"
        return 1
    fi
}

# ==================== PostgreSQL Statistics ====================

reset_pg_stats() {
    log_info "Resetting PostgreSQL query statistics..."
    if PGPASSWORD=$PG_PASSWORD psql -h $PG_HOST -p $PG_PORT -U $PG_USER -d $PG_DB -c "SELECT pg_stat_statements_reset();" > /dev/null 2>&1; then
        log_success "Statistics reset"
        return 0
    else
        log_warning "Could not reset statistics (pg_stat_statements may not be enabled)"
        return 1
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
        return 0
    else
        log_warning "Could not collect query statistics"
        return 1
    fi
}

# ==================== Directory Setup ====================

setup_results_dir() {
    mkdir -p "$RESULTS_DIR"
    log_info "Results directory: $RESULTS_DIR"
}

# Export functions
export -f log_info log_success log_error log_warning
export -f check_jmeter check_backend check_postgresql
export -f reset_pg_stats get_query_stats
export -f setup_results_dir
