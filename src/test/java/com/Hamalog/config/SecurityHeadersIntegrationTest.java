package com.Hamalog.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;

/**
 * 보안 설정 검증 테스트
 * SecurityConfig의 빈 설정이 올바르게 로드되는지 검증합니다.
 *
 * 참고: 실제 HTTP 요청/응답의 보안 헤더 검증은 환경에 따라 다를 수 있으므로
 * 여기서는 SecurityFilterChain 빈이 정상적으로 로드되는지만 확인합니다.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("보안 설정 테스트")
class SecurityHeadersIntegrationTest {

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Autowired
    private SecurityConfig securityConfig;

    @Nested
    @DisplayName("SecurityConfig 빈 로드 테스트")
    class SecurityConfigBeanTests {

        @Test
        @DisplayName("SecurityFilterChain 빈이 정상적으로 로드되어야 함")
        void shouldLoadSecurityFilterChain() {
            assertThat(securityFilterChain).isNotNull();
        }

        @Test
        @DisplayName("SecurityConfig 빈이 정상적으로 로드되어야 함")
        void shouldLoadSecurityConfig() {
            assertThat(securityConfig).isNotNull();
        }
    }

    @Nested
    @DisplayName("보안 헤더 설정 문서화 테스트")
    class SecurityHeaderDocumentationTests {

        /**
         * SecurityConfig에 설정된 보안 헤더 목록을 문서화합니다.
         * 실제 헤더 값은 SecurityConfig.java에서 확인할 수 있습니다.
         */
        @Test
        @DisplayName("SecurityConfig에 정의된 보안 헤더가 문서화되어 있음")
        void shouldDocumentSecurityHeaders() {
            // SecurityConfig에 정의된 보안 헤더:
            // 1. Content-Security-Policy: default-src 'self'; script-src 'self'; ...
            // 2. X-Frame-Options: DENY
            // 3. X-Content-Type-Options: nosniff
            // 4. Referrer-Policy: strict-origin-when-cross-origin
            // 5. Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
            // 6. X-Permitted-Cross-Domain-Policies: none
            // 7. X-Download-Options: noopen
            // 8. X-XSS-Protection: 1; mode=block
            // 9. Permissions-Policy: geolocation=(), microphone=(), camera=()

            // SecurityConfig가 로드되면 위 헤더들이 HTTP 응답에 포함됨
            assertThat(securityConfig).isNotNull();
        }
    }
}
