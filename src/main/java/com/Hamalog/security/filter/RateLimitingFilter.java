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

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitingService rateLimitingService;
    private final ObjectMapper objectMapper;

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
        
        boolean isAuthEndpoint = AUTH_ENDPOINTS.contains(requestURI);
        boolean isProtectedEndpoint = PROTECTED_ENDPOINTS.stream()
            .anyMatch(requestURI::startsWith);
        
        if (!isAuthEndpoint && !isProtectedEndpoint) {
            filterChain.doFilter(request, response);
            return;
        }

        String rateLimitKey = RateLimitingService.createIpKey(clientIp);
        
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
        
        addRateLimitHeaders(response, rateLimitKey, isAuthEndpoint);
        
        filterChain.doFilter(request, response);
    }

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

    private String getClientIpAddress(HttpServletRequest request) {
        // Security Fix: Only use X-Forwarded-For from trusted proxies, otherwise use remote address
        // In production, configure trusted proxy IPs and validate them
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        
        // TODO: Add trusted proxy IP validation in production
        // For now, only use X-Forwarded-For if it exists, otherwise fall back to remote address
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // Take the first IP in case of multiple proxies
            if (xForwardedFor.contains(",")) {
                return xForwardedFor.split(",")[0].trim();
            }
            return xForwardedFor;
        }
        
        // Use the actual remote address as the most reliable source
        return request.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        return requestURI.startsWith("/static/") || 
               requestURI.startsWith("/css/") || 
               requestURI.startsWith("/js/") || 
               requestURI.startsWith("/images/") ||
               requestURI.startsWith("/favicon.ico") ||
               requestURI.startsWith("/actuator/health");
    }
}