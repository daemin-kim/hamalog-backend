package com.Hamalog.handler;

import com.Hamalog.logging.MDCUtil;
import com.Hamalog.security.filter.TrustedProxyService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * 예외 처리 유틸리티 클래스
 * GlobalExceptionHandler에서 사용하는 공통 유틸리티 메서드를 제공합니다.
 */
@Component
public class ExceptionHandlerUtils {

    private final TrustedProxyService trustedProxyService;

    // Critical error patterns that require immediate attention
    private static final String[] CRITICAL_ERROR_PATTERNS = {
        "OutOfMemoryError",
        "StackOverflowError",
        "DatabaseConnectionException",
        "SecurityException",
        "DataIntegrityViolationException"
    };

    public ExceptionHandlerUtils(TrustedProxyService trustedProxyService) {
        this.trustedProxyService = trustedProxyService;
    }

    /**
     * Create comprehensive error context for structured logging
     */
    public Map<String, Object> createErrorContext(
            Exception ex, HttpServletRequest request, ErrorSeverity severity) {
        Map<String, Object> context = new HashMap<>();

        // Basic error information
        context.put("error_type", ex.getClass().getSimpleName());
        context.put("error_message", sanitizeErrorMessage(ex.getMessage()));
        context.put("error_severity", severity.name());
        context.put("stack_trace", getStackTraceString(ex));

        // Request context
        if (request != null) {
            context.put("request_method", request.getMethod());
            context.put("request_path", request.getRequestURI());
            context.put("request_query", request.getQueryString());
            context.put("user_agent", sanitizeUserAgent(request.getHeader("User-Agent")));
            context.put("client_ip", resolveClientIpAddress(request));
        }

        // User context
        context.put("user_id", MDCUtil.get(MDCUtil.USER_ID));
        context.put("session_id", MDCUtil.get(MDCUtil.SESSION_ID));
        context.put("correlation_id", MDCUtil.get(MDCUtil.CORRELATION_ID));

        // System context
        context.put("hostname", MDCUtil.get(MDCUtil.HOSTNAME));
        context.put("thread_name", Thread.currentThread().getName());
        context.put("timestamp", System.currentTimeMillis());

        return context;
    }

    /**
     * Determine error severity based on exception type and patterns
     */
    public ErrorSeverity determineErrorSeverity(Exception ex) {
        String exceptionName = ex.getClass().getSimpleName();
        String errorMessage = ex.getMessage();

        // Critical errors requiring immediate attention
        for (String pattern : CRITICAL_ERROR_PATTERNS) {
            if (exceptionName.contains(pattern)
                    || (errorMessage != null && errorMessage.contains(pattern))) {
                return ErrorSeverity.CRITICAL;
            }
        }

        // High severity errors
        if (ex instanceof SecurityException
                || ex instanceof AccessDeniedException
                || exceptionName.contains("Security")
                || exceptionName.contains("Auth")) {
            return ErrorSeverity.HIGH;
        }

        // Medium severity errors
        if (ex instanceof IllegalArgumentException
                || ex instanceof IllegalStateException
                || exceptionName.contains("Validation")
                || exceptionName.contains("Constraint")) {
            return ErrorSeverity.MEDIUM;
        }

        // Default to low severity
        return ErrorSeverity.LOW;
    }

    /**
     * Get sanitized stack trace as string
     */
    public String getStackTraceString(Exception ex) {
        if (ex == null) return "";

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        String stackTrace = sw.toString();

        // Limit stack trace size to prevent log bloat
        return stackTrace.length() > 2000 ? stackTrace.substring(0, 2000) + "...[truncated]" : stackTrace;
    }

    /**
     * Sanitize error message to prevent log injection
     */
    public String sanitizeErrorMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "No error message available";
        }
        // Remove potentially dangerous characters and limit length
        String sanitized = message.replaceAll("[\\r\\n\\t]", " ").trim();
        return sanitized.length() > 500 ? sanitized.substring(0, 500) + "..." : sanitized;
    }

    /**
     * Sanitize user agent to prevent log injection
     */
    public String sanitizeUserAgent(String userAgent) {
        if (userAgent == null || userAgent.trim().isEmpty()) {
            return "unknown";
        }
        String sanitized = userAgent.replaceAll("[\\r\\n\\t]", "").trim();
        return sanitized.length() > 200 ? sanitized.substring(0, 200) + "..." : sanitized;
    }

    /**
     * Extract client IP address with proxy support
     */
    public String resolveClientIpAddress(HttpServletRequest request) {
        return trustedProxyService.resolveClientIp(request).orElse("unknown");
    }

    /**
     * Check if error requires immediate alerting
     */
    public boolean requiresImmediateAlert(ErrorSeverity severity, Exception ex) {
        return severity == ErrorSeverity.CRITICAL
                || (severity == ErrorSeverity.HIGH && isSecurityRelated(ex));
    }

    /**
     * Check if exception is security-related
     */
    public boolean isSecurityRelated(Exception ex) {
        String exceptionName = ex.getClass().getSimpleName().toLowerCase();
        return exceptionName.contains("security")
                || exceptionName.contains("auth")
                || exceptionName.contains("access")
                || ex instanceof SecurityException
                || ex instanceof AccessDeniedException;
    }

    /**
     * Clean up error-related MDC entries
     */
    public void cleanupErrorMDC() {
        MDC.remove("error.type");
        MDC.remove("error.code");
        MDC.remove("error.httpStatus");
        MDC.remove("error.violationCount");
    }
}
