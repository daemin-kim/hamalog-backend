package com.Hamalog.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles empty JWT and encryption key environment variables by providing hardcoded fallback values
 * before Spring Boot's security configuration occurs.
 * 
 * This prevents IllegalStateException in JwtTokenProvider and DataEncryptionUtil
 * when environment variables are set to empty strings in production deployment.
 * 
 * Uses pre-generated secure keys from .env.hamalog-keys as fallback values.
 */
@Component
public class SecurityKeyEmptyVariableHandler implements EnvironmentPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(SecurityKeyEmptyVariableHandler.class);
    
    // Pre-generated secure keys from .env.hamalog-keys (generated on Tue Sep 10 13:31:24 KST 2025)
    // These are used as hardcoded fallback values when environment variables are empty
    private static final String FALLBACK_JWT_SECRET = "sR7qIT7mmCnihzge+vg8NF9FVneEAtDpUwpeo2haQ8g=";
    private static final String FALLBACK_ENCRYPTION_KEY = "7u+EyAhNyWu2hebIO0XoqUH1gwdoyyHKxcIKC2+A6aM=";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> customProperties = new HashMap<>();

        // Handle JWT_SECRET
        String jwtSecret = environment.getProperty("JWT_SECRET");
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            String[] activeProfiles = environment.getActiveProfiles();
            boolean isProduction = java.util.Arrays.asList(activeProfiles).contains("prod");
            
            if (isProduction) {
                // Use hardcoded fallback key for production to prevent startup failure
                customProperties.put("jwt.secret", FALLBACK_JWT_SECRET);
                log.warn("[SECURITY_KEY_HANDLER] JWT_SECRET is null/empty in production. Using hardcoded fallback key.");
                log.warn("[SECURITY_KEY_HANDLER] For better security, set JWT_SECRET environment variable with a unique key.");
            } else {
                // Let JwtTokenProvider generate random key for development
                log.info("[SECURITY_KEY_HANDLER] JWT_SECRET is null/empty in development. JwtTokenProvider will generate random key.");
            }
        } else {
            // Use provided value
            customProperties.put("jwt.secret", jwtSecret);
            log.info("[SECURITY_KEY_HANDLER] JWT_SECRET found and will be used.");
        }

        // Handle HAMALOG_ENCRYPTION_KEY
        String encryptionKey = environment.getProperty("HAMALOG_ENCRYPTION_KEY");
        if (encryptionKey == null || encryptionKey.trim().isEmpty()) {
            String[] activeProfiles = environment.getActiveProfiles();
            boolean isProduction = java.util.Arrays.asList(activeProfiles).contains("prod");
            
            if (isProduction) {
                // Use hardcoded fallback key for production to prevent startup failure
                customProperties.put("hamalog.encryption.key", FALLBACK_ENCRYPTION_KEY);
                log.warn("[SECURITY_KEY_HANDLER] HAMALOG_ENCRYPTION_KEY is null/empty in production. Using hardcoded fallback key.");
                log.warn("[SECURITY_KEY_HANDLER] For better security, set HAMALOG_ENCRYPTION_KEY environment variable with a unique key.");
            } else {
                // Let DataEncryptionUtil generate random key for development
                log.info("[SECURITY_KEY_HANDLER] HAMALOG_ENCRYPTION_KEY is null/empty in development. DataEncryptionUtil will generate random key.");
            }
        } else {
            // Use provided value
            customProperties.put("hamalog.encryption.key", encryptionKey);
            log.info("[SECURITY_KEY_HANDLER] HAMALOG_ENCRYPTION_KEY found and will be used.");
        }

        // Add custom properties to environment if any were created
        if (!customProperties.isEmpty()) {
            MapPropertySource propertySource = new MapPropertySource("securityKeyEmptyVariableHandler", customProperties);
            environment.getPropertySources().addFirst(propertySource);
            log.info("[SECURITY_KEY_HANDLER] SecurityKeyEmptyVariableHandler added {} custom properties", customProperties.size());
            
            // Log security status for production deployments
            String[] activeProfiles = environment.getActiveProfiles();
            boolean isProduction = java.util.Arrays.asList(activeProfiles).contains("prod");
            if (isProduction) {
                log.warn("[SECURITY_KEY_HANDLER] ==================== PRODUCTION SECURITY STATUS ====================");
                log.warn("[SECURITY_KEY_HANDLER] Using hardcoded fallback keys due to empty environment variables.");
                log.warn("[SECURITY_KEY_HANDLER] This resolves deployment issues but is less secure than unique keys.");
                log.warn("[SECURITY_KEY_HANDLER] Recommendation: Set JWT_SECRET and HAMALOG_ENCRYPTION_KEY in your deployment.");
                log.warn("[SECURITY_KEY_HANDLER] ===================================================================");
            }
        }
    }
}