#!/bin/bash

# Hamalog Local Development Startup Script
# 배포 버전과 동일한 환경에서 로컬 애플리케이션 실행

echo "🚀 Starting Hamalog in deployment-like environment..."
echo "📦 This will run the application with the same configuration as production:"
echo "   - Production Spring profile"
echo "   - MySQL database (containerized)"
echo "   - Redis cache (containerized)"
echo "   - Same environment variables as deployment"

# Check if Docker is running
if ! docker info >/dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

# Optional cleanup before starting
if [ "$1" = "--clean" ] || [ "$1" = "-c" ]; then
    echo "🧹 임시 파일 정리 중..."
    ./cleanup.sh --docker
    echo ""
fi

# Build and start the application
echo "🔨 Building and starting containers..."
docker-compose -f docker-compose.local.yml down --remove-orphans
docker-compose -f docker-compose.local.yml up --build -d

# Wait for services to be healthy
echo "⏳ Waiting for services to be ready..."
sleep 10

# Check container status
echo "📋 Container status:"
docker-compose -f docker-compose.local.yml ps

# Health check
echo "🔍 Performing health check..."
MAX_ATTEMPTS=30
ATTEMPT=1

while [ $ATTEMPT -le $MAX_ATTEMPTS ]; do
    if curl -f -s http://localhost:8080/actuator/health >/dev/null 2>&1; then
        echo "✅ Application is ready!"
        echo "🌐 Application URL: http://localhost:8080"
        echo "📊 Health Check: http://localhost:8080/actuator/health"
        echo "🗄️  Database: MySQL on localhost:3306 (root/root_password)"
        echo "📦 Redis: localhost:6379"
        echo ""
        echo "💡 To stop the application: docker-compose -f docker-compose.local.yml down"
        echo "🔧 To view logs: docker-compose -f docker-compose.local.yml logs -f"
        exit 0
    fi
    
    if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
        echo "❌ Application failed to start after $MAX_ATTEMPTS attempts"
        echo "🔍 Checking logs..."
        docker-compose -f docker-compose.local.yml logs hamalog-app --tail=50
        exit 1
    fi
    
    echo "⏳ Attempt $ATTEMPT/$MAX_ATTEMPTS - waiting for application to start..."
    sleep 5
    ATTEMPT=$((ATTEMPT + 1))
done