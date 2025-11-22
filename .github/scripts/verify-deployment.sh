#!/bin/bash
set -e

echo "================================================"
echo "Verifying Deployment"
echo "================================================"

SERVER_PORT=${SERVER_PORT:-22}

# Setup SSH
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

ssh-keyscan -p ${SERVER_PORT} -T 10 $SERVER_HOST >> ~/.ssh/known_hosts 2>/dev/null || true

echo "Checking application status..."
ssh -p ${SERVER_PORT} \
    -o ConnectTimeout=30 \
    ${SERVER_USER}@${SERVER_HOST} << 'VERIFY_SCRIPT_EOF'
cd ~/hamalog-deploy

echo ""
echo "Container Status:"
docker-compose ps

echo ""
echo "Application Logs (last 20 lines):"
docker-compose logs --tail 20 hamalog-app

if docker-compose ps | grep -q "hamalog-app.*Up"; then
  echo ""
  echo "✅ Application is running successfully"
  exit 0
else
  echo ""
  echo "❌ Application is not running"
  exit 1
fi
VERIFY_SCRIPT_EOF

echo ""
echo "✅ Deployment verification completed"

