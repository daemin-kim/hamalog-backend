#!/bin/bash

# Test script to verify HAMALOG_ENCRYPTION_KEY placeholder resolution fix
echo "=== Testing HAMALOG_ENCRYPTION_KEY placeholder resolution ==="

echo ""
echo "Test 1: Running application without HAMALOG_ENCRYPTION_KEY environment variable"
echo "This should NOT fail with placeholder resolution error anymore"
echo "Expected: Application should start and use empty key (handled by DataEncryptionUtil)"

# Unset the environment variable to simulate the error condition
unset HAMALOG_ENCRYPTION_KEY

# Try to start the application with prod profile (where the error occurred)
echo "Starting application with prod profile..."
timeout 10s ./gradlew bootRun --args='--spring.profiles.active=prod' > test-output-1.log 2>&1 &
APP_PID=$!

# Wait a bit for startup
sleep 8

# Check if process is still running (means it started successfully)
if kill -0 $APP_PID 2>/dev/null; then
    echo "✅ SUCCESS: Application started without placeholder resolution error"
    kill $APP_PID 2>/dev/null
    wait $APP_PID 2>/dev/null
else
    echo "❌ Application may have failed to start"
fi

# Check the log for specific errors
if grep -q "Could not resolve placeholder 'HAMALOG_ENCRYPTION_KEY'" test-output-1.log; then
    echo "❌ FAILED: Still getting placeholder resolution error"
    echo "Error details:"
    grep -A5 -B5 "Could not resolve placeholder" test-output-1.log
else
    echo "✅ SUCCESS: No placeholder resolution error found"
fi

echo ""
echo "Test 2: Running application with HAMALOG_ENCRYPTION_KEY set"
echo "This should work normally"

# Set a valid encryption key
export HAMALOG_ENCRYPTION_KEY="4MoGUKm/b9RXqFtUgxwK3BpVQF/RtZFMb4EwdzaRSlg="

echo "Starting application with encryption key set..."
timeout 10s ./gradlew bootRun --args='--spring.profiles.active=prod' > test-output-2.log 2>&1 &
APP_PID=$!

sleep 8

if kill -0 $APP_PID 2>/dev/null; then
    echo "✅ SUCCESS: Application started with encryption key"
    kill $APP_PID 2>/dev/null
    wait $APP_PID 2>/dev/null
else
    echo "❌ Application may have failed to start with encryption key"
fi

echo ""
echo "=== Test Results Summary ==="
echo "Check test-output-1.log and test-output-2.log for detailed startup logs"

# Cleanup
unset HAMALOG_ENCRYPTION_KEY