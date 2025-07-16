package com.Hamalog.config;

import com.Hamalog.security.CustomUserDetailsService;
import com.Hamalog.security.jwt.JwtAuthenticationFilter;
import com.Hamalog.security.jwt.JwtTokenProvider;
import com.Hamalog.service.oauth2.KakaoOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/login", "/auth/signup", "/test").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider, customUserDetailsService),
                        UsernamePasswordAuthenticationFilter.class
                )
                .formLogin(form -> form
                        .loginPage("/auth/login") // 커스텀 로그인 페이지
                        .loginProcessingUrl("/auth/login") // 로그인 처리 URL
                        .usernameParameter("loginId")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/home", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessUrl("/auth/login")
                )
                .oauth2Login(oauth2 -> oauth2
                                .loginPage("/auth/login") // 소셜 로그인도 같은 로그인 페이지 사용
                                .userInfoEndpoint(userInfo -> userInfo
                                        .userService(kakaoOAuth2UserService)
                                )
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
}
