#!/bin/bash

# Script to reproduce the OAuth2 "Client id must not be empty" issue
# This simulates the production environment where environment variables are set to empty strings

echo "=== Reproducing OAuth2 Issue ==="
echo "Setting empty environment variables to simulate production issue..."

# Set environment variables to empty strings (this causes the issue)
export KAKAO_CLIENT_ID=""
export KAKAO_CLIENT_SECRET=""
export KAKAO_REDIRECT_URI=""

# Set other required environment variables for the application to start
export SPRING_DATASOURCE_URL="jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
export SPRING_DATASOURCE_USERNAME="sa"
export SPRING_DATASOURCE_PASSWORD=""
export JWT_SECRET="EzUuJwKK4vLnvk5r7yAgdNP/sa1dL87febZhlayPGjI="

echo "Environment variables set:"
echo "KAKAO_CLIENT_ID='$KAKAO_CLIENT_ID'"
echo "KAKAO_CLIENT_SECRET='$KAKAO_CLIENT_SECRET'"
echo "KAKAO_REDIRECT_URI='$KAKAO_REDIRECT_URI'"
echo ""

echo "Attempting to build and run the application..."
echo "Expected error: IllegalStateException: Client id of registration 'kakao' must not be empty"
echo ""

# Try to build the application
./gradlew build --no-daemon --quiet

if [ $? -eq 0 ]; then
    echo "Build succeeded. Attempting to run the application..."
    # Try to run the application (this should fail with the OAuth2 error)
    timeout 30s ./gradlew bootRun --no-daemon --quiet 2>&1 | grep -E "(IllegalStateException|Client id|must not be empty|ERROR|WARN.*oauth2|WARN.*kakao)"
    
    if [ $? -eq 124 ]; then
        echo "Application startup timed out (30 seconds) - this might indicate the issue was resolved"
    fi
else
    echo "Build failed. Check the output above for errors."
fi

echo ""
echo "=== Reproduction script completed ==="