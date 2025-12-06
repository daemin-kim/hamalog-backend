package com.Hamalog.logging;

import com.Hamalog.logging.events.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized structured logging utility for production-ready logging
 * Provides consistent JSON-based logging for audit, security, performance, and business events
 */
@Component
public class StructuredLogger {

    private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger("AUDIT");
    private static final Logger SECURITY_LOGGER = LoggerFactory.getLogger("SECURITY");
    private static final Logger PERFORMANCE_LOGGER = LoggerFactory.getLogger("PERFORMANCE");
    private static final Logger APPLICATION_LOGGER = LoggerFactory.getLogger(StructuredLogger.class);
    
    private final ObjectMapper objectMapper;
    private final ObjectWriter jsonWriter;
    private final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    public StructuredLogger() {
        this.objectMapper = new ObjectMapper();
        this.jsonWriter = objectMapper.writer();
    }

    /**
     * Log structured audit event
     */
    public void audit(AuditEvent event) {
        try {
            Map<String, Object> context = createBaseContext("AUDIT");
            context.put("audit_operation", event.getOperation());
            context.put("audit_entity_type", event.getEntityType());
            context.put("audit_entity_id", event.getEntityId());
            context.put("audit_user_id", event.getUserId());
            context.put("audit_ip_address", event.getIpAddress());
            context.put("audit_user_agent", event.getUserAgent());
            context.put("audit_status", event.getStatus());
            context.put("audit_details", event.getDetails());
            
            setMDCContext(context);
            AUDIT_LOGGER.info(buildJsonMessage(context, "AUDIT_EVENT"));
        } finally {
            clearMDCContext();
        }
    }

    /**
     * Log structured security event
     */
    public void security(SecurityEvent event) {
        try {
            Map<String, Object> context = createBaseContext("SECURITY");
            context.put("security_event_type", event.getEventType());
            context.put("security_user_id", event.getUserId());
            context.put("security_ip_address", event.getIpAddress());
            context.put("security_user_agent", event.getUserAgent());
            context.put("security_resource", event.getResource());
            context.put("security_action", event.getAction());
            context.put("security_result", event.getResult());
            context.put("security_risk_level", event.getRiskLevel());
            context.put("security_details", event.getDetails());
            
            setMDCContext(context);
            String marker = "SECURITY_EVENT";
            if ("HIGH".equals(event.getRiskLevel()) || "CRITICAL".equals(event.getRiskLevel())) {
                SECURITY_LOGGER.error(buildJsonMessage(context, marker));
            } else {
                SECURITY_LOGGER.info(buildJsonMessage(context, marker));
            }
        } finally {
            clearMDCContext();
        }
    }

    /**
     * Log structured performance event
     */
    public void performance(PerformanceEvent event) {
        try {
            Map<String, Object> context = createBaseContext("PERFORMANCE");
            context.put("perf_operation", event.getOperation());
            context.put("perf_duration_ms", event.getDurationMs());
            context.put("perf_duration_nanos", event.getDurationNanos());
            context.put("perf_level", event.getPerformanceLevel());
            context.put("perf_success", event.isSuccess());
            context.put("perf_error_type", event.getErrorType());
            context.put("perf_user_id", event.getUserId());
            context.put("perf_method", event.getMethodName());
            context.put("perf_class", event.getClassName());
            context.put("perf_memory_before", event.getMemoryBefore());
            context.put("perf_memory_after", event.getMemoryAfter());
            context.put("perf_cpu_time", event.getCpuTime());
            
            setMDCContext(context);
            String marker = event.getDurationMs() > 3000 ? "PERFORMANCE_SLOW" : "PERFORMANCE";
            String message = buildJsonMessage(context, marker);
            if (event.getDurationMs() > 3000) {
                PERFORMANCE_LOGGER.warn(message);
            } else {
                PERFORMANCE_LOGGER.info(message);
            }
        } finally {
            clearMDCContext();
        }
    }

    /**
     * Log structured business event
     */
    public void business(BusinessEvent event) {
        try {
            Map<String, Object> context = createBaseContext("BUSINESS");
            context.put("business_event_type", event.getEventType());
            context.put("business_entity", event.getEntity());
            context.put("business_action", event.getAction());
            context.put("business_user_id", event.getUserId());
            context.put("business_result", event.getResult());
            context.put("business_metadata", event.getMetadata());
            
            setMDCContext(context);
            APPLICATION_LOGGER.info(buildJsonMessage(context, "BUSINESS_EVENT"));
        } finally {
            clearMDCContext();
        }
    }

