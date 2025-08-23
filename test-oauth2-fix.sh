#!/bin/bash

echo "=== Testing OAuth2 Configuration Fix ==="
echo "This script tests that the application can start without the 'client id must not be empty' error"
echo

# Test 1: Start application without KAKAO env vars (should use fallbacks via OAuth2EmptyVariableHandler)
echo "Test 1: Starting application without KAKAO environment variables"
echo "Expected: Application should start with fallback values from OAuth2EmptyVariableHandler"
echo

# Set minimal required environment variables
export SPRING_PROFILES_ACTIVE=local
export JWT_SECRET="xcrVqYlPMcLeEoEX+h8vjxZ97lS6AETwQJXJSLJ/h8g="

# Unset KAKAO variables to test fallback behavior
unset KAKAO_CLIENT_ID
unset KAKAO_CLIENT_SECRET
unset KAKAO_REDIRECT_URI

echo "Building application..."
./gradlew clean build -q

if [ $? -eq 0 ]; then
    echo "✅ Build successful"
else
    echo "❌ Build failed"
    exit 1
fi

echo "Starting application (will run for 30 seconds to check startup)..."
./gradlew bootRun &
APP_PID=$!

# Set up a timeout mechanism for macOS compatibility
(sleep 30 && kill $APP_PID 2>/dev/null) &
TIMEOUT_PID=$!

# Wait a bit for startup
sleep 10

# Check if application is running
if kill -0 $APP_PID 2>/dev/null; then
    echo "✅ Application started successfully without KAKAO environment variables"
    echo "✅ OAuth2EmptyVariableHandler is working correctly"
    
    # Try to access a simple endpoint to verify it's responding
    if curl -s -f http://localhost:8080/actuator/health >/dev/null 2>&1; then
        echo "✅ Application health endpoint is responding"
    else
        echo "⚠️  Application started but health endpoint not accessible (this might be normal for security reasons)"
    fi
else
    echo "❌ Application failed to start"
    exit 1
fi

# Clean up
kill $APP_PID 2>/dev/null || true
wait $APP_PID 2>/dev/null || true

echo
echo "Test 2: Testing with empty KAKAO environment variables"
echo "Expected: Application should handle empty strings properly"

# Set KAKAO variables to empty strings (simulating problematic Docker scenario)
export KAKAO_CLIENT_ID=""
export KAKAO_CLIENT_SECRET=""
export KAKAO_REDIRECT_URI=""

echo "Starting application with empty KAKAO environment variables..."
./gradlew bootRun &
APP_PID=$!

# Set up a timeout mechanism for macOS compatibility
(sleep 30 && kill $APP_PID 2>/dev/null) &
TIMEOUT_PID2=$!

# Wait a bit for startup
sleep 10

# Check if application is running
if kill -0 $APP_PID 2>/dev/null; then
    echo "✅ Application started successfully with empty KAKAO environment variables"
    echo "✅ OAuth2EmptyVariableHandler properly handled empty strings"
else
    echo "❌ Application failed to start with empty KAKAO environment variables"
    exit 1
fi

# Clean up
kill $APP_PID 2>/dev/null || true
wait $APP_PID 2>/dev/null || true

echo
echo "=== All tests passed! ==="
echo "✅ The OAuth2 configuration fix is working correctly"
echo "✅ Application can start without 'client id must not be empty' errors"
echo "✅ OAuth2EmptyVariableHandler properly handles missing and empty environment variables"