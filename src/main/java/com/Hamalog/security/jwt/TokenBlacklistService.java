package com.Hamalog.security.jwt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * JWT 토큰 블랙리스트 서비스
 * 
 * Redis를 기본으로 사용하며, Redis가 없는 경우 메모리 기반으로 동작합니다.
 * 로그아웃된 토큰을 블랙리스트에 추가하여 재사용을 방지합니다.
 */
@Slf4j
@Service
public class TokenBlacklistService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;
    
    public TokenBlacklistService(@Autowired(required = false) RedisTemplate<String, Object> redisTemplate,
                                @Lazy JwtTokenProvider jwtTokenProvider) {
        this.redisTemplate = redisTemplate;
        this.jwtTokenProvider = jwtTokenProvider;
    }
    
    // Redis 사용 불가능 시 fallback용 메모리 저장소
    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();
    
    private static final String BLACKLIST_PREFIX = "jwt_blacklist:";
    
    /**
     * 토큰을 블랙리스트에 추가합니다.
     * Redis를 우선 사용하고, 실패 시 메모리에 저장합니다.
     * 
     * @param token 블랙리스트에 추가할 JWT 토큰
     */
    public void blacklistToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("Attempted to blacklist null or empty token");
            return;
        }
        
        try {
            // Redis를 사용한 토큰 블랙리스트 (TTL 설정)
            if (redisTemplate != null) {
                String redisKey = BLACKLIST_PREFIX + token;
                long ttlSeconds = getTokenTtlSeconds(token);
                
                if (ttlSeconds > 0) {
                    redisTemplate.opsForValue().set(redisKey, "blacklisted", ttlSeconds, TimeUnit.SECONDS);
                    log.debug("Token blacklisted in Redis with TTL: {} seconds", ttlSeconds);
                } else {
                    // TTL 계산 실패 시 기본 만료 시간 설정 (24시간)
                    redisTemplate.opsForValue().set(redisKey, "blacklisted", 24, TimeUnit.HOURS);
                    log.debug("Token blacklisted in Redis with default TTL: 24 hours");
                }
            } else {
                // Redis 없을 때 메모리 저장소 사용
                blacklistedTokens.add(token);
                log.debug("Token blacklisted in memory storage");
            }
        } catch (Exception e) {
            log.error("Failed to blacklist token in Redis, falling back to memory storage", e);
            blacklistedTokens.add(token);
        }
    }
    
    /**
     * 토큰이 블랙리스트에 있는지 확인합니다.
     * Redis를 우선 확인하고, 실패 시 메모리에서 확인합니다.
     * 
     * @param token 확인할 JWT 토큰
     * @return 토큰이 블랙리스트에 있으면 true, 없으면 false
     */
    public boolean isTokenBlacklisted(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        
        try {
            // Redis에서 우선 확인
            if (redisTemplate != null) {
                String redisKey = BLACKLIST_PREFIX + token;
                Boolean exists = redisTemplate.hasKey(redisKey);
                if (Boolean.TRUE.equals(exists)) {
                    return true;
                }
            }
            
            // Redis에서 찾지 못했거나 Redis가 없는 경우 메모리에서 확인
            return blacklistedTokens.contains(token);
            
        } catch (Exception e) {
            log.error("Error checking token blacklist status, checking memory storage", e);
            return blacklistedTokens.contains(token);
        }
    }
    
    /**
     * 만료된 토큰을 블랙리스트에서 제거합니다.
     * Redis의 경우 TTL로 자동 만료되므로, 메모리 저장소에서만 제거합니다.
     * 
     * @param token 제거할 만료된 토큰
     */
    public void removeExpiredToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return;
        }
        
        try {
            // Redis는 TTL로 자동 만료되므로, 명시적 제거는 선택사항
            if (redisTemplate != null) {
                String redisKey = BLACKLIST_PREFIX + token;
                redisTemplate.delete(redisKey);
            }
            
            // 메모리 저장소에서는 수동 제거 필요
            blacklistedTokens.remove(token);
            log.debug("Expired token removed from blacklist");
            
        } catch (Exception e) {
            log.error("Error removing expired token from blacklist", e);
            // 메모리에서라도 제거
            blacklistedTokens.remove(token);
        }
    }
    
    /**
     * 현재 블랙리스트 크기를 반환합니다 (모니터링 목적).
     * Redis와 메모리 저장소의 크기를 합산합니다.
     * 
     * @return 블랙리스트된 토큰 수
     */
    public int getBlacklistSize() {
        int memorySize = blacklistedTokens.size();
        
        try {
            if (redisTemplate != null) {
                // Redis에서 blacklist prefix로 시작하는 키 개수 조회
                Set<String> keys = redisTemplate.keys(BLACKLIST_PREFIX + "*");
                int redisSize = keys != null ? keys.size() : 0;
                log.debug("Blacklist size - Redis: {}, Memory: {}", redisSize, memorySize);
                return redisSize + memorySize; // 중복 가능하지만 모니터링 목적으로는 충분
            }
        } catch (Exception e) {
            log.error("Error getting Redis blacklist size", e);
        }
        
        return memorySize;
    }
    
    /**
     * JWT 토큰의 남은 만료 시간을 초 단위로 계산합니다.
     * 
     * @param token JWT 토큰
     * @return 남은 만료 시간 (초), 계산 실패 시 -1
     */
    private long getTokenTtlSeconds(String token) {
        try {
            if (jwtTokenProvider != null) {
                // validateToken을 호출하면 블랙리스트 체크도 포함되므로, 
                // 직접 claims를 파싱해서 만료 시간만 확인
                var claims = jwtTokenProvider.getAllClaims(token);
                if (claims != null && claims.getExpiration() != null) {
                    long expirationTime = claims.getExpiration().getTime();
                    long currentTime = System.currentTimeMillis();
                    long ttlMillis = expirationTime - currentTime;
                    return Math.max(0, ttlMillis / 1000); // 초 단위로 변환
                }
            }
        } catch (Exception e) {
            log.warn("Failed to calculate token TTL", e);
        }
        return -1;
    }
    
    /**
     * 블랙리스트를 완전히 정리합니다 (테스트 목적).
     * Redis와 메모리 저장소를 모두 정리합니다.
     */
    public void clearBlacklist() {
        try {
            if (redisTemplate != null) {
                Set<String> keys = redisTemplate.keys(BLACKLIST_PREFIX + "*");
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                    log.debug("Cleared {} tokens from Redis blacklist", keys.size());
                }
            }
        } catch (Exception e) {
            log.error("Error clearing Redis blacklist", e);
        }
        
        int memorySize = blacklistedTokens.size();
        blacklistedTokens.clear();
        log.debug("Cleared {} tokens from memory blacklist", memorySize);
    }
}