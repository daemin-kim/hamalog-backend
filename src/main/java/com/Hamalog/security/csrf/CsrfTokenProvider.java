package com.Hamalog.security.csrf;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CsrfTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(CsrfTokenProvider.class);
    private static final int TOKEN_LENGTH = 32;
    private static final Duration TOKEN_TTL = Duration.ofMinutes(60);
    private static final String REDIS_KEY_PREFIX = "csrf:";

    private final SecureRandom secureRandom = new SecureRandom();
    private final RedisTemplate<String, Object> redisTemplate;
    private final ConcurrentMap<String, TokenRecord> fallbackStore = new ConcurrentHashMap<>();

    @Autowired
    public CsrfTokenProvider(@Nullable RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    CsrfTokenProvider(RedisTemplate<String, Object> redisTemplate, boolean forTest) {
        this.redisTemplate = redisTemplate;
    }

    public String generateToken(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            throw new IllegalArgumentException("Session ID는 필수입니다");
        }

        String token = createRandomToken();
        if (storeInRedis(sessionId, token)) {
            fallbackStore.remove(sessionId);
        } else {
            storeInFallback(sessionId, token);
        }
        return token;
    }

    public boolean validateToken(String sessionId, String token) {
        if (!StringUtils.hasText(sessionId) || !StringUtils.hasText(token)) {
            return false;
        }

        if (validateWithRedis(sessionId, token)) {
            return true;
        }

        return readFromFallback(sessionId)
                .map(storedToken -> constantTimeEquals(storedToken, token))
                .orElse(false);
    }

    public void invalidateToken(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return;
        }

        deleteFromRedis(sessionId);
        fallbackStore.remove(sessionId);
    }

    public Optional<Long> getRemainingTtl(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return Optional.empty();
        }

        Optional<Long> redisTtl = getRedisTtl(sessionId);
        if (redisTtl.isPresent()) {
            return redisTtl;
        }

        return getFallbackTtl(sessionId);
    }

    public boolean isFallbackActive() {
        return redisTemplate == null || !fallbackStore.isEmpty();
    }

    private String createRandomToken() {
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    private String redisKey(String sessionId) {
        return REDIS_KEY_PREFIX + sessionId;
    }

    private boolean storeInRedis(String sessionId, String token) {
        if (redisTemplate == null) {
            return false;
        }

        try {
            redisTemplate.opsForValue().set(redisKey(sessionId), token, TOKEN_TTL);
            return true;
        } catch (RuntimeException ex) {
            log.warn("[CSRF] Redis write 실패 – fallback 전환 (sessionId={})", sessionId, ex);
            return false;
        }
    }

    private boolean validateWithRedis(String sessionId, String token) {
        if (redisTemplate == null) {
            return false;
        }

        try {
            Object storedValue = redisTemplate.opsForValue().get(redisKey(sessionId));
            if (storedValue instanceof String storedToken) {
                return constantTimeEquals(storedToken, token);
            }
            return false;
        } catch (RuntimeException ex) {
            log.warn("[CSRF] Redis read 실패 – fallback 검증 사용 (sessionId={})", sessionId, ex);
            return false;
        }
    }

    private void deleteFromRedis(String sessionId) {
        if (redisTemplate == null) {
            return;
        }

        try {
            redisTemplate.delete(redisKey(sessionId));
        } catch (RuntimeException ex) {
            log.warn("[CSRF] Redis 토큰 삭제 실패 – fallback 데이터만 정리 (sessionId={})", sessionId, ex);
        }
    }

    private Optional<Long> getRedisTtl(String sessionId) {
        if (redisTemplate == null) {
            return Optional.empty();
        }

        try {
            Long ttl = redisTemplate.getExpire(redisKey(sessionId));
            return Optional.ofNullable(ttl).filter(value -> value > 0);
        } catch (RuntimeException ex) {
            log.warn("[CSRF] Redis TTL 조회 실패 – fallback TTL 확인 (sessionId={})", sessionId, ex);
            return Optional.empty();
        }
    }

    private void storeInFallback(String sessionId, String token) {
        fallbackStore.put(sessionId, new TokenRecord(token, Instant.now().plus(TOKEN_TTL)));
        cleanupExpiredEntries();
    }

    private Optional<String> readFromFallback(String sessionId) {
        cleanupExpiredEntries();
        TokenRecord record = fallbackStore.get(sessionId);
        if (record == null || record.isExpired()) {
            fallbackStore.remove(sessionId);
            return Optional.empty();
        }
        return Optional.of(record.token);
    }

    private Optional<Long> getFallbackTtl(String sessionId) {
        cleanupExpiredEntries();
        TokenRecord record = fallbackStore.get(sessionId);
        if (record == null || record.isExpired()) {
            fallbackStore.remove(sessionId);
            return Optional.empty();
        }
        long remaining = record.remainingSeconds();
        if (remaining <= 0) {
            fallbackStore.remove(sessionId);
            return Optional.empty();
        }
        return Optional.of(remaining);
    }

    private void cleanupExpiredEntries() {
        fallbackStore.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private static final class TokenRecord {
        private final String token;
        private final Instant expiresAt;

        private TokenRecord(String token, Instant expiresAt) {
            this.token = token;
            this.expiresAt = expiresAt;
        }

        private boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }

        private long remainingSeconds() {
            return Duration.between(Instant.now(), expiresAt).getSeconds();
        }
    }
}