    /**
     * Log structured API event
     */
    public void api(ApiEvent event) {
        try {
            Map<String, Object> context = createBaseContext("API");
            context.put("api_method", event.getHttpMethod());
            context.put("api_path", event.getPath());
            context.put("api_controller", event.getController());
            context.put("api_action", event.getAction());
            context.put("api_user_id", event.getUserId());
            context.put("api_ip_address", event.getIpAddress());
            context.put("api_user_agent", event.getUserAgent());
            context.put("api_duration_ms", event.getDurationMs());
            context.put("api_status_code", event.getStatusCode());
            context.put("api_request_size", event.getRequestSize());
            context.put("api_response_size", event.getResponseSize());
            context.put("api_request_type", event.getRequestType());
            context.put("api_parameters", SensitiveDataMasker.maskHeadersIfPresent(event.getParameters()));

            setMDCContext(context);
            if (event.getStatusCode() >= 400) {
                APPLICATION_LOGGER.error(buildJsonMessage(context, "API_ERROR"));
            } else {
                APPLICATION_LOGGER.info(buildJsonMessage(context, "API_SUCCESS"));
            }
        } finally {
            clearMDCContext();
        }
    }

    /**
     * Log error with structured context
     */
    public void error(String message, Throwable throwable, Map<String, Object> additionalContext) {
        try {
            Map<String, Object> context = createBaseContext("ERROR");
            context.put("error_message", message);
            context.put("error_type", throwable.getClass().getSimpleName());
            context.put("error_stack_trace", getStackTraceAsString(throwable));
            if (additionalContext != null) {
                context.putAll(additionalContext);
            }
            
            setMDCContext(context);
            APPLICATION_LOGGER.error(buildJsonMessage(context, "ERROR_EVENT"), throwable);
        } finally {
            clearMDCContext();
        }
    }

    /**
     * Create base context with common fields
     * Optimized with initial capacity to reduce HashMap resizing overhead
     */
    private Map<String, Object> createBaseContext(String logType) {
        Map<String, Object> context = new HashMap<>(8);
        context.put("log_type", logType);
        context.put("timestamp", ISO_FORMATTER.format(Instant.now()));
        context.put("correlationId", MDC.get(MDCUtil.CORRELATION_ID));
        context.put("requestId", MDC.get(MDCUtil.REQUEST_ID));
        context.put("threadName", Thread.currentThread().getName());
        return context;
    }

    /**
     * Set MDC context from map
     */
    private void setMDCContext(Map<String, Object> context) {
        SensitiveDataMasker.mask(context);
        context.forEach((key, value) -> {
            if (value != null) {
                MDC.put(key, String.valueOf(value));
            }
        });
    }

    private String buildJsonMessage(Map<String, Object> context, String marker) {
        Map<String, Object> payload = new HashMap<>(context);
        payload.put("marker", marker);
        try {
            return jsonWriter.writeValueAsString(payload);
        } catch (Exception ex) {
            return marker + " " + context;
        }
    }

    /**
     * Clear all MDC context except preserved keys
     */
    private void clearMDCContext() {
        String requestId = MDC.get(MDCUtil.REQUEST_ID);
        String correlationId = MDC.get(MDCUtil.CORRELATION_ID);
        MDC.clear();
        if (requestId != null) {
            MDC.put(MDCUtil.REQUEST_ID, requestId);
        }
        if (correlationId != null) {
            MDC.put(MDCUtil.CORRELATION_ID, correlationId);
        }
    }

    /**
     * Convert stack trace to string
     */
    private String getStackTraceAsString(Throwable throwable) {
        if (throwable == null) return null;
        
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.getClass().getSimpleName()).append(": ").append(throwable.getMessage());
        
        StackTraceElement[] elements = throwable.getStackTrace();
        for (int i = 0; i < Math.min(elements.length, 10); i++) {
            sb.append("\n\tat ").append(elements[i].toString());
        }
        
        return sb.toString();
    }
}