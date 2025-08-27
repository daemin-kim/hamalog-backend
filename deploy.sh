#!/bin/bash

# Hamalog Production Deployment Script
# This script handles the deployment of the Hamalog application using Docker Compose

set -euo pipefail  # Exit on any error, undefined variable, or pipe failure

# Configuration
PROJECT_NAME="hamalog"
REGISTRY="ghcr.io"
IMAGE_NAME="${REGISTRY}/${GITHUB_REPOSITORY:-daemin-kim/hamalog-backend}"
IMAGE_TAG="${GITHUB_SHA:-latest}"
COMPOSE_FILE="docker-compose.prod.yml"

echo "🚀 Starting Hamalog deployment process..."
echo "📋 Deployment Configuration:"
echo "   - Registry: ${REGISTRY}"
echo "   - Image: ${IMAGE_NAME}:${IMAGE_TAG}"
echo "   - Project: ${PROJECT_NAME}"

# Function to handle cleanup on script exit
cleanup() {
    local exit_code=$?
    if [ $exit_code -ne 0 ]; then
        echo "❌ Deployment failed with exit code: $exit_code"
        echo "🔍 Checking container status..."
        docker compose -p ${PROJECT_NAME} ps || true
        echo "📋 Recent logs:"
        docker compose -p ${PROJECT_NAME} logs --tail=50 || true
    fi
    exit $exit_code
}

trap cleanup EXIT

# Step 1: Create production docker-compose file
echo "📝 Creating production docker-compose configuration..."
cat > ${COMPOSE_FILE} << EOF
services:
  hamalog-app:
    image: ${IMAGE_NAME}:${IMAGE_TAG}
    ports:
      - "8080:8080"
    environment:
      # Spring Profile Configuration
      - SPRING_PROFILES_ACTIVE=\${SPRING_PROFILES_ACTIVE:-prod}
      
      # Database Configuration
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql-hamalog:3306/\${DB_NAME:-Hamalog}
      - SPRING_DATASOURCE_USERNAME=\${DB_USERNAME:-hamalog_user}
      - SPRING_DATASOURCE_PASSWORD=\${DB_PASSWORD:-hamalog_password}
      
      # Redis Configuration
      - SPRING_DATA_REDIS_HOST=redis
      - SPRING_DATA_REDIS_PORT=6379
      
      # JWT Configuration
      - JWT_SECRET=\${JWT_SECRET:-EzUuJwKK4vLnvk5r7yAgdNP/sa1dL87febZhlayPGjI=}
      - JWT_EXPIRY=\${JWT_EXPIRY:-3600000}
      
      # OAuth2 Configuration
      - KAKAO_CLIENT_ID=\${KAKAO_CLIENT_ID:-dummy-client-id}
      - KAKAO_CLIENT_SECRET=\${KAKAO_CLIENT_SECRET:-dummy-client-secret}
    depends_on:
      - mysql-hamalog
      - redis
    volumes:
      - hamalog-uploads:/data/hamalog/images
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    command: redis-server --appendonly yes
    volumes:
      - redis-data:/data
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 30s
      timeout: 10s
      retries: 3

  mysql-hamalog:
    image: mysql:8.0
    ports:
      - "3306:3306"
    environment:
      - MYSQL_ROOT_PASSWORD=\${MYSQL_ROOT_PASSWORD:-root_password}
      - MYSQL_DATABASE=\${DB_NAME:-Hamalog}
      - MYSQL_USER=\${DB_USERNAME:-hamalog_user}
      - MYSQL_PASSWORD=\${DB_PASSWORD:-hamalog_password}
    volumes:
      - mysql-data:/var/lib/mysql
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p\${MYSQL_ROOT_PASSWORD:-root_password}"]
      interval: 30s
      timeout: 10s
      retries: 3

volumes:
  mysql-data:
  redis-data:
  hamalog-uploads:
EOF

echo "✅ Production docker-compose configuration created"

