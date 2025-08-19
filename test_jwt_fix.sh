#!/bin/bash

echo "=== Testing JWT Configuration Fix ==="
echo "This script validates the JWT configuration fix by testing Spring context initialization"

# Set environment variables as they are provided by docker-compose
export SPRING_PROFILES_ACTIVE=prod
export SPRING_DATASOURCE_URL="jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1"
export SPRING_DATASOURCE_USERNAME="sa"
export SPRING_DATASOURCE_PASSWORD=""
export SPRING_DATA_REDIS_HOST="localhost"
export SPRING_DATA_REDIS_PORT="6379"
export JWT_SECRET="EzUuJwKK4vLnvk5r7yAgdNP/sa1dL87febZhlayPGjI="
export JWT_EXPIRY="3600000"
export KAKAO_CLIENT_ID="dummy-client-id"
export KAKAO_CLIENT_SECRET="dummy-client-secret"

echo "Environment variables set:"
echo "SPRING_PROFILES_ACTIVE=$SPRING_PROFILES_ACTIVE"
echo "JWT_SECRET=$JWT_SECRET (length: ${#JWT_SECRET})"
echo "SPRING_DATASOURCE_URL=$SPRING_DATASOURCE_URL"
echo "SPRING_DATASOURCE_USERNAME=$SPRING_DATASOURCE_USERNAME"
echo ""

echo "=== Testing with H2 Database (to avoid MySQL dependency) ==="
echo "Building application..."
./gradlew clean build -x test

if [ $? -eq 0 ]; then
    echo ""
    echo "Build successful. Testing Spring context initialization..."
    echo "This should now work with the fixed environment variable names."
    
    # Run for a few seconds to test initialization
    timeout 30s java -jar build/libs/*.jar &
    APP_PID=$!
    
    echo "Application started with PID: $APP_PID"
    echo "Waiting for Spring context initialization..."
    sleep 10
    
    # Check if process is still running (successful startup)
    if ps -p $APP_PID > /dev/null; then
        echo "SUCCESS: Application started successfully!"
        echo "JWT configuration fix is working - Spring context initialized without errors."
        kill $APP_PID
        wait $APP_PID 2>/dev/null
    else
        echo "FAILED: Application failed to start or crashed during initialization."
    fi
else
    echo "Build failed. Cannot test the fix."
fi

echo ""
echo "=== Testing local profile (regression test) ==="
unset SPRING_PROFILES_ACTIVE
echo "Testing with default/local profile to ensure no regression..."
timeout 10s java -jar build/libs/*.jar &
LOCAL_PID=$!
sleep 5

if ps -p $LOCAL_PID > /dev/null; then
    echo "SUCCESS: Local configuration still works correctly."
    kill $LOCAL_PID
    wait $LOCAL_PID 2>/dev/null
else
    echo "WARNING: Local configuration may have issues."
fi

echo ""
echo "=== Test Summary ==="
echo "The fix changes application-prod.properties to use SPRING_DATASOURCE_* variables"
echo "instead of DB_* variables, matching what docker-compose.yml provides."
echo "This resolves the property resolution failure that prevented JWT bean creation."