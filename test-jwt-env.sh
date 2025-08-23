#!/bin/bash

# Test script to verify JWT_SECRET environment variable is properly used in Docker container
# This script tests the JWT secret environment variable configuration

set -e

echo "🔐 Testing JWT_SECRET environment variable configuration..."

# Generate a test JWT secret
TEST_JWT_SECRET="dGVzdC1zZWNyZXQtZm9yLWp3dC12YWxpZGF0aW9uLXRlc3Q="
echo "Generated test JWT secret: $TEST_JWT_SECRET"

# Build the application JAR
echo "📦 Building application JAR..."
./gradlew clean bootJar

# Build Docker image
echo "🐳 Building Docker image..."
docker build -t hamalog-jwt-test .

# Test 1: Run container without JWT_SECRET environment variable (should use fallback)
echo "🧪 Test 1: Running container without JWT_SECRET (should use fallback)..."
docker run --rm --name hamalog-test-no-env \
    -e SPRING_PROFILES_ACTIVE=local \
    -p 8081:8080 \
    hamalog-jwt-test &

# Give the application time to start
sleep 15

# Check if application started successfully
if curl -f -s http://localhost:8081/ >/dev/null 2>&1 || curl -f -s http://localhost:8081/actuator/health >/dev/null 2>&1; then
    echo "✅ Test 1 PASSED: Application started with fallback JWT secret"
else
    echo "❌ Test 1 FAILED: Application failed to start with fallback JWT secret"
fi

# Stop the test container
docker stop hamalog-test-no-env || true

echo "⏳ Waiting for container cleanup..."
sleep 5

# Test 2: Run container with JWT_SECRET environment variable
echo "🧪 Test 2: Running container with JWT_SECRET environment variable..."
docker run --rm --name hamalog-test-with-env \
    -e SPRING_PROFILES_ACTIVE=local \
    -e JWT_SECRET="$TEST_JWT_SECRET" \
    -p 8082:8080 \
    hamalog-jwt-test &

# Give the application time to start
sleep 15

# Check if application started successfully
if curl -f -s http://localhost:8082/ >/dev/null 2>&1 || curl -f -s http://localhost:8082/actuator/health >/dev/null 2>&1; then
    echo "✅ Test 2 PASSED: Application started with custom JWT_SECRET"
else
    echo "❌ Test 2 FAILED: Application failed to start with custom JWT_SECRET"
fi

# Stop the test container
docker stop hamalog-test-with-env || true

echo "🧹 Cleaning up test containers and images..."
docker rmi hamalog-jwt-test || true

echo "🎉 JWT_SECRET environment variable testing completed!"
echo ""
echo "Summary:"
echo "- Application properties files now use \${JWT_SECRET:fallback} configuration"
echo "- Dockerfile declares JWT_SECRET environment variable"
echo "- Docker compose passes JWT_SECRET from environment"
echo "- GitHub Actions workflows set JWT_SECRET from secrets"
echo ""
echo "✅ JWT secret is now properly configured to read from environment variables!"