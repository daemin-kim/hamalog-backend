package com.Hamalog.config;

import com.Hamalog.security.CustomUserDetailsService;
import com.Hamalog.security.filter.BotProtectionFilter;
import com.Hamalog.security.filter.CsrfValidationFilter;
import com.Hamalog.security.filter.RateLimitingFilter;
import com.Hamalog.security.filter.RequestSizeMonitoringFilter;
import com.Hamalog.security.filter.TrustedProxyService;
import com.Hamalog.security.jwt.JwtAuthenticationFilter;
import com.Hamalog.security.jwt.JwtTokenProvider;
import com.Hamalog.security.oauth2.OAuth2AuthenticationSuccessHandler;
import com.Hamalog.service.oauth2.KakaoOAuth2UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final CustomUserDetailsService customUserDetailsService;
    private final KakaoOAuth2UserService kakaoOAuth2UserService;
    private final JwtTokenProvider jwtTokenProvider;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final RateLimitingFilter rateLimitingFilter;
    private final RequestSizeMonitoringFilter requestSizeMonitoringFilter;
    private final CsrfValidationFilter csrfValidationFilter;
    private final BotProtectionFilter botProtectionFilter;
    private final TrustedProxyService trustedProxyService;
    private final org.springframework.core.env.Environment environment;

    public SecurityConfig(
            CustomUserDetailsService customUserDetailsService,
            KakaoOAuth2UserService kakaoOAuth2UserService,
            JwtTokenProvider jwtTokenProvider,
            OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler,
            @Autowired(required = false) RateLimitingFilter rateLimitingFilter,
            RequestSizeMonitoringFilter requestSizeMonitoringFilter,
            @Autowired(required = false) CsrfValidationFilter csrfValidationFilter,
            BotProtectionFilter botProtectionFilter,
            TrustedProxyService trustedProxyService,
            org.springframework.core.env.Environment environment
    ) {
        this.customUserDetailsService = customUserDetailsService;
        this.kakaoOAuth2UserService = kakaoOAuth2UserService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.oAuth2AuthenticationSuccessHandler = oAuth2AuthenticationSuccessHandler;
        this.rateLimitingFilter = rateLimitingFilter;
        this.requestSizeMonitoringFilter = requestSizeMonitoringFilter;
        this.csrfValidationFilter = csrfValidationFilter;
        this.botProtectionFilter = botProtectionFilter;
        this.trustedProxyService = trustedProxyService;
        this.environment = environment;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Value("${hamalog.cors.allowed-origins:http://localhost:3000}") String allowedOriginsCsv
    ) throws Exception {
        http
                // JWT 기반 stateless 인증에서는 CSRF 불필요
                // 대신 SameSite Cookie 및 추가 보안 헤더로 보호
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource(allowedOriginsCsv)))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                            "default-src 'self'; " +
                            "script-src 'self'; " +  // unsafe-inline 제거 (XSS 방지)
                            "style-src 'self' 'unsafe-inline'; " +
                            "img-src 'self' data: https:; " +
                            "font-src 'self'; " +
                            "connect-src 'self'; " +
                            "frame-ancestors 'none'; " +  // Clickjacking 방지
                            "base-uri 'self'; " +
                            "form-action 'self'"
                        ))
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(Customizer.withDefaults())
                        .referrerPolicy(ref -> ref.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                                .maxAgeInSeconds(31536000)
                                .includeSubDomains(true)
                                .preload(true)  // HSTS Preload 추가
                        )
                        .addHeaderWriter(new org.springframework.security.web.header.writers.StaticHeadersWriter("X-Permitted-Cross-Domain-Policies", "none"))
                        .addHeaderWriter(new org.springframework.security.web.header.writers.StaticHeadersWriter("X-Download-Options", "noopen"))
                        .addHeaderWriter(new org.springframework.security.web.header.writers.StaticHeadersWriter("X-XSS-Protection", "1; mode=block"))
                        .addHeaderWriter(new org.springframework.security.web.header.writers.StaticHeadersWriter("Permissions-Policy", "geolocation=(), microphone=(), camera=()"))
                )
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    // 기본 공개 엔드포인트
                    auth.requestMatchers(
                            "/auth/login", "/auth/signup",
                            "/error",
                            "/oauth2/**", "/login/oauth2/**", "/oauth2/authorization/**",
                            "/api/auth/kakao/callback",
                            "/actuator/health"
                    ).permitAll();

                    // Swagger UI는 프로덕션이 아닌 환경에서만 허용
                    boolean isProduction = java.util.Arrays.asList(environment.getActiveProfiles()).contains("prod");
                    if (!isProduction) {
                        auth.requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll();
                        log.info("[SECURITY] Swagger UI enabled for non-production environment");
                    } else {
                        auth.requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").denyAll();
                        log.info("[SECURITY] Swagger UI disabled for production environment");
                    }

                    auth.requestMatchers("/auth/csrf-token", "/auth/csrf-status").authenticated()
                        .requestMatchers(HttpMethod.GET, "/test").permitAll()
                        .anyRequest().authenticated();
                })
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(u -> u.userService(kakaoOAuth2UserService))
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            // API 요청에 대해 401 JSON 응답 반환 (OAuth2 리다이렉트 방지)
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"JWT 토큰이 필요합니다.\"}");
                        })
                )
                // BotProtectionFilter를 가장 먼저 실행하여 악성 봇 요청을 조기에 차단
                .addFilterBefore(botProtectionFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(requestSizeMonitoringFilter, UsernamePasswordAuthenticationFilter.class);
        
        if (rateLimitingFilter != null) {
            http.addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class);
        }
        
        if (csrfValidationFilter != null) {
            http.addFilterBefore(csrfValidationFilter, UsernamePasswordAuthenticationFilter.class);
        }

        http.addFilterBefore(
                new JwtAuthenticationFilter(jwtTokenProvider, customUserDetailsService, trustedProxyService),
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${hamalog.cors.allowed-origins:http://localhost:3000}") String allowedOriginsCsv
    ) {
        CorsConfiguration config = new CorsConfiguration();

        // ✅ 명시적으로 허용된 Origin만 추가
        if (allowedOriginsCsv != null && !allowedOriginsCsv.isBlank()) {
            for (String origin : allowedOriginsCsv.split(",")) {
                String trimmed = origin.trim();
                if (!trimmed.isEmpty()) {
                    // 프로덕션에서는 정확한 도메인만 허용
                    config.addAllowedOriginPattern(trimmed);
                }
            }
        } else {
            // 기본값: 개발 환경
            config.addAllowedOriginPattern("http://localhost:3000");
        }

        config.setAllowCredentials(true);

        // ✅ 필요한 헤더만 명시적으로 허용
        config.addAllowedHeader("Authorization");
        config.addAllowedHeader("Content-Type");
        config.addAllowedHeader("Accept");
        config.addAllowedHeader("X-Requested-With");

        // ✅ 필요한 HTTP 메서드만 허용
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("PATCH");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("OPTIONS");

        // ✅ Preflight 캐시 시간 설정 (1시간)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
