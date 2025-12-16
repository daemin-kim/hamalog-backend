package com.Hamalog.security.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("InputValidationUtil 테스트")
class InputValidationUtilTest {

    private InputValidationUtil validationUtil;

    @BeforeEach
    void setUp() {
        validationUtil = new InputValidationUtil();
    }

    @Test
    @DisplayName("일반적인 안전한 문자열은 허용해야 함")
    void shouldAllowSafeStrings() {
        // given
        String safeString = "Hello World 123! 안녕하세요.";

        // when
        String sanitized = validationUtil.sanitizeForLog(safeString);

        // then
        assertThat(sanitized).isEqualTo(safeString);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "select * from users",
        "UNION SELECT password FROM users",
        "DROP TABLE members",
        "1; DELETE FROM users",
        "1 OR 1=1"
    })
    @DisplayName("SQL Injection 패턴은 필터링되어야 함")
    void shouldFilterSqlInjectionPatterns(String input) {
        // when
        String sanitized = validationUtil.sanitizeForLog(input);

        // then
        assertThat(sanitized).isNotEqualTo(input);
        assertThat(sanitized).contains("[FILTERED]");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "<script>alert('XSS')</script>",
        "javascript:alert(1)",
        "<img src=x onerror=alert(1)>",
        "<svg onload=alert(1)>",
        "<iframe src=javascript:alert(1)>"
    })
    @DisplayName("XSS 패턴은 필터링되어야 함")
    void shouldFilterXssPatterns(String input) {
        // when
        String sanitized = validationUtil.sanitizeForLog(input);

        // then
        assertThat(sanitized).isNotEqualTo(input);
        assertThat(sanitized).contains("[FILTERED]");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "cat /etc/passwd",
        "rm -rf /",
        "chmod 777 file.txt",
        "ping 8.8.8.8",
        "curl http://malicious.com"
    })
    @DisplayName("Command Injection 패턴은 필터링되어야 함")
    void shouldFilterCommandInjectionPatterns(String input) {
        // when
        String sanitized = validationUtil.sanitizeForLog(input);

        // then
        assertThat(sanitized).isNotEqualTo(input);
        assertThat(sanitized).contains("[FILTERED]");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "../../../etc/passwd",
        "..\\Windows\\System32",
        "./config.php",
        "/../secrets/",
        "file:///etc/passwd"
    })
    @DisplayName("Path Traversal 패턴은 필터링되어야 함")
    void shouldFilterPathTraversalPatterns(String input) {
        // when
        String sanitized = validationUtil.sanitizeForLog(input);

        // then
        assertThat(sanitized).isNotEqualTo(input);
        assertThat(sanitized).contains("[FILTERED]");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "line1\nline2",
        "column1\tcolumn2",
        "record1\rrecord2",
        "field1\u0000field2"
    })
    @DisplayName("Log Injection 패턴은 필터링되어야 함")
    void shouldFilterLogInjectionPatterns(String input) {
        // when
        String sanitized = validationUtil.sanitizeForLog(input);

        // then
        assertThat(sanitized).isNotEqualTo(input);
        assertThat(sanitized).doesNotContain("\n", "\r", "\t", "\u0000");
    }

    @ParameterizedTest
    @CsvSource({
        "user@example.com,true",
        "invalid-email,false",
        "user.name+tag@example.co.kr,true",
        "@invalid.com,false",
        "user@.com,false"
    })
    @DisplayName("이메일 유효성 검증이 올바르게 동작해야 함")
    void shouldValidateEmailCorrectly(String email, boolean expected) {
        // when
        boolean isValid = validationUtil.isValidEmail(email);

        // then
        assertThat(isValid).isEqualTo(expected);
    }

    @Test
    @DisplayName("Null 입력값은 빈 문자열로 처리되어야 함")
    void shouldHandleNullInput() {
        // when
        String sanitized = validationUtil.sanitizeForLog(null);

        // then
        assertThat(sanitized).isEmpty();
    }

    @Test
    @DisplayName("최대 길이를 초과하는 입력값은 잘려야 함")
    void shouldTruncateLongInput() {
        // given
        String longInput = "a".repeat(1000);

        // when
        String sanitized = validationUtil.sanitizeForLog(longInput);

        // then
        assertThat(sanitized.length()).isLessThan(longInput.length());
    }
}
