package com.Hamalog.security.jwt;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

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
    
    private final Map<String, Long> blacklistedTokensWithExpiry = new ConcurrentHashMap<>();
    
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
                long expiryTime = calculateTokenExpiryTime(token);
                blacklistedTokensWithExpiry.put(token, expiryTime);
                log.debug("Token blacklisted in memory storage with expiry: {}", expiryTime);
            }
        } catch (Exception e) {
            log.error("Failed to blacklist token in Redis, falling back to memory storage", e);
            long expiryTime = calculateTokenExpiryTime(token);
            blacklistedTokensWithExpiry.put(token, expiryTime);
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
            
            Long expiryTime = blacklistedTokensWithExpiry.get(token);
            if (expiryTime != null) {
                if (System.currentTimeMillis() < expiryTime) {
                    return true;
                } else {
                    blacklistedTokensWithExpiry.remove(token);
                }
            }
            return false;
            
        } catch (Exception e) {
            log.error("Error checking token blacklist status, checking memory storage", e);
            Long expiryTime = blacklistedTokensWithExpiry.get(token);
            if (expiryTime != null) {
                if (System.currentTimeMillis() < expiryTime) {
                    return true;
                } else {
                    blacklistedTokensWithExpiry.remove(token);
                }
            }
            return false;
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
            
            blacklistedTokensWithExpiry.remove(token);
            log.debug("Expired token removed from blacklist");
            
        } catch (Exception e) {
            log.error("Error removing expired token from blacklist", e);
            blacklistedTokensWithExpiry.remove(token);
        }
    }
    
    public int getBlacklistSize() {
        cleanupExpiredTokens();
        int memorySize = blacklistedTokensWithExpiry.size();
        
        try {
            if (redisTemplate != null) {
                int redisSize = 0;
                ScanOptions scanOptions = ScanOptions.scanOptions()
                    .match(BLACKLIST_PREFIX + "*")
                    .count(100)
                    .build();
                
                try (var cursor = redisTemplate.scan(scanOptions)) {
                    while (cursor.hasNext()) {
                        cursor.next();
                        redisSize++;
                    }
                }
                
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
    
    /**
     * Calculate token expiry time in milliseconds for in-memory storage
     */
    private long calculateTokenExpiryTime(String token) {
        try {
            if (jwtTokenProvider != null) {
                var claims = jwtTokenProvider.getAllClaims(token);
                if (claims != null && claims.getExpiration() != null) {
                    return claims.getExpiration().getTime();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to calculate token expiry time", e);
        }
        return System.currentTimeMillis() + TimeUnit.HOURS.toMillis(24);
    }
    
    /**
     * Clean up expired tokens from in-memory storage
     */
    private void cleanupExpiredTokens() {
        long currentTime = System.currentTimeMillis();
        int removedCount = 0;
        
        var iterator = blacklistedTokensWithExpiry.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue() < currentTime) {
                iterator.remove();
                removedCount++;
            }
        }
        
        if (removedCount > 0) {
            log.debug("Cleaned up {} expired tokens from memory storage", removedCount);
        }
    }
    
    /**
     * Scheduled cleanup of expired tokens (runs every 30 minutes)
     */
    @Scheduled(fixedRate = 30 * 60 * 1000)
    public void scheduledCleanup() {
        log.debug("Starting scheduled cleanup of expired tokens");
        cleanupExpiredTokens();
    }
    
    public void clearBlacklist() {
        try {
            if (redisTemplate != null) {
                ScanOptions scanOptions = ScanOptions.scanOptions()
                    .match(BLACKLIST_PREFIX + "*")
                    .count(1000)
                    .build();
                
                int deletedCount = 0;
                try (var cursor = redisTemplate.scan(scanOptions)) {
                    while (cursor.hasNext()) {
                        String key = cursor.next();
                        redisTemplate.delete(key);
                        deletedCount++;
                    }
                }
                
                log.debug("Cleared {} tokens from Redis blacklist", deletedCount);
            }
        } catch (Exception e) {
            log.error("Error clearing Redis blacklist", e);
        }
        
        int memorySize = blacklistedTokensWithExpiry.size();
        blacklistedTokensWithExpiry.clear();
        log.debug("Cleared {} tokens from memory blacklist", memorySize);
    }
}