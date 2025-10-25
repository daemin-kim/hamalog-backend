package com.Hamalog.security.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class InputValidationUtilTest {

    @Autowired
    private InputValidationUtil inputValidationUtil;

    @Test
    @DisplayName("SQL Injection 패턴을 감지해야 한다")
    void shouldDetectSqlInjectionPattern() {
        // given
        String maliciousInput = "SELECT * FROM users";
        String safeInput = "Hello World";

        // when
        boolean isMaliciousDetected = inputValidationUtil.containsSqlInjection(maliciousInput);
        boolean isSafeDetected = inputValidationUtil.containsSqlInjection(safeInput);

        // then
        assertThat(isMaliciousDetected).isTrue();
        assertThat(isSafeDetected).isFalse();
    }

    @Test
    @DisplayName("XSS 패턴을 감지해야 한다")
    void shouldDetectXssPattern() {
        // given
        String maliciousInput = "<script>alert('xss')</script>";
        String safeInput = "Normal text content";

        // when
        boolean isMaliciousDetected = inputValidationUtil.containsXss(maliciousInput);
        boolean isSafeDetected = inputValidationUtil.containsXss(safeInput);

        // then
        assertThat(isMaliciousDetected).isTrue();
        assertThat(isSafeDetected).isFalse();
    }

    @Test
    @DisplayName("Command Injection 패턴을 감지해야 한다")
    void shouldDetectCommandInjectionPattern() {
        // given
        String maliciousInput = "cat /etc/passwd";
        String safeInput = "normal command";

        // when
        boolean isMaliciousDetected = inputValidationUtil.containsCommandInjection(maliciousInput);
        boolean isSafeDetected = inputValidationUtil.containsCommandInjection(safeInput);

        // then
        assertThat(isMaliciousDetected).isTrue();
        assertThat(isSafeDetected).isFalse();
    }

    @Test
    @DisplayName("Path Traversal 패턴을 감지해야 한다")
    void shouldDetectPathTraversalPattern() {
        // given
        String maliciousInput = "../../../etc/passwd";
        String safeInput = "normal/path/file.txt";

        // when
        boolean isMaliciousDetected = inputValidationUtil.containsPathTraversal(maliciousInput);
        boolean isSafeDetected = inputValidationUtil.containsPathTraversal(safeInput);

        // then
        assertThat(isMaliciousDetected).isTrue();
        assertThat(isSafeDetected).isFalse();
    }

    @Test
    @DisplayName("이메일 형식이 유효한지 검증해야 한다")
    void shouldValidateEmailFormat() {
        // given
        String validEmail = "test@example.com";
        String invalidEmail = "invalid.email@";

        // when
        boolean isValidEmailValid = inputValidationUtil.isValidEmail(validEmail);
        boolean isInvalidEmailValid = inputValidationUtil.isValidEmail(invalidEmail);

        // then
        assertThat(isValidEmailValid).isTrue();
        assertThat(isInvalidEmailValid).isFalse();
    }
}
