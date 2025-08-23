package com.Hamalog.security.filter;

import com.Hamalog.service.security.RateLimitingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * 속도 제한 필터
 * 
 * 특정 엔드포인트에 대한 요청 속도를 제한합니다.
 * 인증 엔드포인트에 대해 더 엄격한 제한을 적용합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitingService rateLimitingService;
    private final ObjectMapper objectMapper;

    // Rate limited endpoints
    private static final Set<String> AUTH_ENDPOINTS = Set.of(
        "/auth/login",
        "/auth/signup",
        "/auth/logout"
    );

    private static final Set<String> PROTECTED_ENDPOINTS = Set.of(
        "/medication-record",
        "/medication-schedule", 
        "/side-effect"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        String clientIp = getClientIpAddress(request);
        
        // Check if this endpoint needs rate limiting
        boolean isAuthEndpoint = AUTH_ENDPOINTS.contains(requestURI);
        boolean isProtectedEndpoint = PROTECTED_ENDPOINTS.stream()
            .anyMatch(requestURI::startsWith);
        
        if (!isAuthEndpoint && !isProtectedEndpoint) {
            filterChain.doFilter(request, response);
            return;
        }

        // Create rate limit key (IP-based for now, can be enhanced with user-based)
        String rateLimitKey = RateLimitingService.createIpKey(clientIp);
        
        // Check rate limit
        boolean allowed;
        if (isAuthEndpoint) {
            allowed = rateLimitingService.tryConsumeAuthRequest(rateLimitKey);
        } else {
            allowed = rateLimitingService.tryConsumeApiRequest(rateLimitKey);
        }
        
        if (!allowed) {
            handleRateLimitExceeded(response, clientIp, requestURI, isAuthEndpoint);
            return;
        }
        
        // Add rate limit headers to response
        addRateLimitHeaders(response, rateLimitKey, isAuthEndpoint);
        
        filterChain.doFilter(request, response);
    }

    /**
     * 속도 제한 초과 시 응답을 처리합니다.
     */
    private void handleRateLimitExceeded(HttpServletResponse response, String clientIp, 
                                       String requestURI, boolean isAuthEndpoint) throws IOException {
        
        log.warn("Rate limit exceeded - IP: {}, URI: {}, Auth endpoint: {}", 
                clientIp, requestURI, isAuthEndpoint);
        
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        Map<String, Object> errorResponse = Map.of(
            "error", "Too Many Requests",
            "message", isAuthEndpoint ? 
                "인증 요청이 너무 많습니다. 잠시 후 다시 시도해주세요." : 
                "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.",
            "status", HttpStatus.TOO_MANY_REQUESTS.value(),
            "timestamp", System.currentTimeMillis()
        );
        
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    /**
     * 응답에 속도 제한 관련 헤더를 추가합니다.
     */
    private void addRateLimitHeaders(HttpServletResponse response, String rateLimitKey, 
                                   boolean isAuthEndpoint) {
        try {
            RateLimitingService.RateLimitInfo rateLimitInfo = 
                rateLimitingService.getRateLimitInfo(rateLimitKey, isAuthEndpoint);
            
            response.setHeader("X-RateLimit-Limit-Minute", 
                String.valueOf(rateLimitInfo.maxRequestsPerMinute()));
            response.setHeader("X-RateLimit-Limit-Hour", 
                String.valueOf(rateLimitInfo.maxRequestsPerHour()));
            response.setHeader("X-RateLimit-Remaining", 
                String.valueOf(rateLimitInfo.remainingRequestsThisMinute()));
            
        } catch (Exception e) {
            log.warn("Failed to add rate limit headers", e);
        }
    }

    /**
     * 클라이언트 IP 주소를 추출합니다.
     * 프록시나 로드 밸런서를 고려하여 실제 클라이언트 IP를 찾습니다.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP", 
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };
        
        for (String headerName : headerNames) {
            String ip = request.getHeader(headerName);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
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
        // Skip filtering for static resources
        String requestURI = request.getRequestURI();
        return requestURI.startsWith("/static/") || 
               requestURI.startsWith("/css/") || 
               requestURI.startsWith("/js/") || 
               requestURI.startsWith("/images/") ||
               requestURI.startsWith("/favicon.ico") ||
               requestURI.startsWith("/actuator/health");
    }
}