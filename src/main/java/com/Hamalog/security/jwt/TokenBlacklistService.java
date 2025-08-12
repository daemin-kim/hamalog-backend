package com.Hamalog.security.jwt;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {
    
    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();
    
    /**
     * Add a token to the blacklist
     * @param token The JWT token to blacklist
     */
    public void blacklistToken(String token) {
        blacklistedTokens.add(token);
    }
    
    /**
     * Check if a token is blacklisted
     * @param token The JWT token to check
     * @return true if the token is blacklisted, false otherwise
     */
    public boolean isTokenBlacklisted(String token) {
        return blacklistedTokens.contains(token);
    }
    
    /**
     * Remove expired tokens from blacklist to prevent memory leaks
     * This method should be called periodically by a scheduled task
     * @param token The expired token to remove
     */
    public void removeExpiredToken(String token) {
        blacklistedTokens.remove(token);
    }
    
    /**
     * Get the current size of the blacklist (for monitoring purposes)
     * @return The number of blacklisted tokens
     */
    public int getBlacklistSize() {
        return blacklistedTokens.size();
    }
}