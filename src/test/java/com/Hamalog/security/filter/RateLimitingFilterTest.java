package com.Hamalog.security.filter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.Hamalog.service.security.RateLimitingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

@ExtendWith(MockitoExtension.class)
@DisplayName("Rate Limiting Filter Tests")
class RateLimitingFilterTest {

    @Mock
    private RateLimitingService rateLimitingService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private TrustedProxyService trustedProxyService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private PrintWriter printWriter;

    @InjectMocks
    private RateLimitingFilter rateLimitingFilter;

    private StringWriter responseWriter;

    @BeforeEach
    void setUp() {
        responseWriter = new StringWriter();
        lenient().when(trustedProxyService.isTrustedProxy(anyString())).thenReturn(false);
        lenient().when(request.getRemoteAddr()).thenReturn("192.168.0.10");
    }

    @Test
    @DisplayName("인증 엔드포인트 요청 허용 시 필터 통과")
    void doFilterInternal_AuthEndpointAllowed_PassesFilter() throws ServletException, IOException {
        // given
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(rateLimitingService.tryConsumeAuthRequest(anyString())).thenReturn(true);
        
        RateLimitingService.RateLimitInfo rateLimitInfo = 
            new RateLimitingService.RateLimitInfo(10, 100, 5);
        when(rateLimitingService.getRateLimitInfo(anyString(), eq(true))).thenReturn(rateLimitInfo);

        // when
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(rateLimitingService).tryConsumeAuthRequest(anyString());
        verify(response).setHeader("X-RateLimit-Limit-Minute", "10");
        verify(response).setHeader("X-RateLimit-Limit-Hour", "100");
        verify(response).setHeader("X-RateLimit-Remaining", "5");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("인증 엔드포인트 요청 제한 초과 시 TOO_MANY_REQUESTS 응답")
    void doFilterInternal_AuthEndpointExceeded_ReturnsTooManyRequests() throws ServletException, IOException {
        // given
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(response.getWriter()).thenReturn(printWriter);
        when(rateLimitingService.tryConsumeAuthRequest(anyString())).thenReturn(false);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"error\":\"Too Many Requests\"}");

        // when
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
        verify(response).setCharacterEncoding("UTF-8");
        verify(printWriter).write("{\"error\":\"Too Many Requests\"}");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("보호된 엔드포인트 요청 허용 시 필터 통과")
    void doFilterInternal_ProtectedEndpointAllowed_PassesFilter() throws ServletException, IOException {
        // given
        when(request.getRequestURI()).thenReturn("/api/v1/medication-record/123");
        when(rateLimitingService.tryConsumeApiRequest(anyString())).thenReturn(true);
        
        RateLimitingService.RateLimitInfo rateLimitInfo = 
            new RateLimitingService.RateLimitInfo(60, 1000, 45);
        when(rateLimitingService.getRateLimitInfo(anyString(), eq(false))).thenReturn(rateLimitInfo);

        // when
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(rateLimitingService).tryConsumeApiRequest(anyString());
        verify(response).setHeader("X-RateLimit-Limit-Minute", "60");
        verify(response).setHeader("X-RateLimit-Limit-Hour", "1000");
        verify(response).setHeader("X-RateLimit-Remaining", "45");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("보호된 엔드포인트 요청 제한 초과 시 TOO_MANY_REQUESTS 응답")
    void doFilterInternal_ProtectedEndpointExceeded_ReturnsTooManyRequests() throws ServletException, IOException {
        // given
        when(request.getRequestURI()).thenReturn("/api/v1/medication-schedule/create");
        when(response.getWriter()).thenReturn(printWriter);
        when(rateLimitingService.tryConsumeApiRequest(anyString())).thenReturn(false);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"error\":\"Too Many Requests\"}");

        // when
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
        verify(printWriter).write("{\"error\":\"Too Many Requests\"}");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("보호되지 않은 엔드포인트는 필터 건너뛰기")
    void doFilterInternal_UnprotectedEndpoint_SkipsRateLimit() throws ServletException, IOException {
        // given
        when(request.getRequestURI()).thenReturn("/public/info");

        // when
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(rateLimitingService, never()).tryConsumeAuthRequest(anyString());
        verify(rateLimitingService, never()).tryConsumeApiRequest(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("X-Forwarded-For 헤더를 통한 IP 추출")
    void getClientIpAddress_XForwardedForHeader_ExtractsIp() throws ServletException, IOException {
        // given
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(trustedProxyService.isTrustedProxy(anyString())).thenReturn(true);
        when(trustedProxyService.extractClientIp(anyString())).thenReturn(Optional.of("203.0.113.1"));
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 198.51.100.1");
        when(rateLimitingService.tryConsumeAuthRequest(contains("203.0.113.1"))).thenReturn(true);
        
        RateLimitingService.RateLimitInfo rateLimitInfo = 
            new RateLimitingService.RateLimitInfo(10, 100, 5);
        when(rateLimitingService.getRateLimitInfo(contains("203.0.113.1"), eq(true))).thenReturn(rateLimitInfo);

        // when
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(rateLimitingService).tryConsumeAuthRequest(contains("203.0.113.1"));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("X-Forwarded-For 헤더가 없을 때 RemoteAddr 사용")
    void getClientIpAddress_NoXForwardedFor_UsesRemoteAddr() throws ServletException, IOException {
        // given
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(trustedProxyService.isTrustedProxy(anyString())).thenReturn(true);
        when(trustedProxyService.extractClientIp(any())).thenReturn(Optional.empty());
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(rateLimitingService.tryConsumeAuthRequest(contains("192.168.1.100"))).thenReturn(true);
        
        RateLimitingService.RateLimitInfo rateLimitInfo = 
            new RateLimitingService.RateLimitInfo(10, 100, 5);
        when(rateLimitingService.getRateLimitInfo(contains("192.168.1.100"), eq(true))).thenReturn(rateLimitInfo);

        // when
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(rateLimitingService).tryConsumeAuthRequest(contains("192.168.1.100"));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("X-Forwarded-For 헤더가 unknown일 때 RemoteAddr 사용")
    void getClientIpAddress_XForwardedForUnknown_UsesRemoteAddr() throws ServletException, IOException {
        // given
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(trustedProxyService.isTrustedProxy(anyString())).thenReturn(true);
        when(trustedProxyService.extractClientIp(any())).thenReturn(Optional.empty());
        when(request.getHeader("X-Forwarded-For")).thenReturn("unknown");
        when(request.getRemoteAddr()).thenReturn("192.168.1.200");
        when(rateLimitingService.tryConsumeAuthRequest(contains("192.168.1.200"))).thenReturn(true);
        
        RateLimitingService.RateLimitInfo rateLimitInfo = 
            new RateLimitingService.RateLimitInfo(10, 100, 5);
        when(rateLimitingService.getRateLimitInfo(contains("192.168.1.200"), eq(true))).thenReturn(rateLimitInfo);

        // when
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(rateLimitingService).tryConsumeAuthRequest(contains("192.168.1.200"));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Rate Limit 헤더 추가 실패 시 예외 처리")
    void addRateLimitHeaders_ExceptionThrown_HandlesGracefully() throws ServletException, IOException {
        // given
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(rateLimitingService.tryConsumeAuthRequest(anyString())).thenReturn(true);
        when(rateLimitingService.getRateLimitInfo(anyString(), eq(true)))
            .thenThrow(new RuntimeException("Redis connection error"));

        // when & then
        assertDoesNotThrow(() -> rateLimitingFilter.doFilterInternal(request, response, filterChain));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("shouldNotFilter - 정적 리소스 요청은 필터링하지 않음")
    void shouldNotFilter_StaticResources_ReturnsTrue() {
        // given
        when(request.getRequestURI()).thenReturn("/static/css/main.css");

        // when
        boolean result = rateLimitingFilter.shouldNotFilter(request);

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("shouldNotFilter - CSS 파일은 필터링하지 않음")
    void shouldNotFilter_CssFiles_ReturnsTrue() {
        // given
        when(request.getRequestURI()).thenReturn("/css/bootstrap.css");

        // when
        boolean result = rateLimitingFilter.shouldNotFilter(request);

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("shouldNotFilter - JS 파일은 필터링하지 않음")
    void shouldNotFilter_JsFiles_ReturnsTrue() {
        // given
        when(request.getRequestURI()).thenReturn("/js/main.js");

        // when
        boolean result = rateLimitingFilter.shouldNotFilter(request);

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("shouldNotFilter - 이미지 파일은 필터링하지 않음")
    void shouldNotFilter_ImageFiles_ReturnsTrue() {
        // given
        when(request.getRequestURI()).thenReturn("/images/logo.png");

        // when
        boolean result = rateLimitingFilter.shouldNotFilter(request);

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("shouldNotFilter - favicon.ico는 필터링하지 않음")
    void shouldNotFilter_Favicon_ReturnsTrue() {
        // given
        when(request.getRequestURI()).thenReturn("/favicon.ico");

        // when
        boolean result = rateLimitingFilter.shouldNotFilter(request);

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("shouldNotFilter - actuator health는 필터링하지 않음")
    void shouldNotFilter_ActuatorHealth_ReturnsTrue() {
        // given
        when(request.getRequestURI()).thenReturn("/actuator/health");

        // when
        boolean result = rateLimitingFilter.shouldNotFilter(request);

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("shouldNotFilter - API 엔드포인트는 필터링함")
    void shouldNotFilter_ApiEndpoints_ReturnsFalse() {
        // given
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");

        // when
        boolean result = rateLimitingFilter.shouldNotFilter(request);

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("인증 엔드포인트 signUp 요청 처리")
    void doFilterInternal_SignupEndpoint_ProcessesCorrectly() throws ServletException, IOException {
        // given
        when(request.getRequestURI()).thenReturn("/api/v1/auth/signup");
        when(rateLimitingService.tryConsumeAuthRequest(anyString())).thenReturn(true);
        
        RateLimitingService.RateLimitInfo rateLimitInfo = 
            new RateLimitingService.RateLimitInfo(5, 50, 2);
        when(rateLimitingService.getRateLimitInfo(anyString(), eq(true))).thenReturn(rateLimitInfo);

        // when
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(rateLimitingService).tryConsumeAuthRequest(anyString());
        verify(response).setHeader("X-RateLimit-Limit-Minute", "5");
        verify(response).setHeader("X-RateLimit-Limit-Hour", "50");
        verify(response).setHeader("X-RateLimit-Remaining", "2");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("인증 엔드포인트 logout 요청 처리")
    void doFilterInternal_LogoutEndpoint_ProcessesCorrectly() throws ServletException, IOException {
        // given
        when(request.getRequestURI()).thenReturn("/api/v1/auth/logout");
        when(rateLimitingService.tryConsumeAuthRequest(anyString())).thenReturn(true);
        
        RateLimitingService.RateLimitInfo rateLimitInfo = 
            new RateLimitingService.RateLimitInfo(20, 200, 15);
        when(rateLimitingService.getRateLimitInfo(anyString(), eq(true))).thenReturn(rateLimitInfo);

        // when
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(rateLimitingService).tryConsumeAuthRequest(anyString());
        verify(response).setHeader("X-RateLimit-Limit-Minute", "20");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("side-effect 엔드포인트 요청 처리")
    void doFilterInternal_SideEffectEndpoint_ProcessesCorrectly() throws ServletException, IOException {
        // given
        when(request.getRequestURI()).thenReturn("/api/v1/side-effect/record");
        when(rateLimitingService.tryConsumeApiRequest(anyString())).thenReturn(true);
        
        RateLimitingService.RateLimitInfo rateLimitInfo = 
            new RateLimitingService.RateLimitInfo(30, 500, 25);
        when(rateLimitingService.getRateLimitInfo(anyString(), eq(false))).thenReturn(rateLimitInfo);

        // when
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(rateLimitingService).tryConsumeApiRequest(anyString());
        verify(response).setHeader("X-RateLimit-Limit-Minute", "30");
        verify(response).setHeader("X-RateLimit-Limit-Hour", "500");
        verify(response).setHeader("X-RateLimit-Remaining", "25");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Rate Limit 초과 시 에러 메시지 차별화 - 인증 엔드포인트")
    void handleRateLimitExceeded_AuthEndpoint_ReturnsAuthSpecificMessage() throws ServletException, IOException {
        // given
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(response.getWriter()).thenReturn(printWriter);
        when(rateLimitingService.tryConsumeAuthRequest(anyString())).thenReturn(false);
        
        String expectedJsonResponse = "{\"error\":\"Too Many Requests\",\"message\":\"인증 요청이 너무 많습니다. 잠시 후 다시 시도해주세요.\"}";
        when(objectMapper.writeValueAsString(any())).thenReturn(expectedJsonResponse);

        // when
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(printWriter).write(expectedJsonResponse);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("Rate Limit 초과 시 에러 메시지 차별화 - API 엔드포인트")
    void handleRateLimitExceeded_ApiEndpoint_ReturnsApiSpecificMessage() throws ServletException, IOException {
        // given
        when(request.getRequestURI()).thenReturn("/api/v1/medication-record/list");
        when(response.getWriter()).thenReturn(printWriter);
        when(rateLimitingService.tryConsumeApiRequest(anyString())).thenReturn(false);
        
        String expectedJsonResponse = "{\"error\":\"Too Many Requests\",\"message\":\"요청이 너무 많습니다. 잠시 후 다시 시도해주세요.\"}";
        when(objectMapper.writeValueAsString(any())).thenReturn(expectedJsonResponse);

        // when
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(printWriter).write(expectedJsonResponse);
        verify(filterChain, never()).doFilter(request, response);
    }
}
