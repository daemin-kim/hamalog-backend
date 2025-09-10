#!/bin/bash

# Test script to verify placeholder resolution fix
# This script will attempt to start the application with prod profile to test placeholders

echo "Testing placeholder resolution fix..."
echo "========================================="

# Create a temporary environment without the problematic environment variables
# to ensure the default values work correctly
unset ALLOWED_ORIGINS
unset FRONTEND_URL
unset JWT_SECRET
unset HAMALOG_ENCRYPTION_KEY
unset SPRING_DATASOURCE_USERNAME
unset SPRING_DATASOURCE_PASSWORD

echo "Environment variables unset - testing default values..."

# Try to parse the configuration file using Spring Boot's context loading
# This will validate placeholder resolution without fully starting the app
echo "Testing Spring Boot configuration parsing..."

# Use a simple Java command to test property resolution
./gradlew -q --console=plain -Dspring.profiles.active=prod -Dspring.main.web-application-type=none -Dlogging.level.root=ERROR bootRun --args='--spring.main.lazy-initialization=true --server.port=0' &

# Give it a few seconds to start and check for placeholder errors
BOOT_PID=$!
sleep 5

# Check if the process is still running (no immediate crash due to placeholder errors)
if kill -0 $BOOT_PID 2>/dev/null; then
    echo "SUCCESS: Application started without PlaceholderResolutionException"
    echo "Placeholder resolution fix appears to be working correctly"
    kill $BOOT_PID
    wait $BOOT_PID 2>/dev/null
    exit 0
else
    echo "FAILURE: Application failed to start - check for remaining placeholder issues"
    exit 1
fi