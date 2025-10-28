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
        "(?i)(select|insert|update|delete|drop|create|alter|exec|execute|union)\\s|\\b(or|and)\\s+['\"0-9]|\\b(or|and)\\s+[0-9a-zA-Z]+\\s*=\\s*[0-9a-zA-Z]+",
        Pattern.CASE_INSENSITIVE
    );

    // XSS 패턴
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(?i)(<script|javascript:|onload=|onerror=|<iframe)",
        Pattern.CASE_INSENSITIVE
    );

    // Command Injection 패턴
    private static final Pattern COMMAND_INJECTION_PATTERN = Pattern.compile(
        "(?i)(cat|rm|chmod|wget|curl|ping)\\s|[;&|]",
        Pattern.CASE_INSENSITIVE
    );

    // Path Traversal 패턴
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(
        "(?i)(\\.\\./|\\.\\.\\\\|^/|^\\\\|^\\.\\.?/|^\\.\\.?\\\\|file://|\\\\\\\\|//[^/])",
        Pattern.CASE_INSENSITIVE
    );

    // Log Injection 패턴 (CRLF Injection)
    private static final Pattern LOG_INJECTION_PATTERN = Pattern.compile(
        "[\\r\\n\\t\\x00-\\x1f\\x7f-\\x9f]"
    );

    // 이메일 패턴
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    private static final int MAX_INPUT_LENGTH = 500;

    /**
     * 입력값 검증 예외
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
     * 로깅을 위한 입력값 정제
     */
    public String sanitizeForLog(String input) {
        if (input == null) {
            return "";
        }

        String sanitized = input;

        // 최대 길이 제한
        if (sanitized.length() > MAX_INPUT_LENGTH) {
            sanitized = sanitized.substring(0, MAX_INPUT_LENGTH) + "...";
        }

        // 위험한 패턴 검사 및 필터링
        if (SQL_INJECTION_PATTERN.matcher(sanitized).find()) {
            return "[FILTERED] SQL_INJECTION";
        }
        if (XSS_PATTERN.matcher(sanitized).find()) {
            return "[FILTERED] XSS";
        }
        if (COMMAND_INJECTION_PATTERN.matcher(sanitized).find()) {
            return "[FILTERED] COMMAND_INJECTION";
        }
        if (PATH_TRAVERSAL_PATTERN.matcher(sanitized).find()) {
            return "[FILTERED] PATH_TRAVERSAL";
        }

        // Log Injection (CRLF) 제거
        return LOG_INJECTION_PATTERN.matcher(sanitized).replaceAll(" ");
    }

    /**
     * 이메일 유효성 검증
     */
    public boolean isValidEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

}