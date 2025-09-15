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
    @DisplayName("Should throw IllegalStateException when encryption key is empty in production")
    void initializeSecretKey_EmptyKeyInProduction_ThrowsException() {
        // given
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        String emptyKey = "";

        // when & then
        assertThatThrownBy(() -> new DataEncryptionUtil(emptyKey, environment, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("데이터 암호화 키가 설정되지 않았습니다")
                .hasMessageContaining("HAMALOG_ENCRYPTION_KEY 환경변수를 반드시 설정해야 합니다")
                .hasMessageContaining("현재 HAMALOG_ENCRYPTION_KEY 상태: EMPTY_OR_INVALID");
    }

    @Test
    @DisplayName("Should throw IllegalStateException when encryption key is null in production")
    void initializeSecretKey_NullKeyInProduction_ThrowsException() {
        // given
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        String nullKey = null;

        // when & then
        assertThatThrownBy(() -> new DataEncryptionUtil(nullKey, environment, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("데이터 암호화 키가 설정되지 않았습니다")
                .hasMessageContaining("현재 HAMALOG_ENCRYPTION_KEY 상태: NOT_SET");
    }

    @Test
    @DisplayName("Should create random key for development when encryption key is empty")
    void initializeSecretKey_EmptyKeyInDevelopment_CreatesRandomKey() {
        // given
        when(environment.getActiveProfiles()).thenReturn(new String[]{"local"});
        String emptyKey = "";

        // when & then
        assertThatCode(() -> new DataEncryptionUtil(emptyKey, environment, null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should create encryption util with valid base64 key")
    void initializeSecretKey_ValidBase64Key_Success() {
        // given
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        String validKey = "4MoGUKm/b9RXqFtUgxwK3BpVQF/RtZFMb4EwdzaRSlg="; // 32-byte base64 key

        // when & then
        assertThatCode(() -> new DataEncryptionUtil(validKey, environment, null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should throw exception for invalid base64 key")
    void initializeSecretKey_InvalidBase64Key_ThrowsException() {
        // given
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        String invalidKey = "invalid-base64-key!!!";

        // when & then
        assertThatThrownBy(() -> new DataEncryptionUtil(invalidKey, environment, null))
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
        assertThatThrownBy(() -> new DataEncryptionUtil(shortKey, environment, null))
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
        DataEncryptionUtil encryptionUtil = new DataEncryptionUtil(validKey, environment, null);
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
        DataEncryptionUtil encryptionUtil = new DataEncryptionUtil(validKey, environment, null);

        // when & then
        assertThat(encryptionUtil.encrypt(null)).isNull();
        assertThat(encryptionUtil.encrypt("")).isEmpty();
        assertThat(encryptionUtil.decrypt(null)).isNull();
        assertThat(encryptionUtil.decrypt("")).isEmpty();
    }
}