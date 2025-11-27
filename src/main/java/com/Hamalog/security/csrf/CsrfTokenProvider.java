package com.Hamalog.security.csrf;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * SPA를 위한 CSRF 토큰 제공자
 * 토큰 생성, 검증 및 만료 관리
 */
@Component
@ConditionalOnProperty(name = "spring.data.redis.host")
public class CsrfTokenProvider {

    private static final int TOKEN_LENGTH = 32;
    private static final Duration TOKEN_TTL = Duration.ofMinutes(60);
    private static final String REDIS_KEY_PREFIX = "csrf:";

    private final SecureRandom secureRandom = new SecureRandom();
    private final RedisTemplate<String, Object> redisTemplate;

    public CsrfTokenProvider(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 새로운 CSRF 토큰 생성
     * @param sessionId 세션 ID 또는 사용자 식별자
     * @return CSRF 토큰
     */
    public String generateToken(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            throw new IllegalArgumentException("Session ID는 필수입니다");
        }

        String token = createRandomToken();
        redisTemplate.opsForValue().set(redisKey(sessionId), token, TOKEN_TTL);
        return token;
    }

    /**
     * CSRF 토큰 검증
     * @param sessionId 세션 ID
     * @param token 검증할 토큰
     * @return 유효성 여부
     */
    public boolean validateToken(String sessionId, String token) {
        if (!StringUtils.hasText(sessionId) || !StringUtils.hasText(token)) {
            return false;
        }

        Object storedValue = redisTemplate.opsForValue().get(redisKey(sessionId));
        if (!(storedValue instanceof String storedToken)) {
            return false;
        }

        return constantTimeEquals(storedToken, token);
    }

    /**
     * 특정 세션의 토큰 무효화
     * @param sessionId 세션 ID
     */
    public void invalidateToken(String sessionId) {
        if (StringUtils.hasText(sessionId)) {
            redisTemplate.delete(redisKey(sessionId));
        }
    }

    /**
     * 남은 TTL 조회
     * @param sessionId 세션 ID
     * @return 남은 TTL (초 단위), 존재하지 않거나 유효하지 않은 경우 비어있는 Optional
     */
    public Optional<Long> getRemainingTtl(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return Optional.empty();
        }

        Long ttl = redisTemplate.getExpire(redisKey(sessionId));
        return Optional.ofNullable(ttl).filter(value -> value > 0);
    }

    /**
     * 랜덤 CSRF 토큰 생성
     * @return CSRF 토큰
     */
    private String createRandomToken() {
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    /**
     * 상수 시간 문자열 비교 (타이밍 공격 방지)
     */
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

    /**
     * Redis 키 생성
     * @param sessionId 세션 ID
     * @return Redis 키
     */
    private String redisKey(String sessionId) {
        return REDIS_KEY_PREFIX + sessionId;
    }
}