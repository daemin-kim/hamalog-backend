package com.Hamalog.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.Hamalog.security.CustomUserDetailsService;
import com.Hamalog.security.filter.BotProtectionFilter;
import com.Hamalog.security.filter.CsrfValidationFilter;
import com.Hamalog.security.filter.RateLimitingFilter;
import com.Hamalog.security.filter.RequestSizeMonitoringFilter;
import com.Hamalog.security.filter.TrustedProxyService;
import com.Hamalog.security.jwt.JwtTokenProvider;
import com.Hamalog.security.oauth2.OAuth2AuthenticationSuccessHandler;
import com.Hamalog.service.oauth2.KakaoOAuth2UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityConfig Tests")
class SecurityConfigTest {

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private KakaoOAuth2UserService kakaoOAuth2UserService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @Mock
    private RateLimitingFilter rateLimitingFilter;

    @Mock
    private RequestSizeMonitoringFilter requestSizeMonitoringFilter;

    @Mock
    private CsrfValidationFilter csrfValidationFilter;

    @Mock
    private BotProtectionFilter botProtectionFilter;

    @Mock
    private TrustedProxyService trustedProxyService;

    @Mock
    private AuthenticationConfiguration authenticationConfiguration;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private org.springframework.core.env.Environment environment;

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig(
                customUserDetailsService,
                kakaoOAuth2UserService,
                jwtTokenProvider,
                oAuth2AuthenticationSuccessHandler,
                rateLimitingFilter,
                requestSizeMonitoringFilter,
                csrfValidationFilter,
                botProtectionFilter,
                trustedProxyService,
                environment
        );
    }

    @Test
    @DisplayName("Should create SecurityConfig with all dependencies")
    void constructor_WithAllDependencies_CreatesSuccessfully() {
        // then
        assertThat(securityConfig).isNotNull();
    }

    @Test
    @DisplayName("Should create SecurityConfig with null rateLimitingFilter")
    void constructor_WithNullRateLimitingFilter_CreatesSuccessfully() {
        // when
        SecurityConfig configWithNullFilter = new SecurityConfig(
                customUserDetailsService,
                kakaoOAuth2UserService,
                jwtTokenProvider,
                oAuth2AuthenticationSuccessHandler,
                null, // rateLimitingFilter can be null
                requestSizeMonitoringFilter,
                csrfValidationFilter,
                botProtectionFilter,
                trustedProxyService,
                environment
        );

        // then
        assertThat(configWithNullFilter).isNotNull();
    }

    @Test
    @DisplayName("Should create BCryptPasswordEncoder bean")
    void passwordEncoder_Creation_CreatesBCryptPasswordEncoder() {
        // when
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();

        // then
        assertThat(passwordEncoder).isNotNull();
        assertThat(passwordEncoder).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    @DisplayName("Should create different BCryptPasswordEncoder instances on multiple calls")
    void passwordEncoder_MultipleCalls_CreatesDifferentInstances() {
        // when
        PasswordEncoder passwordEncoder1 = securityConfig.passwordEncoder();
        PasswordEncoder passwordEncoder2 = securityConfig.passwordEncoder();

        // then
        assertThat(passwordEncoder1).isNotNull();
        assertThat(passwordEncoder2).isNotNull();
        assertThat(passwordEncoder1).isNotSameAs(passwordEncoder2);
        assertThat(passwordEncoder1.getClass()).isEqualTo(passwordEncoder2.getClass());
    }

    @Test
    @DisplayName("Should create functional BCryptPasswordEncoder")
    void passwordEncoder_Functionality_WorksCorrectly() {
        // when
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();

        // then
        String plainPassword = "testPassword123";
        String encodedPassword = passwordEncoder.encode(plainPassword);

        assertThat(encodedPassword).isNotNull();
        assertThat(encodedPassword).isNotEqualTo(plainPassword);
        assertThat(passwordEncoder.matches(plainPassword, encodedPassword)).isTrue();
        assertThat(passwordEncoder.matches("wrongPassword", encodedPassword)).isFalse();
    }

    @Test
    @DisplayName("Should create AuthenticationManager bean")
    void authenticationManager_Creation_CreatesAuthenticationManager() throws Exception {
        // given
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(authenticationManager);

        // when
        AuthenticationManager result = securityConfig.authenticationManager(authenticationConfiguration);

        // then
        assertThat(result).isNotNull();
        assertThat(result).isSameAs(authenticationManager);
    }

    @Test
    @DisplayName("Should create CorsConfigurationSource with default origins")
    void corsConfigurationSource_DefaultOrigins_CreatesCorrectConfiguration() {
        // given
        String defaultOrigins = "http://localhost:3000";

        // when
        CorsConfigurationSource corsConfigurationSource = securityConfig.corsConfigurationSource(defaultOrigins);

        // then
        assertThat(corsConfigurationSource).isNotNull();
        assertThat(corsConfigurationSource).isInstanceOf(UrlBasedCorsConfigurationSource.class);

        UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) corsConfigurationSource;
        CorsConfiguration config = source.getCorsConfigurations().get("/**");
        
        assertThat(config).isNotNull();
        assertThat(config.getAllowedOriginPatterns()).contains("http://localhost:3000");
        assertThat(config.getAllowCredentials()).isTrue();
    }

    @Test
    @DisplayName("Should create CorsConfigurationSource with multiple origins")
    void corsConfigurationSource_MultipleOrigins_CreatesCorrectConfiguration() {
        // given
        String multipleOrigins = "http://localhost:3000,https://api.hamalog.com,https://hamalog.com";

        // when
        CorsConfigurationSource corsConfigurationSource = securityConfig.corsConfigurationSource(multipleOrigins);

        // then
        assertThat(corsConfigurationSource).isNotNull();

        UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) corsConfigurationSource;
        CorsConfiguration config = source.getCorsConfigurations().get("/**");

        assertThat(config).isNotNull();
        assertThat(config.getAllowedOriginPatterns()).contains(
                "http://localhost:3000",
                "https://api.hamalog.com",
                "https://hamalog.com"
        );
    }

    @Test
    @DisplayName("Should handle empty origins string in CORS configuration")
    void corsConfigurationSource_EmptyOrigins_UsesDefault() {
        // given
        String emptyOrigins = "";

        // when
        CorsConfigurationSource corsConfigurationSource = securityConfig.corsConfigurationSource(emptyOrigins);

        // then
        assertThat(corsConfigurationSource).isNotNull();

        UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) corsConfigurationSource;
        CorsConfiguration config = source.getCorsConfigurations().get("/**");

        assertThat(config).isNotNull();
        // 기본값 localhost:3000이 설정됨
        assertThat(config.getAllowedOriginPatterns()).containsExactly("http://localhost:3000");
    }

    @Test
    @DisplayName("Should handle null origins string in CORS configuration")
    void corsConfigurationSource_NullOrigins_UsesDefault() {
        // when
        CorsConfigurationSource corsConfigurationSource = securityConfig.corsConfigurationSource(null);

        // then
        assertThat(corsConfigurationSource).isNotNull();

        UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) corsConfigurationSource;
        CorsConfiguration config = source.getCorsConfigurations().get("/**");

        assertThat(config).isNotNull();
        // 기본값 localhost:3000이 설정됨
        assertThat(config.getAllowedOriginPatterns()).containsExactly("http://localhost:3000");
    }

    @Test
    @DisplayName("Should configure CORS with correct headers and methods")
    void corsConfigurationSource_HeadersAndMethods_ConfiguresCorrectly() {
        // given
        String origins = "http://localhost:3000";

        // when
        CorsConfigurationSource corsConfigurationSource = securityConfig.corsConfigurationSource(origins);

        // then
        UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) corsConfigurationSource;
        CorsConfiguration config = source.getCorsConfigurations().get("/**");

        assertThat(config).isNotNull();
        
        // Check allowed headers
        assertThat(config.getAllowedHeaders()).contains(
                "Authorization",
                "Content-Type",
                "Accept"
        );

        // Check allowed methods
        assertThat(config.getAllowedMethods()).contains(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        );

        // Check other configuration
        assertThat(config.getAllowCredentials()).isTrue();
        assertThat(config.getMaxAge()).isEqualTo(3600L);
    }

    @Test
    @DisplayName("Should handle origins with whitespace in CORS configuration")
    void corsConfigurationSource_OriginsWithWhitespace_HandlesCorrectly() {
        // given
        String originsWithWhitespace = " http://localhost:3000 , https://api.hamalog.com , ";

        // when
        CorsConfigurationSource corsConfigurationSource = securityConfig.corsConfigurationSource(originsWithWhitespace);

        // then
        UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) corsConfigurationSource;
        CorsConfiguration config = source.getCorsConfigurations().get("/**");

        assertThat(config).isNotNull();
        assertThat(config.getAllowedOriginPatterns()).contains(
                "http://localhost:3000",
                "https://api.hamalog.com"
        );
        // Should not contain empty strings or whitespace-only strings
        assertThat(config.getAllowedOriginPatterns()).doesNotContain("", " ");
    }

    @Test
    @DisplayName("Should configure CORS source to register for all paths")
    void corsConfigurationSource_PathMapping_RegistersForAllPaths() {
        // given
        String origins = "http://localhost:3000";

        // when
        CorsConfigurationSource corsConfigurationSource = securityConfig.corsConfigurationSource(origins);

        // then
        UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) corsConfigurationSource;
        
        // Should have configuration for /** (all paths)
        assertThat(source.getCorsConfigurations()).containsKey("/**");
        assertThat(source.getCorsConfigurations().get("/**")).isNotNull();
    }

    @Test
    @DisplayName("Should create CORS configuration with proper security settings")
    void corsConfigurationSource_SecuritySettings_ConfiguresSecurely() {
        // given
        String origins = "https://secure.hamalog.com";

        // when
        CorsConfigurationSource corsConfigurationSource = securityConfig.corsConfigurationSource(origins);

        // then
        UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) corsConfigurationSource;
        CorsConfiguration config = source.getCorsConfigurations().get("/**");

        assertThat(config).isNotNull();
        
        // Should allow credentials for authenticated requests
        assertThat(config.getAllowCredentials()).isTrue();
        
        // Should have reasonable max age (1 hour)
        assertThat(config.getMaxAge()).isEqualTo(3600L);
        
        // Should support all necessary HTTP methods including OPTIONS for preflight
        assertThat(config.getAllowedMethods()).contains("OPTIONS");
    }

    @Test
    @DisplayName("Should maintain consistent CORS configuration across multiple calls")
    void corsConfigurationSource_MultipleCalls_MaintainsConsistency() {
        // given
        String origins = "http://localhost:3000";

        // when
        CorsConfigurationSource corsSource1 = securityConfig.corsConfigurationSource(origins);
        CorsConfigurationSource corsSource2 = securityConfig.corsConfigurationSource(origins);

        // then
        assertThat(corsSource1).isNotSameAs(corsSource2); // Different instances
        
        // But same configuration
        UrlBasedCorsConfigurationSource source1 = (UrlBasedCorsConfigurationSource) corsSource1;
        UrlBasedCorsConfigurationSource source2 = (UrlBasedCorsConfigurationSource) corsSource2;
        
        CorsConfiguration config1 = source1.getCorsConfigurations().get("/**");
        CorsConfiguration config2 = source2.getCorsConfigurations().get("/**");
        
        assertThat(config1.getAllowedOriginPatterns()).isEqualTo(config2.getAllowedOriginPatterns());
        assertThat(config1.getAllowedMethods()).isEqualTo(config2.getAllowedMethods());
        assertThat(config1.getAllowedHeaders()).isEqualTo(config2.getAllowedHeaders());
        assertThat(config1.getAllowCredentials()).isEqualTo(config2.getAllowCredentials());
        assertThat(config1.getMaxAge()).isEqualTo(config2.getMaxAge());
    }
}