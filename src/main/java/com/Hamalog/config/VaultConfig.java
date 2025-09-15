package com.Hamalog.config;

import org.springframework.context.annotation.Configuration;

/**
 * Vault Configuration - All key management is now handled exclusively by VaultKeyProvider
 * No fallback mechanisms are provided - Vault must be enabled and configured properly
 */
@Configuration
public class VaultConfig {
    
    // VaultKeyProvider bean is automatically created when hamalog.vault.enabled=true
    // No fallback implementation - enforce Vault-only key management
}