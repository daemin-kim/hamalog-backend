#!/bin/bash

# Hamalog Auto-deployment Script
# This script removes existing containers and rebuilds/restarts them
# Designed to be run when pushing to main branch

set -e  # Exit on any error

echo "🚀 Starting Hamalog deployment..."

# Configuration
COMPOSE_FILE="docker-compose.yml"
APP_SERVICE="hamalog-app"
PROJECT_NAME="hamalog"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if Docker is running
check_docker() {
    if ! docker info >/dev/null 2>&1; then
        print_error "Docker is not running. Please start Docker and try again."
        exit 1
    fi
}

# Function to stop and remove existing containers
cleanup_containers() {
    print_status "Stopping existing containers..."
    
    # Stop all services in the compose file
    if docker-compose -f $COMPOSE_FILE -p $PROJECT_NAME ps -q | grep -q .; then
        docker-compose -f $COMPOSE_FILE -p $PROJECT_NAME down --remove-orphans
        print_status "Existing containers stopped and removed"
    else
        print_warning "No existing containers found"
    fi
}

# Function to remove old images (optional - saves disk space)
cleanup_images() {
    print_status "Cleaning up old application images..."
    
    # Remove dangling images
    if docker images -f "dangling=true" -q | grep -q .; then
        docker rmi $(docker images -f "dangling=true" -q) || true
        print_status "Dangling images removed"
    fi
    
    # Optionally remove the old hamalog application image
    if docker images "${PROJECT_NAME}_${APP_SERVICE}" -q | grep -q .; then
        docker rmi "${PROJECT_NAME}_${APP_SERVICE}:latest" || true
        print_status "Old application image removed"
    fi
}

# Function to build the application
build_application() {
    print_status "Building application JAR..."
    
    # Clean and build the Spring Boot application
    ./gradlew clean bootJar
    
    if [ $? -eq 0 ]; then
        print_status "Application JAR built successfully"
    else
        print_error "Failed to build application JAR"
        exit 1
    fi
}

# Function to build and start containers
build_and_start() {
    print_status "Building and starting containers..."
    
    # Build and start all services
    docker-compose -f $COMPOSE_FILE -p $PROJECT_NAME up -d --build
    
    if [ $? -eq 0 ]; then
        print_status "Containers built and started successfully"
    else
        print_error "Failed to build and start containers"
        exit 1
    fi
}

# Function to verify deployment
verify_deployment() {
    print_status "Verifying deployment..."
    
    # Wait a bit for the application to start
    sleep 10
    
    # Check if the main application container is running
    if docker-compose -f $COMPOSE_FILE -p $PROJECT_NAME ps $APP_SERVICE | grep -q "Up"; then
        print_status "Application container is running"
        
        # Try to connect to the health endpoint (if available)
        if curl -f -s http://localhost:8080/actuator/health >/dev/null 2>&1; then
            print_status "✅ Application is healthy and responding"
        else
            print_warning "Application container is running but health check failed (this may be normal if no health endpoint is configured)"
        fi
    else
        print_error "Application container failed to start"
        print_status "Container logs:"
        docker-compose -f $COMPOSE_FILE -p $PROJECT_NAME logs $APP_SERVICE --tail=50
        exit 1
    fi
}

# Function to show container status
show_status() {
    print_status "Current container status:"
    docker-compose -f $COMPOSE_FILE -p $PROJECT_NAME ps
    
    print_status "\nApplication is available at:"
    echo "  🌐 http://localhost:8080"
    echo ""
    
    print_status "To view logs, run:"
    echo "  docker-compose -f $COMPOSE_FILE -p $PROJECT_NAME logs -f $APP_SERVICE"
}

# Main deployment flow
main() {
    print_status "=== Hamalog Auto-Deployment Script ==="
    
    # Pre-checks
    check_docker
    
    # Deployment steps
    cleanup_containers
    cleanup_images
    build_application
    build_and_start
    verify_deployment
    show_status
    
    print_status "🎉 Deployment completed successfully!"
}

# Handle script interruption
trap 'print_error "Deployment interrupted"; exit 1' INT TERM

# Run main function
main "$@"