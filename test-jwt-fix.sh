#!/bin/bash

# Test script to verify the JWT_SECRET fix for Docker container
# This script tests that removing ENV JWT_SECRET="" from Dockerfile fixes the issue

set -e

echo "🔧 Testing JWT_SECRET fix for Docker container..."

# Generate a test JWT secret
TEST_JWT_SECRET="dGVzdC1zZWNyZXQtZm9yLWp3dC12YWxpZGF0aW9uLXRlc3QtZml4"
echo "Generated test JWT secret: $TEST_JWT_SECRET"

# Build the application JAR
echo "📦 Building application JAR..."
./gradlew clean bootJar

# Build Docker image with the fix
echo "🐳 Building Docker image with JWT_SECRET fix..."
docker build -t hamalog-jwt-fix-test .

# Test 1: Run container WITHOUT JWT_SECRET environment variable
# This should now use the fallback value from application properties
echo ""
echo "🧪 Test 1: Running container WITHOUT JWT_SECRET (should use fallback from application properties)..."
docker run --rm --name hamalog-test-no-jwt \
    -e SPRING_PROFILES_ACTIVE=local \
    -d \
    -p 8081:8080 \
    hamalog-jwt-fix-test

# Give the application time to start and show startup logs
echo "⏳ Waiting for application to start (30 seconds)..."
sleep 30

# Check container logs to verify JWT_SECRET handling
echo "📋 Checking container startup logs..."
docker logs hamalog-test-no-jwt

# Check if application started successfully
echo "🔍 Testing application connectivity..."
if curl -f -s -m 10 http://localhost:8081/ >/dev/null 2>&1 || \
   curl -f -s -m 10 http://localhost:8081/actuator/health >/dev/null 2>&1; then
    echo "✅ Test 1 PASSED: Application started successfully with fallback JWT secret"
    TEST1_RESULT="PASSED"
else
    echo "❌ Test 1 FAILED: Application failed to start with fallback JWT secret"
    echo "📋 Application logs:"
    docker logs hamalog-test-no-jwt --tail 50
    TEST1_RESULT="FAILED"
fi

# Stop the test container
docker stop hamalog-test-no-jwt || true
echo "🧹 Cleaned up test container"

echo "⏳ Waiting for port cleanup..."
sleep 10

# Test 2: Run container WITH JWT_SECRET environment variable
echo ""
echo "🧪 Test 2: Running container WITH JWT_SECRET environment variable..."
docker run --rm --name hamalog-test-with-jwt \
    -e SPRING_PROFILES_ACTIVE=local \
    -e JWT_SECRET="$TEST_JWT_SECRET" \
    -d \
    -p 8082:8080 \
    hamalog-jwt-fix-test

# Give the application time to start
echo "⏳ Waiting for application to start (30 seconds)..."
sleep 30

# Check container logs
echo "📋 Checking container startup logs..."
docker logs hamalog-test-with-jwt

# Check if application started successfully
echo "🔍 Testing application connectivity..."
if curl -f -s -m 10 http://localhost:8082/ >/dev/null 2>&1 || \
   curl -f -s -m 10 http://localhost:8082/actuator/health >/dev/null 2>&1; then
    echo "✅ Test 2 PASSED: Application started successfully with custom JWT_SECRET"
    TEST2_RESULT="PASSED"
else
    echo "❌ Test 2 FAILED: Application failed to start with custom JWT_SECRET"
    echo "📋 Application logs:"
    docker logs hamalog-test-with-jwt --tail 50
    TEST2_RESULT="FAILED"
fi

# Stop the test container
docker stop hamalog-test-with-jwt || true
echo "🧹 Cleaned up test container"

# Clean up Docker image
echo "🧹 Cleaning up test Docker image..."
docker rmi hamalog-jwt-fix-test || true

echo ""
echo "🎉 JWT_SECRET fix testing completed!"
echo ""
echo "📊 Test Results Summary:"
echo "- Test 1 (No JWT_SECRET env var, use fallback): $TEST1_RESULT"
echo "- Test 2 (Custom JWT_SECRET env var): $TEST2_RESULT"
echo ""

if [ "$TEST1_RESULT" = "PASSED" ] && [ "$TEST2_RESULT" = "PASSED" ]; then
    echo "✅ ALL TESTS PASSED! The JWT_SECRET fix is working correctly."
    echo ""
    echo "Key changes made:"
    echo "- Removed 'ENV JWT_SECRET=\"\"' from Dockerfile that was overriding fallback values"
    echo "- Application now properly uses fallback values when JWT_SECRET is unset"
    echo "- Application properly uses environment variables when JWT_SECRET is provided"
    echo ""
    echo "🚀 The Docker container should now work correctly in production!"
    exit 0
else
    echo "❌ Some tests failed. Please check the logs above for details."
    exit 1
fi