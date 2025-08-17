package com.Hamalog.service.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Rate Limiting 서비스
 * 
 * Redis를 사용하여 API 요청에 대한 속도 제한을 구현합니다.
 * IP 기반 및 사용자 기반 속도 제한을 지원합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RateLimitingService {

    private final RedisTemplate<String, Object> redisTemplate;

    // Authentication endpoints rate limits
    private static final int AUTH_REQUESTS_PER_MINUTE = 5;
    private static final int AUTH_REQUESTS_PER_HOUR = 20;
    
    // General API rate limits
    private static final int API_REQUESTS_PER_MINUTE = 60;
    private static final int API_REQUESTS_PER_HOUR = 1000;

    /**
     * 인증 엔드포인트용 속도 제한을 확인합니다.
     * 
     * @param key 속도 제한 키 (IP 주소 또는 사용자 ID)
     * @return boolean 요청이 허용되면 true, 제한되면 false
     */
    public boolean tryConsumeAuthRequest(String key) {
        return checkRateLimit(key, AUTH_REQUESTS_PER_MINUTE, 1, TimeUnit.MINUTES) &&
               checkRateLimit(key, AUTH_REQUESTS_PER_HOUR, 1, TimeUnit.HOURS);
    }

    /**
     * 일반 API 엔드포인트용 속도 제한을 확인합니다.
     * 
     * @param key 속도 제한 키 (IP 주소 또는 사용자 ID)
     * @return boolean 요청이 허용되면 true, 제한되면 false
     */
    public boolean tryConsumeApiRequest(String key) {
        return checkRateLimit(key, API_REQUESTS_PER_MINUTE, 1, TimeUnit.MINUTES) &&
               checkRateLimit(key, API_REQUESTS_PER_HOUR, 1, TimeUnit.HOURS);
    }

    /**
     * 슬라이딩 윈도우 기반 속도 제한을 확인합니다.
     * 
     * @param key 속도 제한 키
     * @param maxRequests 최대 요청 수
     * @param windowSize 윈도우 크기
     * @param timeUnit 시간 단위
     * @return boolean 요청이 허용되면 true, 제한되면 false
     */
    private boolean checkRateLimit(String key, int maxRequests, long windowSize, TimeUnit timeUnit) {
        try {
            String redisKey = "rate_limit:" + key + ":" + timeUnit.name().toLowerCase();
            long currentTime = System.currentTimeMillis();
            long windowSizeMs = timeUnit.toMillis(windowSize);
            long windowStart = currentTime - windowSizeMs;

            // Remove expired entries
            redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, windowStart);

            // Count current requests in window
            Long currentCount = redisTemplate.opsForZSet().count(redisKey, windowStart, currentTime);
            
            if (currentCount != null && currentCount >= maxRequests) {
                log.warn("Rate limit exceeded for key: {}, current count: {}, max: {}", key, currentCount, maxRequests);
                return false;
            }

            // Add current request to window
            redisTemplate.opsForZSet().add(redisKey, String.valueOf(currentTime), currentTime);
            
            // Set expiration for cleanup
            redisTemplate.expire(redisKey, windowSizeMs + 1000, TimeUnit.MILLISECONDS);
            
            return true;
        } catch (Exception e) {
            log.error("Error checking rate limit for key: " + key, e);
            // Redis 오류 시 기본적으로 허용 (fail-open)
            return true;
        }
    }

    /**
     * IP 주소 기반 속도 제한 키를 생성합니다.
     * 
     * @param ipAddress IP 주소
     * @return String 속도 제한 키
     */
    public static String createIpKey(String ipAddress) {
        return "ip:" + ipAddress;
    }

    /**
     * 사용자 기반 속도 제한 키를 생성합니다.
     * 
     * @param userId 사용자 ID
     * @return String 속도 제한 키
     */
    public static String createUserKey(String userId) {
        return "user:" + userId;
    }

    /**
     * 현재 윈도우에서 남은 요청 수를 확인합니다.
     * 
     * @param key 속도 제한 키
     * @param isAuthEndpoint 인증 엔드포인트 여부
     * @return long 분당 남은 요청 수
     */
    public long getRemainingRequests(String key, boolean isAuthEndpoint) {
        try {
            int maxRequests = isAuthEndpoint ? AUTH_REQUESTS_PER_MINUTE : API_REQUESTS_PER_MINUTE;
            String redisKey = "rate_limit:" + key + ":minutes";
            long currentTime = System.currentTimeMillis();
            long windowStart = currentTime - TimeUnit.MINUTES.toMillis(1);

            // Count current requests in window
            Long currentCount = redisTemplate.opsForZSet().count(redisKey, windowStart, currentTime);
            long remainingRequests = maxRequests - (currentCount != null ? currentCount : 0);
            
            return Math.max(0, remainingRequests);
        } catch (Exception e) {
            log.error("Error getting remaining requests for key: " + key, e);
            return Long.MAX_VALUE; // 오류 시 제한 없음으로 처리
        }
    }

    /**
     * 속도 제한 정보를 확인합니다.
     * 
     * @param key 속도 제한 키
     * @param isAuthEndpoint 인증 엔드포인트 여부
     * @return RateLimitInfo 속도 제한 정보
     */
    public RateLimitInfo getRateLimitInfo(String key, boolean isAuthEndpoint) {
        int maxPerMinute = isAuthEndpoint ? AUTH_REQUESTS_PER_MINUTE : API_REQUESTS_PER_MINUTE;
        int maxPerHour = isAuthEndpoint ? AUTH_REQUESTS_PER_HOUR : API_REQUESTS_PER_HOUR;
        long remainingMinute = getRemainingRequests(key, isAuthEndpoint);
        
        return new RateLimitInfo(maxPerMinute, maxPerHour, remainingMinute);
    }

    /**
     * 속도 제한 정보를 담는 레코드
     */
    public record RateLimitInfo(
        int maxRequestsPerMinute,
        int maxRequestsPerHour,
        long remainingRequestsThisMinute
    ) {}
}