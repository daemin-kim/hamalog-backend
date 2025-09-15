package com.Hamalog.security.encryption;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Empty Encryption Key Issue Reproduction Tests")
class EmptyKeyReproductionTest {

    @Test
    @DisplayName("Should throw exception when encryption key is empty string in production")
    void initializeSecretKey_EmptyKeyInProduction_ThrowsException() {
        // given
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        String emptyKey = ""; // This simulates the issue from deploy.sh
        
        // when & then
        IllegalStateException exception = assertThrows(IllegalStateException.class, 
            () -> new DataEncryptionUtil(emptyKey, environment, null));
        
        assertTrue(exception.getMessage().contains("데이터 암호화 키가 설정되지 않았습니다"));
        assertTrue(exception.getMessage().contains("HAMALOG_ENCRYPTION_KEY"));
        System.out.println("[DEBUG_LOG] Exception message: " + exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when encryption key is null in production")
    void initializeSecretKey_NullKeyInProduction_ThrowsException() {
        // given
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        String nullKey = null;
        
        // when & then
        IllegalStateException exception = assertThrows(IllegalStateException.class, 
            () -> new DataEncryptionUtil(nullKey, environment, null));
        
        assertTrue(exception.getMessage().contains("데이터 암호화 키가 설정되지 않았습니다"));
        System.out.println("[DEBUG_LOG] Exception message: " + exception.getMessage());
    }

    @Test
    @DisplayName("Should generate temporary key when encryption key is empty in development")
    void initializeSecretKey_EmptyKeyInDevelopment_GeneratesTemporaryKey() {
        // given
        MockEnvironment environment = new MockEnvironment();
        // No active profiles = development mode
        String emptyKey = "";
        
        // when
        DataEncryptionUtil util = new DataEncryptionUtil(emptyKey, environment, null);
        
        // then
        assertNotNull(util);
        // Should be able to encrypt/decrypt with temporary key
        String testData = "test data";
        String encrypted = util.encrypt(testData);
        String decrypted = util.decrypt(encrypted);
        assertEquals(testData, decrypted);
        
        System.out.println("[DEBUG_LOG] Development mode: temporary key generated successfully");
    }

    @Test
    @DisplayName("Should work correctly when valid encryption key is provided")
    void initializeSecretKey_ValidKey_WorksCorrectly() {
        // given
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        // Generate a valid base64 32-byte key (exactly 32 bytes = 256 bits)
        String validKey = "bkfhn2mnq/kXp6V3yGuPYGmtCTweKwr0IyEkC2Rp4lA="; // base64 encoded 32-byte key
        
        // when
        DataEncryptionUtil util = new DataEncryptionUtil(validKey, environment, null);
        
        // then
        assertNotNull(util);
        // Should be able to encrypt/decrypt
        String testData = "sensitive data";
        String encrypted = util.encrypt(testData);
        String decrypted = util.decrypt(encrypted);
        assertEquals(testData, decrypted);
        
        System.out.println("[DEBUG_LOG] Production mode: valid key works correctly");
    }
}