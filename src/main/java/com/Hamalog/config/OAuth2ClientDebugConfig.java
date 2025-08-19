package com.Hamalog.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class OAuth2ClientDebugConfig {

    private static final Logger log = LoggerFactory.getLogger(OAuth2ClientDebugConfig.class);

    private final Environment environment;
    private final OAuth2ClientProperties oAuth2ClientProperties;

    public OAuth2ClientDebugConfig(Environment environment, OAuth2ClientProperties oAuth2ClientProperties) {
        this.environment = environment;
        this.oAuth2ClientProperties = oAuth2ClientProperties;
        
        // Log during bean creation
        log.info("[DEBUG_LOG] OAuth2ClientDebugConfig 생성자 호출됨");
        logOAuth2Configuration();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logConfigurationOnStartup() {
        log.info("[DEBUG_LOG] ApplicationReadyEvent - OAuth2 설정 재검증");
        logOAuth2Configuration();
    }

    private void logOAuth2Configuration() {
        log.info("[DEBUG_LOG] ===========================================");
        log.info("[DEBUG_LOG] OAuth2 Client 설정 디버깅 시작");
        log.info("[DEBUG_LOG] ===========================================");
        
        // Environment variables 직접 확인
        String kakaoClientId = environment.getProperty("KAKAO_CLIENT_ID");
        String kakaoClientSecret = environment.getProperty("KAKAO_CLIENT_SECRET");
        String kakaoRedirectUri = environment.getProperty("KAKAO_REDIRECT_URI");
        
        log.info("[DEBUG_LOG] 환경변수 직접 조회:");
        log.info("[DEBUG_LOG] KAKAO_CLIENT_ID 환경변수: '{}'", kakaoClientId != null ? kakaoClientId : "NULL");
        log.info("[DEBUG_LOG] KAKAO_CLIENT_SECRET 환경변수: '{}'", kakaoClientSecret != null ? kakaoClientSecret : "NULL");
        log.info("[DEBUG_LOG] KAKAO_REDIRECT_URI 환경변수: '{}'", kakaoRedirectUri != null ? kakaoRedirectUri : "NULL");
        
        // Property 값 확인
        String propClientId = environment.getProperty("spring.security.oauth2.client.registration.kakao.client-id");
        String propClientSecret = environment.getProperty("spring.security.oauth2.client.registration.kakao.client-secret");
        String propRedirectUri = environment.getProperty("spring.security.oauth2.client.registration.kakao.redirect-uri");
        
        log.info("[DEBUG_LOG] 프로퍼티 값 조회:");
        log.info("[DEBUG_LOG] spring.security.oauth2.client.registration.kakao.client-id: '{}'", propClientId != null ? propClientId : "NULL");
        log.info("[DEBUG_LOG] spring.security.oauth2.client.registration.kakao.client-secret: '{}'", propClientSecret != null ? propClientSecret : "NULL");
        log.info("[DEBUG_LOG] spring.security.oauth2.client.registration.kakao.redirect-uri: '{}'", propRedirectUri != null ? propRedirectUri : "NULL");
        
        // OAuth2ClientProperties 객체 확인
        log.info("[DEBUG_LOG] OAuth2ClientProperties 객체 확인:");
        if (oAuth2ClientProperties != null) {
            log.info("[DEBUG_LOG] OAuth2ClientProperties 객체 존재함");
            if (oAuth2ClientProperties.getRegistration() != null) {
                log.info("[DEBUG_LOG] Registration 맵 크기: {}", oAuth2ClientProperties.getRegistration().size());
                if (oAuth2ClientProperties.getRegistration().containsKey("kakao")) {
                    var kakaoReg = oAuth2ClientProperties.getRegistration().get("kakao");
                    log.info("[DEBUG_LOG] Kakao registration 존재함");
                    log.info("[DEBUG_LOG] Kakao client-id: '{}'", kakaoReg.getClientId() != null ? kakaoReg.getClientId() : "NULL");
                    log.info("[DEBUG_LOG] Kakao client-secret: '{}'", kakaoReg.getClientSecret() != null ? "***MASKED***" : "NULL");
                    log.info("[DEBUG_LOG] Kakao redirect-uri: '{}'", kakaoReg.getRedirectUri() != null ? kakaoReg.getRedirectUri() : "NULL");
                } else {
                    log.warn("[DEBUG_LOG] ❌ Kakao registration이 OAuth2ClientProperties에 없음");
                }
            } else {
                log.warn("[DEBUG_LOG] ❌ OAuth2ClientProperties.registration이 null임");
            }
        } else {
            log.error("[DEBUG_LOG] ❌ OAuth2ClientProperties 객체가 null임");
        }
        
        // Active profiles 확인
        String[] activeProfiles = environment.getActiveProfiles();
        log.info("[DEBUG_LOG] Active Profiles: {}", java.util.Arrays.toString(activeProfiles));
        
        log.info("[DEBUG_LOG] ===========================================");
        log.info("[DEBUG_LOG] OAuth2 Client 설정 디버깅 완료");
        log.info("[DEBUG_LOG] ===========================================");
    }
}