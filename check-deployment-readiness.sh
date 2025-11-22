#!/bin/bash

# Hamalog Pre-Deployment Check Script
# This script verifies server is ready for deployment

set -e

DEPLOY_DIR="${HOME}/hamalog-deploy"
ENV_FILE="${DEPLOY_DIR}/.env.prod"

echo "================================================"
echo "Hamalog Pre-Deployment Check"
echo "================================================"
echo ""

# Check deployment directory
echo "1. Checking deployment directory..."
if [ ! -d "$DEPLOY_DIR" ]; then
    echo "❌ Deployment directory not found: $DEPLOY_DIR"
    echo "Creating deployment directory..."
    mkdir -p "$DEPLOY_DIR"
    echo "✅ Created: $DEPLOY_DIR"
else
    echo "✅ Deployment directory exists: $DEPLOY_DIR"
fi
echo ""

# Check .env.prod file
echo "2. Checking .env.prod file..."
if [ ! -f "$ENV_FILE" ]; then
    echo "❌ .env.prod not found!"
    echo ""
    echo "To create .env.prod:"
    echo "1. Copy generate-credentials.sh to server"
    echo "2. Run: ./generate-credentials.sh"
    echo "3. Edit .env.prod and set KAKAO credentials"
    echo ""
    exit 1
else
    echo "✅ .env.prod exists"

    # Check file permissions
    PERMS=$(stat -f "%OLp" "$ENV_FILE" 2>/dev/null || stat -c "%a" "$ENV_FILE" 2>/dev/null)
    if [ "$PERMS" != "600" ]; then
        echo "⚠️  Warning: .env.prod permissions are $PERMS (should be 600)"
        echo "Fixing permissions..."
        chmod 600 "$ENV_FILE"
        echo "✅ Fixed: chmod 600 .env.prod"
    else
        echo "✅ File permissions correct: 600"
    fi
fi
echo ""

# Check required environment variables
echo "3. Checking environment variables in .env.prod..."
set -a
source "$ENV_FILE"
set +a

REQUIRED_VARS=(
    "JWT_SECRET"
    "HAMALOG_ENCRYPTION_KEY"
    "KAKAO_CLIENT_ID"
    "KAKAO_CLIENT_SECRET"
    "KAKAO_REDIRECT_URI"
    "DB_NAME"
    "DB_USERNAME"
    "DB_PASSWORD"
    "MYSQL_ROOT_PASSWORD"
    "SPRING_DATA_REDIS_PASSWORD"
    "SPRING_PROFILES_ACTIVE"
    "FRONTEND_URL"
    "ALLOWED_ORIGINS"
)

ALL_SET=true
MISSING_VARS=""

for var in "${REQUIRED_VARS[@]}"; do
    VALUE="${!var}"
    if [ -z "$VALUE" ]; then
        echo "❌ Missing: $var"
        ALL_SET=false
        MISSING_VARS="$MISSING_VARS $var"
    else
        # Mask sensitive values
        MASKED="${VALUE:0:3}***"
        LEN=${#VALUE}

        # Validate minimum length for security
        MIN_LEN=16
        if [[ "$var" == *"SECRET"* ]] || [[ "$var" == *"PASSWORD"* ]] || [[ "$var" == "JWT_SECRET" ]] || [[ "$var" == "HAMALOG_ENCRYPTION_KEY" ]]; then
            if [ $LEN -lt $MIN_LEN ]; then
                echo "⚠️  Warning: $var is too short (length: $LEN, minimum: $MIN_LEN)"
                ALL_SET=false
            else
                echo "✅ $var = $MASKED (length: $LEN)"
            fi
        else
            echo "✅ $var = $MASKED (length: $LEN)"
        fi
    fi
done
echo ""

if [ "$ALL_SET" = false ]; then
    echo "================================================"
    echo "❌ Pre-deployment check FAILED"
    echo "================================================"
    echo ""
    if [ -n "$MISSING_VARS" ]; then
        echo "Missing variables:$MISSING_VARS"
        echo ""
        echo "Edit .env.prod and set all required variables:"
        echo "nano $ENV_FILE"
    fi
    exit 1
fi

# Check Docker
echo "4. Checking Docker..."
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed"
    exit 1
else
    echo "✅ Docker is installed"
    docker --version
fi
echo ""

# Check Docker Compose
echo "5. Checking Docker Compose..."
if ! docker compose version &> /dev/null; then
    echo "❌ Docker Compose is not installed"
    exit 1
else
    echo "✅ Docker Compose is installed"
    docker compose version
fi
echo ""

# Check if docker-compose.yml exists
echo "6. Checking docker-compose.yml..."
if [ ! -f "$DEPLOY_DIR/docker-compose.yml" ]; then
    echo "⚠️  Warning: docker-compose.yml not found in $DEPLOY_DIR"
    echo "   It will be copied during deployment"
else
    echo "✅ docker-compose.yml exists"
fi
echo ""

# Check network connectivity
echo "7. Checking network connectivity..."
if curl -s -o /dev/null -w "%{http_code}" http://www.google.com | grep -q "200"; then
    echo "✅ Internet connection OK"
else
    echo "⚠️  Warning: Cannot reach internet"
fi
echo ""

# Summary
echo "================================================"
echo "✅ Pre-deployment check PASSED"
echo "================================================"
echo ""
echo "Server is ready for deployment!"
echo ""
echo "Next steps:"
echo "1. Commit and push your code to main branch"
echo "2. GitHub Actions will automatically deploy"
echo "3. Monitor deployment: docker-compose logs -f hamalog-app"
echo ""

