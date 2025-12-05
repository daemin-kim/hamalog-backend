package com.Hamalog.logging;

import com.Hamalog.logging.LoggingConstants;
import com.Hamalog.logging.MDCUtil;
import com.Hamalog.logging.events.ApiEvent;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final StructuredLogger structuredLogger;

    @Autowired
    public RequestLoggingFilter(StructuredLogger structuredLogger) {
        this.structuredLogger = structuredLogger;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();
        boolean requestFailed = false;

        String correlationId = headerOrGenerate(request, LoggingConstants.CORRELATION_ID_HEADER);
        boolean putCorrelationId = false;
        if (MDC.get(MDCUtil.CORRELATION_ID) == null) {
            MDC.put(MDCUtil.CORRELATION_ID, correlationId);
            putCorrelationId = true;
        }
        String requestId = headerOrGenerate(request, REQUEST_ID_HEADER);
        boolean putRequestId = false;
        if (MDC.get(MDCUtil.REQUEST_ID) == null) {
            MDC.put(MDCUtil.REQUEST_ID, requestId);
            putRequestId = true;
        }
        MDC.put("method", request.getMethod());
        MDC.put("path", request.getRequestURI());

        response.setHeader(LoggingConstants.CORRELATION_ID_HEADER, correlationId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        request.setAttribute(LoggingConstants.API_LOGGING_OWNER_ATTRIBUTE, "FILTER");

        String user = currentPrincipal();
        String ip = request.getRemoteAddr();
        String ua = safeHeader(request, "User-Agent");
        String referer = safeHeader(request, "Referer");
        String requestType = determineRequestType(request.getRequestURI());

        Map<String, Object> requestParams = new HashMap<>();
        if (request.getQueryString() != null) {
            requestParams.put("queryString", request.getQueryString());
        }
        requestParams.put("referer", shorten(referer));
        requestParams.put("headers", SensitiveDataMasker.maskHeaders(collectSafeHeaders(request)));

        StatusAwareResponseWrapper statusAwareResponse = wrapResponse(response);

        try {
            log.debug("REQ [{}] {} {} | User: {} | IP: {} | UA: {} | Ref: {}",
                    requestType, request.getMethod(), request.getRequestURI(), user, ip, shorten(ua), shorten(referer));
            filterChain.doFilter(request, statusAwareResponse);
        } catch (Exception ex) {
            requestFailed = true;
            long took = System.currentTimeMillis() - start;
            int status = statusAwareResponse.getStatusCode() == 0 ? 500 : statusAwareResponse.getStatusCode();

            ApiEvent errorEvent = ApiEvent.builder()
                    .httpMethod(request.getMethod())
                    .path(request.getRequestURI())
                    .controller("FILTER")
                    .action("HTTP_REQUEST")
                    .userId(user)
                    .ipAddress(ip)
                    .userAgent(ua)
                    .durationMs(took)
                    .statusCode(status)
                    .requestType(requestType)
                    .parameters(requestParams)
                    .build();

            logStructuredApiOnce(request, errorEvent);

            log.error("ERR [{}] {} {} | User: {} | Status: {} | Error: {}",
                    requestType, request.getMethod(), request.getRequestURI(), user, status, ex.toString(), ex);
            throw ex;
        } finally {
            long took = System.currentTimeMillis() - start;
            int status = statusAwareResponse.getStatusCode();
            if (status == 0) {
                status = requestFailed ? 500 : 500;
            }
            String statusText = getStatusText(status);
            request.setAttribute(LoggingConstants.RESPONSE_STATUS_ATTRIBUTE, status);

            if (!requestFailed) {
                ApiEvent successEvent = ApiEvent.builder()
                        .httpMethod(request.getMethod())
                        .path(request.getRequestURI())
                        .controller("FILTER")
                        .action("HTTP_REQUEST")
                        .userId(user)
                        .ipAddress(ip)
                        .userAgent(ua)
                        .durationMs(took)
                        .statusCode(status)
                        .requestType(requestType)
                        .parameters(requestParams)
                        .build();

                logStructuredApiOnce(request, successEvent);
            }
            request.setAttribute(LoggingConstants.API_LOGGING_OWNER_ATTRIBUTE, null);

            log.debug("RES [{}] {} {} | User: {} | Status: {} {} | Time: {}ms",
                    requestType, request.getMethod(), request.getRequestURI(), user, status, statusText, took);
            MDC.remove("method");
            MDC.remove("path");
            if (putRequestId) {
                MDC.remove(MDCUtil.REQUEST_ID);
            }
            if (putCorrelationId) {
                MDC.remove(MDCUtil.CORRELATION_ID);
            }
        }
    }

    private void logStructuredApiOnce(HttpServletRequest request, ApiEvent event) {
        if (request == null) {
            structuredLogger.api(event);
            return;
        }
        Object alreadyLogged = request.getAttribute(LoggingConstants.API_EVENT_LOGGED_ATTRIBUTE);
        if (Boolean.TRUE.equals(alreadyLogged)) {
            return;
        }
        structuredLogger.api(event);
        request.setAttribute(LoggingConstants.API_EVENT_LOGGED_ATTRIBUTE, true);
    }

    private String currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return "anonymous";
        try {
            return String.valueOf(auth.getName());
        } catch (Exception e) {
            return "anonymous";
        }
    }

    private String headerOrGenerate(HttpServletRequest request, String name) {
        String v = request.getHeader(name);
        if (v == null || v.isBlank()) return UUID.randomUUID().toString();
        return v;
    }

    private String safeHeader(HttpServletRequest request, String name) {
        if (name == null) {
            return "";
        }
        String lower = name.toLowerCase();
        if (lower.contains("authorization") || lower.contains("cookie") || lower.contains("token")) {
            return "***";
        }
        String v = request.getHeader(name);
        return v == null ? "" : shorten(v);
    }

    private Map<String, String> collectSafeHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        var names = request.getHeaderNames();
        if (names == null) {
            return headers;
        }
        while (names.hasMoreElements()) {
            String header = names.nextElement();
            headers.put(header, safeHeader(request, header));
        }
        return headers;
    }

    private String shorten(String s) {
        if (s == null) return "";
        return s.length() > 200 ? s.substring(0, 200) + "..." : s;
    }

    private String getStatusText(int status) {
        if (status >= 200 && status < 300) return "SUCCESS"; // Success
        if (status >= 300 && status < 400) return "REDIRECT"; // Redirect
        if (status >= 400 && status < 500) return "CLIENT_ERROR"; // Client Error
        if (status >= 500) return "SERVER_ERROR"; // Server Error
        return "UNKNOWN"; // Unknown
    }
    
    /**
     * Determine request type based on the request path
     */
    private String determineRequestType(String path) {
        if (path == null) return "외부 요청";
        
        // Internal requests: actuator endpoints for health checks and monitoring
        if (path.startsWith("/actuator/")) {
            return "내부 요청";
        }
        
        // All other requests are considered external
        return "외부 요청";
    }

    private StatusAwareResponseWrapper wrapResponse(HttpServletResponse response) {
        if (response instanceof StatusAwareResponseWrapper existingWrapper) {
            return existingWrapper;
        }
        return new StatusAwareResponseWrapper(response);
    }
}
