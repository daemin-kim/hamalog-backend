package com.Hamalog.logging.security;

import com.Hamalog.logging.MDCUtil;
import com.Hamalog.logging.StructuredLogger;
import com.Hamalog.logging.events.SecurityEvent;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.event.*;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.stereotype.Component;

/**
 * Production-ready security event monitor that tracks authentication failures,
 * suspicious activities, and potential security threats with automated alerting
 */
@Slf4j
@Component
public class SecurityEventMonitor {

    @Autowired
    private StructuredLogger structuredLogger;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    // In-memory fallback for tracking when Redis is not available
    private final ConcurrentHashMap<String, FailureTracker> failureTrackers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SuspiciousActivityTracker> activityTrackers = new ConcurrentHashMap<>();

    // Security thresholds
    private static final int MAX_LOGIN_FAILURES = 5;
    private static final Duration FAILURE_WINDOW = Duration.ofMinutes(15);
    private static final int SUSPICIOUS_REQUEST_THRESHOLD = 20;
    private static final Duration SUSPICIOUS_ACTIVITY_WINDOW = Duration.ofMinutes(5);
    private static final int BRUTE_FORCE_THRESHOLD = 10;

    /**
     * Handle authentication success events
     */
    @EventListener
    public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
        try {
            String username = event.getAuthentication().getName();
            String clientIp = getCurrentClientIp();
            
            // Clear failure tracking for successful authentication
            clearFailureTracking(clientIp, username);
            
            // Log successful authentication
            SecurityEvent securityEvent = SecurityEvent.builder()
                    .eventType("AUTHENTICATION_SUCCESS")
                    .userId(username)
                    .ipAddress(clientIp)
                    .userAgent(getCurrentUserAgent())
                    .resource("LOGIN")
                    .action("AUTHENTICATE")
                    .result("SUCCESS")
                    .riskLevel("LOW")
                    .details("User authenticated successfully")
                    .build();
            
            structuredLogger.security(securityEvent);
            
            log.info("AUTHENTICATION_SUCCESS: User {} authenticated from {} - correlationId: {}", 
                username, clientIp, MDCUtil.get(MDCUtil.CORRELATION_ID));
                
        } catch (Exception e) {
            log.error("Failed to process authentication success event", e);
        }
    }

    /**
     * Handle authentication failure events
     */
    @EventListener
    public void handleAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        try {
            String username = extractUsername(event);
            String clientIp = getCurrentClientIp();
            String failureReason = event.getException().getClass().getSimpleName();
            
            // Track failure
            int failureCount = trackAuthenticationFailure(clientIp, username);
            
            // Determine risk level based on failure count
            String riskLevel = determineRiskLevel(failureCount);
            
            // Log authentication failure
            SecurityEvent securityEvent = SecurityEvent.builder()
                    .eventType("AUTHENTICATION_FAILURE")
                    .userId(username)
                    .ipAddress(clientIp)
                    .userAgent(getCurrentUserAgent())
                    .resource("LOGIN")
                    .action("AUTHENTICATE")
                    .result("FAILURE")
                    .riskLevel(riskLevel)
                    .details("Authentication failed: " + failureReason + " (Attempt " + failureCount + ")")
                    .build();
            
            structuredLogger.security(securityEvent);
            
            // Log with appropriate level based on risk
            if ("CRITICAL".equals(riskLevel) || "HIGH".equals(riskLevel)) {
                log.error("AUTHENTICATION_FAILURE [{}]: User {} failed authentication from {} - Reason: {} - Attempt: {} - correlationId: {}", 
                    riskLevel, username, clientIp, failureReason, failureCount, MDCUtil.get(MDCUtil.CORRELATION_ID));
                
                // Check for brute force attack
                if (failureCount >= BRUTE_FORCE_THRESHOLD) {
                    handleBruteForceDetection(clientIp, username, failureCount);
                }
            } else {
                log.warn("AUTHENTICATION_FAILURE [{}]: User {} failed authentication from {} - Reason: {} - Attempt: {}", 
                    riskLevel, username, clientIp, failureReason, failureCount);
            }
            
        } catch (Exception e) {
            log.error("Failed to process authentication failure event", e);
        }
    }

    /**
     * Handle authorization denied events
     */
    @EventListener
    public void handleAuthorizationDenied(AuthorizationDeniedEvent event) {
        try {
            String username = "unknown";
            if (event.getAuthentication() != null && event.getAuthentication().get() != null) {
                Object principal = event.getAuthentication().get().getPrincipal();
                username = principal != null ? principal.toString() : "unknown";
            }
            
            String clientIp = getCurrentClientIp();
            String resource = event.getObject() != null ? event.getObject().toString() : "unknown_resource";
            
            // Track suspicious activity
            trackSuspiciousActivity(clientIp, "AUTHORIZATION_DENIED");
            
            SecurityEvent securityEvent = SecurityEvent.builder()
                    .eventType("AUTHORIZATION_DENIED")
                    .userId(username)
                    .ipAddress(clientIp)
                    .userAgent(getCurrentUserAgent())
                    .resource(resource)
                    .action("ACCESS")
                    .result("DENIED")
                    .riskLevel("MEDIUM")
                    .details("Access denied to resource: " + resource)
                    .build();
            
            structuredLogger.security(securityEvent);
            
            log.warn("AUTHORIZATION_DENIED: User {} denied access to {} from {} - correlationId: {}", 
                username, resource, clientIp, MDCUtil.get(MDCUtil.CORRELATION_ID));
                
        } catch (Exception e) {
            log.error("Failed to process authorization denied event", e);
        }
    }

    /**
     * Track suspicious HTTP request patterns
     */
    public void trackSuspiciousRequest(String clientIp, String requestPath, String reason) {
        try {
            int requestCount = trackSuspiciousActivity(clientIp, "SUSPICIOUS_REQUEST");
            
            String riskLevel = requestCount > SUSPICIOUS_REQUEST_THRESHOLD ? "HIGH" : "MEDIUM";
            
            SecurityEvent securityEvent = SecurityEvent.builder()
                    .eventType("SUSPICIOUS_REQUEST")
                    .userId(MDCUtil.get(MDCUtil.USER_ID))
                    .ipAddress(clientIp)
                    .userAgent(getCurrentUserAgent())
                    .resource(requestPath)
                    .action("HTTP_REQUEST")
                    .result("SUSPICIOUS")
                    .riskLevel(riskLevel)
                    .details("Suspicious request pattern detected: " + reason + " (Count: " + requestCount + ")")
                    .build();
            
            structuredLogger.security(securityEvent);
            
            if ("HIGH".equals(riskLevel)) {
                log.error("SUSPICIOUS_REQUEST [HIGH]: High frequency suspicious requests from {} - Path: {} - Reason: {} - Count: {}", 
                    clientIp, requestPath, reason, requestCount);
            } else {
                log.warn("SUSPICIOUS_REQUEST [MEDIUM]: Suspicious request from {} - Path: {} - Reason: {}", 
                    clientIp, requestPath, reason);
            }
            
        } catch (Exception e) {
            log.error("Failed to track suspicious request", e);
        }
    }

    /**
     * Track authentication failure and return current count
     */
    private int trackAuthenticationFailure(String clientIp, String username) {
        String key = "auth_failures:" + clientIp + ":" + username;
        
        if (redisTemplate != null) {
            try {
                Long count = redisTemplate.opsForValue().increment(key);
                redisTemplate.expire(key, FAILURE_WINDOW.toMinutes(), TimeUnit.MINUTES);
                return count.intValue();
            } catch (Exception e) {
                log.debug("Redis not available, using in-memory tracking", e);
            }
        }
        
        // Fallback to in-memory tracking
        FailureTracker tracker = failureTrackers.computeIfAbsent(key, k -> new FailureTracker());
        return tracker.incrementAndGet();
    }

    /**
     * Track suspicious activity and return current count
     */
    private int trackSuspiciousActivity(String clientIp, String activityType) {
        String key = "suspicious:" + clientIp + ":" + activityType;
        
        if (redisTemplate != null) {
            try {
                Long count = redisTemplate.opsForValue().increment(key);
                redisTemplate.expire(key, SUSPICIOUS_ACTIVITY_WINDOW.toMinutes(), TimeUnit.MINUTES);
                return count.intValue();
            } catch (Exception e) {
                log.debug("Redis not available, using in-memory tracking", e);
            }
        }
        
        // Fallback to in-memory tracking
        SuspiciousActivityTracker tracker = activityTrackers.computeIfAbsent(key, k -> new SuspiciousActivityTracker());
        return tracker.incrementAndGet();
    }

    /**
     * Clear failure tracking for successful authentication
     */
    private void clearFailureTracking(String clientIp, String username) {
        String key = "auth_failures:" + clientIp + ":" + username;
        
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(key);
            } catch (Exception e) {
                log.debug("Failed to clear Redis failure tracking", e);
            }
        }
        
        // Clear in-memory tracking
        failureTrackers.remove(key);
    }

    /**
     * Handle brute force attack detection
     */
    private void handleBruteForceDetection(String clientIp, String username, int failureCount) {
        SecurityEvent bruteForceEvent = SecurityEvent.builder()
                .eventType("BRUTE_FORCE_ATTACK")
                .userId(username)
                .ipAddress(clientIp)
                .userAgent(getCurrentUserAgent())
                .resource("LOGIN")
                .action("ATTACK")
                .result("DETECTED")
                .riskLevel("CRITICAL")
                .details("Brute force attack detected - " + failureCount + " failed attempts")
                .build();
        
        structuredLogger.security(bruteForceEvent);
        
        log.error("BRUTE_FORCE_ATTACK [CRITICAL]: Brute force attack detected from {} targeting user {} - {} attempts", 
            clientIp, username, failureCount);
        
        // TODO: Integrate with IP blocking service or rate limiting
        // blockIpAddress(clientIp, Duration.ofHours(1));
    }

    /**
     * Determine risk level based on failure count
     */
    private String determineRiskLevel(int failureCount) {
        if (failureCount >= BRUTE_FORCE_THRESHOLD) {
            return "CRITICAL";
        } else if (failureCount >= MAX_LOGIN_FAILURES) {
            return "HIGH";
        } else if (failureCount >= 3) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    /**
     * Extract username from authentication event
     */
    private String extractUsername(AbstractAuthenticationFailureEvent event) {
        if (event.getAuthentication() != null && event.getAuthentication().getName() != null) {
            return event.getAuthentication().getName();
        }
        
        // Try to extract from principal
        Object principal = event.getAuthentication() != null ? event.getAuthentication().getPrincipal() : null;
        if (principal != null) {
            return principal.toString();
        }
        
        return "unknown";
    }

    /**
     * Get current client IP address
     */
    private String getCurrentClientIp() {
        try {
            return MDCUtil.get(MDCUtil.IP_ADDRESS);
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Get current user agent
     */
    private String getCurrentUserAgent() {
        try {
            return MDCUtil.get(MDCUtil.USER_AGENT);
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * In-memory failure tracker for when Redis is not available
     */
    private static class FailureTracker {
        private int count = 0;
        private LocalDateTime firstFailure = LocalDateTime.now();
        
        synchronized int incrementAndGet() {
            // Reset if window expired
            if (Duration.between(firstFailure, LocalDateTime.now()).compareTo(FAILURE_WINDOW) > 0) {
                count = 0;
                firstFailure = LocalDateTime.now();
            }
            return ++count;
        }
    }

    /**
     * In-memory suspicious activity tracker for when Redis is not available
     */
    private static class SuspiciousActivityTracker {
        private int count = 0;
        private LocalDateTime firstActivity = LocalDateTime.now();
        
        synchronized int incrementAndGet() {
            // Reset if window expired
            if (Duration.between(firstActivity, LocalDateTime.now()).compareTo(SUSPICIOUS_ACTIVITY_WINDOW) > 0) {
                count = 0;
                firstActivity = LocalDateTime.now();
            }
            return ++count;
        }
    }
}