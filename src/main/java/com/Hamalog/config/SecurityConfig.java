package com.Hamalog.config;

import com.Hamalog.security.CustomUserDetailsService;
import com.Hamalog.security.jwt.JwtAuthenticationFilter;
import com.Hamalog.security.jwt.JwtTokenProvider;
import com.Hamalog.service.oauth2.KakaoOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
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
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final KakaoOAuth2UserService kakaoOAuth2UserService;
    private final JwtTokenProvider jwtTokenProvider;

    public SecurityConfig(
            CustomUserDetailsService customUserDetailsService,
            KakaoOAuth2UserService kakaoOAuth2UserService,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.customUserDetailsService = customUserDetailsService;
        this.kakaoOAuth2UserService = kakaoOAuth2UserService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
                        .frameOptions(frame -> frame.sameOrigin())
                )
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/auth/login", "/auth/signup",
                                "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**",
                                "/error"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/test").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider, customUserDetailsService),
                        UsernamePasswordAuthenticationFilter.class
                );

        // Keep optional formLogin/oauth2 only if needed; comment out to encourage pure JWT stateless API
        // .formLogin(form -> form.loginPage("/auth/login").permitAll())
        // .oauth2Login(oauth2 -> oauth2.loginPage("/auth/login").userInfoEndpoint(u -> u.userService(kakaoOAuth2UserService)));

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
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
