package com.Hamalog.logging;

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
    
    @Autowired
    private StructuredLogger structuredLogger;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();

        String requestId = headerOrGenerate(request, REQUEST_ID_HEADER);
        boolean putRequestId = false;
        if (MDC.get("requestId") == null) {
            MDC.put("requestId", requestId);
            putRequestId = true;
        }
        MDC.put("method", request.getMethod());
        MDC.put("path", request.getRequestURI());

        response.setHeader(REQUEST_ID_HEADER, requestId);

        String user = currentPrincipal();
        String ip = request.getRemoteAddr();
        String ua = safeHeader(request, "User-Agent");
        String referer = safeHeader(request, "Referer");
        String requestType = determineRequestType(request.getRequestURI());

        // Create request parameters map
        Map<String, Object> requestParams = new HashMap<>();
        if (request.getQueryString() != null) {
            requestParams.put("queryString", request.getQueryString());
        }
        requestParams.put("referer", shorten(referer));

        try {
            log.debug("REQ [{}] {} {} | User: {} | IP: {} | UA: {} | Ref: {}", 
                    requestType, request.getMethod(), request.getRequestURI(), user, ip, shorten(ua), shorten(referer));
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            long took = System.currentTimeMillis() - start;
            int status = response.getStatus() == 0 ? 500 : response.getStatus();
            
            // Create structured error event
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
            
            structuredLogger.api(errorEvent);
            
            log.error("ERR [{}] {} {} | User: {} | Status: {} | Error: {}", 
                    requestType, request.getMethod(), request.getRequestURI(), user, status, ex.toString(), ex);
            throw ex;
        } finally {
            long took = System.currentTimeMillis() - start;
            int status = response.getStatus();
            String statusText = getStatusText(status);
            
            // Create structured success event
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
            
            structuredLogger.api(successEvent);
            
            log.debug("RES [{}] {} {} | User: {} | Status: {} {} | Time: {}ms", 
                    requestType, request.getMethod(), request.getRequestURI(), user, status, statusText, took);
            MDC.remove("method");
            MDC.remove("path");
            if (putRequestId) {
                MDC.remove("requestId");
            }
        }
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
        if ("Authorization".equalsIgnoreCase(name) || "Cookie".equalsIgnoreCase(name)) {
            return "***";
        }
        String v = request.getHeader(name);
        return v == null ? "" : v;
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
}
