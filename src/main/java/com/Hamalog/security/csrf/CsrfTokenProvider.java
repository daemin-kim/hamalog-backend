package com.Hamalog.security.csrf;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SPA를 위한 CSRF 토큰 제공자
 * 토큰 생성, 검증 및 만료 관리
 */
@Component
public class CsrfTokenProvider {

    private static final int TOKEN_LENGTH = 32;
    private static final int TOKEN_EXPIRY_MINUTES = 60;
    private final SecureRandom secureRandom = new SecureRandom();
    private final ConcurrentHashMap<String, CsrfToken> tokenStorage = new ConcurrentHashMap<>();

    /**
     * 새로운 CSRF 토큰 생성
     * @param sessionId 세션 ID 또는 사용자 식별자
     * @return CSRF 토큰
     */
    public String generateToken(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            throw new IllegalArgumentException("Session ID는 필수입니다");
        }

        // 이전 토큰 무효화
        invalidateToken(sessionId);

        // 새 토큰 생성
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        // 토큰 저장
        CsrfToken csrfToken = new CsrfToken(token, sessionId, LocalDateTime.now().plus(TOKEN_EXPIRY_MINUTES, ChronoUnit.MINUTES));
        tokenStorage.put(sessionId, csrfToken);

        // 만료된 토큰 정리
        cleanExpiredTokens();

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

        CsrfToken csrfToken = tokenStorage.get(sessionId);
        if (csrfToken == null) {
            return false;
        }

        // 만료 확인
        if (LocalDateTime.now().isAfter(csrfToken.getExpiryTime())) {
            tokenStorage.remove(sessionId);
            return false;
        }

        // 토큰 일치 확인 (상수 시간 비교)
        return constantTimeEquals(csrfToken.getToken(), token);
    }

    /**
     * 특정 세션의 토큰 무효화
     * @param sessionId 세션 ID
     */
    public void invalidateToken(String sessionId) {
        if (StringUtils.hasText(sessionId)) {
            tokenStorage.remove(sessionId);
        }
    }

    /**
     * 만료된 토큰들 정리
     */
    private void cleanExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        tokenStorage.entrySet().removeIf(entry -> now.isAfter(entry.getValue().getExpiryTime()));
    }

    /**
     * 상수 시간 문자열 비교 (타이밍 공격 방지)
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        
        if (a.length() != b.length()) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    /**
     * CSRF 토큰 정보 클래스
     */
    private static class CsrfToken {
        private final String token;
        private final String sessionId;
        private final LocalDateTime expiryTime;

        public CsrfToken(String token, String sessionId, LocalDateTime expiryTime) {
            this.token = token;
            this.sessionId = sessionId;
            this.expiryTime = expiryTime;
        }

        public String getToken() {
            return token;
        }

        public String getSessionId() {
            return sessionId;
        }

        public LocalDateTime getExpiryTime() {
            return expiryTime;
        }
    }
}