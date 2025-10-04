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

    @Test
    @DisplayName("Should generate different encrypted text for same input due to random IV")
    void encrypt_SameInput_ShouldGenerateDifferentEncryptedText() {
        // given
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        String validKey = "4MoGUKm/b9RXqFtUgxwK3BpVQF/RtZFMb4EwdzaRSlg=";
        DataEncryptionUtil encryptionUtil = new DataEncryptionUtil(validKey, environment);
        String plainText = "Same text for encryption";

        // when
        String encrypted1 = encryptionUtil.encrypt(plainText);
        String encrypted2 = encryptionUtil.encrypt(plainText);

        // then
        assertThat(encrypted1).isNotEqualTo(encrypted2);
        assertThat(encryptionUtil.decrypt(encrypted1)).isEqualTo(plainText);
        assertThat(encryptionUtil.decrypt(encrypted2)).isEqualTo(plainText);
    }

    @Test
    @DisplayName("Should encrypt and decrypt Korean characters correctly")
    void encryptAndDecrypt_WithKoreanText_ShouldWorkCorrectly() {
        // given
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        String validKey = "4MoGUKm/b9RXqFtUgxwK3BpVQF/RtZFMb4EwdzaRSlg=";
        DataEncryptionUtil encryptionUtil = new DataEncryptionUtil(validKey, environment);
        String koreanText = "한글 데이터 암호화 테스트 🔐";

        // when
        String encrypted = encryptionUtil.encrypt(koreanText);
        String decrypted = encryptionUtil.decrypt(encrypted);

        // then
        assertThat(decrypted).isEqualTo(koreanText);
    }

    @Test
    @DisplayName("Should encrypt and decrypt special characters correctly")
    void encryptAndDecrypt_WithSpecialCharacters_ShouldWorkCorrectly() {
        // given
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        String validKey = "4MoGUKm/b9RXqFtUgxwK3BpVQF/RtZFMb4EwdzaRSlg=";
        DataEncryptionUtil encryptionUtil = new DataEncryptionUtil(validKey, environment);
        String specialText = "!@#$%^&*()_+-=[]{}|;:,.<>?/~`\"'\\";

        // when
        String encrypted = encryptionUtil.encrypt(specialText);
        String decrypted = encryptionUtil.decrypt(encrypted);

        // then
        assertThat(decrypted).isEqualTo(specialText);
    }

    @Test
    @DisplayName("Should encrypt and decrypt long text correctly")
    void encryptAndDecrypt_WithLongText_ShouldWorkCorrectly() {
        // given
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        String validKey = "4MoGUKm/b9RXqFtUgxwK3BpVQF/RtZFMb4EwdzaRSlg=";
        DataEncryptionUtil encryptionUtil = new DataEncryptionUtil(validKey, environment);
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longText.append("Long patient medical record data item ").append(i).append(" ");
        }

        // when
        String encrypted = encryptionUtil.encrypt(longText.toString());
        String decrypted = encryptionUtil.decrypt(encrypted);

        // then
        assertThat(decrypted).isEqualTo(longText.toString());
    }

    @Test
    @DisplayName("Should handle invalid encrypted data during decryption")
    void decrypt_WithInvalidData_ShouldThrowException() {
        // given
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        String validKey = "4MoGUKm/b9RXqFtUgxwK3BpVQF/RtZFMb4EwdzaRSlg=";
        DataEncryptionUtil encryptionUtil = new DataEncryptionUtil(validKey, environment);

        // when & then
        assertThatThrownBy(() -> encryptionUtil.decrypt("invalid-encrypted-data"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Decryption failed");
    }

    @Test
    @DisplayName("Should prioritize environment property over fallback key")
    void constructor_WithEnvironmentProperty_ShouldUseEnvironmentKey() {
        // given
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("HAMALOG_ENCRYPTION_KEY")).thenReturn("4MoGUKm/b9RXqFtUgxwK3BpVQF/RtZFMb4EwdzaRSlg=");
        when(environment.getProperty("hamalog.encryption.key")).thenReturn(null);
        String fallbackKey = "different-fallback-key";

        // when
        DataEncryptionUtil encryptionUtil = new DataEncryptionUtil(fallbackKey, environment);

        // then - should work without exception, indicating valid key was used
        String encrypted = encryptionUtil.encrypt("test data");
        String decrypted = encryptionUtil.decrypt(encrypted);
        assertThat(decrypted).isEqualTo("test data");
    }
}