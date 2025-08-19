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
    
    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();
    
    private static final String BLACKLIST_PREFIX = "jwt_blacklist:";
    
    public void blacklistToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("Attempted to blacklist null or empty token");
            return;
        }
        
        try {
            if (redisTemplate != null) {
                String redisKey = BLACKLIST_PREFIX + token;
                long ttlSeconds = getTokenTtlSeconds(token);
                
                if (ttlSeconds > 0) {
                    redisTemplate.opsForValue().set(redisKey, "blacklisted", ttlSeconds, TimeUnit.SECONDS);
                    log.debug("Token blacklisted in Redis with TTL: {} seconds", ttlSeconds);
                } else {
                    redisTemplate.opsForValue().set(redisKey, "blacklisted", 24, TimeUnit.HOURS);
                    log.debug("Token blacklisted in Redis with default TTL: 24 hours");
                }
            } else {
                blacklistedTokens.add(token);
                log.debug("Token blacklisted in memory storage");
            }
        } catch (Exception e) {
            log.error("Failed to blacklist token in Redis, falling back to memory storage", e);
            blacklistedTokens.add(token);
        }
    }
    
    public boolean isTokenBlacklisted(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        
        try {
            if (redisTemplate != null) {
                String redisKey = BLACKLIST_PREFIX + token;
                Boolean exists = redisTemplate.hasKey(redisKey);
                if (Boolean.TRUE.equals(exists)) {
                    return true;
                }
            }
            
            return blacklistedTokens.contains(token);
            
        } catch (Exception e) {
            log.error("Error checking token blacklist status, checking memory storage", e);
            return blacklistedTokens.contains(token);
        }
    }
    
    public void removeExpiredToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return;
        }
        
        try {
            if (redisTemplate != null) {
                String redisKey = BLACKLIST_PREFIX + token;
                redisTemplate.delete(redisKey);
            }
            
            blacklistedTokens.remove(token);
            log.debug("Expired token removed from blacklist");
            
        } catch (Exception e) {
            log.error("Error removing expired token from blacklist", e);
            blacklistedTokens.remove(token);
        }
    }
    
    public int getBlacklistSize() {
        int memorySize = blacklistedTokens.size();
        
        try {
            if (redisTemplate != null) {
                Set<String> keys = redisTemplate.keys(BLACKLIST_PREFIX + "*");
                int redisSize = keys != null ? keys.size() : 0;
                log.debug("Blacklist size - Redis: {}, Memory: {}", redisSize, memorySize);
                return redisSize + memorySize;
            }
        } catch (Exception e) {
            log.error("Error getting Redis blacklist size", e);
        }
        
        return memorySize;
    }
    
    private long getTokenTtlSeconds(String token) {
        try {
            if (jwtTokenProvider != null) {
                var claims = jwtTokenProvider.getAllClaims(token);
                if (claims != null && claims.getExpiration() != null) {
                    long expirationTime = claims.getExpiration().getTime();
                    long currentTime = System.currentTimeMillis();
                    long ttlMillis = expirationTime - currentTime;
                    return Math.max(0, ttlMillis / 1000);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to calculate token TTL", e);
        }
        return -1;
    }
    
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