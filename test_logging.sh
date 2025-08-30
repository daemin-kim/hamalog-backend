#!/bin/bash

# Test script to observe current logging behavior
echo "Starting application to observe logging..."

# Build and run the application in background for a short time
./gradlew bootRun --args='--server.port=8081 --spring.profiles.active=local' &
APP_PID=$!

# Wait for application to start
echo "Waiting for application to start..."
sleep 10

# Make a test request to generate logs
echo "Making test requests to generate logs..."
curl -X GET http://localhost:8081/actuator/health || echo "Health check failed - app might not be ready"

# Wait a bit more for logs to be generated
sleep 5

# Kill the application
echo "Stopping application..."
kill $APP_PID 2>/dev/null || echo "Application already stopped"

echo "Test completed. Check the console output above for current logging format."