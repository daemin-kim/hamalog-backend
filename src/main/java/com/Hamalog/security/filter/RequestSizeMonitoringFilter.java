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

/**
 * 요청 크기 모니터링 필터
 * 
 * 요청 크기를 모니터링하고 로깅하여 DoS 공격을 탐지합니다.
 * Spring Boot의 기본 제한과 함께 동작하여 추가적인 모니터링을 제공합니다.
 */
@Slf4j
@Component
public class RequestSizeMonitoringFilter extends OncePerRequestFilter {

    // Configuration values (with defaults)
    @Value("${hamalog.security.request.max-size-mb:10}")
    private long maxRequestSizeMB;

    @Value("${hamalog.security.request.warn-size-mb:5}")
    private long warnRequestSizeMB;

    @Value("${hamalog.security.request.monitoring-enabled:true}")
    private boolean monitoringEnabled;

    // Endpoints that should have stricter limits
    private static final Set<String> STRICT_ENDPOINTS = Set.of(
        "/auth/login",
        "/auth/signup"
    );

    // Endpoints excluded from monitoring
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
        
        // Monitor request size
        long contentLength = request.getContentLengthLong();
        
        if (contentLength > 0) {
            monitorRequestSize(requestURI, method, contentLength, getClientIpAddress(request));
        }
        
        // Monitor headers size
        monitorHeadersSize(request);
        
        filterChain.doFilter(request, response);
    }

    /**
     * 요청 크기를 모니터링하고 경고를 로깅합니다.
     */
    private void monitorRequestSize(String uri, String method, long contentLength, String clientIp) {
        long contentSizeMB = contentLength / (1024 * 1024);
        boolean isStrictEndpoint = STRICT_ENDPOINTS.contains(uri);
        
        // Authentication endpoints should have smaller limits
        long effectiveMaxSize = isStrictEndpoint ? 1 : maxRequestSizeMB;
        long effectiveWarnSize = isStrictEndpoint ? 0 : warnRequestSizeMB; // 0MB = always warn for auth endpoints
        
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

    /**
     * HTTP 헤더 크기를 모니터링합니다.
     */
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
                                          (headerValue != null ? headerValue.length() : 0) + 4; // +4 for ": " and CRLF
                    }
                }
            }
            
            long headersSizeKB = totalHeadersSize / 1024;
            
            if (headersSizeKB > 32) { // 32KB warning threshold
                log.warn("Large headers detected - URI: {}, Headers size: {}KB, Client IP: {}", 
                        request.getRequestURI(), headersSizeKB, getClientIpAddress(request));
            } else if (headersSizeKB > 16) { // 16KB info threshold
                log.info("Moderate headers size - URI: {}, Headers size: {}KB, Client IP: {}", 
                        request.getRequestURI(), headersSizeKB, getClientIpAddress(request));
            }
            
        } catch (Exception e) {
            log.debug("Error monitoring headers size", e);
        }
    }

    /**
     * 클라이언트 IP 주소를 추출합니다.
     */
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
                // X-Forwarded-For can contain multiple IPs, take the first one
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
        
        // Skip monitoring for excluded endpoints
        return EXCLUDED_ENDPOINTS.contains(requestURI) ||
               requestURI.startsWith("/static/") ||
               requestURI.startsWith("/css/") ||
               requestURI.startsWith("/js/") ||
               requestURI.startsWith("/images/");
    }

    /**
     * 현재 요청 크기 제한 설정을 반환합니다 (모니터링 목적).
     */
    public RequestSizeLimits getCurrentLimits() {
        return new RequestSizeLimits(maxRequestSizeMB, warnRequestSizeMB, monitoringEnabled);
    }

    /**
     * 요청 크기 제한 정보를 담는 레코드
     */
    public record RequestSizeLimits(
        long maxRequestSizeMB,
        long warnRequestSizeMB,
        boolean monitoringEnabled
    ) {}
}