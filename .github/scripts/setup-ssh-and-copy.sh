#!/bin/bash
set -e

echo "================================================"
echo "SSH Setup and File Transfer"
echo "================================================"

# Validate required environment variables
echo "Validating SSH credentials..."

if [ -z "$SSH_PRIVATE_KEY" ]; then
  echo "❌ ERROR: SSH_PRIVATE_KEY is not set!"
  exit 1
fi

if [ -z "$SERVER_HOST" ]; then
  echo "❌ ERROR: SERVER_HOST is not set!"
  exit 1
fi

if [ -z "$SERVER_USER" ]; then
  echo "❌ ERROR: SERVER_USER is not set!"
  exit 1
fi

SERVER_PORT=${SERVER_PORT:-22}

echo "✅ SSH Configuration: HOST=$SERVER_HOST, USER=$SERVER_USER, PORT=$SERVER_PORT"

# Setup SSH
mkdir -p ~/.ssh
echo "$SSH_PRIVATE_KEY" > ~/.ssh/id_rsa
chmod 600 ~/.ssh/id_rsa

# Create SSH config file
cat > ~/.ssh/config << 'EOF'
Host *
  ConnectTimeout 30
  StrictHostKeyChecking accept-new
  UserKnownHostsFile ~/.ssh/known_hosts
  BatchMode yes
EOF

chmod 600 ~/.ssh/config

echo "Scanning SSH host key..."
ssh-keyscan -p ${SERVER_PORT} -T 10 $SERVER_HOST >> ~/.ssh/known_hosts 2>/dev/null || true

echo "Testing SSH connection..."
MAX_RETRIES=3
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
  RETRY_COUNT=$((RETRY_COUNT + 1))
  echo "SSH connection attempt $RETRY_COUNT of $MAX_RETRIES..."
  echo "  Connecting to: ${SERVER_USER}@${SERVER_HOST}:${SERVER_PORT}"

  if ssh -vvv \
      -p ${SERVER_PORT} \
      -o ConnectTimeout=30 \
      -o StrictHostKeyChecking=accept-new \
      ${SERVER_USER}@${SERVER_HOST} "echo '✅ SSH connection verified'" 2>&1; then
    echo "✅ SSH connection successful"
    break
  else
    CONNECTION_STATUS=$?
    echo "❌ Connection attempt $RETRY_COUNT failed (exit code: $CONNECTION_STATUS)"
    if [ $RETRY_COUNT -lt $MAX_RETRIES ]; then
      WAIT_TIME=$((RETRY_COUNT * 5))
      echo "⏳ Waiting ${WAIT_TIME}s before retry..."
      sleep ${WAIT_TIME}
    else
      echo "❌ DEPLOYMENT FAILED: Could not connect to server after $MAX_RETRIES attempts"
      echo "   Server: $SERVER_HOST:$SERVER_PORT"
      echo "   User: $SERVER_USER"
      echo ""
      echo "📋 Troubleshooting Steps:"
      echo "   1. Verify server is online and SSH daemon is running"
      echo "   2. Check if firewall allows inbound SSH on port $SERVER_PORT"
      echo "   3. Verify GitHub Actions can reach your server from outside"
      echo "   4. Test locally: ssh -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_HOST}"
      exit 1
    fi
  fi
done

echo ""
echo "Creating deployment directory on server..."
if ssh -p ${SERVER_PORT} \
    -o ConnectTimeout=30 \
    -o StrictHostKeyChecking=accept-new \
    ${SERVER_USER}@${SERVER_HOST} "mkdir -p ~/hamalog-deploy"; then
  echo "✅ Deployment directory created"
else
  echo "❌ Failed to create deployment directory"
  exit 1
fi

echo ""
echo "Copying files to server..."

echo "  → Copying Docker image..."
scp -P ${SERVER_PORT} \
    -o ConnectTimeout=30 \
    hamalog-image.tar ${SERVER_USER}@${SERVER_HOST}:~/hamalog-deploy/

echo "  → Copying docker-compose.yml..."
scp -P ${SERVER_PORT} \
    -o ConnectTimeout=30 \
    docker-compose.yml ${SERVER_USER}@${SERVER_HOST}:~/hamalog-deploy/

echo ""
echo "✅ Files copied successfully"

