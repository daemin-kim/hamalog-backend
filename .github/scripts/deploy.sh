#!/bin/bash
set -e

echo "================================================"
echo "Hamalog Deployment - Environment Setup"
echo "================================================"

# Validate required secrets
MISSING_VARS=""

if [ -z "$JWT_SECRET" ]; then
  MISSING_VARS="$MISSING_VARS JWT_SECRET"
fi

if [ -z "$KAKAO_CLIENT_ID" ]; then
  MISSING_VARS="$MISSING_VARS KAKAO_CLIENT_ID"
fi

if [ -z "$KAKAO_CLIENT_SECRET" ]; then
  MISSING_VARS="$MISSING_VARS KAKAO_CLIENT_SECRET"
fi

if [ -z "$DB_PASSWORD" ]; then
  MISSING_VARS="$MISSING_VARS DB_PASSWORD"
fi

if [ -z "$MYSQL_ROOT_PASSWORD" ]; then
  MISSING_VARS="$MISSING_VARS MYSQL_ROOT_PASSWORD"
fi

if [ -z "$HAMALOG_ENCRYPTION_KEY" ]; then
  MISSING_VARS="$MISSING_VARS HAMALOG_ENCRYPTION_KEY"
fi

if [ -n "$MISSING_VARS" ]; then
  echo "❌ ERROR: Missing required secrets:$MISSING_VARS"
  exit 1
fi

echo "✅ All required secrets are set"

# Set default values
DB_NAME=${DB_NAME:-Hamalog}
DB_USERNAME=${DB_USERNAME:-hamalog_user}
JWT_EXPIRY=${JWT_EXPIRY:-900000}
JWT_REFRESH_TOKEN_EXPIRY=${JWT_REFRESH_TOKEN_EXPIRY:-604800000}
FRONTEND_URL=${FRONTEND_URL:-http://49.142.154.182:3000}
ALLOWED_ORIGINS=${ALLOWED_ORIGINS:-http://49.142.154.182:3000,http://49.142.154.182:8080}
SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-prod}

# Create environment file
cat > /tmp/.env.prod << ENVEOF
SPRING_PROFILES_ACTIVE=$SPRING_PROFILES_ACTIVE
JWT_SECRET=$JWT_SECRET
JWT_EXPIRY=$JWT_EXPIRY
JWT_REFRESH_TOKEN_EXPIRY=$JWT_REFRESH_TOKEN_EXPIRY
HAMALOG_ENCRYPTION_KEY=$HAMALOG_ENCRYPTION_KEY
KAKAO_CLIENT_ID=$KAKAO_CLIENT_ID
KAKAO_CLIENT_SECRET=$KAKAO_CLIENT_SECRET
KAKAO_REDIRECT_URI=$KAKAO_REDIRECT_URI
DB_NAME=$DB_NAME
DB_USERNAME=$DB_USERNAME
DB_PASSWORD=$DB_PASSWORD
MYSQL_ROOT_PASSWORD=$MYSQL_ROOT_PASSWORD
SPRING_DATA_REDIS_PASSWORD=$SPRING_DATA_REDIS_PASSWORD
FRONTEND_URL=$FRONTEND_URL
ALLOWED_ORIGINS=$ALLOWED_ORIGINS
FILE_UPLOAD_DIR=/data/hamalog/uploads
FILE_UPLOAD_MAX_SIZE=5242880
LOG_DIR=/var/log/hamalog
ENVEOF

# Setup SSH for file transfer
mkdir -p ~/.ssh
echo "$SSH_PRIVATE_KEY" > ~/.ssh/id_rsa
chmod 600 ~/.ssh/id_rsa

cat > ~/.ssh/config << 'SSH_CONFIG_EOF'
Host *
  ConnectTimeout 30
  StrictHostKeyChecking accept-new
  UserKnownHostsFile ~/.ssh/known_hosts
  BatchMode yes
SSH_CONFIG_EOF

chmod 600 ~/.ssh/config

ssh-keyscan -p ${SERVER_PORT:-22} -T 10 $SERVER_HOST >> ~/.ssh/known_hosts 2>/dev/null || true

echo ""
echo "Transferring configuration file to server..."
scp -P ${SERVER_PORT:-22} \
    -o ConnectTimeout=30 \
    /tmp/.env.prod ${SERVER_USER}@${SERVER_HOST}:~/hamalog-deploy/.env.prod

ssh -p ${SERVER_PORT:-22} \
    -o ConnectTimeout=30 \
    ${SERVER_USER}@${SERVER_HOST} "chmod 600 ~/hamalog-deploy/.env.prod"

rm -f /tmp/.env.prod

echo ""
echo "Executing remote deployment..."

ssh -p ${SERVER_PORT:-22} \
    -o ConnectTimeout=30 \
    ${SERVER_USER}@${SERVER_HOST} << 'REMOTE_SCRIPT_EOF'
cd ~/hamalog-deploy

echo "================================================"
echo "Hamalog Deployment - Remote Execution"
echo "================================================"

if [ ! -f .env.prod ]; then
  echo "❌ DEPLOYMENT FAILED: .env.prod not found!"
  exit 1
fi

echo "Loading environment variables..."
set -a
source .env.prod
set +a

echo "Loading Docker image..."
docker load -i hamalog-image.tar

echo "Stopping existing containers..."
docker-compose down 2>/dev/null || true

echo "Starting new deployment..."
docker-compose up -d

echo ""
echo "✅ Deployment completed successfully"
echo ""
echo "Verifying deployment..."
sleep 5

if docker-compose ps | grep -q "hamalog-app.*Up"; then
  echo "✅ Application is running"
  echo ""
  echo "Application URL: http://$(hostname -I | awk '{print $1}'):8080"
else
  echo "❌ Application failed to start"
  echo ""
  echo "Checking logs..."
  docker-compose logs --tail 50
  exit 1
fi
REMOTE_SCRIPT_EOF

echo ""
echo "✅ Deployment completed successfully"

