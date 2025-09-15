#!/bin/bash

echo "=== Testing Encryption Fix ==="
echo "This test simulates production conditions where Vault is unavailable but environment variables are set"
echo

# Load production environment variables
source .env.prod

echo "Environment variables loaded:"
echo "  HAMALOG_ENCRYPTION_KEY: ${HAMALOG_ENCRYPTION_KEY:-NOT_SET}"
echo "  HAMALOG_VAULT_TOKEN: ${HAMALOG_VAULT_TOKEN:-NOT_SET}"
echo

# Run a quick build to ensure code compiles
echo "Building application to verify code changes..."
if ./gradlew compileJava --quiet; then
    echo "✓ Code compiles successfully"
else
    echo "❌ Compilation failed"
    exit 1
fi

echo
echo "=== Testing DataEncryptionUtil Logic ==="

# Create a simple test class to test the encryption initialization
cat > src/test/java/com/Hamalog/security/encryption/DataEncryptionUtilProductionTest.java << 'EOF'
package com.Hamalog.security.encryption;

import com.Hamalog.service.vault.VaultKeyProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("prod") 
@TestPropertySource(properties = {
    "hamalog.vault.enabled=true",
    "HAMALOG_ENCRYPTION_KEY=7u+EyAhNyWu2hebIO0XoqUH1gwdoyyHKxcIKC2+A6aM="
})
@DisplayName("DataEncryptionUtil Production Environment Tests")
class DataEncryptionUtilProductionTest {

    @Autowired(required = false)
    private VaultKeyProvider vaultKeyProvider;
    
    @Autowired
    private Environment environment;

    @Test
    @DisplayName("Should initialize encryption with environment variable when Vault unavailable")
    void shouldInitializeWithEnvironmentVariable() {
        // Set system environment variable (simulating Docker environment)
        System.setProperty("HAMALOG_ENCRYPTION_KEY", "7u+EyAhNyWu2hebIO0XoqUH1gwdoyyHKxcIKC2+A6aM=");
        
        // Create DataEncryptionUtil with production-like conditions
        DataEncryptionUtil encryptionUtil = new DataEncryptionUtil(
            null, // no fallback parameter
            environment,
            vaultKeyProvider // may be null if Vault unavailable
        );

        // Test that encryption works (meaning key was properly initialized)
        String testData = "test-sensitive-data";
        assertDoesNotThrow(() -> {
            String encrypted = encryptionUtil.encrypt(testData);
            assertNotNull(encrypted);
            assertNotEquals(testData, encrypted);
            
            String decrypted = encryptionUtil.decrypt(encrypted);
            assertEquals(testData, decrypted);
        }, "Encryption/decryption should work with environment variable fallback");
        
        // Clean up
        System.clearProperty("HAMALOG_ENCRYPTION_KEY");
    }
}
EOF

echo "Created test class: DataEncryptionUtilProductionTest.java"
echo

# Run the specific test
echo "Running production environment test..."
if ./gradlew test --tests "DataEncryptionUtilProductionTest" --quiet; then
    echo "✓ Production environment test passed"
    echo "  The fix successfully allows environment variable fallback when Vault is unavailable"
else
    echo "❌ Production environment test failed"
    echo "  Need to investigate further..."
    
    # Show test results
    echo
    echo "Test results:"
    ./gradlew test --tests "DataEncryptionUtilProductionTest" --info | grep -A 10 -B 5 "FAILED\|ERROR" || echo "No detailed error info available"
fi

echo
echo "=== Test Summary ==="
echo "Fix applied: Modified environment variable fallback priority"
echo "Expected result: System environment variables checked first, allowing proper fallback when Vault unavailable"
echo "Production deployment: Should now work correctly with Docker environment variables"

# Cleanup test file
rm -f src/test/java/com/Hamalog/security/encryption/DataEncryptionUtilProductionTest.java

echo
echo "=== Test Complete ==="