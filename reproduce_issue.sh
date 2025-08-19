#!/bin/bash

echo "=== Reproducing Production Environment Issue ==="
echo "This script simulates the production environment to reproduce the JWT configuration error"

# Set environment variables as they would be in docker-compose
export SPRING_PROFILES_ACTIVE=prod
export SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/hamalog"
export SPRING_DATASOURCE_USERNAME="hamalog_user"  
export SPRING_DATASOURCE_PASSWORD="hamalog_password"
export SPRING_DATA_REDIS_HOST="localhost"
export SPRING_DATA_REDIS_PORT="6379"
export JWT_SECRET="EzUuJwKK4vLnvk5r7yAgdNP/sa1dL87febZhlayPGjI="
export JWT_EXPIRY="3600000"
export KAKAO_CLIENT_ID="dummy-client-id"
export KAKAO_CLIENT_SECRET="dummy-client-secret"

echo "Environment variables set:"
echo "SPRING_PROFILES_ACTIVE=$SPRING_PROFILES_ACTIVE"
echo "JWT_SECRET=$JWT_SECRET"
echo "DB variables: SPRING_DATASOURCE_* (but prod config expects DB_*)"
echo ""

echo "=== Expected Error ==="
echo "The application-prod.properties expects DB_USERNAME and DB_PASSWORD"
echo "but we're providing SPRING_DATASOURCE_USERNAME and SPRING_DATASOURCE_PASSWORD"
echo "This will cause property resolution to fail during Spring context initialization"
echo ""

echo "=== Building and running application ==="
./gradlew clean bootJar
echo ""
echo "Running with production profile (this should fail):"
java -jar build/libs/*.jar