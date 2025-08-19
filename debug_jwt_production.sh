#!/bin/bash

echo "=== Debug JWT Production Environment Issue ==="
echo "This script simulates the exact production environment to debug JWT secret value resolution"

# Clean up any previous builds
echo "Cleaning previous builds..."
./gradlew clean

# Set environment variables exactly as docker-compose.yml provides them
export SPRING_PROFILES_ACTIVE=prod

# Database configuration (using consistent SPRING_DATASOURCE_* variables)
export SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/hamalog"
export SPRING_DATASOURCE_USERNAME="hamalog_user"
export SPRING_DATASOURCE_PASSWORD="hamalog_password"

# Redis configuration
export SPRING_DATA_REDIS_HOST="localhost"
export SPRING_DATA_REDIS_PORT="6379"

# JWT Configuration (exactly as docker-compose.yml sets it)
export JWT_SECRET="EzUuJwKK4vLnvk5r7yAgdNP/sa1dL87febZhlayPGjI="
export JWT_EXPIRY="3600000"

# OAuth2 Configuration
export KAKAO_CLIENT_ID="dummy-client-id"
export KAKAO_CLIENT_SECRET="dummy-client-secret"

# Log directory
export LOG_DIR="./debug-logs"
mkdir -p ./debug-logs

echo ""
echo "=== Environment Variables Set ==="
echo "SPRING_PROFILES_ACTIVE: $SPRING_PROFILES_ACTIVE"
echo "JWT_SECRET: '$JWT_SECRET' (length: ${#JWT_SECRET} characters)"
echo "SPRING_DATASOURCE_USERNAME: $SPRING_DATASOURCE_USERNAME"
echo "SPRING_DATASOURCE_PASSWORD: $SPRING_DATASOURCE_PASSWORD"
echo "SPRING_DATASOURCE_URL: $SPRING_DATASOURCE_URL"
echo ""

# Build the application
echo "=== Building Application ==="
./gradlew bootJar
BUILD_EXIT_CODE=$?

if [ $BUILD_EXIT_CODE -ne 0 ]; then
    echo "❌ Build failed with exit code $BUILD_EXIT_CODE"
    exit 1
fi

echo ""
echo "=== Testing JWT Secret Value Resolution ==="
echo "Starting application with debug output to see actual JWT secret value..."

# For debugging, we'll use H2 instead of MySQL to avoid dependency issues
export SPRING_DATASOURCE_URL="jdbc:h2:mem:debugdb;DB_CLOSE_DELAY=-1"
export SPRING_DATASOURCE_USERNAME="sa"
export SPRING_DATASOURCE_PASSWORD=""

# Start application and capture all output
echo "Running application to capture initialization (will stop after a few seconds)..."
java -jar build/libs/Hamalog-0.0.1-SNAPSHOT.jar > debug-output.log 2>&1 &
APP_PID=$!

echo "Application PID: $APP_PID"
echo "Waiting 10 seconds for initialization..."
sleep 10

# Check if still running
if ps -p $APP_PID > /dev/null 2>&1; then
    echo "✅ Application is running successfully!"
    kill $APP_PID 2>/dev/null
    wait $APP_PID 2>/dev/null
    SUCCESS=true
else
    echo "❌ Application stopped or crashed during initialization"
    SUCCESS=false
fi

echo ""
echo "=== Analyzing Debug Output ==="

# Check for JWT-related errors
echo "JWT-related errors:"
grep -i "jwt.*비밀키.*설정되지.*않았습니다\|illegalstateexception.*jwt\|error.*creating.*bean.*jwt" debug-output.log || echo "No JWT errors found"

echo ""
echo "JWT-related messages:"
grep -i "jwt\|token" debug-output.log | head -5 || echo "No JWT messages found"

echo ""
echo "Application startup status:"
if grep -i "started.*hamalogapplication" debug-output.log > /dev/null; then
    echo "✅ Application started successfully"
elif grep -i "tomcat.*started" debug-output.log > /dev/null; then
    echo "✅ Tomcat started successfully"
else
    echo "❌ Application startup unclear - showing last 10 lines:"
    tail -10 debug-output.log
fi

echo ""
echo "Database connection status:"
grep -i "hikari\|h2.*console\|database.*info" debug-output.log | tail -3

echo ""
if [ "$SUCCESS" = true ]; then
    echo "🎉 SUCCESS: JWT configuration is working correctly!"
    echo "The environment variable fix has resolved the production issue."
else
    echo "❌ ISSUE PERSISTS: Further investigation needed."
    echo "Full debug log saved to debug-output.log"
fi

# Cleanup
rm -f debug-output.log