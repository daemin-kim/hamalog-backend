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
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * 전역 예외 처리기
 * REST API에서 발생하는 모든 예외를 일관된 형식으로 처리합니다.
 */
@RequiredArgsConstructor
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final StructuredLogger structuredLogger;
    private final ExceptionHandlerUtils handlerUtils;

    @ExceptionHandler({
            MedicationScheduleNotFoundException.class,
            MedicationRecordNotFoundException.class,
            MedicationTimeNotFoundException.class,
            MemberNotFoundException.class,
            SideEffectNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(CustomException ex, HttpServletRequest request) {
        ErrorSeverity severity = ErrorSeverity.LOW;

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

        Map<String, Object> errorContext = handlerUtils.createErrorContext(ex, request, severity);
        structuredLogger.error("Resource not found", ex, errorContext);

        log.warn("[RESOURCE_NOT_FOUND] Resource not found - path={} code={} message={} severity={} correlationId={}",
            request.getRequestURI(), ex.getErrorCode().getCode(), ex.getErrorCode().getMessage(),
            severity, MDCUtil.get(MDCUtil.CORRELATION_ID));

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

        Map<String, Object> errorContext = handlerUtils.createErrorContext(ex, request, severity);
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

        Map<String, Object> errorContext = handlerUtils.createErrorContext(ex, request, severity);
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

        Map<String, Object> errorContext = handlerUtils.createErrorContext(ex, request, severity);
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

        Map<String, Object> errorContext = handlerUtils.createErrorContext(ex, request, severity);
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

        Map<String, Object> errorContext = handlerUtils.createErrorContext(ex, request, severity);
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

        MDC.put("error.type", "VALIDATION_ERROR");
        MDC.put("error.code", "INVALID_INPUT");
        MDC.put("error.httpStatus", "400");
        MDC.put("error.violationCount", String.valueOf(violations.size()));

        log.info("[VALIDATION_ERROR] Input validation failed - path={} traceId={} violationCount={} violations={}",
            request.getRequestURI(), MDC.get("requestId"), violations.size(), violations);

        handlerUtils.cleanupErrorMDC();
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

        MDC.put("error.type", "AUTHENTICATION_ERROR");
        MDC.put("error.code", "UNAUTHORIZED");
        MDC.put("error.httpStatus", "401");

        log.warn("[AUTHENTICATION_ERROR] Authentication failed - path={} traceId={} message={}",
            request.getRequestURI(), MDC.get("requestId"), ex.getMessage());

        handlerUtils.cleanupErrorMDC();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException ex, HttpServletRequest request) {
        HttpStatus status = determineHttpStatus(ex.getErrorCode());

        ErrorResponse error = ErrorResponse.of(
                status,
                ex.getErrorCode().getCode(),
                ex.getErrorCode().getMessage(),
                request.getRequestURI(),
                null
        );

        MDC.put("error.type", "BUSINESS_ERROR");
        MDC.put("error.code", ex.getErrorCode().getCode());
        MDC.put("error.httpStatus", String.valueOf(status.value()));

        log.warn("[BUSINESS_ERROR] Business logic error - path={} code={} message={} traceId={}",
            request.getRequestURI(), ex.getErrorCode().getCode(), ex.getErrorCode().getMessage(), MDC.get("requestId"));

        handlerUtils.cleanupErrorMDC();
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        ErrorSeverity severity = ErrorSeverity.LOW;

        MDCUtil.addErrorContext(ex);
        MDCUtil.put("error.severity", severity.name());
        MDCUtil.put("error.category", "MEDIA_TYPE_ERROR");

        String supportedTypes = ex.getSupportedMediaTypes().stream()
            .map(Object::toString)
            .reduce((a, b) -> a + ", " + b)
            .orElse("unknown");

        String errorMessage = String.format(
            "Content-Type '%s' is not supported. Supported types: %s. " +
            "For multipart requests, ensure 'data' part has Content-Type: application/json",
            ex.getContentType(), supportedTypes
        );

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "UNSUPPORTED_MEDIA_TYPE",
                errorMessage,
                request.getRequestURI(),
                null
        );

        Map<String, Object> errorContext = handlerUtils.createErrorContext(ex, request, severity);
        structuredLogger.error("Unsupported media type", ex, errorContext);

        log.warn("[MEDIA_TYPE_ERROR] Unsupported content type - path={} contentType={} supportedTypes={} correlationId={}",
            request.getRequestURI(), ex.getContentType(), supportedTypes, MDCUtil.get(MDCUtil.CORRELATION_ID));

        MDCUtil.clearErrorContext();
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(error);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingPart(MissingServletRequestPartException ex, HttpServletRequest request) {
        ErrorSeverity severity = ErrorSeverity.LOW;

        MDCUtil.addErrorContext(ex);
        MDCUtil.put("error.severity", severity.name());
        MDCUtil.put("error.category", "REQUEST_PART_ERROR");

        String errorMessage = String.format(
            "Required request part '%s' is missing. " +
            "Ensure multipart/form-data request includes required parts.",
            ex.getRequestPartName()
        );

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.BAD_REQUEST,
                "MISSING_REQUEST_PART",
                errorMessage,
                request.getRequestURI(),
                null
        );

        Map<String, Object> errorContext = handlerUtils.createErrorContext(ex, request, severity);
        structuredLogger.error("Missing request part", ex, errorContext);

        log.warn("[REQUEST_PART_ERROR] Missing required part - path={} partName={} correlationId={}",
            request.getRequestURI(), ex.getRequestPartName(), MDCUtil.get(MDCUtil.CORRELATION_ID));

        MDCUtil.clearErrorContext();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        ErrorSeverity severity = ErrorSeverity.LOW;

        MDCUtil.addErrorContext(ex);
        MDCUtil.put("error.severity", severity.name());
        MDCUtil.put("error.category", "MESSAGE_PARSE_ERROR");

        String errorMessage = "Failed to parse request body. Ensure JSON is valid and " +
            "Content-Type header is correctly set (application/json for JSON data).";

        Throwable cause = ex.getCause();
        if (cause != null && cause.getMessage() != null) {
            String causeMsg = cause.getMessage();
            if (causeMsg.contains("Cannot deserialize") || causeMsg.contains("Unexpected token")) {
                errorMessage = "JSON parsing error: " + handlerUtils.sanitizeErrorMessage(causeMsg);
            }
        }

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.BAD_REQUEST,
                "MESSAGE_NOT_READABLE",
                errorMessage,
                request.getRequestURI(),
                null
        );

        Map<String, Object> errorContext = handlerUtils.createErrorContext(ex, request, severity);
        structuredLogger.error("HTTP message not readable", ex, errorContext);

        log.warn("[MESSAGE_PARSE_ERROR] Failed to parse request - path={} correlationId={}",
            request.getRequestURI(), MDCUtil.get(MDCUtil.CORRELATION_ID));

        MDCUtil.clearErrorContext();
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

        MDC.put("error.type", "UNEXPECTED_ERROR");
        MDC.put("error.code", ErrorCode.INTERNAL_SERVER_ERROR.getCode());
        MDC.put("error.httpStatus", "500");
        MDC.put("error.exception", ex.getClass().getSimpleName());
        MDC.put("error.details", ex.getMessage());

        log.error("[UNEXPECTED_ERROR] Unexpected system error - path={} traceId={} exception={} details={}",
            request.getRequestURI(), MDC.get("requestId"), ex.getClass().getSimpleName(), ex.getMessage(), ex);

        handlerUtils.cleanupErrorMDC();
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
}
