package com.Hamalog.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Slf4j
@Component
public class RequestSizeMonitoringFilter extends OncePerRequestFilter {

    @Value("${hamalog.security.request.max-size-mb:10}")
    private long maxRequestSizeMB;

    @Value("${hamalog.security.request.warn-size-mb:5}")
    private long warnRequestSizeMB;

    @Value("${hamalog.security.request.monitoring-enabled:true}")
    private boolean monitoringEnabled;

    private static final Set<String> STRICT_ENDPOINTS = Set.of(
        "/auth/login",
        "/auth/signup"
    );

    private static final Set<String> EXCLUDED_ENDPOINTS = Set.of(
        "/actuator/health",
        "/favicon.ico"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        if (!monitoringEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        
        long contentLength = request.getContentLengthLong();
        
        if (contentLength > 0) {
            monitorRequestSize(requestURI, method, contentLength, getClientIpAddress(request));
        }
        
        monitorHeadersSize(request);
        
        filterChain.doFilter(request, response);
    }

    private void monitorRequestSize(String uri, String method, long contentLength, String clientIp) {
        long contentSizeMB = contentLength / (1024 * 1024);
        boolean isStrictEndpoint = STRICT_ENDPOINTS.contains(uri);
        
        long effectiveMaxSize = isStrictEndpoint ? 1 : maxRequestSizeMB;
        long effectiveWarnSize = isStrictEndpoint ? 0 : warnRequestSizeMB;
        
        if (contentSizeMB > effectiveMaxSize) {
            log.error("Large request detected - URI: {}, Method: {}, Size: {}MB, Client IP: {}, " +
                     "Strict endpoint: {}, Max allowed: {}MB", 
                     uri, method, contentSizeMB, clientIp, isStrictEndpoint, effectiveMaxSize);
        } else if (contentSizeMB > effectiveWarnSize) {
            log.warn("Large request warning - URI: {}, Method: {}, Size: {}MB, Client IP: {}, " +
                    "Strict endpoint: {}, Warning threshold: {}MB", 
                    uri, method, contentSizeMB, clientIp, isStrictEndpoint, effectiveWarnSize);
        } else {
            log.debug("Request size - URI: {}, Method: {}, Size: {}MB, Client IP: {}", 
                     uri, method, contentSizeMB, clientIp);
        }
    }

    private void monitorHeadersSize(HttpServletRequest request) {
        try {
            long totalHeadersSize = 0;
            var headerNames = request.getHeaderNames();
            
            if (headerNames != null) {
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    var headerValues = request.getHeaders(headerName);
                    
                    while (headerValues.hasMoreElements()) {
                        String headerValue = headerValues.nextElement();
                        totalHeadersSize += headerName.length() + 
                                          (headerValue != null ? headerValue.length() : 0) + 4;
                    }
                }
            }
            
            long headersSizeKB = totalHeadersSize / 1024;
            
            if (headersSizeKB > 32) {
                log.warn("Large headers detected - URI: {}, Headers size: {}KB, Client IP: {}", 
                        request.getRequestURI(), headersSizeKB, getClientIpAddress(request));
            } else if (headersSizeKB > 16) {
                log.info("Moderate headers size - URI: {}, Headers size: {}KB, Client IP: {}", 
                        request.getRequestURI(), headersSizeKB, getClientIpAddress(request));
            }
            
        } catch (Exception e) {
            log.debug("Error monitoring headers size", e);
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP", 
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP"
        };
        
        for (String headerName : headerNames) {
            String ip = request.getHeader(headerName);
            if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }
        
        return request.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        
        return EXCLUDED_ENDPOINTS.contains(requestURI) ||
               requestURI.startsWith("/static/") ||
               requestURI.startsWith("/css/") ||
               requestURI.startsWith("/js/") ||
               requestURI.startsWith("/images/");
    }

    public RequestSizeLimits getCurrentLimits() {
        return new RequestSizeLimits(maxRequestSizeMB, warnRequestSizeMB, monitoringEnabled);
    }

    public record RequestSizeLimits(
        long maxRequestSizeMB,
        long warnRequestSizeMB,
        boolean monitoringEnabled
    ) {}
}