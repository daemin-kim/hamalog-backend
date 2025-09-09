package com.Hamalog.logging;

import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * Utility class for managing MDC (Mapped Diagnostic Context) in a production-ready manner.
 * Provides centralized management of logging context including correlation IDs, user information,
 * and request tracing.
 */
public class MDCUtil {

    // Standard MDC keys for consistent logging
    public static final String CORRELATION_ID = "correlationId";
    public static final String REQUEST_ID = "requestId";
    public static final String USER_ID = "userId";
    public static final String SESSION_ID = "sessionId";
    public static final String IP_ADDRESS = "ipAddress";
    public static final String USER_AGENT = "userAgent";
    public static final String HTTP_METHOD = "httpMethod";
    public static final String REQUEST_URI = "requestUri";
    public static final String HOSTNAME = "hostname";
    public static final String THREAD_NAME = "threadName";
    public static final String TRACE_ID = "traceId";
    public static final String SPAN_ID = "spanId";
    public static final String APPLICATION_VERSION = "appVersion";
    public static final String ENVIRONMENT = "environment";

    // Performance and monitoring keys
    public static final String OPERATION_TYPE = "operationType";
    public static final String OPERATION_NAME = "operationName";
    public static final String START_TIME = "startTime";
    public static final String EXECUTION_TIME = "executionTime";
    public static final String ERROR_TYPE = "errorType";
    public static final String ERROR_MESSAGE = "errorMessage";

    // Business context keys
    public static final String BUSINESS_CONTEXT = "businessContext";
    public static final String ENTITY_TYPE = "entityType";
    public static final String ENTITY_ID = "entityId";
    public static final String ACTION_TYPE = "actionType";

    private static final String HOSTNAME_VALUE = initializeHostname();
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    /**
     * Initialize basic request context with correlation ID and request information
     */
    public static void initializeRequestContext() {
        String correlationId = generateCorrelationId();
        String requestId = generateRequestId();

        MDC.put(CORRELATION_ID, correlationId);
        MDC.put(REQUEST_ID, requestId);
        MDC.put(HOSTNAME, HOSTNAME_VALUE);
        MDC.put(THREAD_NAME, Thread.currentThread().getName());
        MDC.put(START_TIME, ISO_FORMATTER.format(Instant.now()));

        // Add request-specific information if available
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            MDC.put(HTTP_METHOD, request.getMethod());
            MDC.put(REQUEST_URI, request.getRequestURI());
            MDC.put(IP_ADDRESS, extractClientIpAddress(request));
            MDC.put(USER_AGENT, sanitizeUserAgent(request.getHeader("User-Agent")));
            MDC.put(SESSION_ID, request.getSession(false) != null ? request.getSession(false).getId() : "no-session");
        }

