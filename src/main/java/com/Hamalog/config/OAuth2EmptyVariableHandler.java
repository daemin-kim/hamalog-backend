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
 * Handles empty OAuth2 environment variables by providing proper fallback values
 * before Spring Boot's OAuth2 auto-configuration validation occurs.
 * 
 * This prevents the "Client id of registration 'kakao' must not be empty" error
 * when environment variables are set to empty strings in production.
 */
@Component
public class OAuth2EmptyVariableHandler implements EnvironmentPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(OAuth2EmptyVariableHandler.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> customProperties = new HashMap<>();

        String kakaoClientId = environment.getProperty("KAKAO_CLIENT_ID");
        if (kakaoClientId == null || kakaoClientId.trim().isEmpty()) {
            String[] activeProfiles = environment.getActiveProfiles();
            boolean isProduction = java.util.Arrays.asList(activeProfiles).contains("prod");
            
            String fallbackClientId = isProduction ? 
                "dummy-client-id-for-production" : 
                "dummy-client-id-for-development";
            
            customProperties.put("hamalog.oauth2.kakao.client-id", fallbackClientId);
            log.info("[DEBUG_LOG] KAKAO_CLIENT_ID is null/empty, using fallback: {}", fallbackClientId);
        } else {
            customProperties.put("hamalog.oauth2.kakao.client-id", kakaoClientId);
            log.info("[DEBUG_LOG] KAKAO_CLIENT_ID found: {}", kakaoClientId);
        }

        String kakaoClientSecret = environment.getProperty("KAKAO_CLIENT_SECRET");
        if (kakaoClientSecret == null || kakaoClientSecret.trim().isEmpty()) {
            String[] activeProfiles = environment.getActiveProfiles();
            boolean isProduction = java.util.Arrays.asList(activeProfiles).contains("prod");
            
            String fallbackClientSecret = isProduction ? 
                "dummy-client-secret-for-production" : 
                "dummy-client-secret-for-development";
            
            customProperties.put("hamalog.oauth2.kakao.client-secret", fallbackClientSecret);
            log.info("[DEBUG_LOG] KAKAO_CLIENT_SECRET is null/empty, using fallback: ***MASKED***");
        } else {
            customProperties.put("hamalog.oauth2.kakao.client-secret", kakaoClientSecret);
            log.info("[DEBUG_LOG] KAKAO_CLIENT_SECRET found: ***MASKED***");
        }

        String kakaoRedirectUri = environment.getProperty("KAKAO_REDIRECT_URI");
        if (kakaoRedirectUri == null || kakaoRedirectUri.trim().isEmpty()) {
            String[] activeProfiles = environment.getActiveProfiles();
            boolean isProduction = java.util.Arrays.asList(activeProfiles).contains("prod");
            
            String fallbackRedirectUri = isProduction ? 
                "http://112.72.248.195:8080/api/auth/kakao/callback" : 
                "http://localhost:8080/api/auth/kakao/callback";
            
            customProperties.put("hamalog.oauth2.kakao.redirect-uri", fallbackRedirectUri);
            log.info("[DEBUG_LOG] KAKAO_REDIRECT_URI is null/empty, using fallback: {}", fallbackRedirectUri);
        } else {
            customProperties.put("hamalog.oauth2.kakao.redirect-uri", kakaoRedirectUri);
            log.info("[DEBUG_LOG] KAKAO_REDIRECT_URI found: {}", kakaoRedirectUri);
        }

        // Handle FRONTEND_URL for OAuth2 success handler redirect
        String frontendUrl = environment.getProperty("FRONTEND_URL");
        if (frontendUrl == null || frontendUrl.trim().isEmpty()) {
            String[] activeProfiles = environment.getActiveProfiles();
            boolean isProduction = java.util.Arrays.asList(activeProfiles).contains("prod");
            
            String fallbackFrontendUrl = isProduction ? 
                "http://112.72.248.195:3000" : 
                "http://localhost:3000";
            
            customProperties.put("hamalog.oauth2.redirect-uri", fallbackFrontendUrl + "/oauth/kakao");
            log.info("[DEBUG_LOG] FRONTEND_URL is null/empty, using fallback: {}/oauth/kakao", fallbackFrontendUrl);
        } else {
            customProperties.put("hamalog.oauth2.redirect-uri", frontendUrl + "/oauth/kakao");
            log.info("[DEBUG_LOG] FRONTEND_URL found, setting OAuth2 redirect: {}/oauth/kakao", frontendUrl);
        }

        if (!customProperties.isEmpty()) {
            MapPropertySource propertySource = new MapPropertySource("oauth2EmptyVariableHandler", customProperties);
            environment.getPropertySources().addFirst(propertySource);
            log.info("[DEBUG_LOG] OAuth2EmptyVariableHandler added {} custom properties", customProperties.size());
        }
    }
}