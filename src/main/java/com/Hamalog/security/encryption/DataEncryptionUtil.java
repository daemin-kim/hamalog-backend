package com.Hamalog.security.encryption;

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

@Component
public class DataEncryptionUtil {

    private static final Logger log = LoggerFactory.getLogger(DataEncryptionUtil.class);
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;

    private final SecretKey secretKey;
    private final Environment environment;

    public DataEncryptionUtil(
            @Value("${hamalog.encryption.key:}") String encryptionKey,
            Environment environment) {
        this.environment = environment;
        this.secretKey = initializeSecretKey(encryptionKey);
    }

    private SecretKey initializeSecretKey(String encryptionKey) {
        // Check if running in production profile
        boolean isProduction = environment.getActiveProfiles().length > 0 && 
                              java.util.Arrays.asList(environment.getActiveProfiles()).contains("prod");
        
        // Check encryption key configuration status (production-safe logging)
        String directEnvValue = environment.getProperty("hamalog.encryption.key");
        log.info("Encryption key configuration status - Key present: {}, Length: {}", 
                directEnvValue != null && !directEnvValue.trim().isEmpty(), 
                directEnvValue == null ? 0 : directEnvValue.length());
        
        if (encryptionKey == null || encryptionKey.trim().isEmpty()) {
            log.error("❌ 프로덕션 환경에서 데이터 암호화 키가 비어있습니다!");
            if (isProduction) {
                String errorMessage = String.format(
                    "데이터 암호화 키가 설정되지 않았습니다. 프로덕션 환경에서는 HAMALOG_ENCRYPTION_KEY 환경변수를 반드시 설정해야 합니다.\n" +
                    "현재 HAMALOG_ENCRYPTION_KEY 상태: %s\n" +
                    "현재 HAMALOG_ENCRYPTION_KEY 길이: %d\n" +
                    "Environment 키 존재 여부: %s\n" +
                    "활성 프로필: %s\n" +
                    "해결 방법: HAMALOG_ENCRYPTION_KEY 환경변수를 Base64 인코딩된 256비트 키로 설정하세요.\n" +
                    "키 생성 예시: openssl rand -base64 32\n" +
                    "Docker 실행 예시: docker run -e HAMALOG_ENCRYPTION_KEY=$(openssl rand -base64 32) your-image",
                    encryptionKey == null ? "NOT_SET" : "EMPTY_OR_INVALID",
                    encryptionKey == null ? 0 : encryptionKey.length(),
                    directEnvValue != null && !directEnvValue.trim().isEmpty() ? "PRESENT" : "NOT_SET",
                    java.util.Arrays.toString(environment.getActiveProfiles())
                );
                log.error("==================== 데이터 암호화 키 초기화 실패 ====================");
                throw new IllegalStateException(errorMessage);
            }
            
            // Generate secure random key for development
            log.warn("⚠️ WARNING: 데이터 암호화 키가 설정되지 않았습니다. 개발용 임시 키를 생성합니다.");
            log.warn("이 키는 재시작마다 변경됩니다. HAMALOG_ENCRYPTION_KEY 환경변수를 설정하여 영구적인 암호화를 사용하세요.");
            
            byte[] randomKey = new byte[32]; // 256 bits
            new SecureRandom().nextBytes(randomKey);
            log.info("✅ 개발용 임시 암호화 키가 생성되었습니다.");
            log.info("==================== 데이터 암호화 키 초기화 완료 (개발 모드) ====================");
            return new SecretKeySpec(randomKey, ALGORITHM);
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
            return new SecretKeySpec(decodedKey, ALGORITHM);
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