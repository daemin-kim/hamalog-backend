package com.Hamalog.logging;

import com.Hamalog.logging.events.*;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    public StructuredLogger() {
        this.objectMapper = new ObjectMapper();
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
            AUDIT_LOGGER.info("AUDIT_EVENT: {} on {} [{}] by user {} from {}", 
                    event.getOperation(), event.getEntityType(), event.getEntityId(), 
                    event.getUserId(), event.getIpAddress());
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
            
            if ("HIGH".equals(event.getRiskLevel()) || "CRITICAL".equals(event.getRiskLevel())) {
                SECURITY_LOGGER.error("SECURITY_EVENT: {} - {} attempted {} on {} from {} - Result: {}", 
                        event.getEventType(), event.getUserId(), event.getAction(), 
                        event.getResource(), event.getIpAddress(), event.getResult());
            } else {
                SECURITY_LOGGER.info("SECURITY_EVENT: {} - {} attempted {} on {} from {} - Result: {}", 
                        event.getEventType(), event.getUserId(), event.getAction(), 
                        event.getResource(), event.getIpAddress(), event.getResult());
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
            
            if (event.getDurationMs() > 3000) {
                PERFORMANCE_LOGGER.warn("PERFORMANCE_SLOW: {} took {}ms [{}] - Success: {}", 
                        event.getOperation(), event.getDurationMs(), 
                        event.getPerformanceLevel(), event.isSuccess());
            } else {
                PERFORMANCE_LOGGER.info("PERFORMANCE: {} took {}ms [{}] - Success: {}", 
                        event.getOperation(), event.getDurationMs(), 
                        event.getPerformanceLevel(), event.isSuccess());
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
            APPLICATION_LOGGER.info("BUSINESS_EVENT: {} performed {} on {} - Result: {}", 
                    event.getUserId(), event.getAction(), event.getEntity(), event.getResult());
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
            context.put("api_parameters", event.getParameters());
            
            setMDCContext(context);
            
            if (event.getStatusCode() >= 400) {
                APPLICATION_LOGGER.error("API_ERROR: [{}] {} {} - User: {} - Status: {} - Duration: {}ms", 
                        event.getRequestType(), event.getHttpMethod(), event.getPath(), event.getUserId(), 
                        event.getStatusCode(), event.getDurationMs());
            } else {
                APPLICATION_LOGGER.info("API_SUCCESS: [{}] {} {} - User: {} - Status: {} - Duration: {}ms", 
                        event.getRequestType(), event.getHttpMethod(), event.getPath(), event.getUserId(), 
                        event.getStatusCode(), event.getDurationMs());
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
            APPLICATION_LOGGER.error("ERROR: {} - Type: {}", message, throwable.getClass().getSimpleName(), throwable);
        } finally {
            clearMDCContext();
        }
    }

    /**
     * Create base context with common fields
     */
    private Map<String, Object> createBaseContext(String logType) {
        Map<String, Object> context = new HashMap<>();
        context.put("log_type", logType);
        context.put("timestamp", ISO_FORMATTER.format(Instant.now()));
        context.put("correlation_id", MDC.get("requestId"));
        context.put("thread_name", Thread.currentThread().getName());
        return context;
    }

    /**
     * Set MDC context from map
     */
    private void setMDCContext(Map<String, Object> context) {
        context.forEach((key, value) -> {
            if (value != null) {
                MDC.put(key, String.valueOf(value));
            }
        });
    }

    /**
     * Clear all MDC context
     */
    private void clearMDCContext() {
        MDC.clear();
        // Preserve requestId if it exists
        String requestId = MDC.get("requestId");
        if (requestId != null) {
            MDC.put("requestId", requestId);
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