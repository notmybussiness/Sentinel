#!/bin/bash
# =====================================================
# Blue-Green Deployment Script for Sentinel
# =====================================================
# Usage: ./blue-green-switch.sh [blue|green]
# =====================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="${SCRIPT_DIR}/../docker/docker-compose.prod.yml"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check current active deployment
get_current_active() {
    if curl -s http://localhost:8081/actuator/health | grep -q "UP"; then
        echo "blue"
    elif curl -s http://localhost:8082/actuator/health | grep -q "UP"; then
        echo "green"
    else
        echo "none"
    fi
}

# Health check with retries
health_check() {
    local port=$1
    local max_retries=30
    local retry_interval=2
    
    log_info "Waiting for service on port $port to become healthy..."
    
    for i in $(seq 1 $max_retries); do
        if curl -s "http://localhost:$port/actuator/health" | grep -q "UP"; then
            log_success "Service on port $port is healthy!"
            return 0
        fi
        echo -n "."
        sleep $retry_interval
    done
    
    echo ""
    log_error "Service on port $port failed health check after $((max_retries * retry_interval)) seconds"
    return 1
}

# Deploy to Blue
deploy_blue() {
    log_info "Deploying to Blue (port 8081)..."
    
    # Build and start Blue
    docker-compose -f "$COMPOSE_FILE" build was-blue
    docker-compose -f "$COMPOSE_FILE" up -d was-blue
    
    # Wait for health
    if health_check 8081; then
        log_success "Blue deployment successful!"
        
        # Stop Green if running
        if docker ps | grep -q "sentinel-was-green"; then
            log_info "Stopping Green instance..."
            docker-compose -f "$COMPOSE_FILE" --profile blue-green stop was-green
        fi
        
        return 0
    else
        log_error "Blue deployment failed!"
        return 1
    fi
}

# Deploy to Green
deploy_green() {
    log_info "Deploying to Green (port 8082)..."
    
    # Build and start Green
    docker-compose -f "$COMPOSE_FILE" --profile blue-green build was-green
    docker-compose -f "$COMPOSE_FILE" --profile blue-green up -d was-green
    
    # Wait for health
    if health_check 8082; then
        log_success "Green deployment successful!"
        
        # Stop Blue if running
        if docker ps | grep -q "sentinel-was-blue"; then
            log_info "Stopping Blue instance..."
            docker-compose -f "$COMPOSE_FILE" stop was-blue
        fi
        
        return 0
    else
        log_error "Green deployment failed!"
        return 1
    fi
}

# Rolling update (deploy to inactive, switch, stop old)
rolling_update() {
    local current=$(get_current_active)
    log_info "Current active deployment: $current"
    
    if [ "$current" == "blue" ] || [ "$current" == "none" ]; then
        log_info "Will deploy to Green and switch..."
        deploy_green
    else
        log_info "Will deploy to Blue and switch..."
        deploy_blue
    fi
}

# Rollback to previous deployment
rollback() {
    local current=$(get_current_active)
    log_warning "Rolling back from $current..."
    
    if [ "$current" == "blue" ]; then
        deploy_green
    else
        deploy_blue
    fi
}

# Show status
show_status() {
    echo ""
    echo "======================================"
    echo "  Sentinel Deployment Status"
    echo "======================================"
    echo ""
    
    echo -n "Blue  (8081): "
    if curl -s http://localhost:8081/actuator/health | grep -q "UP"; then
        echo -e "${GREEN}HEALTHY${NC}"
    else
        echo -e "${RED}DOWN${NC}"
    fi
    
    echo -n "Green (8082): "
    if curl -s http://localhost:8082/actuator/health | grep -q "UP"; then
        echo -e "${GREEN}HEALTHY${NC}"
    else
        echo -e "${RED}DOWN${NC}"
    fi
    
    echo -n "RAG   (8000): "
    if curl -s http://localhost:8000/health | grep -q -i "ok\|healthy"; then
        echo -e "${GREEN}HEALTHY${NC}"
    else
        echo -e "${RED}DOWN${NC}"
    fi
    
    echo ""
    echo "Active: $(get_current_active)"
    echo ""
}

# Main
case "${1:-}" in
    blue)
        deploy_blue
        ;;
    green)
        deploy_green
        ;;
    update|deploy)
        rolling_update
        ;;
    rollback)
        rollback
        ;;
    status)
        show_status
        ;;
    *)
        echo "Usage: $0 {blue|green|update|rollback|status}"
        echo ""
        echo "Commands:"
        echo "  blue     - Deploy to Blue instance (port 8081)"
        echo "  green    - Deploy to Green instance (port 8082)"
        echo "  update   - Rolling update to inactive instance"
        echo "  rollback - Rollback to previous instance"
        echo "  status   - Show current deployment status"
        exit 1
        ;;
esac
