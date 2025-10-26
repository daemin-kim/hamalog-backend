package com.Hamalog.security.encryption;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
@ActiveProfiles("test")
class DataEncryptionUtilTest {

    @Autowired
    private DataEncryptionUtil dataEncryptionUtil;

    @Autowired
    private Environment environment;

    @Test
    @DisplayName("문자열을 암호화하고 복호화할 수 있어야 한다")
    void shouldEncryptAndDecryptString() {
        // given
        String originalText = "민감한 개인정보";

        // when
        String encryptedText = dataEncryptionUtil.encrypt(originalText);
        String decryptedText = dataEncryptionUtil.decrypt(encryptedText);

        // then
        assertThat(encryptedText).isNotEqualTo(originalText);
        assertThat(decryptedText).isEqualTo(originalText);
    }

    @Test
    @DisplayName("null이나 빈 문자열을 암호화하면 그대로 반환해야 한다")
    void shouldHandleNullAndEmptyString() {
        // given
        String nullString = null;
        String emptyString = "";

        // when & then
        assertThat(dataEncryptionUtil.encrypt(nullString)).isNull();
        assertThat(dataEncryptionUtil.encrypt(emptyString)).isEmpty();
        assertThat(dataEncryptionUtil.decrypt(nullString)).isNull();
        assertThat(dataEncryptionUtil.decrypt(emptyString)).isEmpty();
    }

    @Test
    @DisplayName("암호화된 문자열은 Base64로 인코딩되어야 한다")
    void shouldProduceBase64EncodedString() {
        // given
        String originalText = "테스트 데이터";

        // when
        String encryptedText = dataEncryptionUtil.encrypt(originalText);

        // then
        assertThatCode(() -> java.util.Base64.getDecoder().decode(encryptedText))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("동일한 평문을 여러 번 암호화하면 다른 결과가 나와야 한다")
    void shouldProduceDifferentCiphertextForSamePlaintext() {
        // given
        String originalText = "동일한 평문";

        // when
        String firstEncryption = dataEncryptionUtil.encrypt(originalText);
        String secondEncryption = dataEncryptionUtil.encrypt(originalText);

        // then
        assertThat(firstEncryption).isNotEqualTo(secondEncryption);
        assertThat(dataEncryptionUtil.decrypt(firstEncryption)).isEqualTo(originalText);
        assertThat(dataEncryptionUtil.decrypt(secondEncryption)).isEqualTo(originalText);
    }

    @Test
    @DisplayName("긴 문자열도 정상적으로 암호화/복호화되어야 한다")
    void shouldHandleLongString() {
        // given
        String longText = "a".repeat(1000);

        // when
        String encryptedText = dataEncryptionUtil.encrypt(longText);
        String decryptedText = dataEncryptionUtil.decrypt(encryptedText);

        // then
        assertThat(decryptedText).isEqualTo(longText);
    }
}
