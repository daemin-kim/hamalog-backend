#!/bin/bash

echo "=== Hamalog Encryption Configuration Debug Script ==="
echo "Date: $(date)"
echo

# Load environment variables from .env.prod
if [ -f .env.prod ]; then
    echo "Loading .env.prod file..."
    source .env.prod
    echo "✓ .env.prod loaded"
else
    echo "⚠️  .env.prod file not found"
fi

echo
echo "=== Environment Variables Check ==="

# Check Vault configuration
echo "Vault Configuration:"
echo "  HAMALOG_VAULT_ENABLED: ${HAMALOG_VAULT_ENABLED:-NOT_SET}"
echo "  HAMALOG_VAULT_URI: ${HAMALOG_VAULT_URI:-NOT_SET}"
echo "  HAMALOG_VAULT_TOKEN: ${HAMALOG_VAULT_TOKEN:-NOT_SET}"
echo "  HAMALOG_VAULT_KV_BACKEND: ${HAMALOG_VAULT_KV_BACKEND:-NOT_SET}"
echo "  HAMALOG_VAULT_KV_DEFAULT_CONTEXT: ${HAMALOG_VAULT_KV_DEFAULT_CONTEXT:-NOT_SET}"

echo
echo "Encryption Key Configuration:"
echo "  HAMALOG_ENCRYPTION_KEY: ${HAMALOG_ENCRYPTION_KEY:-NOT_SET}"
if [ -n "$HAMALOG_ENCRYPTION_KEY" ]; then
    echo "  Key Length: ${#HAMALOG_ENCRYPTION_KEY} characters"
    # Test Base64 decoding
    if decoded=$(echo "$HAMALOG_ENCRYPTION_KEY" | base64 -d 2>/dev/null); then
        echo "  Base64 Valid: ✓ (${#decoded} bytes)"
        if [ ${#decoded} -eq 32 ]; then
            echo "  Key Size: ✓ 256-bit (32 bytes)"
        else
            echo "  Key Size: ❌ Expected 32 bytes, got ${#decoded} bytes"
        fi
    else
        echo "  Base64 Valid: ❌ Invalid Base64 encoding"
    fi
fi

echo
echo "Other Secret Configuration:"
echo "  JWT_SECRET: ${JWT_SECRET:-NOT_SET}"
echo "  KAKAO_CLIENT_ID: ${KAKAO_CLIENT_ID:-NOT_SET}"
echo "  KAKAO_CLIENT_SECRET: ${KAKAO_CLIENT_SECRET:-NOT_SET}"

echo
echo "=== Vault Health Check ==="
if command -v curl >/dev/null 2>&1; then
    VAULT_URI=${HAMALOG_VAULT_URI:-http://localhost:8200}
    echo "Testing Vault connectivity at: $VAULT_URI"
    
    if response=$(curl -s -w "%{http_code}" --max-time 10 "$VAULT_URI/v1/sys/health" -o /tmp/vault_health 2>&1); then
        http_code="${response: -3}"
        if [ "$http_code" = "200" ] || [ "$http_code" = "429" ] || [ "$http_code" = "472" ] || [ "$http_code" = "503" ]; then
            echo "  Vault Health Check: ✓ HTTP $http_code"
            if [ -f /tmp/vault_health ]; then
                echo "  Response: $(cat /tmp/vault_health)"
            fi
        else
            echo "  Vault Health Check: ❌ HTTP $http_code"
        fi
    else
        echo "  Vault Health Check: ❌ Connection failed"
    fi
    rm -f /tmp/vault_health
else
    echo "  curl not available, skipping Vault health check"
fi

echo
echo "=== Test Vault Secret Retrieval ==="
if [ -n "$HAMALOG_VAULT_TOKEN" ] && [ -n "$HAMALOG_VAULT_URI" ]; then
    echo "Testing secret retrieval from Vault..."
    VAULT_PATH="${HAMALOG_VAULT_URI}/v1/${HAMALOG_VAULT_KV_BACKEND:-secret}/data/${HAMALOG_VAULT_KV_DEFAULT_CONTEXT:-hamalog}"
    echo "  Path: $VAULT_PATH"
    
    if command -v curl >/dev/null 2>&1; then
        if response=$(curl -s -w "%{http_code}" --max-time 10 -H "X-Vault-Token: $HAMALOG_VAULT_TOKEN" "$VAULT_PATH" -o /tmp/vault_secrets 2>&1); then
            http_code="${response: -3}"
            if [ "$http_code" = "200" ]; then
                echo "  Secret Retrieval: ✓ HTTP $http_code"
                if command -v jq >/dev/null 2>&1; then
                    if [ -f /tmp/vault_secrets ]; then
                        echo "  Available secrets:"
                        jq -r '.data.data | keys[]' /tmp/vault_secrets 2>/dev/null | sed 's/^/    - /' || echo "    Unable to parse secrets"
                        
                        # Check for encryption-key specifically
                        if jq -e '.data.data."encryption-key"' /tmp/vault_secrets >/dev/null 2>&1; then
                            echo "  encryption-key: ✓ Found in Vault"
                            key_length=$(jq -r '.data.data."encryption-key"' /tmp/vault_secrets | wc -c)
                            echo "  encryption-key length: $((key_length-1)) characters"
                        else
                            echo "  encryption-key: ❌ Not found in Vault"
                        fi
                    fi
                else
                    echo "  jq not available, cannot parse JSON response"
                fi
            else
                echo "  Secret Retrieval: ❌ HTTP $http_code"
                if [ -f /tmp/vault_secrets ]; then
                    echo "  Response: $(cat /tmp/vault_secrets)"
                fi
            fi
        else
            echo "  Secret Retrieval: ❌ Request failed"
        fi
        rm -f /tmp/vault_secrets
    fi
else
    echo "  Vault token or URI not configured, skipping secret retrieval test"
fi

echo
echo "=== Spring Boot Environment Properties Test ==="
echo "Testing how Spring Boot would resolve these properties..."

# Check if application-prod.properties exists
if [ -f "src/main/resources/application-prod.properties" ]; then
    echo "✓ application-prod.properties found"
    echo "Vault configuration in properties file:"
    grep -E "^hamalog\.vault\." src/main/resources/application-prod.properties | sed 's/^/  /'
    echo "Encryption key configuration in properties file:"
    grep -E "(encryption|ENCRYPTION)" src/main/resources/application-prod.properties | sed 's/^/  /'
else
    echo "❌ application-prod.properties not found"
fi

echo
echo "=== Recommendations ==="
if [ -z "$HAMALOG_ENCRYPTION_KEY" ]; then
    echo "❌ HAMALOG_ENCRYPTION_KEY is not set"
    echo "   Set with: export HAMALOG_ENCRYPTION_KEY='7u+EyAhNyWu2hebIO0XoqUH1gwdoyyHKxcIKC2+A6aM='"
else
    echo "✓ HAMALOG_ENCRYPTION_KEY is configured"
fi

if [ -z "$HAMALOG_VAULT_TOKEN" ]; then
    echo "❌ HAMALOG_VAULT_TOKEN is not set"
    echo "   Set with: export HAMALOG_VAULT_TOKEN='hamalog-dev-token'"
else
    echo "✓ HAMALOG_VAULT_TOKEN is configured"
fi

echo
echo "=== Debug Script Complete ==="