#!/bin/bash

# Vault Initialization Script for Hamalog
# This script initializes HashiCorp Vault with secrets for Hamalog application

set -e  # Exit on any error

VAULT_ADDR=${VAULT_ADDR:-"http://localhost:8200"}
VAULT_TOKEN=${VAULT_TOKEN:-"hamalog-dev-token"}
KV_BACKEND=${HAMALOG_VAULT_KV_BACKEND:-"secret"}
DEFAULT_CONTEXT=${HAMALOG_VAULT_KV_DEFAULT_CONTEXT:-"hamalog"}

echo "🔐 Initializing HashiCorp Vault for Hamalog..."
echo "Vault Address: $VAULT_ADDR"
echo "KV Backend: $KV_BACKEND"
echo "Context: $DEFAULT_CONTEXT"

# Set vault address and token
export VAULT_ADDR
export VAULT_TOKEN

# Function to wait for Vault to be ready
wait_for_vault() {
    echo "⏳ Waiting for Vault to be ready..."
    local max_attempts=30
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if vault status > /dev/null 2>&1; then
            echo "✅ Vault is ready!"
            return 0
        fi
        echo "Attempt $attempt/$max_attempts: Vault not ready, waiting..."
        sleep 2
        attempt=$((attempt + 1))
    done
    
    echo "❌ Vault failed to become ready within $max_attempts attempts"
    exit 1
}

# Function to generate secure key
generate_secure_key() {
    openssl rand -base64 32
}

# Wait for Vault to be ready
wait_for_vault

# Generate secure keys if not provided
JWT_SECRET=${JWT_SECRET:-$(generate_secure_key)}
HAMALOG_ENCRYPTION_KEY=${HAMALOG_ENCRYPTION_KEY:-$(generate_secure_key)}

echo "🔑 Generated/Using JWT Secret: ${JWT_SECRET:0:10}..."
echo "🔑 Generated/Using Encryption Key: ${HAMALOG_ENCRYPTION_KEY:0:10}..."

# Enable KV v2 secrets engine if not already enabled
echo "🛠️  Enabling KV v2 secrets engine..."
if vault secrets list | grep -q "^$KV_BACKEND/"; then
    echo "ℹ️  KV secrets engine '$KV_BACKEND' already enabled"
else
    vault secrets enable -path="$KV_BACKEND" kv-v2
    echo "✅ KV secrets engine '$KV_BACKEND' enabled"
fi

# Store secrets in Vault
echo "💾 Storing secrets in Vault..."
vault kv put "$KV_BACKEND/$DEFAULT_CONTEXT" \
    jwt-secret="$JWT_SECRET" \
    encryption-key="$HAMALOG_ENCRYPTION_KEY"

echo "✅ Secrets stored successfully in Vault at $KV_BACKEND/$DEFAULT_CONTEXT"

# Verify secrets are stored
echo "🔍 Verifying stored secrets..."
if vault kv get "$KV_BACKEND/$DEFAULT_CONTEXT" > /dev/null 2>&1; then
    echo "✅ Secrets verification successful"
    
    # Show secret metadata (without values for security)
    echo "📋 Secret metadata:"
    vault kv metadata get "$KV_BACKEND/$DEFAULT_CONTEXT"
else
    echo "❌ Failed to verify stored secrets"
    exit 1
fi

# Create policy for Hamalog application (optional, for production use)
echo "📜 Creating Hamalog policy..."
cat > hamalog-policy.hcl << EOF
# Policy for Hamalog application to read secrets
path "$KV_BACKEND/data/$DEFAULT_CONTEXT" {
  capabilities = ["read"]
}

path "$KV_BACKEND/metadata/$DEFAULT_CONTEXT" {
  capabilities = ["read"]
}
EOF

vault policy write hamalog hamalog-policy.hcl
echo "✅ Hamalog policy created"

# Clean up policy file
rm -f hamalog-policy.hcl

echo "🎉 Vault initialization completed successfully!"
echo ""
echo "📝 Configuration Summary:"
echo "  - Vault Address: $VAULT_ADDR"
echo "  - KV Backend: $KV_BACKEND"
echo "  - Secret Path: $KV_BACKEND/$DEFAULT_CONTEXT"
echo "  - JWT Secret: Stored ✅"
echo "  - Encryption Key: Stored ✅"
echo "  - Policy: hamalog ✅"
echo ""
echo "🔧 To use with Hamalog application:"
echo "  export HAMALOG_VAULT_ENABLED=true"
echo "  export HAMALOG_VAULT_URI=$VAULT_ADDR"
echo "  export HAMALOG_VAULT_TOKEN=$VAULT_TOKEN"
echo ""
echo "🚀 You can now start the Hamalog application!"