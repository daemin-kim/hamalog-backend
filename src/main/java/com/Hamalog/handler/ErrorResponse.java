package com.Hamalog.handler;

import com.Hamalog.logging.MDCUtil;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * 에러 응답 DTO
 * REST API 에러 응답의 표준 형식을 정의합니다.
 */
public class ErrorResponse {

    private final String code;
    private final String message;
    private final String path;
    private final Map<String, String> violations;
    private final LocalDateTime timestamp;
    private final String traceId;

    private ErrorResponse(
            HttpStatus status,
            String code,
            String message,
            String path,
            Map<String, String> violations,
            LocalDateTime timestamp,
            String traceId) {
        this.code = code;
        this.message = message;
        this.path = path;
        this.violations = violations;
        this.timestamp = timestamp;
        this.traceId = traceId;
    }

    public static ErrorResponse of(
            HttpStatus status, String code, String message, String path, Map<String, String> violations) {
        return new ErrorResponse(
                status,
                code,
                message,
                path,
                violations,
                LocalDateTime.now(),
                MDCUtil.get(MDCUtil.CORRELATION_ID));
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getTraceId() {
        return traceId;
    }
}
