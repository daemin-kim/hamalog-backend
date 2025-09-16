#!/bin/bash

# Redis Health Check Test Script
# This script verifies the Redis configuration fix resolves the 503 health check error

echo "=== Redis Health Check Test ==="
echo "Date: $(date)"
echo

# Function to check if a service is running
check_service() {
    local service_name=$1
    local port=$2
    echo "Checking $service_name on port $port..."
    
    if command -v nc >/dev/null 2>&1; then
        if nc -z localhost $port; then
            echo "✅ $service_name is running on port $port"
            return 0
        else
            echo "❌ $service_name is not accessible on port $port"
            return 1
        fi
    else
        echo "⚠️  netcat not available, skipping port check"
        return 0
    fi
}

# Check if Docker is running
if ! command -v docker >/dev/null 2>&1; then
    echo "❌ Docker is not installed or not in PATH"
    exit 1
fi

if ! docker info >/dev/null 2>&1; then
    echo "❌ Docker is not running"
    exit 1
fi

echo "✅ Docker is running"
echo

# Check if containers are running
echo "Checking Docker containers..."
if docker-compose ps | grep -q "Up"; then
    echo "✅ Docker Compose services are running"
    docker-compose ps
    echo
else
    echo "ℹ️  Docker Compose services are not running"
    echo "Starting services with docker-compose up -d..."
    if docker-compose up -d; then
        echo "✅ Services started successfully"
        echo "Waiting 30 seconds for services to be ready..."
        sleep 30
    else
        echo "❌ Failed to start services"
        exit 1
    fi
fi

# Check individual services
echo "=== Service Connectivity Check ==="
check_service "Redis" 6379
redis_status=$?

check_service "MySQL" 3306
mysql_status=$?

check_service "Hamalog App" 8080
app_status=$?

echo

# Test the health endpoint
echo "=== Health Endpoint Test ==="
if [ $app_status -eq 0 ]; then
    HEALTH_URL="http://localhost:8080/actuator/health"
    echo "Testing health endpoint: $HEALTH_URL"
    
    # Get HTTP status and response
    HTTP_STATUS=$(curl -s -o /tmp/health_response.txt -w "%{http_code}" "$HEALTH_URL" 2>/dev/null)
    CURL_EXIT_CODE=$?
    
    if [ $CURL_EXIT_CODE -ne 0 ]; then
        echo "❌ Failed to connect to health endpoint"
        echo "   Application may still be starting up"
    else
        echo "HTTP Status: $HTTP_STATUS"
        
        case $HTTP_STATUS in
            200)
                echo "✅ SUCCESS: Health endpoint is working correctly"
                echo "Response:"
                cat /tmp/health_response.txt | jq . 2>/dev/null || cat /tmp/health_response.txt
                ;;
            503)
                echo "❌ FAILED: Service Unavailable (503) - Redis connection likely still failing"
                echo "Response:"
                cat /tmp/health_response.txt
                ;;
            *)
                echo "⚠️  Unexpected HTTP status: $HTTP_STATUS"
                echo "Response:"
                cat /tmp/health_response.txt
                ;;
        esac
    fi
    
    # Clean up
    rm -f /tmp/health_response.txt
else
    echo "❌ Cannot test health endpoint - application is not accessible"
fi

echo
echo "=== Redis Connection Test ==="
if [ $redis_status -eq 0 ]; then
    echo "Testing Redis connection directly..."
    if docker-compose exec -T redis redis-cli ping 2>/dev/null | grep -q PONG; then
        echo "✅ Redis is responding to PING"
    else
        echo "❌ Redis is not responding to PING"
    fi
    
    # Check Redis logs for any connection issues
    echo "Recent Redis logs:"
    docker-compose logs --tail=10 redis
else
    echo "❌ Cannot test Redis - service not accessible"
fi

echo
echo "=== Application Logs (Last 20 lines) ==="
if docker-compose ps | grep -q hamalog-app; then
    docker-compose logs --tail=20 hamalog-app
else
    echo "❌ Hamalog application container not found"
fi

echo
echo "=== Test Summary ==="
if [ $HTTP_STATUS = "200" ]; then
    echo "✅ OVERALL: Health check is now working correctly"
    echo "   The Redis configuration fix was successful"
elif [ $HTTP_STATUS = "503" ]; then
    echo "❌ OVERALL: Health check still failing with 503"
    echo "   Redis connection issue may persist - check logs above"
else
    echo "⚠️  OVERALL: Unexpected result - manual investigation needed"
fi

echo
echo "=== End Test ==="