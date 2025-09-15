#!/bin/bash

# =============================================================================
# Hamalog Production-Like Local Environment Startup Script
# =============================================================================
# This script provides a single command to start the complete production-like
# environment locally for testing deployment readiness and identifying issues
# before pushing to GitHub and production deployment.
#
# Author: Hamalog Development Team
# Date: 2025-09-15
# Version: 1.0.0
#
# Usage:
#   ./run-production-local.sh [command] [options]
#
# Commands:
#   start     - Start production-like environment
#   stop      - Stop all services
#   restart   - Restart all services
#   logs      - Show logs from all services
#   status    - Show status of all services
#   clean     - Stop and remove all containers, volumes, and networks
#   test      - Run basic health checks on all services
#   help      - Show this help message
# =============================================================================

set -e  # Exit on any error

# Configuration
COMPOSE_FILE="docker-compose.local.yml"
ENV_FILE=".env.prod-local"
PROJECT_NAME="hamalog"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
NC='\033[0m' # No Color

# Logging functions
log_header() {
    echo -e "\n${WHITE}===========================================${NC}"
    echo -e "${WHITE}$1${NC}"
    echo -e "${WHITE}===========================================${NC}\n"
}

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

log_step() {
    echo -e "${CYAN}[STEP]${NC} $1"
}

# Check prerequisites
check_prerequisites() {
    log_step "Checking prerequisites..."
    
    # Check if Docker is running
    if ! docker info >/dev/null 2>&1; then
        log_error "Docker is not running. Please start Docker Desktop and try again."
        exit 1
    fi
    
    # Check if docker-compose is available
    if ! command -v docker-compose &> /dev/null; then
        log_error "docker-compose not found. Please install docker-compose."
        exit 1
    fi
    
    # Check if required files exist
    if [ ! -f "$COMPOSE_FILE" ]; then
        log_error "Docker compose file '$COMPOSE_FILE' not found."
        exit 1
    fi
    
    if [ ! -f "$ENV_FILE" ]; then
        log_error "Environment file '$ENV_FILE' not found."
        exit 1
    fi
    
    log_success "All prerequisites met"
}

# Show environment info
show_environment_info() {
    log_header "PRODUCTION-LOCAL ENVIRONMENT INFORMATION"
    log_info "Compose File: $COMPOSE_FILE"
    log_info "Environment File: $ENV_FILE"
    log_info "Project Name: $PROJECT_NAME"
    log_info "Profile: Production (with local adaptations)"
    echo
    log_info "Services to be started:"
    echo -e "  ${GREEN}•${NC} MySQL 8.0 (Port 3306) - Production-strength database"
    echo -e "  ${GREEN}•${NC} Redis 7 Alpine (Port 6379) - Caching and sessions"
    echo -e "  ${GREEN}•${NC} HashiCorp Vault 1.15.2 (Port 8200) - Secret management"
    echo -e "  ${GREEN}•${NC} Vault Initialization - Automated secret setup"
    echo -e "  ${GREEN}•${NC} Hamalog Application (Port 8080) - Main application"
    echo
    log_info "Production-like features enabled:"
    echo -e "  ${GREEN}•${NC} Production Spring profile"
    echo -e "  ${GREEN}•${NC} Production-strength passwords and encryption"
    echo -e "  ${GREEN}•${NC} Connection pooling and performance optimization"
    echo -e "  ${GREEN}•${NC} Vault-based secret management"
    echo -e "  ${GREEN}•${NC} Production security headers"
    echo -e "  ${GREEN}•${NC} Realistic resource limits and health checks"
}

# Start the environment
start_environment() {
    log_header "STARTING PRODUCTION-LOCAL ENVIRONMENT"
    
    check_prerequisites
    show_environment_info
    
    log_step "Starting services with production configuration..."
    
    # Build and start all services
    docker-compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d --build
    
    log_success "All services started successfully"
    
    # Wait for services to be ready
    wait_for_services
    
    # Show service status
    show_service_status
    
    # Show access information
    show_access_info
}

