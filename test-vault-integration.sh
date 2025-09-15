#!/bin/bash

echo "=== Vault Integration Diagnostic Test ==="
echo "Testing Vault connectivity, authentication, and secret retrieval"
echo "Date: $(date)"
echo

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
VAULT_URI="http://localhost:8200"
VAULT_TOKEN="hamalog-dev-token"
KV_BACKEND="secret"
CONTEXT="hamalog"

echo "=== Configuration ==="
echo "Vault URI: $VAULT_URI"
echo "Vault Token: $VAULT_TOKEN"
echo "KV Backend: $KV_BACKEND"
echo "Context: $CONTEXT"
echo

# Function to test Vault health
test_vault_health() {
    echo "=== Testing Vault Health ==="
    
    if curl -s --max-time 10 "$VAULT_URI/v1/sys/health" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Vault health check: ACCESSIBLE${NC}"
        
        # Get detailed health info
        health_response=$(curl -s "$VAULT_URI/v1/sys/health" 2>/dev/null)
        if [ $? -eq 0 ]; then
            echo "Health Response: $health_response"
        fi
        return 0
    else
        echo -e "${RED}❌ Vault health check: NOT_ACCESSIBLE${NC}"
        echo "Vault may not be running or accessible at $VAULT_URI"
        return 1
    fi
}

# Function to test token authentication
test_token_auth() {
    echo "=== Testing Token Authentication ==="
    
    # Test token with auth/token/lookup-self endpoint
    auth_response=$(curl -s -w "%{http_code}" -o /tmp/vault_auth_test.json \
        -H "X-Vault-Token: $VAULT_TOKEN" \
        "$VAULT_URI/v1/auth/token/lookup-self" 2>/dev/null)
    
    http_code=$(echo "$auth_response" | tail -c 4)
    
    if [ "$http_code" = "200" ]; then
        echo -e "${GREEN}✓ Token authentication: SUCCESS${NC}"
        
        # Show token details (safe parts only)
        if [ -f /tmp/vault_auth_test.json ]; then
            echo "Token details:"
            jq -r '.data | {id: .id[0:8], policies: .policies, ttl: .ttl}' /tmp/vault_auth_test.json 2>/dev/null || cat /tmp/vault_auth_test.json
        fi
        return 0
    else
        echo -e "${RED}❌ Token authentication: FAILED (HTTP $http_code)${NC}"
        if [ -f /tmp/vault_auth_test.json ]; then
            echo "Error response:"
            cat /tmp/vault_auth_test.json
        fi
        return 1
    fi
}

# Function to list available secrets engines
test_secrets_engines() {
    echo "=== Testing Secrets Engines ==="
    
    engines_response=$(curl -s -w "%{http_code}" -o /tmp/vault_engines.json \
        -H "X-Vault-Token: $VAULT_TOKEN" \
        "$VAULT_URI/v1/sys/mounts" 2>/dev/null)
    
    http_code=$(echo "$engines_response" | tail -c 4)
    
    if [ "$http_code" = "200" ]; then
        echo -e "${GREEN}✓ Secrets engines accessible${NC}"
        
        # Check if secret/ KV v2 engine exists
        if [ -f /tmp/vault_engines.json ]; then
            if jq -e ".data[\"secret/\"]" /tmp/vault_engines.json > /dev/null 2>&1; then
                echo -e "${GREEN}✓ KV v2 engine 'secret/' is enabled${NC}"
                kv_version=$(jq -r '.data["secret/"].options.version // "unknown"' /tmp/vault_engines.json 2>/dev/null)
                echo "KV version: $kv_version"
            else
                echo -e "${YELLOW}⚠️ KV v2 engine 'secret/' not found${NC}"
                echo "Available engines:"
                jq -r '.data | keys[]' /tmp/vault_engines.json 2>/dev/null || echo "Could not parse engines"
            fi
        fi
        return 0
    else
        echo -e "${RED}❌ Secrets engines check: FAILED (HTTP $http_code)${NC}"
        return 1
    fi
}

# Function to test secret retrieval
test_secret_retrieval() {
    echo "=== Testing Secret Retrieval ==="
    
    # Test retrieving the hamalog secret
    secret_path="/v1/$KV_BACKEND/data/$CONTEXT"
    echo "Testing path: $secret_path"
    
    secret_response=$(curl -s -w "%{http_code}" -o /tmp/vault_secret.json \
        -H "X-Vault-Token: $VAULT_TOKEN" \
        "$VAULT_URI$secret_path" 2>/dev/null)
    
    http_code=$(echo "$secret_response" | tail -c 4)
    
    if [ "$http_code" = "200" ]; then
        echo -e "${GREEN}✓ Secret retrieval: SUCCESS${NC}"
        
        if [ -f /tmp/vault_secret.json ]; then
            echo "Available secret keys:"
            jq -r '.data.data | keys[]' /tmp/vault_secret.json 2>/dev/null || echo "Could not parse secret keys"
            
            # Check specific keys we need
            for key in "encryption-key" "jwt-secret" "kakao-client-id" "kakao-client-secret"; do
                if jq -e ".data.data[\"$key\"]" /tmp/vault_secret.json > /dev/null 2>&1; then
                    value_length=$(jq -r ".data.data[\"$key\"] | length" /tmp/vault_secret.json 2>/dev/null)
                    echo -e "  ${GREEN}✓ $key: present (length: $value_length)${NC}"
                else
                    echo -e "  ${RED}❌ $key: missing${NC}"
                fi
            done
        fi
        return 0
    elif [ "$http_code" = "404" ]; then
        echo -e "${YELLOW}⚠️ Secret not found at path: $secret_path${NC}"
        echo "This might be normal if secrets haven't been initialized yet"
        return 1
    else
        echo -e "${RED}❌ Secret retrieval: FAILED (HTTP $http_code)${NC}"
        if [ -f /tmp/vault_secret.json ]; then
            echo "Error response:"
            cat /tmp/vault_secret.json
        fi
        return 1
    fi
}

