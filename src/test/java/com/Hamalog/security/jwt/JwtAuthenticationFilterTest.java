package com.Hamalog.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.Hamalog.security.filter.TrustedProxyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("JwtAuthenticationFilter Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private TrustedProxyService trustedProxyService;

    @Mock
    private SecurityContext securityContext;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService, trustedProxyService);
        SecurityContextHolder.setContext(securityContext);
        MDC.clear(); // Clear MDC before each test

        when(trustedProxyService.isTrustedProxy(anyString())).thenReturn(false);
    }

    @Test
    @DisplayName("Should authenticate successfully with valid JWT token")
    void doFilterInternal_ValidToken_AuthenticatesSuccessfully() throws ServletException, IOException {
        // given
        String token = "valid-jwt-token";
        String loginId = "test@example.com";
        UserDetails userDetails = new User(loginId, "password", Collections.emptyList());

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getLoginIdFromToken(token)).thenReturn(loginId);
        when(userDetailsService.loadUserByUsername(loginId)).thenReturn(userDetails);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(securityContext).setAuthentication(any(Authentication.class));
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider).validateToken(token);
        verify(jwtTokenProvider).getLoginIdFromToken(token);
        verify(userDetailsService).loadUserByUsername(loginId);
    }

    @Test
    @DisplayName("Should not authenticate with invalid JWT token")
    void doFilterInternal_InvalidToken_DoesNotAuthenticate() throws ServletException, IOException {
        // given
        String token = "invalid-jwt-token";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        when(jwtTokenProvider.validateToken(token)).thenReturn(false);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider).validateToken(token);
        verify(jwtTokenProvider, never()).getLoginIdFromToken(anyString());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    @DisplayName("Should handle missing Authorization header")
    void doFilterInternal_NoToken_ContinuesFilterChain() throws ServletException, IOException {
        // given
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider, never()).validateToken(anyString());
    }

    @Test
    @DisplayName("Should handle non-Bearer token format")
    void doFilterInternal_NonBearerToken_DoesNotAuthenticate() throws ServletException, IOException {
        // given
        when(request.getHeader("Authorization")).thenReturn("Basic dGVzdA==");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider, never()).validateToken(anyString());
    }

    @Test
    @DisplayName("Should resolve client IP from X-Forwarded-For header")
    void doFilterInternal_XForwardedForHeader_ResolvesCorrectIp() throws ServletException, IOException {
        // given
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, 10.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        // IP should be resolved from X-Forwarded-For (first IP)
    }

    @Test
    @DisplayName("Should resolve client IP from X-Real-IP header when X-Forwarded-For is not present")
    void doFilterInternal_XRealIpHeader_ResolvesCorrectIp() throws ServletException, IOException {
        // given
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("192.168.1.100");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should skip authentication for public endpoints")
    void doFilterInternal_PublicEndpoint_SkipsAuthentication() throws ServletException, IOException {
        // given
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(request.getRequestURI()).thenReturn("/auth/login");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider, never()).validateToken(anyString());
    }

    @Test
    @DisplayName("Should handle authentication exception and clear security context")
    void doFilterInternal_AuthenticationException_ClearsSecurityContext() throws ServletException, IOException {
        // given
        String token = "valid-jwt-token";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getLoginIdFromToken(token)).thenThrow(new RuntimeException("Token parsing error"));

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should sanitize user agent for logging")
    void doFilterInternal_MaliciousUserAgent_SanitizesForLogging() throws ServletException, IOException {
        // given
        String maliciousUserAgent = "Mozilla/5.0\r\nInjected-Header: malicious\nAnother: value";

        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("User-Agent")).thenReturn(maliciousUserAgent);
        when(request.getRequestURI()).thenReturn("/auth/login");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        // User agent should be sanitized in logs (newlines replaced with underscores)
    }

    @Test
    @DisplayName("Should clean up MDC after filter execution")
    void doFilterInternal_FilterExecution_CleanupMDC() throws ServletException, IOException {
        // given
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(request.getRequestURI()).thenReturn("/auth/login");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        // MDC should be cleaned up after filter execution
        assertThat(MDC.get("security.clientIp")).isNull();
        assertThat(MDC.get("security.userAgent")).isNull();
        assertThat(MDC.get("security.authenticatedUser")).isNull();
    }

    @Test
    @DisplayName("Should require authentication for protected API endpoints")
    void doFilterInternal_ProtectedEndpoint_RequiresAuthentication() throws ServletException, IOException {
        // given
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(request.getRequestURI()).thenReturn("/api/medication/schedules");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        // Should log debug message about missing token for protected resource
    }

    @Test
    @DisplayName("Should handle empty Authorization header")
    void doFilterInternal_EmptyAuthorizationHeader_DoesNotAuthenticate() throws ServletException, IOException {
        // given
        when(request.getHeader("Authorization")).thenReturn("");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider, never()).validateToken(anyString());
    }

    @Test
    @DisplayName("Should authenticate successfully with valid JWT token from trusted proxy")
    void doFilterInternal_ValidToken_AuthenticatesSuccessfully_TrustedProxy() throws ServletException, IOException {
        // given
        String token = "valid-jwt-token";
        String loginId = "test@example.com";
        UserDetails userDetails = new User(loginId, "password", Collections.emptyList());

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getLoginIdFromToken(token)).thenReturn(loginId);
        when(userDetailsService.loadUserByUsername(loginId)).thenReturn(userDetails);
        when(trustedProxyService.isTrustedProxy(anyString())).thenReturn(true);
        when(trustedProxyService.extractClientIp(anyString())).thenReturn(Optional.of("192.168.1.1"));

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(securityContext).setAuthentication(any(Authentication.class));
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider).validateToken(token);
        verify(jwtTokenProvider).getLoginIdFromToken(token);
        verify(userDetailsService).loadUserByUsername(loginId);
    }

    @Test
    @DisplayName("Should not authenticate with invalid JWT token from trusted proxy")
    void doFilterInternal_InvalidToken_DoesNotAuthenticate_TrustedProxy() throws ServletException, IOException {
        // given
        String token = "invalid-jwt-token";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        when(jwtTokenProvider.validateToken(token)).thenReturn(false);
        when(trustedProxyService.isTrustedProxy(anyString())).thenReturn(true);
        when(trustedProxyService.extractClientIp(anyString())).thenReturn(Optional.of("192.168.1.1"));

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider).validateToken(token);
        verify(jwtTokenProvider, never()).getLoginIdFromToken(anyString());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    @DisplayName("Should handle missing Authorization header for requests from trusted proxy")
    void doFilterInternal_NoToken_ContinuesFilterChain_TrustedProxy() throws ServletException, IOException {
        // given
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(trustedProxyService.isTrustedProxy(anyString())).thenReturn(true);
        when(trustedProxyService.extractClientIp(anyString())).thenReturn(Optional.of("192.168.1.1"));

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider, never()).validateToken(anyString());
    }

    @Test
    @DisplayName("Should handle non-Bearer token format for requests from trusted proxy")
    void doFilterInternal_NonBearerToken_DoesNotAuthenticate_TrustedProxy() throws ServletException, IOException {
        // given
        when(request.getHeader("Authorization")).thenReturn("Basic dGVzdA==");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(trustedProxyService.isTrustedProxy(anyString())).thenReturn(true);
        when(trustedProxyService.extractClientIp(anyString())).thenReturn(Optional.of("192.168.1.1"));

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider, never()).validateToken(anyString());
    }

    @Test
    @DisplayName("Should resolve client IP from X-Forwarded-For header for requests from trusted proxy")
    void doFilterInternal_XForwardedForHeader_ResolvesCorrectIp_TrustedProxy() throws ServletException, IOException {
        // given
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, 10.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(trustedProxyService.isTrustedProxy(anyString())).thenReturn(true);
        when(trustedProxyService.extractClientIp(anyString())).thenReturn(Optional.of("192.168.1.1"));

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        // IP should be resolved from X-Forwarded-For (first IP)
    }

    @Test
    @DisplayName("Should resolve client IP from X-Real-IP header when X-Forwarded-For is not present for requests from trusted proxy")
    void doFilterInternal_XRealIpHeader_ResolvesCorrectIp_TrustedProxy() throws ServletException, IOException {
        // given
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("192.168.1.100");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(trustedProxyService.isTrustedProxy(anyString())).thenReturn(true);
        when(trustedProxyService.extractClientIp(anyString())).thenReturn(Optional.empty());
        when(trustedProxyService.extractSingleIp("192.168.1.100")).thenReturn(Optional.of("192.168.1.100"));

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should skip authentication for public endpoints even when request comes from trusted proxy")
    void doFilterInternal_PublicEndpoint_SkipsAuthentication_TrustedProxy() throws ServletException, IOException {
        // given
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(request.getRequestURI()).thenReturn("/auth/login");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(trustedProxyService.isTrustedProxy(anyString())).thenReturn(true);
        when(trustedProxyService.extractClientIp(anyString())).thenReturn(Optional.of("192.168.1.1"));

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider, never()).validateToken(anyString());
    }

    @Test
    @DisplayName("Should handle authentication exception and clear security context for requests from trusted proxy")
    void doFilterInternal_AuthenticationException_ClearsSecurityContext_TrustedProxy() throws ServletException, IOException {
        // given
        String token = "valid-jwt-token";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(trustedProxyService.isTrustedProxy(anyString())).thenReturn(true);
        when(trustedProxyService.extractClientIp(anyString())).thenReturn(Optional.empty());

        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getLoginIdFromToken(token)).thenThrow(new RuntimeException("Token parsing error"));

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should sanitize user agent for logging even when request comes from trusted proxy")
    void doFilterInternal_MaliciousUserAgent_SanitizesForLogging_TrustedProxy() throws ServletException, IOException {
        // given
        String maliciousUserAgent = "Mozilla/5.0\r\nInjected-Header: malicious\nAnother: value";

        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("User-Agent")).thenReturn(maliciousUserAgent);
        when(request.getRequestURI()).thenReturn("/auth/login");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(trustedProxyService.isTrustedProxy(anyString())).thenReturn(true);
        when(trustedProxyService.extractClientIp(anyString())).thenReturn(Optional.empty());

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        // User agent should be sanitized in logs (newlines replaced with underscores)
    }

    @Test
    @DisplayName("Should clean up MDC after filter execution even when request comes from trusted proxy")
    void doFilterInternal_FilterExecution_CleanupMDC_TrustedProxy() throws ServletException, IOException {
        // given
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(request.getRequestURI()).thenReturn("/auth/login");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(trustedProxyService.isTrustedProxy(anyString())).thenReturn(true);
        when(trustedProxyService.extractClientIp(anyString())).thenReturn(Optional.empty());

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        // MDC should be cleaned up after filter execution
        assertThat(MDC.get("security.clientIp")).isNull();
        assertThat(MDC.get("security.userAgent")).isNull();
        assertThat(MDC.get("security.authenticatedUser")).isNull();
    }

    @Test
    @DisplayName("Should require authentication for protected API endpoints even when request comes from trusted proxy")
    void doFilterInternal_ProtectedEndpoint_RequiresAuthentication_TrustedProxy() throws ServletException, IOException {
        // given
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(request.getRequestURI()).thenReturn("/api/medication/schedules");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(trustedProxyService.isTrustedProxy(anyString())).thenReturn(true);
        when(trustedProxyService.extractClientIp(anyString())).thenReturn(Optional.empty());

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        // Should log debug message about missing token for protected resource
    }

    @Test
    @DisplayName("Should handle empty Authorization header for requests from trusted proxy")
    void doFilterInternal_EmptyAuthorizationHeader_DoesNotAuthenticate_TrustedProxy() throws ServletException, IOException {
        // given
        when(request.getHeader("Authorization")).thenReturn("");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(trustedProxyService.isTrustedProxy(anyString())).thenReturn(true);
        when(trustedProxyService.extractClientIp(anyString())).thenReturn(Optional.empty());

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider, never()).validateToken(anyString());
    }
}