# Stop the environment
stop_environment() {
    log_header "STOPPING PRODUCTION-LOCAL ENVIRONMENT"
    
    docker-compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" down
    
    log_success "All services stopped"
}

# Restart the environment
restart_environment() {
    log_header "RESTARTING PRODUCTION-LOCAL ENVIRONMENT"
    
    stop_environment
    sleep 2
    start_environment
}

# Show logs
show_logs() {
    log_header "SERVICE LOGS"
    
    docker-compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" logs -f --tail=50
}

# Clean everything
clean_environment() {
    log_header "CLEANING PRODUCTION-LOCAL ENVIRONMENT"
    
    log_warning "This will remove all containers, volumes, and networks."
    read -p "Are you sure? [y/N]: " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        docker-compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" down -v --remove-orphans
        
        # Remove any dangling images
        docker image prune -f
        
        log_success "Environment cleaned successfully"
    else
        log_info "Clean operation cancelled"
    fi
}

# Wait for services to be ready
wait_for_services() {
    log_step "Waiting for services to be ready..."
    
    local max_attempts=30
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        log_info "Health check attempt $attempt/$max_attempts..."
        
        # Check if Hamalog app is responding
        if curl -f -s http://localhost:8080/actuator/health >/dev/null 2>&1; then
            log_success "Hamalog application is ready!"
            break
        fi
        
        if [ $attempt -eq $max_attempts ]; then
            log_warning "Services may still be starting up. Check logs if there are issues."
            break
        fi
        
        sleep 5
        attempt=$((attempt + 1))
    done
}

# Show service status
show_service_status() {
    log_header "SERVICE STATUS"
    
    docker-compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" ps
}

# Show access information
show_access_info() {
    log_header "ACCESS INFORMATION"
    
    echo -e "${WHITE}🌐 Application URLs:${NC}"
    echo -e "  • Hamalog Application: ${GREEN}http://localhost:8080${NC}"
    echo -e "  • Health Check: ${GREEN}http://localhost:8080/actuator/health${NC}"
    echo -e "  • Vault UI: ${GREEN}http://localhost:8200${NC}"
    echo
    
    echo -e "${WHITE}🔑 Vault Information:${NC}"
    echo -e "  • Root Token: ${YELLOW}hamalog-dev-token${NC}"
    echo -e "  • Access secrets: ${CYAN}vault kv get secret/hamalog${NC}"
    echo
    
    echo -e "${WHITE}📊 Database Information:${NC}"
    echo -e "  • MySQL Host: ${GREEN}localhost:3306${NC}"
    echo -e "  • Database: ${GREEN}Hamalog${NC}"
    echo -e "  • Username: ${GREEN}hamalog_prod_user${NC}"
    echo -e "  • Password: [See .env.prod-local file]"
    echo
    
    echo -e "${WHITE}🔍 Monitoring:${NC}"
    echo -e "  • View logs: ${CYAN}./run-production-local.sh logs${NC}"
    echo -e "  • Check status: ${CYAN}./run-production-local.sh status${NC}"
}

