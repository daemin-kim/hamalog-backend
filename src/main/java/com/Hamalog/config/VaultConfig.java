package com.Hamalog.config;

import com.Hamalog.service.vault.VaultKeyProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

/**
 * Configuration class to conditionally create VaultKeyProvider bean
 */
@Configuration
public class VaultConfig {
    
    @Bean
    @ConditionalOnProperty(name = "hamalog.vault.enabled", havingValue = "false", matchIfMissing = true)
    public VaultKeyProvider disabledVaultKeyProvider() {
        // Create a fallback VaultKeyProvider with dummy Vault configuration
        return new VaultKeyProvider(
                "http://localhost:8200", // dummy vault URI
                "", // empty token (will fallback to env vars)
                "secret", // default backend
                "hamalog" // default context
        ) {
            @Override
            public Optional<String> getJwtSecret() {
                return Optional.ofNullable(System.getenv("JWT_SECRET"))
                        .or(() -> Optional.ofNullable(System.getProperty("jwt.secret")));
            }

            @Override
            public Optional<String> getEncryptionKey() {
                return Optional.ofNullable(System.getenv("HAMALOG_ENCRYPTION_KEY"))
                        .or(() -> Optional.ofNullable(System.getProperty("hamalog.encryption.key")));
            }

            @Override
            public boolean isVaultAvailable() {
                return false;
            }
        };
    }
}