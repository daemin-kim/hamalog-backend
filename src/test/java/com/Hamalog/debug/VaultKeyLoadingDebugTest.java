package com.Hamalog.debug;

import com.Hamalog.security.encryption.DataEncryptionUtil;
import com.Hamalog.service.vault.VaultKeyProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Vault Key Loading Debug Tests")
class VaultKeyLoadingDebugTest {

    @Mock
    private Environment environment;

    @Test
    @DisplayName("Should show detailed diagnostics for Vault key loading in production")
    void debugVaultKeyLoading_ProductionScenario_ShowsDetailedLogs() {
        // given
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        
        System.out.println("=== VAULT KEY LOADING DEBUG TEST ===");
        System.out.println("Active profiles: " + String.join(",", environment.getActiveProfiles()));
        System.out.println("Testing DataEncryptionUtil behavior with production profile and no Vault");

        // when - Create DataEncryptionUtil which should trigger the detailed logging
        try {
            DataEncryptionUtil encryptionUtil = new DataEncryptionUtil("", environment, null);
            System.out.println("DataEncryptionUtil created successfully with disabled encryption");
            
            // Try to encrypt - this should fail with a clear message
            try {
                encryptionUtil.encrypt("test data");
                System.out.println("Encryption succeeded (unexpected)");
            } catch (IllegalStateException e) {
                System.out.println("Encryption failed as expected: " + e.getMessage().substring(0, Math.min(100, e.getMessage().length())));
            }
        } catch (Exception e) {
            System.out.println("DataEncryptionUtil creation failed: " + e.getMessage());
        }

        // then
        assertThat(true).isTrue(); // Just ensure test runs and logs are produced
    }

    @Test
    @DisplayName("Should show what happens when Vault token is missing")
    void debugVaultKeyLoading_MissingToken_ShowsTokenIssue() {
        // This test will help identify token-related issues
        System.out.println("=== VAULT TOKEN DEBUG TEST ===");
        
        // Create a VaultKeyProvider with empty token to simulate the production issue
        VaultKeyProvider testProvider = new VaultKeyProvider(
            "http://localhost:8200",
            "", // Empty token
            "secret",
            "hamalog"
        );
        
        System.out.println("Test VaultKeyProvider created with empty token");
        System.out.println("Vault availability check: " + testProvider.isVaultAvailable());
        
        var encryptionKey = testProvider.getEncryptionKey();
        System.out.println("Encryption key result with empty token: " + (encryptionKey.isPresent() ? "PRESENT" : "EMPTY"));
        
        assertThat(encryptionKey).isEmpty();
    }
}