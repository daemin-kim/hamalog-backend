package com.Hamalog.security.validation;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * 입력값 검증 및 정제 유틸리티
 * OWASP Top 10의 Injection 공격 방어
 */
@Component
public class InputValidationUtil {

    // SQL Injection 패턴
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i).*(union|select|insert|update|delete|drop|create|alter|exec|execute|script|javascript|vbscript|onload|onerror|alert|eval|expression|import|meta|link|object|embed|applet|form|iframe|img|svg).*",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    // XSS 패턴
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(?i).*(<script|</script>|javascript:|vbscript:|onload=|onerror=|onmouseover=|onfocus=|onblur=|onclick=|ondblclick=|onmousedown=|onmouseup=|onkeydown=|onkeyup=|onkeypress=|alert\\(|eval\\(|expression\\(|<iframe|</iframe>|<object|</object>|<embed|</embed>|<applet|</applet>|<meta|<link).*",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    // Command Injection 패턴
    private static final Pattern COMMAND_INJECTION_PATTERN = Pattern.compile(
        "(?i)(^|\\s+)(cat|ls|dir|type|copy|move|del|rm|chmod|chown|wget|curl|ping|netstat|ps|kill|sudo|su)(\\s+.*|$)|[;&|`]",
        Pattern.CASE_INSENSITIVE
    );

    // Path Traversal 패턴
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(
        ".*(\\.\\.[\\\\/]|[\\\\/]\\.\\.[\\\\/]|\\.\\.\\\\|\\.\\./).*"
    );

    // Log Injection 패턴 (CRLF Injection)
    private static final Pattern LOG_INJECTION_PATTERN = Pattern.compile(
        ".*[\\r\\n\\t\\x00-\\x1f\\x7f-\\x9f].*"
    );

    // 안전한 문자 패턴 (알파벳, 숫자, 일부 특수문자)
    private static final Pattern SAFE_STRING_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9\\s가-힣ㄱ-ㅎㅏ-ㅣ._@#$%^&*()\\-+=\\[\\]{}|\\\\:;\"'<>,.?/~`!]*$"
    );

    // 이메일 패턴
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    // 전화번호 패턴 (한국 형식)
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^01[016789]-\\d{3,4}-\\d{4}$"
    );

    /**
     * 일반적인 입력값 검증
     */
    public boolean isValidInput(String input) {
        if (!StringUtils.hasText(input)) {
            return true; // 빈 값은 허용 (필수값 검증은 별도)
        }

        // 위험한 패턴 체크
        if (containsSqlInjection(input) ||
            containsXss(input) ||
            containsCommandInjection(input) ||
            containsPathTraversal(input) ||
            containsLogInjection(input)) {
            return false;
        }

        // 안전한 문자 패턴 체크
        return SAFE_STRING_PATTERN.matcher(input).matches();
    }

    /**
     * SQL Injection 패턴 검사
     */
    public boolean containsSqlInjection(String input) {
        if (!StringUtils.hasText(input)) {
            return false;
        }
        return SQL_INJECTION_PATTERN.matcher(input).matches();
    }

    /**
     * XSS 패턴 검사
     */
    public boolean containsXss(String input) {
        if (!StringUtils.hasText(input)) {
            return false;
        }
        return XSS_PATTERN.matcher(input).matches();
    }

    /**
     * Command Injection 패턴 검사
     */
    public boolean containsCommandInjection(String input) {
        if (!StringUtils.hasText(input)) {
            return false;
        }
        return COMMAND_INJECTION_PATTERN.matcher(input).matches();
    }

    /**
     * Path Traversal 패턴 검사
     */
    public boolean containsPathTraversal(String input) {
        if (!StringUtils.hasText(input)) {
            return false;
        }
        return PATH_TRAVERSAL_PATTERN.matcher(input).matches();
    }

    /**
     * Log Injection 패턴 검사 (CRLF Injection)
     */
    public boolean containsLogInjection(String input) {
        if (!StringUtils.hasText(input)) {
            return false;
        }
        return LOG_INJECTION_PATTERN.matcher(input).matches();
    }

    /**
     * 이메일 주소 검증
     */
    public boolean isValidEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches() && 
               email.length() <= 320 && // RFC 5321 제한
               isValidInput(email);
    }

    /**
     * 전화번호 검증 (한국 형식)
     */
    public boolean isValidPhoneNumber(String phone) {
        if (!StringUtils.hasText(phone)) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone).matches();
    }

    /**
     * 문자열 길이 검증
     */
    public boolean isValidLength(String input, int minLength, int maxLength) {
        if (input == null) {
            return minLength <= 0;
        }
        int length = input.length();
        return length >= minLength && length <= maxLength;
    }

    /**
     * 로그용 문자열 정제 (Log Injection 방지)
     */
    public String sanitizeForLog(String input) {
        if (!StringUtils.hasText(input)) {
            return input;
        }
        
        // 제어 문자 및 CRLF 제거
        return input.replaceAll("[\\r\\n\\t\\x00-\\x1f\\x7f-\\x9f]", "_")
                   .replaceAll("\\s+", " ") // 연속 공백 정리
                   .trim();
    }

    /**
     * HTML 출력용 문자열 정제 (XSS 방지)
     */
    public String sanitizeForHtml(String input) {
        if (!StringUtils.hasText(input)) {
            return input;
        }
        
        return input.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;")
                   .replace("/", "&#x2F;");
    }

    /**
     * 파일명 정제 (Path Traversal 방지)
     */
    public String sanitizeFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return fileName;
        }
        
        // 위험한 문자 제거
        return fileName.replaceAll("[^a-zA-Z0-9가-힣._\\-]", "_")
                      .replaceAll("\\.{2,}", ".") // 연속된 점 제거
                      .replaceAll("^[._\\-]+", "") // 시작 부분 특수문자 제거
                      .trim();
    }

    /**
     * 검증 실패 시 발생시킬 예외 생성
     */
    public static class InputValidationException extends RuntimeException {
        public InputValidationException(String message) {
            super(message);
        }
        
        public InputValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * 입력값 검증 및 예외 발생
     */
    public void validateInput(String input, String fieldName) {
        if (!isValidInput(input)) {
            throw new InputValidationException(
                String.format("유효하지 않은 입력값입니다: %s", fieldName)
            );
        }
    }

    /**
     * 이메일 검증 및 예외 발생
     */
    public void validateEmail(String email) {
        if (!isValidEmail(email)) {
            throw new InputValidationException("유효하지 않은 이메일 주소입니다");
        }
    }

    /**
     * 전화번호 검증 및 예외 발생
     */
    public void validatePhoneNumber(String phone) {
        if (!isValidPhoneNumber(phone)) {
            throw new InputValidationException("유효하지 않은 전화번호입니다");
        }
    }


}