package com.Hamalog.handler;

import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.exception.medication.MedicationRecordNotFoundException;
import com.Hamalog.exception.medication.MedicationScheduleNotFoundException;
import com.Hamalog.exception.medication.MedicationTimeNotFoundException;
import com.Hamalog.exception.member.MemberNotFoundException;
import com.Hamalog.logging.MDCUtil;
import com.Hamalog.logging.StructuredLogger;
import com.Hamalog.logging.events.SecurityEvent;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

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
            MemberNotFoundException.class
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
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.BAD_REQUEST,
                ex.getErrorCode().getCode(),
                ex.getErrorCode().getMessage(),
                request.getRequestURI(),
                null
        );
        // Add error context to MDC
        MDC.put("error.type", "BUSINESS_ERROR");
        MDC.put("error.code", ex.getErrorCode().getCode());
        MDC.put("error.httpStatus", "400");
        
        log.warn("[BUSINESS_ERROR] Business logic error - path={} code={} message={} traceId={}", 
            request.getRequestURI(), ex.getErrorCode().getCode(), ex.getErrorCode().getMessage(), MDC.get("requestId"));
        
        // Clean up error context
        cleanupErrorMDC();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                ErrorCode.INTERNAL_SERVER_ERROR.getMessage(),
                request.getRequestURI(),
                null
        );
        
        // Add error context to MDC
        MDC.put("error.type", "UNEXPECTED_ERROR");
        MDC.put("error.code", ErrorCode.INTERNAL_SERVER_ERROR.getCode());
        MDC.put("error.httpStatus", "500");
        MDC.put("error.exception", ex.getClass().getSimpleName());
        
        log.error("[UNEXPECTED_ERROR] Unexpected system error - path={} traceId={} exception={} message={}", 
            request.getRequestURI(), MDC.get("requestId"), ex.getClass().getSimpleName(), ex.getMessage(), ex);
        
        // Clean up error context
        cleanupErrorMDC();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
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
            context.put("client_ip", extractClientIpAddress(request));
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
    private String extractClientIpAddress(HttpServletRequest request) {
        if (request == null) return "unknown";
        
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
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
