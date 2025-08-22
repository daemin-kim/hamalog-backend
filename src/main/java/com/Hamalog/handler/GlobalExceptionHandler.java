package com.Hamalog.handler;

import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.exception.medication.MedicationRecordNotFoundException;
import com.Hamalog.exception.medication.MedicationScheduleNotFoundException;
import com.Hamalog.exception.medication.MedicationTimeNotFoundException;
import com.Hamalog.exception.member.MemberNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @ExceptionHandler({
            MedicationScheduleNotFoundException.class,
            MedicationRecordNotFoundException.class,
            MedicationTimeNotFoundException.class,
            MemberNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(CustomException ex, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.NOT_FOUND,
                ex.getErrorCode().getCode(),
                ex.getErrorCode().getMessage(),
                request.getRequestURI(),
                null
        );
        // Add error context to MDC
        MDC.put("error.type", "NOT_FOUND");
        MDC.put("error.code", ex.getErrorCode().getCode());
        MDC.put("error.httpStatus", "404");
        
        log.warn("[NOT_FOUND] Resource not found - path={} code={} message={} traceId={}", 
            request.getRequestURI(), ex.getErrorCode().getCode(), ex.getErrorCode().getMessage(), MDC.get("requestId"));
        
        // Clean up error context
        cleanupErrorMDC();
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
    
    private void cleanupErrorMDC() {
        MDC.remove("error.type");
        MDC.remove("error.code");
        MDC.remove("error.httpStatus");
        MDC.remove("error.violationCount");
        MDC.remove("error.exception");
    }

    public record ErrorResponse(
            int status,
            String code,
            String message,
            String path,
            OffsetDateTime timestamp,
            Map<String, String> details
    ) {
        public static ErrorResponse of(HttpStatus status, String code, String message, String path, Map<String, String> details) {
            return new ErrorResponse(status.value(), code, message, path, OffsetDateTime.now(), details);
        }
    }
}