# Run basic health checks
test_environment() {
    log_header "RUNNING ENVIRONMENT HEALTH CHECKS"
    
    local all_passed=true
    
    # Test Hamalog application
    log_step "Testing Hamalog application health..."
    if curl -f -s http://localhost:8080/actuator/health | grep -q "UP"; then
        log_success "✅ Hamalog application is healthy"
    else
        log_error "❌ Hamalog application health check failed"
        all_passed=false
    fi
    
    # Test Vault
    log_step "Testing Vault accessibility..."
    if curl -f -s http://localhost:8200/v1/sys/health >/dev/null 2>&1; then
        log_success "✅ Vault is accessible"
    else
        log_error "❌ Vault accessibility test failed"
        all_passed=false
    fi
    
    # Test MySQL connection
    log_step "Testing MySQL connection..."
    if docker exec "hamalog-mysql-hamalog-1" mysqladmin ping -h localhost -u root -p"MySql_L0c@l_R00t_Ultra_Str0ng_P@ssw0rd_2025!_S3cur3" >/dev/null 2>&1; then
        log_success "✅ MySQL is responding"
    else
        log_error "❌ MySQL connection test failed"
        all_passed=false
    fi
    
    # Test Redis connection
    log_step "Testing Redis connection..."
    if docker exec "hamalog-redis-1" redis-cli ping | grep -q "PONG"; then
        log_success "✅ Redis is responding"
    else
        log_error "❌ Redis connection test failed"
        all_passed=false
    fi
    
    echo
    if [ "$all_passed" = true ]; then
        log_success "🎉 All health checks passed! Your production-local environment is ready."
        echo
        log_info "Next steps:"
        echo -e "  1. Test your application features at ${GREEN}http://localhost:8080${NC}"
        echo -e "  2. Check Vault secrets at ${GREEN}http://localhost:8200${NC}"
        echo -e "  3. Monitor logs with: ${CYAN}./run-production-local.sh logs${NC}"
    else
        log_error "❌ Some health checks failed. Check the logs for more information."
        echo -e "  • View logs: ${CYAN}./run-production-local.sh logs${NC}"
        echo -e "  • Check status: ${CYAN}./run-production-local.sh status${NC}"
    fi
}

# Show help
show_help() {
    echo -e "${WHITE}Hamalog Production-Local Environment Script${NC}"
    echo
    echo -e "${CYAN}DESCRIPTION:${NC}"
    echo "  This script provides a complete production-like environment for local development"
    echo "  and testing. It helps identify potential issues before GitHub deployment."
    echo
    echo -e "${CYAN}USAGE:${NC}"
    echo "  ./run-production-local.sh [COMMAND]"
    echo
    echo -e "${CYAN}COMMANDS:${NC}"
    echo -e "  ${GREEN}start${NC}     Start the production-like environment"
    echo -e "  ${GREEN}stop${NC}      Stop all services"
    echo -e "  ${GREEN}restart${NC}   Restart all services"
    echo -e "  ${GREEN}status${NC}    Show status of all services"
    echo -e "  ${GREEN}logs${NC}      Show logs from all services"
    echo -e "  ${GREEN}test${NC}      Run health checks on all services"
    echo -e "  ${GREEN}clean${NC}     Remove all containers, volumes, and networks"
    echo -e "  ${GREEN}help${NC}      Show this help message"
    echo
    echo -e "${CYAN}EXAMPLES:${NC}"
    echo "  ./run-production-local.sh start    # Start the environment"
    echo "  ./run-production-local.sh logs     # View real-time logs"
    echo "  ./run-production-local.sh test     # Check if everything is working"
    echo "  ./run-production-local.sh clean    # Clean up everything"
    echo
    echo -e "${CYAN}WHAT THIS ENVIRONMENT PROVIDES:${NC}"
    echo "  • Complete production configuration with local adaptations"
    echo "  • MySQL 8.0 with production-strength passwords"
    echo "  • Redis 7 for caching and session management"
    echo "  • HashiCorp Vault for secure secret management"
    echo "  • Production Spring profile with optimized settings"
    echo "  • Connection pooling and performance optimization"
    echo "  • Security headers and encryption"
    echo "  • Health monitoring and logging"
}

# Main script logic
main() {
    case "${1:-start}" in
        start)
            start_environment
            ;;
        stop)
            stop_environment
            ;;
        restart)
            restart_environment
            ;;
        status)
            show_service_status
            ;;
        logs)
            show_logs
            ;;
        test)
            test_environment
            ;;
        clean)
            clean_environment
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            log_error "Unknown command: $1"
            echo
            show_help
            exit 1
            ;;
    esac
}

# Make sure the script is executable
if [ ! -x "$0" ]; then
    chmod +x "$0"
fi

# Run main function with all arguments
main "$@"