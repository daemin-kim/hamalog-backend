package com.Hamalog.security.encryption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class DataEncryptionUtil {

    private static final Logger log = LoggerFactory.getLogger(DataEncryptionUtil.class);
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;

    private final SecretKeySpec secretKey;
    private final Environment environment;
    private final boolean encryptionDisabled;
    private final SecureRandom secureRandom;

    public DataEncryptionUtil(
            @Value("${hamalog.encryption.key:${HAMALOG_ENCRYPTION_KEY:}}") String fallbackEncryptionKey,
            Environment environment) {
        this.environment = environment;
        this.secureRandom = new SecureRandom();
        
        KeyInitializationResult result = initializeSecretKey(fallbackEncryptionKey, this.secureRandom);
        this.secretKey = result.secretKey;
        this.encryptionDisabled = result.encryptionDisabled;
    }
    
    private static class KeyInitializationResult {
        final SecretKeySpec secretKey;
        final boolean encryptionDisabled;
        
        KeyInitializationResult(SecretKeySpec secretKey, boolean encryptionDisabled) {
            this.secretKey = secretKey;
            this.encryptionDisabled = encryptionDisabled;
        }
    }

    private KeyInitializationResult initializeSecretKey(String fallbackEncryptionKey, SecureRandom secureRandom) {
        // Check if running in production profile
        boolean isProduction = environment.getActiveProfiles().length > 0 && 
                              java.util.Arrays.asList(environment.getActiveProfiles()).contains("prod");
        
        // Get encryption key from environment variables and properties
        String encryptionKey = null;
        String keySource = "NONE";
        
        // Check multiple possible sources - prioritize system environment variables over Spring properties
        // This ensures Docker/production environment variables take precedence
        String[] possibleSources = {
            System.getenv("HAMALOG_ENCRYPTION_KEY"),
            environment.getProperty("HAMALOG_ENCRYPTION_KEY"),
            environment.getProperty("hamalog.encryption.key"),
            fallbackEncryptionKey
        };
        
        String[] sourceNames = {
            "HAMALOG_ENCRYPTION_KEY system env",
            "HAMALOG_ENCRYPTION_KEY property", 
            "hamalog.encryption.key property",
            "fallback parameter"
        };
        
        for (int i = 0; i < possibleSources.length; i++) {
            String source = possibleSources[i];
            String sourceName = sourceNames[i];
            
            if (source != null && !source.trim().isEmpty()) {
                encryptionKey = source;
                keySource = sourceName.toUpperCase().replace(" ", "_");
                log.debug("Using encryption key from {}", sourceName);
                break;
            }
        }
        
        if (encryptionKey == null || encryptionKey.trim().isEmpty()) {
            log.error("데이터 암호화 키가 설정되지 않았습니다. 키 소스: {}", keySource);
            
            if (isProduction) {
                log.error("프로덕션 환경에서 암호화 키 누락. HAMALOG_ENCRYPTION_KEY 환경변수를 Base64 인코딩된 256비트 키로 설정하세요. 키 생성: openssl rand -base64 32");
                log.warn("암호화가 비활성화된 상태로 애플리케이션을 시작합니다. 보안을 위해 즉시 키를 설정해야 합니다.");
                
                // Return a special marker key that will cause encryption operations to fail gracefully
                byte[] disabledKey = new byte[32]; // All zeros - invalid but allows startup
                return new KeyInitializationResult(new SecretKeySpec(disabledKey, ALGORITHM), true);
            }
            
            // Generate secure random key for development
            log.warn("개발용 임시 암호화 키를 생성합니다. 재시작 시마다 변경됩니다.");
            
            byte[] randomKey = new byte[32]; // 256 bits
            secureRandom.nextBytes(randomKey);
            return new KeyInitializationResult(new SecretKeySpec(randomKey, ALGORITHM), false);
        }
        
        try {
            byte[] decodedKey = Base64.getDecoder().decode(encryptionKey);
            if (decodedKey.length != 32) {
                throw new IllegalStateException("데이터 암호화 키는 정확히 256비트(32바이트)여야 합니다. 현재 키 길이: " + (decodedKey.length * 8) + "비트");
            }
            
            log.debug("데이터 암호화 키 초기화 완료");
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
                "❌ 데이터 암호화가 비활성화되었습니다. 환경변수에서 올바른 암호화 키를 설정하세요.\n" +
                "현재 상태: 암호화 키가 설정되지 않아 애플리케이션이 안전하지 않은 모드로 실행 중입니다.\n" +
                "해결 방법:\n" +
                "1. HAMALOG_ENCRYPTION_KEY 환경변수를 Base64 인코딩된 256비트 키로 설정\n" +
                "2. 키 생성: openssl rand -base64 32\n" +
                "3. 애플리케이션 재시작"
            );
        }

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);

            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);

            byte[] encryptedData = cipher.doFinal(plainText.getBytes());

            // Combine IV and encrypted data
            byte[] encryptedWithIv = new byte[GCM_IV_LENGTH + encryptedData.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedData, 0, encryptedWithIv, GCM_IV_LENGTH, encryptedData.length);

            return Base64.getEncoder().encodeToString(encryptedWithIv);
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
                "❌ 데이터 암호화가 비활성화되었습니다. 환경변수에서 올바른 암호화 키를 설정하세요.\n" +
                "현재 상태: 암호화 키가 설정되지 않아 복호화를 수행할 수 없습니다.\n" +
                "해결 방법:\n" +
                "1. HAMALOG_ENCRYPTION_KEY 환경변수를 Base64 인코딩된 256비트 키로 설정\n" +
                "2. 키 생성: openssl rand -base64 32\n" +
                "3. 애플리케이션 재시작"
            );
        }

        try {
            byte[] encryptedWithIv = Base64.getDecoder().decode(encryptedText);

            if (encryptedWithIv.length < GCM_IV_LENGTH) {
                throw new IllegalArgumentException("Invalid encrypted data length");
            }

            // Extract IV and encrypted data
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encryptedData = new byte[encryptedWithIv.length - GCM_IV_LENGTH];
            System.arraycopy(encryptedWithIv, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedWithIv, GCM_IV_LENGTH, encryptedData, 0, encryptedData.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);

            byte[] plainText = cipher.doFinal(encryptedData);
            return new String(plainText);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}