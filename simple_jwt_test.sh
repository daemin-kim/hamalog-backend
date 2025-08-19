#!/bin/bash

echo "=== Simple JWT Configuration Test ==="

# Set production environment variables
export SPRING_PROFILES_ACTIVE=prod
export SPRING_DATASOURCE_URL="jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1"
export SPRING_DATASOURCE_USERNAME="sa"
export SPRING_DATASOURCE_PASSWORD=""
export JWT_SECRET="EzUuJwKK4vLnvk5r7yAgdNP/sa1dL87febZhlayPGjI="
export JWT_EXPIRY="3600000"
export KAKAO_CLIENT_ID="dummy-client-id"
export KAKAO_CLIENT_SECRET="dummy-client-secret"
export LOG_DIR="./test-logs"
mkdir -p ./test-logs

echo "Testing with production profile and fixed environment variables..."
echo "JWT_SECRET: $JWT_SECRET"
echo "SPRING_DATASOURCE_USERNAME: $SPRING_DATASOURCE_USERNAME"

# Start application and capture output
echo "Starting application (will stop after a few seconds)..."
java -jar build/libs/Hamalog-0.0.1-SNAPSHOT.jar > startup.log 2>&1 &
APP_PID=$!

echo "Application PID: $APP_PID"
echo "Waiting 8 seconds for startup..."
sleep 8

# Kill the application
kill $APP_PID 2>/dev/null
wait $APP_PID 2>/dev/null

echo ""
echo "=== Checking startup log for errors ==="
if grep -i "jwt.*비밀키.*설정되지.*않았습니다" startup.log; then
    echo "❌ FAILED: JWT secret configuration error found!"
elif grep -i "illegalstateexception.*jwt" startup.log; then
    echo "❌ FAILED: JWT IllegalStateException found!"
elif grep -i "error.*creating.*bean.*jwt" startup.log; then
    echo "❌ FAILED: JWT bean creation error found!"
elif grep -i "started.*hamalogapplication" startup.log; then
    echo "✅ SUCCESS: Application started successfully!"
    echo "✅ JWT configuration fix is working!"
else
    echo "⚠️  Checking for other startup indicators..."
    if grep -i "completed initialization" startup.log || grep -i "tomcat.*started" startup.log; then
        echo "✅ SUCCESS: Application appears to have started correctly!"
    else
        echo "❌ Unclear result. Showing last 10 lines of log:"
        tail -10 startup.log
    fi
fi

echo ""
echo "=== Full log analysis ==="
echo "Errors in log:"
grep -i "error\|exception\|failed" startup.log | head -5

echo ""
echo "JWT-related messages:"
grep -i "jwt\|token" startup.log | head -3

# Cleanup
rm -f startup.log