package com.Hamalog.security.encryption;

import com.Hamalog.service.vault.VaultKeyProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

@Component
public class DataEncryptionUtil {

    private static final Logger log = LoggerFactory.getLogger(DataEncryptionUtil.class);
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;

    private final SecretKey secretKey;
    private final Environment environment;
    private final VaultKeyProvider vaultKeyProvider;
    private final boolean encryptionDisabled;

    public DataEncryptionUtil(
            @Value("${hamalog.encryption.key:${HAMALOG_ENCRYPTION_KEY:}}") String fallbackEncryptionKey,
            Environment environment,
            @Autowired(required = false) VaultKeyProvider vaultKeyProvider) {
        this.environment = environment;
        this.vaultKeyProvider = vaultKeyProvider;
        
        KeyInitializationResult result = initializeSecretKey(fallbackEncryptionKey);
        this.secretKey = result.secretKey;
        this.encryptionDisabled = result.encryptionDisabled;
    }
    
    private static class KeyInitializationResult {
        final SecretKey secretKey;
        final boolean encryptionDisabled;
        
        KeyInitializationResult(SecretKey secretKey, boolean encryptionDisabled) {
            this.secretKey = secretKey;
            this.encryptionDisabled = encryptionDisabled;
        }
    }

    private KeyInitializationResult initializeSecretKey(String fallbackEncryptionKey) {
        // Check if running in production profile
        boolean isProduction = environment.getActiveProfiles().length > 0 && 
                              java.util.Arrays.asList(environment.getActiveProfiles()).contains("prod");
        
        log.info("[ENCRYPTION_UTIL] Starting encryption key initialization. Production mode: {}, Active profiles: {}", 
                isProduction, java.util.Arrays.toString(environment.getActiveProfiles()));
        
        // Try to get encryption key from Vault first, then fallback to environment variables
        String encryptionKey = null;
        String keySource = "NONE";
        
        // Check VaultKeyProvider availability and configuration
        if (vaultKeyProvider != null) {
            log.info("[ENCRYPTION_UTIL] VaultKeyProvider is available, attempting to retrieve encryption key from Vault");
            
            // Check Vault availability before attempting key retrieval
            boolean vaultAvailable = false;
            try {
                vaultAvailable = vaultKeyProvider.isVaultAvailable();
                if (vaultAvailable) {
                    log.info("[ENCRYPTION_UTIL] Vault availability check: ACCESSIBLE");
                } else {
                    if (isProduction) {
                        log.warn("[ENCRYPTION_UTIL] Vault availability check: NOT_ACCESSIBLE - falling back to environment variables in production");
                    } else {
                        log.info("[ENCRYPTION_UTIL] Vault availability check: NOT_ACCESSIBLE");
                    }
                }
            } catch (Exception e) {
                if (isProduction) {
                    log.warn("[ENCRYPTION_UTIL] Vault availability check failed: {} - {} (falling back to environment variables)", 
                             e.getClass().getSimpleName(), e.getMessage());
                } else {
                    log.error("[ENCRYPTION_UTIL] Failed to check Vault availability: {} - {}", 
                             e.getClass().getSimpleName(), e.getMessage());
                }
            }
            
            // Only attempt vault key retrieval if vault is available
            if (vaultAvailable) {
                try {
                    Optional<String> vaultKey = vaultKeyProvider.getEncryptionKey();
                    if (vaultKey.isPresent() && !vaultKey.get().trim().isEmpty()) {
                        encryptionKey = vaultKey.get();
                        keySource = "VAULT";
                        log.info("[ENCRYPTION_UTIL] Successfully retrieved encryption key from Vault (length: {})", encryptionKey.length());
                    } else {
                        log.warn("[ENCRYPTION_UTIL] Vault returned empty or null encryption key despite being available");
                        log.warn("[ENCRYPTION_UTIL] This could indicate: 1) Missing vault token, 2) Key not found in Vault, 3) Vault authentication issue");
                    }
                } catch (Exception e) {
                    if (isProduction) {
                        log.warn("[ENCRYPTION_UTIL] Failed to retrieve encryption key from Vault: {} - {} (falling back to environment variables)", 
                                 e.getClass().getSimpleName(), e.getMessage());
                    } else {
                        log.error("[ENCRYPTION_UTIL] Failed to retrieve encryption key from Vault: {} - {}", 
                                 e.getClass().getSimpleName(), e.getMessage());
                    }
                }
            } else {
                log.info("[ENCRYPTION_UTIL] Skipping vault key retrieval due to unavailability, proceeding with environment variable fallback");
            }
        } else {
            if (isProduction) {
                log.info("[ENCRYPTION_UTIL] VaultKeyProvider not available - using environment variables for production deployment");
            } else {
                log.warn("[ENCRYPTION_UTIL] VaultKeyProvider is not available (null). Vault integration disabled or failed to initialize.");
            }
        }
        
        // Fallback to direct environment/property injection
        if (encryptionKey == null || encryptionKey.trim().isEmpty()) {
            log.info("[ENCRYPTION_UTIL] Attempting fallback to environment variables/properties");
            
            // Check multiple possible sources
            String[] possibleSources = {
                fallbackEncryptionKey,
                environment.getProperty("hamalog.encryption.key"),
                environment.getProperty("HAMALOG_ENCRYPTION_KEY"),
                System.getenv("HAMALOG_ENCRYPTION_KEY")
            };
            
            for (int i = 0; i < possibleSources.length; i++) {
                String source = possibleSources[i];
                String sourceName = switch (i) {
                    case 0 -> "fallback parameter";
                    case 1 -> "hamalog.encryption.key property";
                    case 2 -> "HAMALOG_ENCRYPTION_KEY property";
                    case 3 -> "HAMALOG_ENCRYPTION_KEY system env";
                    default -> "unknown";
                };
                
                log.debug("[ENCRYPTION_UTIL] Checking {}: {}", sourceName, 
                         source == null ? "null" : (source.isEmpty() ? "empty" : "present (length: " + source.length() + ")"));
                
                if (source != null && !source.trim().isEmpty()) {
                    encryptionKey = source;
                    keySource = sourceName.toUpperCase().replace(" ", "_");
                    log.info("[ENCRYPTION_UTIL] Using encryption key from {}", sourceName);
                    break;
                }
            }
        }
        
        // Check encryption key configuration status (production-safe logging)
        log.info("[ENCRYPTION_UTIL] Encryption key configuration status - Key present: {}, Length: {}, Source: {}", 
                encryptionKey != null && !encryptionKey.trim().isEmpty(), 
                encryptionKey == null ? 0 : encryptionKey.length(),
                keySource);
        
        if (encryptionKey == null || encryptionKey.trim().isEmpty()) {
            log.error("❌ 프로덕션 환경에서 데이터 암호화 키가 비어있습니다!");
            
            // Log detailed diagnostic information
            log.error("[ENCRYPTION_UTIL] Detailed diagnostics:");
            log.error("  - VaultKeyProvider available: {}", vaultKeyProvider != null);
            log.error("  - Key source attempted: {}", keySource);
            log.error("  - Fallback parameter: {}", fallbackEncryptionKey == null ? "null" : "present");
            log.error("  - Environment property 'hamalog.encryption.key': {}", 
                     environment.getProperty("hamalog.encryption.key") == null ? "null" : "present");
            log.error("  - Environment property 'HAMALOG_ENCRYPTION_KEY': {}", 
                     environment.getProperty("HAMALOG_ENCRYPTION_KEY") == null ? "null" : "present");
            log.error("  - System environment 'HAMALOG_ENCRYPTION_KEY': {}", 
                     System.getenv("HAMALOG_ENCRYPTION_KEY") == null ? "null" : "present");
            
            if (isProduction) {
                String errorMessage = String.format(
                    "데이터 암호화 키가 설정되지 않았습니다. 프로덕션 환경에서는 HAMALOG_ENCRYPTION_KEY 환경변수를 반드시 설정해야 합니다.\n" +
                    "현재 키 상태: %s\n" +
                    "시도된 키 소스: %s\n" +
                    "활성 프로필: %s\n" +
                    "VaultKeyProvider 사용 가능: %s\n" +
                    "해결 방법:\n" +
                    "1. Vault를 사용하는 경우: Vault에 encryption-key 시크릿이 올바르게 저장되었는지 확인\n" +
                    "2. 환경변수를 사용하는 경우: HAMALOG_ENCRYPTION_KEY를 Base64 인코딩된 256비트 키로 설정\n" +
                    "키 생성 예시: openssl rand -base64 32\n" +
                    "Docker 실행 예시: docker run -e HAMALOG_ENCRYPTION_KEY=$(openssl rand -base64 32) your-image",
                    encryptionKey == null ? "NOT_SET" : "EMPTY_OR_INVALID",
                    keySource,
                    java.util.Arrays.toString(environment.getActiveProfiles()),
                    vaultKeyProvider != null ? "YES" : "NO"
                );
                log.error("==================== 데이터 암호화 키 초기화 실패 ====================");
                
                // In production, allow the application to start but with a disabled encryption key
                // This prevents application startup failures due to temporary Vault issues
                log.error("⚠️ PRODUCTION WARNING: Starting application with disabled encryption due to missing key");
                log.error("⚠️ All encryption operations will fail until the key is properly configured");
                log.error("⚠️ This should be resolved immediately to ensure data security");
                
                // Return a special marker key that will cause encryption operations to fail gracefully
                byte[] disabledKey = new byte[32]; // All zeros - invalid but allows startup
                log.error("==================== 애플리케이션 시작 (암호화 비활성화) ====================");
                return new KeyInitializationResult(new SecretKeySpec(disabledKey, ALGORITHM), true);
            }
            
            // Generate secure random key for development
            log.warn("⚠️ WARNING: 데이터 암호화 키가 설정되지 않았습니다. 개발용 임시 키를 생성합니다.");
            log.warn("이 키는 재시작마다 변경됩니다. HAMALOG_ENCRYPTION_KEY 환경변수를 설정하여 영구적인 암호화를 사용하세요.");
            
            byte[] randomKey = new byte[32]; // 256 bits
            new SecureRandom().nextBytes(randomKey);
            log.info("✅ 개발용 임시 암호화 키가 생성되었습니다.");
            log.info("==================== 데이터 암호화 키 초기화 완료 (개발 모드) ====================");
            return new KeyInitializationResult(new SecretKeySpec(randomKey, ALGORITHM), false);
        }
        
        try {
            byte[] decodedKey = Base64.getDecoder().decode(encryptionKey);
            if (decodedKey.length != 32) {
                throw new IllegalStateException("데이터 암호화 키는 정확히 256비트(32바이트)여야 합니다. 현재 키 길이: " + (decodedKey.length * 8) + "비트");
            }
            
            if (!isProduction) {
                log.info("✅ 데이터 암호화가 제공된 키로 구성되었습니다.");
            } else {
                log.info("✅ 프로덕션 환경에서 데이터 암호화 키가 성공적으로 설정되었습니다.");
            }
            
            log.info("==================== 데이터 암호화 키 초기화 완료 (제공된 키) ====================");
            return new KeyInitializationResult(new SecretKeySpec(decodedKey, ALGORITHM), false);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("데이터 암호화 키는 유효한 Base64 형식이어야 합니다. 올바른 Base64 형식의 256비트 키를 사용하세요.", e);
        } catch (Exception e) {
            throw new IllegalStateException("데이터 암호화 키 초기화 중 오류가 발생했습니다.", e);
        }
    }

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }

        if (encryptionDisabled) {
            throw new IllegalStateException(
                "❌ 데이터 암호화가 비활성화되었습니다. Vault 또는 환경변수에서 올바른 암호화 키를 설정하세요.\n" +
                "현재 상태: 암호화 키가 설정되지 않아 애플리케이션이 안전하지 않은 모드로 실행 중입니다.\n" +
                "해결 방법:\n" +
                "1. Vault에 encryption-key 시크릿이 올바르게 저장되어 있는지 확인\n" +
                "2. HAMALOG_ENCRYPTION_KEY 환경변수를 Base64 인코딩된 256비트 키로 설정\n" +
                "3. 애플리케이션을 재시작하여 키를 다시 로드"
            );
        }

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);
            byte[] cipherText = cipher.doFinal(plainText.getBytes());
            
            // Combine IV and ciphertext
            byte[] encryptedData = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, encryptedData, 0, iv.length);
            System.arraycopy(cipherText, 0, encryptedData, iv.length, cipherText.length);
            
            return Base64.getEncoder().encodeToString(encryptedData);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }

        if (encryptionDisabled) {
            throw new IllegalStateException(
                "❌ 데이터 복호화가 비활성화되었습니다. Vault 또는 환경변수에서 올바른 암호화 키를 설정하세요.\n" +
                "현재 상태: 암호화 키가 설정되지 않아 기존 암호화된 데이터를 복호화할 수 없습니다.\n" +
                "해결 방법:\n" +
                "1. Vault에 encryption-key 시크릿이 올바르게 저장되어 있는지 확인\n" +
                "2. HAMALOG_ENCRYPTION_KEY 환경변수를 Base64 인코딩된 256비트 키로 설정\n" +
                "3. 애플리케이션을 재시작하여 키를 다시 로드"
            );
        }

        try {
            byte[] encryptedData = Base64.getDecoder().decode(encryptedText);
            
            // Extract IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(encryptedData, 0, iv, 0, iv.length);
            
            // Extract ciphertext
            byte[] cipherText = new byte[encryptedData.length - GCM_IV_LENGTH];
            System.arraycopy(encryptedData, GCM_IV_LENGTH, cipherText, 0, cipherText.length);
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);
            
            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}