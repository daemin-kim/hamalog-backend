package com.Hamalog.handler;

import com.Hamalog.security.filter.TrustedProxyService;
import com.Hamalog.security.validation.InputValidationUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 보안 강화된 전역 에러 핸들러
 * OWASP Top 10 A05: Security Misconfiguration 대응
 * 민감한 정보 노출 방지 및 안전한 에러 응답 제공
 */
@RestControllerAdvice
public class SecureErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(SecureErrorHandler.class);
    private static final Logger securityLog = LoggerFactory.getLogger("SECURITY");

    @Autowired
    private Environment environment;

    @Autowired
    private InputValidationUtil inputValidationUtil;

    @Autowired
    private TrustedProxyService trustedProxyService;

    /**
     * 일반적인 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        String errorId = UUID.randomUUID().toString();
        String sanitizedPath = inputValidationUtil.sanitizeForLog(request.getRequestURI());
        
        // 상세한 에러 정보는 로그에만 기록
        log.error("일반 예외 발생 [{}]: {} at {}", errorId, ex.getMessage(), sanitizedPath, ex);
        
        return createErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_ERROR",
            "서버 내부 오류가 발생했습니다",
            errorId,
            request
        );
    }

    /**
     * 인증 실패 예외 처리
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        
        String errorId = UUID.randomUUID().toString();
        String clientIp = getClientIp(request);
        String sanitizedPath = inputValidationUtil.sanitizeForLog(request.getRequestURI());
        
        // 보안 로그에 인증 실패 기록
        securityLog.warn("인증 실패 [{}]: IP={}, Path={}, Error={}", 
                        errorId, clientIp, sanitizedPath, ex.getMessage());
        
        String message = "인증에 실패했습니다";
        if (ex instanceof BadCredentialsException) {
            message = "아이디 또는 비밀번호가 올바르지 않습니다";
        }
        
        return createErrorResponse(
            HttpStatus.UNAUTHORIZED,
            "AUTHENTICATION_FAILED",
            message,
            errorId,
            request
        );
    }

    /**
     * 접근 거부 예외 처리
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        
        String errorId = UUID.randomUUID().toString();
        String clientIp = getClientIp(request);
        String sanitizedPath = inputValidationUtil.sanitizeForLog(request.getRequestURI());
        
        // 보안 로그에 접근 거부 기록
        securityLog.warn("접근 거부 [{}]: IP={}, Path={}, Error={}", 
                        errorId, clientIp, sanitizedPath, ex.getMessage());
        
        return createErrorResponse(
            HttpStatus.FORBIDDEN,
            "ACCESS_DENIED",
            "해당 리소스에 접근할 권한이 없습니다",
            errorId,
            request
        );
    }

    /**
     * 입력값 검증 실패 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        String errorId = UUID.randomUUID().toString();
        String sanitizedPath = inputValidationUtil.sanitizeForLog(request.getRequestURI());
        
        log.warn("입력값 검증 실패 [{}]: {} at {}", errorId, ex.getMessage(), sanitizedPath);
        
        // 검증 실패한 필드 정보 수집 (민감한 정보 제외)
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            String fieldName = inputValidationUtil.sanitizeForLog(error.getField());
            String errorMessage = inputValidationUtil.sanitizeForLog(error.getDefaultMessage());
            fieldErrors.put(fieldName, errorMessage);
        });
        
        Map<String, Object> errorResponse = createErrorResponse(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_FAILED",
            "입력값 검증에 실패했습니다",
            errorId,
            request
        ).getBody();
        
        errorResponse.put("fieldErrors", fieldErrors);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Input Validation 예외 처리
     */
    @ExceptionHandler(InputValidationUtil.InputValidationException.class)
    public ResponseEntity<Map<String, Object>> handleInputValidationException(
            InputValidationUtil.InputValidationException ex, HttpServletRequest request) {
        
        String errorId = UUID.randomUUID().toString();
        String clientIp = getClientIp(request);
        String sanitizedPath = inputValidationUtil.sanitizeForLog(request.getRequestURI());
        
        // 보안 로그에 입력값 검증 실패 기록
        securityLog.warn("입력값 보안 검증 실패 [{}]: IP={}, Path={}, Error={}", 
                        errorId, clientIp, sanitizedPath, ex.getMessage());
        
        return createErrorResponse(
            HttpStatus.BAD_REQUEST,
            "INVALID_INPUT",
            "유효하지 않은 입력값입니다",
            errorId,
            request
        );
    }

    /**
     * 리소스를 찾을 수 없는 경우
     */
    @ExceptionHandler(value = {
        org.springframework.web.servlet.NoHandlerFoundException.class
    })
    public ResponseEntity<Map<String, Object>> handleNotFoundException(
            Exception ex, HttpServletRequest request) {
        
        String errorId = UUID.randomUUID().toString();
        String sanitizedPath = inputValidationUtil.sanitizeForLog(request.getRequestURI());
        
        log.info("리소스를 찾을 수 없음 [{}]: {}", errorId, sanitizedPath);
        
        return createErrorResponse(
            HttpStatus.NOT_FOUND,
            "RESOURCE_NOT_FOUND",
            "요청하신 리소스를 찾을 수 없습니다",
            errorId,
            request
        );
    }

    /**
     * 지원하지 않는 HTTP 메서드
     */
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupportedException(
            org.springframework.web.HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        
        String errorId = UUID.randomUUID().toString();
        String sanitizedPath = inputValidationUtil.sanitizeForLog(request.getRequestURI());
        String method = inputValidationUtil.sanitizeForLog(request.getMethod());
        
        log.warn("지원하지 않는 HTTP 메서드 [{}]: {} {}", errorId, method, sanitizedPath);
        
        return createErrorResponse(
            HttpStatus.METHOD_NOT_ALLOWED,
            "METHOD_NOT_ALLOWED",
            "지원하지 않는 HTTP 메서드입니다",
            errorId,
            request
        );
    }

    /**
     * 요청 크기 초과
     */
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceededException(
            org.springframework.web.multipart.MaxUploadSizeExceededException ex, HttpServletRequest request) {
        
        String errorId = UUID.randomUUID().toString();
        String clientIp = getClientIp(request);
        
        securityLog.warn("파일 업로드 크기 초과 [{}]: IP={}", errorId, clientIp);
        
        return createErrorResponse(
            HttpStatus.PAYLOAD_TOO_LARGE,
            "FILE_SIZE_EXCEEDED",
            "업로드 파일 크기가 제한을 초과했습니다",
            errorId,
            request
        );
    }

    /**
     * 안전한 에러 응답 생성
     */
    private ResponseEntity<Map<String, Object>> createErrorResponse(
            HttpStatus status, String code, String message, String errorId, HttpServletRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("code", code);
        body.put("message", message);
        body.put("path", request.getRequestURI());
        body.put("errorId", errorId);

        return ResponseEntity.status(status).body(body);
    }

    /**
     * 클라이언트 IP 주소 추출
     */
    private String getClientIp(HttpServletRequest request) {
        return trustedProxyService.resolveClientIp(request).orElse(request.getRemoteAddr());
    }
}
