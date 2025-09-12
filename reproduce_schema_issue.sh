#!/bin/bash
# Script to reproduce SchemaManagementException in deployment environment

echo "=== Reproducing SchemaManagementException ==="
echo "This script simulates production environment without proper encryption key"

# Simulate production environment without encryption key
export SPRING_PROFILES_ACTIVE=prod
export SPRING_DATASOURCE_URL="jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
export SPRING_DATASOURCE_USERNAME="sa"
export SPRING_DATASOURCE_PASSWORD=""
# Intentionally NOT setting HAMALOG_ENCRYPTION_KEY to reproduce the error

echo "Environment setup:"
echo "- Profile: $SPRING_PROFILES_ACTIVE"
echo "- Database: H2 (simulating production database)"
echo "- Encryption Key: NOT SET (this should cause the error)"
echo ""

# Try to start the application - this should fail with SchemaManagementException
echo "Attempting to start application (this should fail)..."
./gradlew bootRun --args="--spring.profiles.active=prod" --quiet

echo ""
echo "=== Expected Error ==="
echo "The application should fail to start because:"
echo "1. DataEncryptionUtil throws IllegalStateException when HAMALOG_ENCRYPTION_KEY is missing in prod"
echo "2. This prevents encryption converter beans from being created"
echo "3. Schema validation fails because converters are required for entity initialization"