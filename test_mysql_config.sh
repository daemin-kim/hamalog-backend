#!/bin/bash

# Test script to validate MySQL configuration
# This simulates the environment variables that would be set in Docker deployment

echo "Testing MySQL configuration with environment variables..."

# Set MySQL environment variables (simulating Docker deployment)
export SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/testdb"
export SPRING_DATASOURCE_USERNAME="testuser"
export SPRING_DATASOURCE_PASSWORD="testpass"

# Build the application first (skip tests to avoid environment variable conflicts)
echo "Building application (skipping tests)..."
./gradlew clean bootJar -x test -q

if [ $? -eq 0 ]; then
    echo "Build successful."
    
    # Try to run the application with MySQL configuration
    # This will fail to connect to MySQL (since we don't have one running)
    # but it should show that it's trying to use the MySQL driver, not H2
    echo ""
    echo "Testing application startup with MySQL URL..."
    echo "Expected: Should attempt to use MySQL driver (not H2 driver)"
    echo "Will fail due to no MySQL server, but should show correct driver detection"
    echo ""
    
    # Run the JAR directly with timeout to prevent hanging
    timeout 15s java -jar build/libs/Hamalog-0.0.1-SNAPSHOT.jar 2>&1 | head -30
    
    echo ""
    echo "Test completed. Check above output for driver detection."
    echo "Key indicators:"
    echo "  - Should NOT see 'H2 Driver claims to not accept jdbcUrl, jdbc:mysql'"
    echo "  - Should see MySQL connection attempts or MySQL-related errors"
else
    echo "Build failed. Cannot proceed with test."
fi

# Clean up environment variables
unset SPRING_DATASOURCE_URL
unset SPRING_DATASOURCE_USERNAME
unset SPRING_DATASOURCE_PASSWORD