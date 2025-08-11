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
        log.warn("[NOT_FOUND] path={} code={} message={} traceId={}", request.getRequestURI(), ex.getErrorCode().getCode(), ex.getErrorCode().getMessage(), MDC.get("requestId"));
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
                "요청 데이터가 유효하지 않습니다.",
                request.getRequestURI(),
                violations
        );
        log.info("[BAD_REQUEST] path={} reason=validation_error traceId={} violations={}", request.getRequestURI(), MDC.get("requestId"), violations);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
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
        log.warn("[BUSINESS_ERROR] path={} code={} message={} traceId={}", request.getRequestURI(), ex.getErrorCode().getCode(), ex.getErrorCode().getMessage(), MDC.get("requestId"));
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
        log.error("[UNEXPECTED_ERROR] path={} traceId={} message={}", request.getRequestURI(), MDC.get("requestId"), ex.toString(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
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
