package com.Hamalog.service.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RateLimitingService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final int AUTH_REQUESTS_PER_MINUTE = 5;
    private static final int AUTH_REQUESTS_PER_HOUR = 20;
    private static final int API_REQUESTS_PER_MINUTE = 60;
    private static final int API_REQUESTS_PER_HOUR = 1000;

    public boolean tryConsumeAuthRequest(String key) {
        return checkRateLimit(key, AUTH_REQUESTS_PER_MINUTE, 1, TimeUnit.MINUTES) &&
               checkRateLimit(key, AUTH_REQUESTS_PER_HOUR, 1, TimeUnit.HOURS);
    }

    public boolean tryConsumeApiRequest(String key) {
        return checkRateLimit(key, API_REQUESTS_PER_MINUTE, 1, TimeUnit.MINUTES) &&
               checkRateLimit(key, API_REQUESTS_PER_HOUR, 1, TimeUnit.HOURS);
    }

    private boolean checkRateLimit(String key, int maxRequests, long windowSize, TimeUnit timeUnit) {
        try {
            String redisKey = "rate_limit:" + key + ":" + timeUnit.name().toLowerCase();
            long currentTime = System.currentTimeMillis();
            long windowSizeMs = timeUnit.toMillis(windowSize);
            long windowStart = currentTime - windowSizeMs;

            redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, windowStart);

            Long currentCount = redisTemplate.opsForZSet().count(redisKey, windowStart, currentTime);
            
            if (currentCount != null && currentCount >= maxRequests) {
                log.warn("[RATE_LIMIT] Rate limit exceeded for key: {}, current count: {}, max: {}",
                    key, currentCount, maxRequests);
                return false;
            }

            redisTemplate.opsForZSet().add(redisKey, String.valueOf(currentTime), currentTime);
            redisTemplate.expire(redisKey, windowSizeMs + 1000, TimeUnit.MILLISECONDS);
            
            return true;
        } catch (Exception e) {
            log.error("[RATE_LIMIT] Error checking rate limit for key: " + key +
                ". Denying access for security (fail-safe)", e);
            // Redis 장애 시 보수적으로 접근 차단 (보안 우선)
            return false;
        }
    }

    public static String createIpKey(String ipAddress) {
        return "ip:" + ipAddress;
    }

    public static String createUserKey(String userId) {
        return "user:" + userId;
    }

    public long getRemainingRequests(String key, boolean isAuthEndpoint) {
        try {
            int maxRequests = isAuthEndpoint ? AUTH_REQUESTS_PER_MINUTE : API_REQUESTS_PER_MINUTE;
            String redisKey = "rate_limit:" + key + ":minutes";
            long currentTime = System.currentTimeMillis();
            long windowStart = currentTime - TimeUnit.MINUTES.toMillis(1);

            Long currentCount = redisTemplate.opsForZSet().count(redisKey, windowStart, currentTime);
            long remainingRequests = maxRequests - (currentCount != null ? currentCount : 0);
            
            return Math.max(0, remainingRequests);
        } catch (Exception e) {
            log.error("Error getting remaining requests for key: " + key, e);
            return Long.MAX_VALUE;
        }
    }

    public RateLimitInfo getRateLimitInfo(String key, boolean isAuthEndpoint) {
        int maxPerMinute = isAuthEndpoint ? AUTH_REQUESTS_PER_MINUTE : API_REQUESTS_PER_MINUTE;
        int maxPerHour = isAuthEndpoint ? AUTH_REQUESTS_PER_HOUR : API_REQUESTS_PER_HOUR;
        long remainingMinute = getRemainingRequests(key, isAuthEndpoint);
        
        return new RateLimitInfo(maxPerMinute, maxPerHour, remainingMinute);
    }

    public record RateLimitInfo(
        int maxRequestsPerMinute,
        int maxRequestsPerHour,
        long remainingRequestsThisMinute
    ) {}
}