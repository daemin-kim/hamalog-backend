package com.Hamalog.service.security;

import com.Hamalog.config.RateLimitProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

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
    private static final Duration DEFAULT_DEGRADE_DURATION = Duration.ofMinutes(5);
    private static final long MIN_DEGRADED_LOG_INTERVAL_MS = Duration.ofSeconds(30).toMillis();

    private final AtomicLong degradedUntilEpochMs = new AtomicLong(0);
    private final AtomicLong lastDegradedLogEpochMs = new AtomicLong(0);

    @Autowired(required = false)
    private RateLimitProperties rateLimitProperties;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    public boolean tryConsumeAuthRequest(String key) {
        boolean allowed = checkRateLimit(key, authPerMinute(), 1, TimeUnit.MINUTES)
                && checkRateLimit(key, authPerHour(), 1, TimeUnit.HOURS);
        recordMetric("auth", allowed);
        return allowed;
    }

    public boolean tryConsumeApiRequest(String key) {
        boolean allowed = checkRateLimit(key, apiPerMinute(), 1, TimeUnit.MINUTES)
                && checkRateLimit(key, apiPerHour(), 1, TimeUnit.HOURS);
        recordMetric("api", allowed);
        return allowed;
    }

    private boolean checkRateLimit(String key, int maxRequests, long windowSize, TimeUnit timeUnit) {
        if (isDegradedModeActive()) {
            logDegradedRequest(key);
            return true;
        }

        try {
            String redisKey = "rate_limit:" + key + ":" + timeUnit.name().toLowerCase();
            long currentTime = System.currentTimeMillis();
            long windowSizeMs = timeUnit.toMillis(windowSize);
            long windowStart = currentTime - windowSizeMs;

            var zSetOps = redisTemplate.opsForZSet();
            zSetOps.removeRangeByScore(redisKey, 0, windowStart);

            Long currentCount = zSetOps.count(redisKey, windowStart, currentTime);

            if (currentCount != null && currentCount >= maxRequests) {
                log.warn("[RATE_LIMIT] Rate limit exceeded for key: {}, current count: {}, max: {}",
                    key, currentCount, maxRequests);
                return false;
            }

            zSetOps.add(redisKey, String.valueOf(currentTime), currentTime);
            redisTemplate.expire(redisKey, windowSizeMs + 1000, TimeUnit.MILLISECONDS);

            return true;
        } catch (Exception e) {
            enterDegradedMode(e);
            return true;
        }
    }

    public static String createIpKey(String ipAddress) {
        return "ip:" + ipAddress;
    }

    public static String createUserKey(String userId) {
        return "user:" + userId;
    }

    public long getRemainingRequests(String key, boolean isAuthEndpoint) {
        if (isDegradedModeActive()) {
            return Long.MAX_VALUE;
        }

        try {
            int maxRequests = isAuthEndpoint ? authPerMinute() : apiPerMinute();
            String redisKey = "rate_limit:" + key + ":minutes";
            long currentTime = System.currentTimeMillis();
            long windowStart = currentTime - TimeUnit.MINUTES.toMillis(1);

            Long currentCount = redisTemplate.opsForZSet().count(redisKey, windowStart, currentTime);
            long remainingRequests = maxRequests - (currentCount != null ? currentCount : 0);

            return Math.max(0, remainingRequests);
        } catch (Exception e) {
            enterDegradedMode(e);
            return Long.MAX_VALUE;
        }
    }

    public RateLimitInfo getRateLimitInfo(String key, boolean isAuthEndpoint) {
        int maxPerMinute = isAuthEndpoint ? authPerMinute() : apiPerMinute();
        int maxPerHour = isAuthEndpoint ? authPerHour() : apiPerHour();
        long remainingMinute = getRemainingRequests(key, isAuthEndpoint);

        return new RateLimitInfo(maxPerMinute, maxPerHour, remainingMinute);
    }

    public boolean isDegraded() {
        return isDegradedModeActive();
    }

    private boolean isDegradedModeActive() {
        long until = degradedUntilEpochMs.get();
        if (until == 0L) {
            return false;
        }

        long now = System.currentTimeMillis();
        if (now >= until && degradedUntilEpochMs.compareAndSet(until, 0L)) {
            log.info("[RATE_LIMIT] Fail-open window expired. Resuming normal rate limiting.");
            return false;
        }

        return true;
    }

    private void enterDegradedMode(Exception e) {
        long now = System.currentTimeMillis();
        Duration degradeDuration = getDegradeDuration();
        degradedUntilEpochMs.set(now + degradeDuration.toMillis());
        logDegradedTransition(now, e);
    }

    private void logDegradedTransition(long now, Exception e) {
        long lastLog = lastDegradedLogEpochMs.get();
        if (now - lastLog >= MIN_DEGRADED_LOG_INTERVAL_MS &&
                lastDegradedLogEpochMs.compareAndSet(lastLog, now)) {
            log.error("[RATE_LIMIT] Redis unavailable. Entering fail-open mode for {} seconds.",
                getDegradeDuration().toSeconds(), e);
        }
    }

    private void logDegradedRequest(String key) {
        long now = System.currentTimeMillis();
        long lastLog = lastDegradedLogEpochMs.get();
        if (now - lastLog >= MIN_DEGRADED_LOG_INTERVAL_MS &&
                lastDegradedLogEpochMs.compareAndSet(lastLog, now)) {
            log.warn("[RATE_LIMIT] Fail-open mode active. Allowing request from key {} without Redis enforcement.", key);
        }
    }

    public record RateLimitInfo(
        int maxRequestsPerMinute,
        int maxRequestsPerHour,
        long remainingRequestsThisMinute
    ) {}

    // ===== Helper methods for dynamic configuration & metrics =====

    private int authPerMinute() {
        return rateLimitProperties != null ? rateLimitProperties.getAuth().getPerMinute() : AUTH_REQUESTS_PER_MINUTE;
    }

    private int authPerHour() {
        return rateLimitProperties != null ? rateLimitProperties.getAuth().getPerHour() : AUTH_REQUESTS_PER_HOUR;
    }

    private int apiPerMinute() {
        return rateLimitProperties != null ? rateLimitProperties.getApi().getPerMinute() : API_REQUESTS_PER_MINUTE;
    }

    private int apiPerHour() {
        return rateLimitProperties != null ? rateLimitProperties.getApi().getPerHour() : API_REQUESTS_PER_HOUR;
    }

    private Duration getDegradeDuration() {
        return rateLimitProperties != null ? Duration.ofSeconds(rateLimitProperties.getDegradeSeconds()) : DEFAULT_DEGRADE_DURATION;
    }

    private void recordMetric(String endpointType, boolean allowed) {
        if (meterRegistry == null) return;
        if (rateLimitProperties != null && !rateLimitProperties.isMetricsEnabled()) return;
        Counter.builder("rate_limit.requests")
            .tag("endpoint_type", endpointType)
            .tag("outcome", allowed ? "allowed" : "blocked")
            .register(meterRegistry)
            .increment();
    }
}