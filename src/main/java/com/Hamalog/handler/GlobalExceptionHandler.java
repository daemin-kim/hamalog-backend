package com.Hamalog.handler;

import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.exception.medication.MedicationRecordNotFoundException;
import com.Hamalog.exception.medication.MedicationScheduleNotFoundException;
import com.Hamalog.exception.medication.MedicationTimeNotFoundException;
import com.Hamalog.exception.member.MemberNotFoundException;
import com.Hamalog.exception.oauth2.OAuth2Exception;
import com.Hamalog.exception.sideEffect.SideEffectNotFoundException;
import com.Hamalog.exception.token.TokenException;
import com.Hamalog.exception.validation.InvalidInputException;
import com.Hamalog.logging.MDCUtil;
import com.Hamalog.logging.StructuredLogger;
import com.Hamalog.logging.events.SecurityEvent;
import com.Hamalog.security.filter.TrustedProxyService;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final TrustedProxyService trustedProxyService;

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @Autowired
    private StructuredLogger structuredLogger;
    
    // Error severity levels for alerting and monitoring
    private enum ErrorSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    // Critical error patterns that require immediate attention
    private static final String[] CRITICAL_ERROR_PATTERNS = {
        "OutOfMemoryError", "StackOverflowError", "DatabaseConnectionException", 
        "SecurityException", "DataIntegrityViolationException"
    };

    @ExceptionHandler({
            MedicationScheduleNotFoundException.class,
            MedicationRecordNotFoundException.class,
            MedicationTimeNotFoundException.class,
            MemberNotFoundException.class,
            SideEffectNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(CustomException ex, HttpServletRequest request) {
        ErrorSeverity severity = ErrorSeverity.LOW;
        
        // Enhanced error context using MDCUtil
        MDCUtil.addErrorContext(ex);
        MDCUtil.put("error.severity", severity.name());
        MDCUtil.put("error.category", "RESOURCE_NOT_FOUND");
        
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.NOT_FOUND,
                ex.getErrorCode().getCode(),
                ex.getErrorCode().getMessage(),
                request.getRequestURI(),
                null
        );
        
        // Structured error logging
        Map<String, Object> errorContext = createErrorContext(ex, request, severity);
        structuredLogger.error("Resource not found", ex, errorContext);
        
        log.warn("[RESOURCE_NOT_FOUND] Resource not found - path={} code={} message={} severity={} correlationId={}", 
            request.getRequestURI(), ex.getErrorCode().getCode(), ex.getErrorCode().getMessage(), 
            severity, MDCUtil.get(MDCUtil.CORRELATION_ID));
        
        // Clean up error context
        MDCUtil.clearErrorContext();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(OAuth2Exception.class)
    public ResponseEntity<ErrorResponse> handleOAuth2Exception(OAuth2Exception ex, HttpServletRequest request) {
        ErrorSeverity severity = ErrorSeverity.HIGH;

        MDCUtil.addErrorContext(ex);
        MDCUtil.put("error.severity", severity.name());
        MDCUtil.put("error.category", "OAUTH2_ERROR");

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.BAD_REQUEST,
                ex.getErrorCode().getCode(),
                ex.getErrorCode().getMessage(),
                request.getRequestURI(),
                null
        );

        Map<String, Object> errorContext = createErrorContext(ex, request, severity);
        structuredLogger.error("OAuth2 error", ex, errorContext);

        log.error("[OAUTH2_ERROR] OAuth2 authentication failed - path={} code={} message={} correlationId={}",
            request.getRequestURI(), ex.getErrorCode().getCode(), ex.getErrorCode().getMessage(),
            MDCUtil.get(MDCUtil.CORRELATION_ID));

        MDCUtil.clearErrorContext();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(TokenException.class)
    public ResponseEntity<ErrorResponse> handleTokenException(TokenException ex, HttpServletRequest request) {
        ErrorSeverity severity = ErrorSeverity.MEDIUM;

        MDCUtil.addErrorContext(ex);
        MDCUtil.put("error.severity", severity.name());
        MDCUtil.put("error.category", "TOKEN_ERROR");

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.UNAUTHORIZED,
                ex.getErrorCode().getCode(),
                ex.getErrorCode().getMessage(),
                request.getRequestURI(),
                null
        );

        Map<String, Object> errorContext = createErrorContext(ex, request, severity);
        structuredLogger.error("Token error", ex, errorContext);

        log.warn("[TOKEN_ERROR] Token validation failed - path={} code={} message={} correlationId={}",
            request.getRequestURI(), ex.getErrorCode().getCode(), ex.getErrorCode().getMessage(),
            MDCUtil.get(MDCUtil.CORRELATION_ID));

        MDCUtil.clearErrorContext();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<ErrorResponse> handleInvalidInput(InvalidInputException ex, HttpServletRequest request) {
        ErrorSeverity severity = ErrorSeverity.LOW;

        MDCUtil.addErrorContext(ex);
        MDCUtil.put("error.severity", severity.name());
        MDCUtil.put("error.category", "VALIDATION_ERROR");

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.BAD_REQUEST,
                ex.getErrorCode().getCode(),
                ex.getErrorCode().getMessage(),
                request.getRequestURI(),
                null
        );

        Map<String, Object> errorContext = createErrorContext(ex, request, severity);
        structuredLogger.error("Input validation failed", ex, errorContext);

        log.warn("[VALIDATION_ERROR] Invalid input - path={} code={} message={} correlationId={}",
            request.getRequestURI(), ex.getErrorCode().getCode(), ex.getErrorCode().getMessage(),
            MDCUtil.get(MDCUtil.CORRELATION_ID));

        MDCUtil.clearErrorContext();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler({OptimisticLockException.class, OptimisticLockingFailureException.class})
    public ResponseEntity<ErrorResponse> handleOptimisticLock(Exception ex, HttpServletRequest request) {
        ErrorSeverity severity = ErrorSeverity.MEDIUM;

        MDCUtil.addErrorContext(ex);
        MDCUtil.put("error.severity", severity.name());
        MDCUtil.put("error.category", "CONCURRENCY_ERROR");

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.CONFLICT,
                ErrorCode.OPTIMISTIC_LOCK_FAILED.getCode(),
                ErrorCode.OPTIMISTIC_LOCK_FAILED.getMessage(),
                request.getRequestURI(),
                null
        );

        Map<String, Object> errorContext = createErrorContext(ex, request, severity);
        structuredLogger.error("Optimistic lock failure", ex, errorContext);

        log.warn("[CONCURRENCY_ERROR] Optimistic lock failed - path={} correlationId={}",
            request.getRequestURI(), MDCUtil.get(MDCUtil.CORRELATION_ID));

        MDCUtil.clearErrorContext();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        ErrorSeverity severity = ErrorSeverity.HIGH;

        MDCUtil.addErrorContext(ex);
        MDCUtil.put("error.severity", severity.name());
        MDCUtil.put("error.category", "DATABASE_ERROR");

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.CONFLICT,
                ErrorCode.RESOURCE_CONFLICT.getCode(),
                ErrorCode.RESOURCE_CONFLICT.getMessage(),
                request.getRequestURI(),
                null
        );

        Map<String, Object> errorContext = createErrorContext(ex, request, severity);
        structuredLogger.error("Data integrity violation", ex, errorContext);

        log.error("[DATABASE_ERROR] Data integrity violation - path={} correlationId={}",
            request.getRequestURI(), MDCUtil.get(MDCUtil.CORRELATION_ID), ex);

        MDCUtil.clearErrorContext();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> violations = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            violations.put(fe.getField(), fe.getDefaultMessage());
        }
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.BAD_REQUEST,
                "INVALID_INPUT",
                "Request data validation failed",
                request.getRequestURI(),
                violations
        );
        
        // Add error context to MDC
        MDC.put("error.type", "VALIDATION_ERROR");
        MDC.put("error.code", "INVALID_INPUT");
        MDC.put("error.httpStatus", "400");
        MDC.put("error.violationCount", String.valueOf(violations.size()));
        
        log.info("[VALIDATION_ERROR] Input validation failed - path={} traceId={} violationCount={} violations={}", 
            request.getRequestURI(), MDC.get("requestId"), violations.size(), violations);
        
        // Clean up error context
        cleanupErrorMDC();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuth(AuthenticationException ex, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED",
                "Authentication failed",
                request.getRequestURI(),
                null
        );
        
        // Add error context to MDC
        MDC.put("error.type", "AUTHENTICATION_ERROR");
        MDC.put("error.code", "UNAUTHORIZED");
        MDC.put("error.httpStatus", "401");
        
        log.warn("[AUTHENTICATION_ERROR] Authentication failed - path={} traceId={} message={}", 
            request.getRequestURI(), MDC.get("requestId"), ex.getMessage());
        
        // Clean up error context
        cleanupErrorMDC();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException ex, HttpServletRequest request) {
        // HTTP 상태 코드 결정
        HttpStatus status = determineHttpStatus(ex.getErrorCode());

        ErrorResponse error = ErrorResponse.of(
                status,
                ex.getErrorCode().getCode(),
                ex.getErrorCode().getMessage(),
                request.getRequestURI(),
                null
        );

        // Add error context to MDC
        MDC.put("error.type", "BUSINESS_ERROR");
        MDC.put("error.code", ex.getErrorCode().getCode());
        MDC.put("error.httpStatus", String.valueOf(status.value()));

        log.warn("[BUSINESS_ERROR] Business logic error - path={} code={} message={} traceId={}", 
            request.getRequestURI(), ex.getErrorCode().getCode(), ex.getErrorCode().getMessage(), MDC.get("requestId"));
        
        // Clean up error context
        cleanupErrorMDC();
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex, HttpServletRequest request) {
        // ✅ 클라이언트에는 일반적인 메시지만 전달
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                ErrorCode.INTERNAL_SERVER_ERROR.getMessage(),
                request.getRequestURI(),
                null
        );
        
        // ✅ 상세 정보는 로그에만 기록
        MDC.put("error.type", "UNEXPECTED_ERROR");
        MDC.put("error.code", ErrorCode.INTERNAL_SERVER_ERROR.getCode());
        MDC.put("error.httpStatus", "500");
        MDC.put("error.exception", ex.getClass().getSimpleName());
        MDC.put("error.details", ex.getMessage());

        log.error("[UNEXPECTED_ERROR] Unexpected system error - path={} traceId={} exception={} details={}",
            request.getRequestURI(), MDC.get("requestId"), ex.getClass().getSimpleName(), ex.getMessage(), ex);
        
        // Clean up error context
        cleanupErrorMDC();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    
    /**
     * ErrorCode에 따라 적절한 HTTP 상태 코드 결정
     */
    private HttpStatus determineHttpStatus(ErrorCode errorCode) {
        return switch (errorCode) {
            case MEMBER_NOT_FOUND, MEDICATION_SCHEDULE_NOT_FOUND, MEDICATION_RECORD_NOT_FOUND,
                 MEDICATION_TIME_NOT_FOUND, SIDE_EFFECT_NOT_FOUND, FILE_NOT_FOUND,
                 MOOD_DIARY_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case UNAUTHORIZED, INVALID_TOKEN, TOKEN_EXPIRED, TOKEN_BLACKLISTED,
                 INVALID_REFRESH_TOKEN, REFRESH_TOKEN_EXPIRED, REFRESH_TOKEN_REVOKED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case OPTIMISTIC_LOCK_FAILED, RESOURCE_CONFLICT, DUPLICATE_MEMBER,
                 DIARY_ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case EXTERNAL_API_TIMEOUT -> HttpStatus.GATEWAY_TIMEOUT;
            case FILE_SIZE_EXCEEDED -> HttpStatus.PAYLOAD_TOO_LARGE;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    /**
     * Create comprehensive error context for structured logging
     */
    private Map<String, Object> createErrorContext(Exception ex, HttpServletRequest request, ErrorSeverity severity) {
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
    private ErrorSeverity determineErrorSeverity(Exception ex) {
        String exceptionName = ex.getClass().getSimpleName();
        String errorMessage = ex.getMessage();
        
        // Critical errors requiring immediate attention
        for (String pattern : CRITICAL_ERROR_PATTERNS) {
            if (exceptionName.contains(pattern) || (errorMessage != null && errorMessage.contains(pattern))) {
                return ErrorSeverity.CRITICAL;
            }
        }
        
        // High severity errors
        if (ex instanceof SecurityException || 
            ex instanceof AccessDeniedException ||
            exceptionName.contains("Security") ||
            exceptionName.contains("Auth")) {
            return ErrorSeverity.HIGH;
        }
        
        // Medium severity errors
        if (ex instanceof IllegalArgumentException ||
            ex instanceof IllegalStateException ||
            exceptionName.contains("Validation") ||
            exceptionName.contains("Constraint")) {
            return ErrorSeverity.MEDIUM;
        }
        
        // Default to low severity
        return ErrorSeverity.LOW;
    }
    
    /**
     * Get sanitized stack trace as string
     */
    private String getStackTraceString(Exception ex) {
        if (ex == null) return "";
        
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        ex.printStackTrace(pw);
        String stackTrace = sw.toString();
        
        // Limit stack trace size to prevent log bloat
        return stackTrace.length() > 2000 ? stackTrace.substring(0, 2000) + "...[truncated]" : stackTrace;
    }
    
    /**
     * Sanitize error message to prevent log injection
     */
    private String sanitizeErrorMessage(String message) {
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
    private String sanitizeUserAgent(String userAgent) {
        if (userAgent == null || userAgent.trim().isEmpty()) {
            return "unknown";
        }
        String sanitized = userAgent.replaceAll("[\\r\\n\\t]", "").trim();
        return sanitized.length() > 200 ? sanitized.substring(0, 200) + "..." : sanitized;
    }
    
    /**
     * Extract client IP address with proxy support
     */
    private String resolveClientIpAddress(HttpServletRequest request) {
        return trustedProxyService.resolveClientIp(request).orElse("unknown");
    }
    
    /**
     * Check if error requires immediate alerting
     */
    private boolean requiresImmediateAlert(ErrorSeverity severity, Exception ex) {
        return severity == ErrorSeverity.CRITICAL || 
               (severity == ErrorSeverity.HIGH && isSecurityRelated(ex));
    }
    
    /**
     * Check if exception is security-related
     */
    private boolean isSecurityRelated(Exception ex) {
        String exceptionName = ex.getClass().getSimpleName().toLowerCase();
        return exceptionName.contains("security") || 
               exceptionName.contains("auth") || 
               exceptionName.contains("access") ||
               ex instanceof SecurityException ||
               ex instanceof AccessDeniedException;
    }
    
    public static class ErrorResponse {
        private final String code;
        private final String message;
        private final String path;
        private final Map<String, String> violations;

        private ErrorResponse(HttpStatus status, String code, String message, String path, Map<String, String> violations) {
            this.code = code;
            this.message = message;
            this.path = path;
            this.violations = violations;
        }

        public static ErrorResponse of(HttpStatus status, String code, String message, String path, Map<String, String> violations) {
            return new ErrorResponse(status, code, message, path, violations);
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        public String getPath() {
            return path;
        }

        public Map<String, String> getViolations() {
            return violations;
        }
    }

    private void cleanupErrorMDC() {
        MDC.remove("error.type");
        MDC.remove("error.code");
        MDC.remove("error.httpStatus");
        MDC.remove("error.violationCount");
    }
}