# Step 2: Stop and remove existing containers
echo "🛑 Stopping existing containers..."
if docker compose -p ${PROJECT_NAME} ps -q | grep -q .; then
    docker compose -p ${PROJECT_NAME} down --remove-orphans || {
        echo "⚠️  Failed to gracefully stop containers, forcing removal..."
        docker compose -p ${PROJECT_NAME} kill || true
        docker compose -p ${PROJECT_NAME} rm -f || true
    }
    echo "✅ Existing containers stopped and removed"
else
    echo "ℹ️  No existing containers found"
fi

# Step 3: Pull the latest images
echo "⬇️  Pulling latest images..."
echo "🔍 Target image: ${IMAGE_NAME}:${IMAGE_TAG}"

# Check if the specific image exists in the registry first
echo "🔍 Checking if image exists in registry..."
docker manifest inspect "${IMAGE_NAME}:${IMAGE_TAG}" > /dev/null 2>&1 || {
    echo "❌ Image ${IMAGE_NAME}:${IMAGE_TAG} not found in registry"
    echo "🔍 Available images in registry (checking latest):"
    docker search "${IMAGE_NAME}" 2>/dev/null || true
    
    # Try to pull the latest tag as fallback
    echo "🔄 Attempting to use 'latest' tag as fallback..."
    IMAGE_TAG="latest"
    echo "📝 Updated target image: ${IMAGE_NAME}:${IMAGE_TAG}"
    
    # Recreate docker-compose file with updated tag
    sed -i "s|image: ${IMAGE_NAME}:.*|image: ${IMAGE_NAME}:${IMAGE_TAG}|" ${COMPOSE_FILE}
}

docker compose -f ${COMPOSE_FILE} -p ${PROJECT_NAME} pull || {
    echo "❌ Failed to pull images. Checking if images exist locally..."
    
    # List all images with this repository name
    echo "🔍 Local images for repository ${IMAGE_NAME}:"
    docker images "${IMAGE_NAME}" 2>/dev/null || echo "No local images found for ${IMAGE_NAME}"
    
    # Check if specific tag exists locally
    if docker images "${IMAGE_NAME}:${IMAGE_TAG}" --format "{{.Repository}}:{{.Tag}}" | grep -q "${IMAGE_NAME}:${IMAGE_TAG}"; then
        echo "ℹ️  Using local image ${IMAGE_NAME}:${IMAGE_TAG}"
    else
        echo "❌ Image ${IMAGE_NAME}:${IMAGE_TAG} not found locally either"
        echo "🔍 Listing all available local images:"
        docker images | head -20
        exit 1
    fi
}

# Step 4: Start the application
echo "🚀 Starting application services..."
docker compose -f ${COMPOSE_FILE} -p ${PROJECT_NAME} up -d

# Step 5: Wait for services to be healthy
echo "⏳ Waiting for services to be ready..."
max_wait=180  # 3 minutes
wait_time=0

while [ $wait_time -lt $max_wait ]; do
    if docker compose -f ${COMPOSE_FILE} -p ${PROJECT_NAME} ps --filter "health=healthy" | grep -q hamalog-app; then
        echo "✅ Application is healthy and ready!"
        break
    fi
    
    echo "⏳ Services still starting... (${wait_time}s/${max_wait}s)"
    sleep 10
    wait_time=$((wait_time + 10))
    
    # Show service status
    docker compose -f ${COMPOSE_FILE} -p ${PROJECT_NAME} ps
done

if [ $wait_time -ge $max_wait ]; then
    echo "❌ Services did not become healthy within ${max_wait} seconds"
    echo "📋 Final service status:"
    docker compose -f ${COMPOSE_FILE} -p ${PROJECT_NAME} ps
    echo "📋 Application logs:"
    docker compose -f ${COMPOSE_FILE} -p ${PROJECT_NAME} logs hamalog-app --tail=100
    exit 1
fi

# Step 6: Final verification
echo "🔍 Final deployment verification..."
docker compose -f ${COMPOSE_FILE} -p ${PROJECT_NAME} ps

echo "🎉 Deployment completed successfully!"
echo "✅ Hamalog application is now running"
echo "🌐 Application should be accessible at: http://localhost:8080"
echo "📊 Health check endpoint: http://localhost:8080/actuator/health"

# Cleanup temporary files
rm -f ${COMPOSE_FILE}
echo "🧹 Cleanup completed"