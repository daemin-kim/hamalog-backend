#!/bin/bash

# Hamalog Vault Manual Initialization Script
# This script manually sets up Vault with the required secrets for Hamalog application
# Use this script when you need to manually initialize Vault or reset secrets

set -e

# Configuration
VAULT_ADDR=${VAULT_ADDR:-"http://localhost:8200"}
VAULT_TOKEN=${VAULT_TOKEN:-"hamalog-dev-token"}
KV_BACKEND=${KV_BACKEND:-"secret"}
CONTEXT=${CONTEXT:-"hamalog"}

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Vault CLI is available
check_vault_cli() {
    if ! command -v vault &> /dev/null; then
        log_error "Vault CLI not found. Please install Vault CLI or run this script inside a Vault container."
        exit 1
    fi
}

# Check if Vault is accessible
check_vault_status() {
    log_info "Checking Vault status at $VAULT_ADDR..."
    
    export VAULT_ADDR
    export VAULT_TOKEN
    
    if ! vault status >/dev/null 2>&1; then
        log_error "Vault is not accessible at $VAULT_ADDR or is sealed"
        log_info "Make sure Vault is running and unsealed"
        exit 1
    fi
    
    log_success "Vault is accessible and unsealed"
}

# Enable KV v2 secrets engine
enable_kv_engine() {
    log_info "Enabling KV v2 secrets engine at path '$KV_BACKEND'..."
    
    if vault secrets list | grep -q "^${KV_BACKEND}/"; then
        log_warning "KV secrets engine already exists at path '$KV_BACKEND'"
    else
        if vault secrets enable -path="$KV_BACKEND" kv-v2; then
            log_success "KV v2 secrets engine enabled at path '$KV_BACKEND'"
        else
            log_error "Failed to enable KV v2 secrets engine"
            exit 1
        fi
    fi
}

# Create or update secrets
create_secrets() {
    log_info "Creating secrets in $KV_BACKEND/$CONTEXT..."
    
    # Get secret values from environment (no fallback to dummy values)
    JWT_SECRET=${JWT_SECRET}
    ENCRYPTION_KEY=${HAMALOG_ENCRYPTION_KEY}
    KAKAO_CLIENT_ID=${KAKAO_CLIENT_ID}
    KAKAO_CLIENT_SECRET=${KAKAO_CLIENT_SECRET}
    
    # Validate all required secrets are provided
    if [ -z "$JWT_SECRET" ]; then
        log_error "JWT_SECRET is required but not provided. Set JWT_SECRET environment variable."
        exit 1
    fi
    
    if [ -z "$ENCRYPTION_KEY" ]; then
        log_error "HAMALOG_ENCRYPTION_KEY is required but not provided. Set HAMALOG_ENCRYPTION_KEY environment variable."
        exit 1
    fi
    
    if [ -z "$KAKAO_CLIENT_ID" ]; then
        log_error "KAKAO_CLIENT_ID is required but not provided. Set KAKAO_CLIENT_ID environment variable."
        exit 1
    fi
    
    if [ -z "$KAKAO_CLIENT_SECRET" ]; then
        log_error "KAKAO_CLIENT_SECRET is required but not provided. Set KAKAO_CLIENT_SECRET environment variable."
        exit 1
    fi
    
    # Validate no dummy values are being used
    if [[ "$KAKAO_CLIENT_ID" == *"dummy"* ]]; then
        log_error "KAKAO_CLIENT_ID contains dummy value. Use real Kakao client ID."
        exit 1
    fi
    
    if [[ "$KAKAO_CLIENT_SECRET" == *"dummy"* ]]; then
        log_error "KAKAO_CLIENT_SECRET contains dummy value. Use real Kakao client secret."
        exit 1
    fi
    
    # Create the secret bundle
    if vault kv put "$KV_BACKEND/$CONTEXT" \
        jwt-secret="$JWT_SECRET" \
        encryption-key="$ENCRYPTION_KEY" \
        kakao-client-id="$KAKAO_CLIENT_ID" \
        kakao-client-secret="$KAKAO_CLIENT_SECRET"; then
        log_success "Secrets created successfully in $KV_BACKEND/$CONTEXT"
    else
        log_error "Failed to create secrets"
        exit 1
    fi
}

# Verify secrets
verify_secrets() {
    log_info "Verifying created secrets..."
    
    if vault kv get "$KV_BACKEND/$CONTEXT" >/dev/null 2>&1; then
        log_success "Secrets verified successfully"
        log_info "Secret metadata:"
        vault kv metadata get "$KV_BACKEND/$CONTEXT" | grep -E "(Created Time|Version|Destroyed)"
    else
        log_error "Failed to verify secrets"
        exit 1
    fi
}

# Display configuration summary
display_summary() {
    log_info "Configuration Summary:"
    echo "  Vault Address: $VAULT_ADDR"
    echo "  KV Backend: $KV_BACKEND"
    echo "  Context: $CONTEXT"
    echo "  Secret Path: $KV_BACKEND/$CONTEXT"
    echo ""
    log_info "Available secrets:"
    echo "  - jwt-secret (for JWT token signing)"
    echo "  - encryption-key (for data encryption)"
    echo "  - kakao-client-id (OAuth2 client ID)"
    echo "  - kakao-client-secret (OAuth2 client secret)"
}

# Print usage information
usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Environment Variables:"
    echo "  VAULT_ADDR              Vault server address (default: http://localhost:8200)"
    echo "  VAULT_TOKEN             Vault authentication token (default: hamalog-dev-token)"
    echo "  KV_BACKEND              KV secrets engine path (default: secret)"
    echo "  CONTEXT                 Secret context/path (default: hamalog)"
    echo "  JWT_SECRET              JWT signing secret (required)"
    echo "  HAMALOG_ENCRYPTION_KEY  Data encryption key (optional)"
    echo "  KAKAO_CLIENT_ID         Kakao OAuth2 client ID (optional)"
    echo "  KAKAO_CLIENT_SECRET     Kakao OAuth2 client secret (optional)"
    echo ""
    echo "Examples:"
    echo "  # Basic usage with defaults"
    echo "  $0"
    echo ""
    echo "  # Custom Vault address and token"
    echo "  VAULT_ADDR=https://vault.example.com:8200 VAULT_TOKEN=my-token $0"
    echo ""
    echo "  # With custom secrets"
    echo "  JWT_SECRET=my-jwt-secret KAKAO_CLIENT_ID=real-client-id $0"
}

# Main execution
main() {
    # Check for help flag
    if [[ "$1" == "-h" || "$1" == "--help" ]]; then
        usage
        exit 0
    fi
    
    log_info "Starting Hamalog Vault initialization..."
    
    # Perform initialization steps
    check_vault_cli
    check_vault_status
    enable_kv_engine
    create_secrets
    verify_secrets
    display_summary
    
    log_success "Vault initialization completed successfully!"
    log_info "The Hamalog application can now retrieve secrets from Vault"
}

# Execute main function with all arguments
main "$@"