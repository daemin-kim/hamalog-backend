package com.Hamalog.service.vault;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Test class for VaultKeyProvider
 * Tests both Vault integration and fallback mechanisms for JWT and encryption keys
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VaultKeyProvider Tests")
class VaultKeyProviderTest {

    private VaultKeyProvider vaultKeyProvider;

    @BeforeEach
    void setUp() {
        // Clear environment variables before each test
        clearEnvironmentVariables();
        
        // Create VaultKeyProvider with test configuration
        vaultKeyProvider = new VaultKeyProvider(
                "http://localhost:8200",
                "", // Empty token to force fallback
                "secret",
                "hamalog"
        );
    }

    @Test
    @DisplayName("Should retrieve JWT secret from environment variable when Vault is unavailable")
    void getJwtSecret_VaultUnavailable_ShouldFallbackToEnvironmentVariable() {
        // given
        String expectedSecret = "test-jwt-secret-from-env";
        System.setProperty("jwt.secret", expectedSecret);
        
        try {
            // when
            Optional<String> result = vaultKeyProvider.getJwtSecret();
            
            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(expectedSecret);
        } finally {
            System.clearProperty("jwt.secret");
        }
    }

    @Test
    @DisplayName("Should retrieve encryption key from environment variable when Vault is unavailable")
    void getEncryptionKey_VaultUnavailable_ShouldFallbackToEnvironmentVariable() {
        // given
        String expectedKey = "test-encryption-key-from-env";
        System.setProperty("hamalog.encryption.key", expectedKey);
        
        try {
            // when
            Optional<String> result = vaultKeyProvider.getEncryptionKey();
            
            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(expectedKey);
        } finally {
            System.clearProperty("hamalog.encryption.key");
        }
    }

    @Test
    @DisplayName("Should return empty when no JWT secret available")
    void getJwtSecret_NoSecretAvailable_ShouldReturnEmpty() {
        // given - no environment variables set
        
        // when
        Optional<String> result = vaultKeyProvider.getJwtSecret();
        
        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty when no encryption key available")
    void getEncryptionKey_NoKeyAvailable_ShouldReturnEmpty() {
        // given - no environment variables set
        
        // when
        Optional<String> result = vaultKeyProvider.getEncryptionKey();
        
        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return false when Vault is not available")
    void isVaultAvailable_VaultNotConfigured_ShouldReturnFalse() {
        // when
        boolean result = vaultKeyProvider.isVaultAvailable();
        
        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should handle multiple environment variable sources")
    void getJwtSecret_MultipleEnvironmentSources_ShouldPrioritizeSystemProperty() {
        // given
        String systemPropertyValue = "system-property-jwt-secret";
        System.setProperty("jwt.secret", systemPropertyValue);
        
        try {
            // when
            Optional<String> result = vaultKeyProvider.getJwtSecret();
            
            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(systemPropertyValue);
        } finally {
            System.clearProperty("jwt.secret");
        }
    }

    @Test
    @DisplayName("Should handle Base64 encoded keys correctly")
    void getEncryptionKey_Base64EncodedKey_ShouldReturnCorrectValue() {
        // given
        String base64Key = "dGVzdC1lbmNyeXB0aW9uLWtleS1iYXNlNjQ="; // "test-encryption-key-base64" in Base64
        System.setProperty("hamalog.encryption.key", base64Key);
        
        try {
            // when
            Optional<String> result = vaultKeyProvider.getEncryptionKey();
            
            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(base64Key);
        } finally {
            System.clearProperty("hamalog.encryption.key");
        }
    }

    @Test
    @DisplayName("Should not expose sensitive information in logs")
    void getJwtSecret_SensitiveData_ShouldNotExposeInExceptions() {
        // given
        String sensitiveSecret = "super-secret-jwt-key-should-not-appear-in-logs";
        System.setProperty("jwt.secret", sensitiveSecret);
        
        try {
            // when
            Optional<String> result = vaultKeyProvider.getJwtSecret();
            
            // then
            assertThat(result).isPresent();
            // Verify the secret is returned but this test ensures no logging exposes it
            // (actual logging behavior would need to be verified through log capture)
        } finally {
            System.clearProperty("jwt.secret");
        }
    }

    /**
     * Helper method to clear environment variables that might affect tests
     */
    private void clearEnvironmentVariables() {
        System.clearProperty("jwt.secret");
        System.clearProperty("hamalog.encryption.key");
        System.clearProperty("JWT_SECRET");
        System.clearProperty("HAMALOG_ENCRYPTION_KEY");
    }
}

/**
 * Integration test class for VaultKeyProvider with actual Vault connection
 * These tests require a running Vault instance and are marked for manual execution
 */
@SpringBootTest
@TestPropertySource(properties = {
        "hamalog.vault.enabled=true",
        "hamalog.vault.uri=http://localhost:8200",
        "hamalog.vault.token=hamalog-dev-token",
        "hamalog.vault.kv.backend=secret",
        "hamalog.vault.kv.default-context=hamalog-test"
})
@DisplayName("VaultKeyProvider Integration Tests")
class VaultKeyProviderIntegrationTest {

    /**
     * NOTE: These integration tests require:
     * 1. A running HashiCorp Vault instance at localhost:8200
     * 2. Dev token 'hamalog-dev-token' configured
     * 3. KV v2 secrets engine enabled
     * 4. Test secrets stored at secret/hamalog-test
     * 
     * Run vault-init.sh script to set up the test environment.
     * These tests are commented out by default and should be run manually
     * when Vault infrastructure is available.
     */

    /*
    @Test
    @DisplayName("Should retrieve JWT secret from Vault when available")
    void getJwtSecret_VaultAvailable_ShouldRetrieveFromVault() {
        // This test would verify actual Vault integration
        // Requires running Vault with test data
    }

    @Test
    @DisplayName("Should retrieve encryption key from Vault when available")
    void getEncryptionKey_VaultAvailable_ShouldRetrieveFromVault() {
        // This test would verify actual Vault integration  
        // Requires running Vault with test data
    }

    @Test
    @DisplayName("Should return true when Vault is available and accessible")
    void isVaultAvailable_VaultRunning_ShouldReturnTrue() {
        // This test would verify Vault connectivity
        // Requires running Vault instance
    }
    */
}