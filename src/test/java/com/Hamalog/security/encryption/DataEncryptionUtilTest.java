package com.Hamalog.security.encryption;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Data Encryption Utility Tests")
class DataEncryptionUtilTest {

    @Mock
    private Environment environment;

    @Test
    @DisplayName("Should start with disabled encryption when encryption key is empty in production")
    void initializeSecretKey_EmptyKeyInProduction_StartsWithDisabledEncryption() {
        // given
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        String emptyKey = "";

        // when
        DataEncryptionUtil encryptionUtil = new DataEncryptionUtil(emptyKey, environment);

        // then - Should not throw during initialization
        assertThat(encryptionUtil).isNotNull();
        
        // But should throw when trying to encrypt
        assertThatThrownBy(() -> encryptionUtil.encrypt("test data"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("❌ 데이터 암호화가 비활성화되었습니다")
                .hasMessageContaining("환경변수에서 올바른 암호화 키를 설정하세요");
    }

    @Test
    @DisplayName("Should start with disabled encryption when encryption key is null in production")
    void initializeSecretKey_NullKeyInProduction_StartsWithDisabledEncryption() {
        // given
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        String nullKey = null;

        // when
        DataEncryptionUtil encryptionUtil = new DataEncryptionUtil(nullKey, environment);

        // then - Should not throw during initialization
        assertThat(encryptionUtil).isNotNull();
        
        // But should throw when trying to decrypt
        assertThatThrownBy(() -> encryptionUtil.decrypt("encrypted-data"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("❌ 데이터 암호화가 비활성화되었습니다")
                .hasMessageContaining("환경변수에서 올바른 암호화 키를 설정하세요");
    }

    @Test
    @DisplayName("Should create random key for development when encryption key is empty")
    void initializeSecretKey_EmptyKeyInDevelopment_CreatesRandomKey() {
        // given
        when(environment.getActiveProfiles()).thenReturn(new String[]{"local"});
        String emptyKey = "";

        // when & then
        assertThatCode(() -> new DataEncryptionUtil(emptyKey, environment))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should create encryption util with valid base64 key")
    void initializeSecretKey_ValidBase64Key_Success() {
        // given
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        String validKey = "4MoGUKm/b9RXqFtUgxwK3BpVQF/RtZFMb4EwdzaRSlg="; // 32-byte base64 key

        // when & then
        assertThatCode(() -> new DataEncryptionUtil(validKey, environment))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should throw exception for invalid base64 key")
    void initializeSecretKey_InvalidBase64Key_ThrowsException() {
        // given
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        String invalidKey = "invalid-base64-key!!!";

        // when & then
        assertThatThrownBy(() -> new DataEncryptionUtil(invalidKey, environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("데이터 암호화 키는 유효한 Base64 형식이어야 합니다");
    }

    @Test
    @DisplayName("Should throw exception for wrong key length")
    void initializeSecretKey_WrongKeyLength_ThrowsException() {
        // given
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        String shortKey = "c2hvcnQta2V5"; // "short-key" in base64 (too short)

        // when & then
        assertThatThrownBy(() -> new DataEncryptionUtil(shortKey, environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("데이터 암호화 키 초기화 중 오류가 발생했습니다")
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("데이터 암호화 키는 정확히 256비트(32바이트)여야 합니다. 현재 키 길이: 72비트");
    }

    @Test
    @DisplayName("Should encrypt and decrypt text successfully")
    void encryptAndDecrypt_ValidInput_Success() {
        // given
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        String validKey = "4MoGUKm/b9RXqFtUgxwK3BpVQF/RtZFMb4EwdzaRSlg=";
        DataEncryptionUtil encryptionUtil = new DataEncryptionUtil(validKey, environment);
        String plainText = "sensitive data to encrypt";

        // when
        String encrypted = encryptionUtil.encrypt(plainText);
        String decrypted = encryptionUtil.decrypt(encrypted);

        // then
        assertThat(encrypted).isNotEqualTo(plainText);
        assertThat(decrypted).isEqualTo(plainText);
    }

    @Test
    @DisplayName("Should handle null and empty strings without encryption")
    void encryptAndDecrypt_NullAndEmpty_ReturnsAsIs() {
        // given
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        String validKey = "4MoGUKm/b9RXqFtUgxwK3BpVQF/RtZFMb4EwdzaRSlg=";
        DataEncryptionUtil encryptionUtil = new DataEncryptionUtil(validKey, environment);

        // when & then
        assertThat(encryptionUtil.encrypt(null)).isNull();
        assertThat(encryptionUtil.encrypt("")).isEmpty();
        assertThat(encryptionUtil.decrypt(null)).isNull();
        assertThat(encryptionUtil.decrypt("")).isEmpty();
    }
}