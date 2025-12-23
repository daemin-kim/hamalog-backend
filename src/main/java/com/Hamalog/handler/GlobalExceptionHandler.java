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

    // ==================== 리소스 Not Found 예외 ====================

    @ExceptionHandler({
            MedicationScheduleNotFoundException.class,
            MedicationRecordNotFoundException.class,
            MedicationTimeNotFoundException.class,
            MemberNotFoundException.class,
            SideEffectNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(CustomException ex, HttpServletRequest request) {
        return handleException(ex, request, HttpStatus.NOT_FOUND,
                ErrorSeverity.LOW, "RESOURCE_NOT_FOUND", "Resource not found");
    }

    // ==================== 인증/인가 예외 ====================

    @ExceptionHandler(OAuth2Exception.class)
    public ResponseEntity<ErrorResponse> handleOAuth2Exception(OAuth2Exception ex, HttpServletRequest request) {
        return handleException(ex, request, HttpStatus.BAD_REQUEST,
                ErrorSeverity.HIGH, "OAUTH2_ERROR", "OAuth2 error");
    }

    @ExceptionHandler(TokenException.class)
    public ResponseEntity<ErrorResponse> handleTokenException(TokenException ex, HttpServletRequest request) {
        return handleException(ex, request, HttpStatus.UNAUTHORIZED,
                ErrorSeverity.MEDIUM, "TOKEN_ERROR", "Token error");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuth(AuthenticationException ex, HttpServletRequest request) {
        return handleSimpleException(ex, request, HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED", "Authentication failed", "AUTHENTICATION_ERROR");
    }

    // ==================== 유효성 검증 예외 ====================

    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<ErrorResponse> handleInvalidInput(InvalidInputException ex, HttpServletRequest request) {
        return handleException(ex, request, HttpStatus.BAD_REQUEST,
                ErrorSeverity.LOW, "VALIDATION_ERROR", "Input validation failed");
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

        log.info("[VALIDATION_ERROR] Input validation failed - path={} violations={}",
                request.getRequestURI(), violations);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String errorMessage = "Failed to parse request body. Ensure JSON is valid.";
        Throwable cause = ex.getCause();
        if (cause != null && cause.getMessage() != null) {
            errorMessage = "JSON parsing error: " + handlerUtils.sanitizeErrorMessage(cause.getMessage());
        }

        return handleSimpleException(ex, request, HttpStatus.BAD_REQUEST,
                "MESSAGE_NOT_READABLE", errorMessage, "MESSAGE_PARSE_ERROR");
    }

    // ==================== 동시성/데이터 예외 ====================

    @ExceptionHandler({OptimisticLockException.class, OptimisticLockingFailureException.class})
    public ResponseEntity<ErrorResponse> handleOptimisticLock(Exception ex, HttpServletRequest request) {
        return handleSimpleException(ex, request, HttpStatus.CONFLICT,
                ErrorCode.OPTIMISTIC_LOCK_FAILED.getCode(),
                ErrorCode.OPTIMISTIC_LOCK_FAILED.getMessage(), "CONCURRENCY_ERROR");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        return handleSimpleException(ex, request, HttpStatus.CONFLICT,
                ErrorCode.RESOURCE_CONFLICT.getCode(),
                ErrorCode.RESOURCE_CONFLICT.getMessage(), "DATABASE_ERROR");
    }

    // ==================== HTTP 요청 예외 ====================

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        String supportedTypes = ex.getSupportedMediaTypes().stream()
                .map(Object::toString)
                .reduce((a, b) -> a + ", " + b)
                .orElse("unknown");
        String errorMessage = String.format("Content-Type '%s' is not supported. Supported: %s",
                ex.getContentType(), supportedTypes);

        return handleSimpleException(ex, request, HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "UNSUPPORTED_MEDIA_TYPE", errorMessage, "MEDIA_TYPE_ERROR");
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingPart(MissingServletRequestPartException ex, HttpServletRequest request) {
        String errorMessage = String.format("Required request part '%s' is missing.", ex.getRequestPartName());
        return handleSimpleException(ex, request, HttpStatus.BAD_REQUEST,
                "MISSING_REQUEST_PART", errorMessage, "REQUEST_PART_ERROR");
    }

    // ==================== 범용 CustomException 처리 ====================

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

        log.warn("[BUSINESS_ERROR] Business logic error - path={} code={} message={}",
                request.getRequestURI(), ex.getErrorCode().getCode(), ex.getErrorCode().getMessage());

        return ResponseEntity.status(status).body(error);
    }

    // ==================== 예상치 못한 예외 ====================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                ErrorCode.INTERNAL_SERVER_ERROR.getMessage(),
                request.getRequestURI(),
                null
        );

        log.error("[UNEXPECTED_ERROR] Unexpected system error - path={} exception={}",
                request.getRequestURI(), ex.getClass().getSimpleName(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    // ==================== 공통 처리 메서드 ====================

    /**
     * CustomException 계열 예외를 처리하는 템플릿 메서드
     */
    private ResponseEntity<ErrorResponse> handleException(
            CustomException ex, HttpServletRequest request,
            HttpStatus status, ErrorSeverity severity,
            String category, String logMessage) {

        MDCUtil.addErrorContext(ex);
        MDCUtil.put("error.severity", severity.name());
        MDCUtil.put("error.category", category);

        ErrorResponse error = ErrorResponse.of(
                status,
                ex.getErrorCode().getCode(),
                ex.getErrorCode().getMessage(),
                request.getRequestURI(),
                null
        );

        Map<String, Object> errorContext = handlerUtils.createErrorContext(ex, request, severity);
        structuredLogger.error(logMessage, ex, errorContext);

        logByLevel(severity, "[{}] {} - path={} code={}",
                category, logMessage, request.getRequestURI(), ex.getErrorCode().getCode());

        MDCUtil.clearErrorContext();
        return ResponseEntity.status(status).body(error);
    }

    /**
     * 일반 Exception을 처리하는 템플릿 메서드
     */
    private ResponseEntity<ErrorResponse> handleSimpleException(
            Exception ex, HttpServletRequest request,
            HttpStatus status, String code, String message, String category) {

        ErrorResponse error = ErrorResponse.of(
                status,
                code,
                message,
                request.getRequestURI(),
                null
        );

        log.warn("[{}] {} - path={}", category, message, request.getRequestURI());

        return ResponseEntity.status(status).body(error);
    }

    /**
     * 심각도에 따른 로깅 레벨 선택
     */
    private void logByLevel(ErrorSeverity severity, String format, Object... args) {
        switch (severity) {
            case HIGH, CRITICAL -> log.error(format, args);
            case MEDIUM -> log.warn(format, args);
            default -> log.info(format, args);
        }
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