# Function to test Docker Compose Vault service
test_docker_vault() {
    echo "=== Testing Docker Compose Vault Service ==="
    
    if docker-compose ps vault 2>/dev/null | grep -q "Up"; then
        echo -e "${GREEN}✓ Vault container is running${NC}"
        
        # Test vault-init container
        if docker-compose ps vault-init 2>/dev/null | grep -q "Exit 0"; then
            echo -e "${GREEN}✓ Vault initialization completed successfully${NC}"
        elif docker-compose ps vault-init 2>/dev/null | grep -q "Up"; then
            echo -e "${YELLOW}⚠️ Vault initialization is still running${NC}"
        else
            echo -e "${RED}❌ Vault initialization failed or not started${NC}"
        fi
        
        # Show container logs
        echo "Recent Vault logs:"
        docker-compose logs --tail=10 vault 2>/dev/null || echo "Could not retrieve vault logs"
        
        echo "Recent Vault-init logs:"
        docker-compose logs --tail=10 vault-init 2>/dev/null || echo "Could not retrieve vault-init logs"
        
    else
        echo -e "${RED}❌ Vault container is not running${NC}"
        echo "Try running: docker-compose up -d vault"
        return 1
    fi
}

# Main test sequence
echo "Starting Vault integration tests..."
echo

# Test 1: Docker service status
test_docker_vault
docker_result=$?

# Test 2: Vault health
test_vault_health
health_result=$?

# Test 3: Token authentication
if [ $health_result -eq 0 ]; then
    test_token_auth
    auth_result=$?
else
    echo "Skipping token authentication test (Vault not accessible)"
    auth_result=1
fi

# Test 4: Secrets engines
if [ $auth_result -eq 0 ]; then
    test_secrets_engines
    engines_result=$?
else
    echo "Skipping secrets engines test (authentication failed)"
    engines_result=1
fi

# Test 5: Secret retrieval
if [ $auth_result -eq 0 ]; then
    test_secret_retrieval
    secret_result=$?
else
    echo "Skipping secret retrieval test (authentication failed)"
    secret_result=1
fi

# Cleanup temp files
rm -f /tmp/vault_*.json

echo
echo "=== Test Results Summary ==="
echo -e "Docker service: $([ $docker_result -eq 0 ] && echo -e "${GREEN}PASS${NC}" || echo -e "${RED}FAIL${NC}")"
echo -e "Vault health: $([ $health_result -eq 0 ] && echo -e "${GREEN}PASS${NC}" || echo -e "${RED}FAIL${NC}")"
echo -e "Token auth: $([ $auth_result -eq 0 ] && echo -e "${GREEN}PASS${NC}" || echo -e "${RED}FAIL${NC}")"
echo -e "Secrets engines: $([ $engines_result -eq 0 ] && echo -e "${GREEN}PASS${NC}" || echo -e "${RED}FAIL${NC}")"
echo -e "Secret retrieval: $([ $secret_result -eq 0 ] && echo -e "${GREEN}PASS${NC}" || echo -e "${RED}FAIL${NC}")"

echo
if [ $secret_result -eq 0 ]; then
    echo -e "${GREEN}✅ Vault integration is working correctly!${NC}"
    echo "The application should be able to retrieve secrets from Vault."
elif [ $auth_result -eq 0 ]; then
    echo -e "${YELLOW}⚠️ Vault authentication works but secrets need to be initialized${NC}"
    echo "Run the vault-init service or manually create secrets."
elif [ $health_result -eq 0 ]; then
    echo -e "${YELLOW}⚠️ Vault is accessible but token authentication failed${NC}"
    echo "Check the token configuration in docker-compose.yml"
else
    echo -e "${RED}❌ Vault is not accessible${NC}"
    echo "Start the Vault service: docker-compose up -d vault"
fi

echo
echo "=== Next Steps ==="
if [ $secret_result -eq 0 ]; then
    echo "✅ Ready to test application with Vault integration"
    echo "Run: docker-compose up -d"
else
    echo "🔧 Fix the issues identified above, then:"
    echo "1. Ensure Vault service is running: docker-compose up -d vault"
    echo "2. Initialize secrets: docker-compose up vault-init"
    echo "3. Re-run this test script to verify"
fi

exit $((health_result + auth_result + secret_result))