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
import com.Hamalog.security.filter.TrustedProxyService;
import com.Hamalog.security.validation.InputValidationUtil;
import com.Hamalog.service.alert.DiscordAlertService;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * 전역 예외 처리기
 * REST API에서 발생하는 모든 예외를 일관된 형식으로 처리합니다.
 * OWASP Top 10 A05: Security Misconfiguration 대응을 포함합니다.
 * Critical/High 심각도 에러 발생 시 Discord 알림을 발송합니다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final Logger securityLog = LoggerFactory.getLogger("SECURITY");

    private final StructuredLogger structuredLogger;
    private final ExceptionHandlerUtils handlerUtils;
    private final TrustedProxyService trustedProxyService;
    private final InputValidationUtil inputValidationUtil;
    private final DiscordAlertService discordAlertService;

    public GlobalExceptionHandler(
            StructuredLogger structuredLogger,
            ExceptionHandlerUtils handlerUtils,
            TrustedProxyService trustedProxyService,
            InputValidationUtil inputValidationUtil,
            @Autowired(required = false) DiscordAlertService discordAlertService
    ) {
        this.structuredLogger = structuredLogger;
        this.handlerUtils = handlerUtils;
        this.trustedProxyService = trustedProxyService;
        this.inputValidationUtil = inputValidationUtil;
        this.discordAlertService = discordAlertService;
    }

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
        String clientIp = getClientIp(request);
        String sanitizedPath = inputValidationUtil.sanitizeForLog(request.getRequestURI());

        securityLog.warn("인증 실패: IP={}, Path={}, Error={}",
                        clientIp, sanitizedPath, ex.getMessage());

        return handleSimpleException(ex, request, HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED", "Authentication failed", "AUTHENTICATION_ERROR");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        String clientIp = getClientIp(request);
        String sanitizedPath = inputValidationUtil.sanitizeForLog(request.getRequestURI());

        securityLog.warn("접근 거부: IP={}, Path={}, Error={}",
                        clientIp, sanitizedPath, ex.getMessage());

        return handleSimpleException(ex, request, HttpStatus.FORBIDDEN,
                "ACCESS_DENIED", "해당 리소스에 접근할 권한이 없습니다", "ACCESS_DENIED_ERROR");
    }

    @ExceptionHandler(InputValidationUtil.InputValidationException.class)
    public ResponseEntity<ErrorResponse> handleInputValidationException(
            InputValidationUtil.InputValidationException ex, HttpServletRequest request) {
        String clientIp = getClientIp(request);
        String sanitizedPath = inputValidationUtil.sanitizeForLog(request.getRequestURI());

        securityLog.warn("입력값 보안 검증 실패: IP={}, Path={}, Error={}",
                        clientIp, sanitizedPath, ex.getMessage());

        return handleSimpleException(ex, request, HttpStatus.BAD_REQUEST,
                "INVALID_INPUT", "유효하지 않은 입력값입니다", "INPUT_VALIDATION_ERROR");
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

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {
        String clientIp = getClientIp(request);

        securityLog.warn("파일 업로드 크기 초과: IP={}", clientIp);

        return handleSimpleException(ex, request, HttpStatus.PAYLOAD_TOO_LARGE,
                ErrorCode.FILE_SIZE_EXCEEDED.getCode(),
                ErrorCode.FILE_SIZE_EXCEEDED.getMessage(), "FILE_SIZE_ERROR");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        String sanitizedPath = inputValidationUtil.sanitizeForLog(request.getRequestURI());
        String method = inputValidationUtil.sanitizeForLog(request.getMethod());

        log.warn("지원하지 않는 HTTP 메서드: {} {}", method, sanitizedPath);

        return handleSimpleException(ex, request, HttpStatus.METHOD_NOT_ALLOWED,
                "METHOD_NOT_ALLOWED", "지원하지 않는 HTTP 메서드입니다", "METHOD_ERROR");
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(
            NoHandlerFoundException ex, HttpServletRequest request) {
        String sanitizedPath = inputValidationUtil.sanitizeForLog(request.getRequestURI());

        log.info("리소스를 찾을 수 없음: {}", sanitizedPath);

        return handleSimpleException(ex, request, HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND", "요청하신 리소스를 찾을 수 없습니다", "NOT_FOUND_ERROR");
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

        // Critical 에러 발생 시 Discord 알림 발송
        sendDiscordAlert(ex, request, ErrorSeverity.CRITICAL);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    // ==================== 공통 처리 메서드 ====================

    /**
     * Discord 알림 발송 (Optional 의존성)
     */
    private void sendDiscordAlert(Exception ex, HttpServletRequest request, ErrorSeverity severity) {
        if (discordAlertService != null) {
            discordAlertService.sendServerErrorAlert(ex, request, severity);
        }
    }

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

        // HIGH 이상 심각도일 때 Discord 알림 발송
        if (severity == ErrorSeverity.HIGH || severity == ErrorSeverity.CRITICAL) {
            sendDiscordAlert(ex, request, severity);
        }

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

    /**
     * 클라이언트 IP 주소 추출 (보안 로깅용)
     */
    private String getClientIp(HttpServletRequest request) {
        return trustedProxyService.resolveClientIp(request).orElse(request.getRemoteAddr());
    }
}