        // Add authenticated user information
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            MDC.put(USER_ID, auth.getName());
        } else {
            MDC.put(USER_ID, "anonymous");
        }
    }

    /**
     * Initialize business operation context
     */
    public static void initializeBusinessContext(String operationType, String operationName, 
                                               String entityType, String entityId) {
        MDC.put(OPERATION_TYPE, operationType);
        MDC.put(OPERATION_NAME, operationName);
        MDC.put(ENTITY_TYPE, entityType);
        MDC.put(ENTITY_ID, entityId);
    }

    /**
     * Initialize performance monitoring context
     */
    public static void initializePerformanceContext(String operationName) {
        MDC.put(OPERATION_NAME, operationName);
        MDC.put(START_TIME, String.valueOf(System.currentTimeMillis()));
    }

    /**
     * Add error context to MDC
     */
    public static void addErrorContext(Throwable throwable) {
        if (throwable != null) {
            MDC.put(ERROR_TYPE, throwable.getClass().getSimpleName());
            MDC.put(ERROR_MESSAGE, sanitizeErrorMessage(throwable.getMessage()));
        }
    }

    /**
     * Add custom field to MDC with null safety
     */
    public static void put(String key, String value) {
        if (key != null && value != null) {
            MDC.put(key, value);
        }
    }

    /**
     * Add custom field to MDC with object conversion
     */
    public static void put(String key, Object value) {
        if (key != null && value != null) {
            MDC.put(key, String.valueOf(value));
        }
    }

    /**
     * Get value from MDC with null safety
     */
    public static String get(String key) {
        return MDC.get(key);
    }

    /**
     * Remove specific key from MDC
     */
    public static void remove(String key) {
        if (key != null) {
            MDC.remove(key);
        }
    }

    /**
     * Clear all request-specific MDC context
     */
    public static void clearRequestContext() {
        MDC.remove(CORRELATION_ID);
        MDC.remove(REQUEST_ID);
        MDC.remove(HTTP_METHOD);
        MDC.remove(REQUEST_URI);
        MDC.remove(IP_ADDRESS);
        MDC.remove(USER_AGENT);
        MDC.remove(SESSION_ID);
        MDC.remove(START_TIME);
    }

    /**
     * Clear business operation context
     */
    public static void clearBusinessContext() {
        MDC.remove(OPERATION_TYPE);
        MDC.remove(OPERATION_NAME);
        MDC.remove(ENTITY_TYPE);
        MDC.remove(ENTITY_ID);
        MDC.remove(ACTION_TYPE);
        MDC.remove(BUSINESS_CONTEXT);
    }

    /**
     * Clear performance context
     */
    public static void clearPerformanceContext() {
        MDC.remove(OPERATION_NAME);
        MDC.remove(START_TIME);
        MDC.remove(EXECUTION_TIME);
    }

    /**
     * Clear error context
     */
    public static void clearErrorContext() {
        MDC.remove(ERROR_TYPE);
        MDC.remove(ERROR_MESSAGE);
    }

    /**
     * Clear all MDC context
     */
    public static void clearAllContext() {
        MDC.clear();
    }

    /**
     * Get copy of current MDC context
     */
    public static Map<String, String> getCopyOfContext() {
        return MDC.getCopyOfContextMap();
    }

    /**
     * Set MDC context from map
     */
    public static void setContext(Map<String, String> contextMap) {
        if (contextMap != null) {
            MDC.setContextMap(contextMap);
        }
    }

    /**
     * Generate unique correlation ID for tracking requests across services
     */
    private static String generateCorrelationId() {
        // Check if correlation ID already exists in headers
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            String existingCorrelationId = request.getHeader("X-Correlation-ID");
            if (existingCorrelationId != null && !existingCorrelationId.trim().isEmpty()) {
                return existingCorrelationId;
            }
        }
        return UUID.randomUUID().toString();
    }

    /**
     * Generate unique request ID
     */
    private static String generateRequestId() {
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            String existingRequestId = request.getHeader("X-Request-Id");
            if (existingRequestId != null && !existingRequestId.trim().isEmpty()) {
                return existingRequestId;
            }
        }
        return UUID.randomUUID().toString();
    }

    /**
     * Extract client IP address with support for load balancers and proxies
     */
    private static String extractClientIpAddress(HttpServletRequest request) {
        if (request == null) return "unknown";

        // Check X-Forwarded-For header (most common)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // X-Forwarded-For can contain multiple IPs, the first one is the original client IP
            return xForwardedFor.split(",")[0].trim();
        }

        // Check X-Real-IP header
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        // Check other common proxy headers
        String[] headerNames = {
            "X-Forwarded", "Forwarded-For", "Forwarded", 
            "X-Cluster-Client-IP", "X-Original-Forwarded-For"
        };

        for (String headerName : headerNames) {
            String headerValue = request.getHeader(headerName);
            if (headerValue != null && !headerValue.isEmpty() && !"unknown".equalsIgnoreCase(headerValue)) {
                return headerValue.split(",")[0].trim();
            }
        }

        // Fallback to remote address
        return request.getRemoteAddr();
    }

    /**
     * Sanitize user agent string to prevent log injection
     */
    private static String sanitizeUserAgent(String userAgent) {
        if (userAgent == null || userAgent.trim().isEmpty()) {
            return "unknown";
        }
        // Remove potentially dangerous characters and limit length
        String sanitized = userAgent.replaceAll("[\\r\\n\\t]", "").trim();
        return sanitized.length() > 200 ? sanitized.substring(0, 200) + "..." : sanitized;
    }

    /**
     * Sanitize error message to prevent log injection and limit size
     */
    private static String sanitizeErrorMessage(String errorMessage) {
        if (errorMessage == null || errorMessage.trim().isEmpty()) {
            return "No error message";
        }
        // Remove potentially dangerous characters and limit length
        String sanitized = errorMessage.replaceAll("[\\r\\n\\t]", " ").trim();
        return sanitized.length() > 500 ? sanitized.substring(0, 500) + "..." : sanitized;
    }

    /**
     * Get current HTTP request from Spring context
     */
    private static HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            return attributes.getRequest();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Initialize hostname for logging context
     */
    private static String initializeHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown-host";
        }
    }

    /**
     * Add application metadata to MDC
     */
    public static void initializeApplicationContext(String version, String environment) {
        MDC.put(APPLICATION_VERSION, version != null ? version : "unknown");
        MDC.put(ENVIRONMENT, environment != null ? environment : "unknown");
    }

    /**
     * Add distributed tracing context
     */
    public static void initializeTracingContext(String traceId, String spanId) {
        if (traceId != null && !traceId.trim().isEmpty()) {
            MDC.put(TRACE_ID, traceId);
        }
        if (spanId != null && !spanId.trim().isEmpty()) {
            MDC.put(SPAN_ID, spanId);
        }
    }
}