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
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${hamalog.vault.uri:http://localhost:8200}")
    private String vaultUri;

    @Value("${hamalog.vault.token:}")
    private String vaultToken;

    @Value("${hamalog.vault.kv.backend:secret}")
    private String kvBackendConfig;

    @Value("${hamalog.vault.kv.default-context:hamalog}")
    private String defaultContextConfig;

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
            log.debug("[VAULT_KEY_PROVIDER] Vault token not configured, skipping Vault lookup for: {}", secretKey);
            return Optional.empty();
        }

        try {
            String path = String.format("/v1/%s/data/%s", kvBackend, defaultContext);
            
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
                
                if (dataNode.has(secretKey)) {
                    String secretValue = dataNode.path(secretKey).asText();
                    log.info("[VAULT_KEY_PROVIDER] Successfully retrieved secret: {} from Vault", secretKey);
                    return Optional.of(secretValue);
                }
            }
            
            log.debug("[VAULT_KEY_PROVIDER] Secret {} not found in Vault", secretKey);
            return Optional.empty();
            
        } catch (Exception e) {
            log.error("[VAULT_KEY_PROVIDER] Error retrieving secret {} from Vault: {}", secretKey, e.getMessage());
            throw new RuntimeException("Failed to retrieve secret from Vault", e);
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