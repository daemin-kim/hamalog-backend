package com.Hamalog.service.vault;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Optional;

/**
 * Vault Key Provider Service for retrieving secrets from HashiCorp Vault.
 * This service provides exclusive vault-based secret retrieval for all application keys.
 * No fallback mechanisms are available - all keys must be stored in Vault.
 */
@Service
@ConditionalOnProperty(name = "hamalog.vault.enabled", havingValue = "true", matchIfMissing = false)
public class VaultKeyProvider {

    private static final Logger log = LoggerFactory.getLogger(VaultKeyProvider.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    
    private final WebClient webClient;
    private final String kvBackend;
    private final String defaultContext;
    private final String vaultUri;
    private final String vaultToken;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public VaultKeyProvider(
            @Value("${hamalog.vault.uri:http://localhost:8200}") String vaultUri,
            @Value("${hamalog.vault.token:}") String vaultToken,
            @Value("${hamalog.vault.kv.backend:secret}") String kvBackend,
            @Value("${hamalog.vault.kv.default-context:hamalog}") String defaultContext) {
        
        this.vaultUri = vaultUri;
        this.vaultToken = vaultToken;
        this.kvBackend = kvBackend;
        this.defaultContext = defaultContext;
        
        this.webClient = WebClient.builder()
                .baseUrl(vaultUri)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
                
        log.info("[VAULT_KEY_PROVIDER] Initialized with URI: {}, Backend: {}, Context: {}", 
                vaultUri, kvBackend, defaultContext);
        log.info("[VAULT_KEY_PROVIDER] Token status: {}, URI accessible: checking...", 
                vaultToken != null && !vaultToken.trim().isEmpty() ? "PRESENT" : "MISSING_OR_EMPTY");
    }

    /**
     * Retrieves JWT secret exclusively from Vault
     */
    public Optional<String> getJwtSecret() {
        try {
            return getSecretFromVault("jwt-secret");
        } catch (Exception e) {
            log.error("[VAULT_KEY_PROVIDER] Failed to retrieve JWT secret from Vault: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Retrieves encryption key exclusively from Vault
     */
    public Optional<String> getEncryptionKey() {
        try {
            return getSecretFromVault("encryption-key");
        } catch (Exception e) {
            log.error("[VAULT_KEY_PROVIDER] Failed to retrieve encryption key from Vault: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Generic method to retrieve a secret from Vault KV v2 engine
     */
    private Optional<String> getSecretFromVault(String secretKey) {
        if (vaultToken == null || vaultToken.trim().isEmpty()) {
            log.warn("[VAULT_KEY_PROVIDER] Vault token not configured, skipping Vault lookup for: {} (Token present: {}, URI: {})", 
                    secretKey, vaultToken != null, vaultUri);
            return Optional.empty();
        }

        try {
            String path = String.format("/v1/%s/data/%s", kvBackend, defaultContext);
            log.debug("[VAULT_KEY_PROVIDER] Attempting to retrieve secret '{}' from path '{}' at URI '{}'", 
                     secretKey, path, vaultUri);
            
            String response = webClient.get()
                    .uri(path)
                    .header("X-Vault-Token", vaultToken)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(REQUEST_TIMEOUT)
                    .block();

            if (response != null) {
                JsonNode jsonNode = objectMapper.readTree(response);
                JsonNode dataNode = jsonNode.path("data").path("data");
                
                log.debug("[VAULT_KEY_PROVIDER] Response received from Vault for secret '{}'. Available keys: {}", 
                         secretKey, dataNode.fieldNames());
                
                if (dataNode.has(secretKey)) {
                    String secretValue = dataNode.path(secretKey).asText();
                    if (secretValue != null && !secretValue.trim().isEmpty()) {
                        log.info("[VAULT_KEY_PROVIDER] Successfully retrieved secret: {} from Vault (length: {})", 
                                secretKey, secretValue.length());
                        return Optional.of(secretValue);
                    } else {
                        log.warn("[VAULT_KEY_PROVIDER] Secret {} found in Vault but is empty or null", secretKey);
                        return Optional.empty();
                    }
                } else {
                    log.warn("[VAULT_KEY_PROVIDER] Secret {} not found in Vault response. Available keys: {}", 
                            secretKey, dataNode.fieldNames());
                    return Optional.empty();
                }
            } else {
                log.warn("[VAULT_KEY_PROVIDER] Received null response from Vault for secret: {}", secretKey);
                return Optional.empty();
            }
            
        } catch (Exception e) {
            log.error("[VAULT_KEY_PROVIDER] Error retrieving secret {} from Vault. URI: {}, Path: /v1/{}/data/{}, Error: {} - {}", 
                     secretKey, vaultUri, kvBackend, defaultContext, e.getClass().getSimpleName(), e.getMessage());
            
            // Don't throw exception, return empty to allow fallback
            return Optional.empty();
        }
    }

    /**
     * Checks if Vault is configured and accessible
     */
    public boolean isVaultAvailable() {
        if (vaultToken == null || vaultToken.trim().isEmpty()) {
            return false;
        }

        try {
            String response = webClient.get()
                    .uri("/v1/sys/health")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(REQUEST_TIMEOUT)
                    .block();
            
            return response != null;
        } catch (Exception e) {
            log.debug("[VAULT_KEY_PROVIDER] Vault health check failed: {}", e.getMessage());
            return false;
        }
    }
}