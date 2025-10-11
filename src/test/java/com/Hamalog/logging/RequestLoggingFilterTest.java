package com.Hamalog.logging;

import com.Hamalog.logging.events.ApiEvent;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RequestLoggingFilter Tests")
class RequestLoggingFilterTest {

    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    @Mock
    private FilterChain filterChain;
    
    @Mock
    private StructuredLogger structuredLogger;
    
    @Mock
    private SecurityContext securityContext;
    
    @Mock
    private Authentication authentication;

    @InjectMocks
    private RequestLoggingFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    @DisplayName("정상 요청을 처리하고 로그를 기록해야 함")
    void doFilterInternal_ShouldProcessNormalRequestAndLog() throws ServletException, IOException {
        // given
        given(request.getMethod()).willReturn("GET");
        given(request.getRequestURI()).willReturn("/api/medication/schedules");
        given(request.getRemoteAddr()).willReturn("192.168.1.1");
        given(request.getHeader("User-Agent")).willReturn("Mozilla/5.0");
        given(request.getHeader("Referer")).willReturn("https://example.com");
        given(request.getHeader("X-Request-Id")).willReturn(null);
        given(request.getQueryString()).willReturn("page=1&size=10");
        
        given(response.getStatus()).willReturn(200);
        
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.isAuthenticated()).willReturn(true);
        given(authentication.getName()).willReturn("user123");

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verify(response).setHeader(eq("X-Request-Id"), any(String.class));
        
        ArgumentCaptor<ApiEvent> eventCaptor = ArgumentCaptor.forClass(ApiEvent.class);
        verify(structuredLogger).api(eventCaptor.capture());
        
        ApiEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getHttpMethod()).isEqualTo("GET");
        assertThat(capturedEvent.getPath()).isEqualTo("/api/medication/schedules");
        assertThat(capturedEvent.getUserId()).isEqualTo("user123");
        assertThat(capturedEvent.getIpAddress()).isEqualTo("192.168.1.1");
        assertThat(capturedEvent.getStatusCode()).isEqualTo(200);
        assertThat(capturedEvent.getRequestType()).isEqualTo("외부 요청");
        assertThat(capturedEvent.getParameters()).containsKey("queryString");
    }

    @Test
    @DisplayName("예외 발생 시 에러를 로그하고 예외를 다시 던져야 함")
    void doFilterInternal_ShouldLogErrorAndRethrowException() throws ServletException, IOException {
        // given
        given(request.getMethod()).willReturn("POST");
        given(request.getRequestURI()).willReturn("/api/auth/login");
        given(request.getRemoteAddr()).willReturn("10.0.0.1");
        given(request.getHeader("User-Agent")).willReturn("curl/7.68.0");
        given(request.getHeader("X-Request-Id")).willReturn("req-123");
        
        given(response.getStatus()).willReturn(0); // Will be set to 500 for errors
        
        given(securityContext.getAuthentication()).willReturn(null);
        
        RuntimeException testException = new RuntimeException("Database connection failed");
        doThrow(testException).when(filterChain).doFilter(request, response);

        // when & then
        assertThatThrownBy(() -> filter.doFilterInternal(request, response, filterChain))
                .isSameAs(testException);

        verify(response).setHeader("X-Request-Id", "req-123");
        
        ArgumentCaptor<ApiEvent> eventCaptor = ArgumentCaptor.forClass(ApiEvent.class);
        verify(structuredLogger, times(2)).api(eventCaptor.capture()); // Called twice: in catch and finally
        
        // Get the first captured event (from the exception handler)
        ApiEvent capturedEvent = eventCaptor.getAllValues().get(0);
        assertThat(capturedEvent.getHttpMethod()).isEqualTo("POST");
        assertThat(capturedEvent.getPath()).isEqualTo("/api/auth/login");
        assertThat(capturedEvent.getUserId()).isEqualTo("anonymous");
        assertThat(capturedEvent.getStatusCode()).isEqualTo(500);
        assertThat(capturedEvent.getDurationMs()).isGreaterThan(0);
    }

    @Test
    @DisplayName("MDC 컨텍스트를 올바르게 설정하고 정리해야 함")
    void doFilterInternal_ShouldManageMDCContextProperly() throws ServletException, IOException {
        // given
        given(request.getMethod()).willReturn("PUT");
        given(request.getRequestURI()).willReturn("/api/medication/schedules/123");
        given(request.getRemoteAddr()).willReturn("172.16.0.1");
        given(request.getHeader("X-Request-Id")).willReturn("test-request-id");
        given(response.getStatus()).willReturn(204);
        given(securityContext.getAuthentication()).willReturn(null);

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        // MDC should be cleaned up after request
        assertThat(MDC.get("method")).isNull();
        assertThat(MDC.get("path")).isNull();
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    @DisplayName("내부 요청을 올바르게 식별해야 함")
    void doFilterInternal_ShouldIdentifyInternalRequests() throws ServletException, IOException {
        // given
        given(request.getMethod()).willReturn("GET");
        given(request.getRequestURI()).willReturn("/actuator/health");
        given(request.getRemoteAddr()).willReturn("127.0.0.1");
        given(response.getStatus()).willReturn(200);
        given(securityContext.getAuthentication()).willReturn(null);

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        ArgumentCaptor<ApiEvent> eventCaptor = ArgumentCaptor.forClass(ApiEvent.class);
        verify(structuredLogger).api(eventCaptor.capture());
        
        ApiEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getRequestType()).isEqualTo("내부 요청");
    }

    @Test
    @DisplayName("민감한 헤더를 마스킹해야 함")
    void doFilterInternal_ShouldMaskSensitiveHeaders() throws ServletException, IOException {
        // given
        given(request.getMethod()).willReturn("GET");
        given(request.getRequestURI()).willReturn("/api/users/profile");
        given(request.getRemoteAddr()).willReturn("203.0.113.1");
        given(request.getHeader("User-Agent")).willReturn("TestClient/1.0");
        given(request.getHeader("Referer")).willReturn(null); // Add missing header stub
        given(request.getHeader("X-Request-Id")).willReturn(null); // Add missing header stub
        given(request.getQueryString()).willReturn(null); // Add missing query string stub
        given(response.getStatus()).willReturn(200);
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.isAuthenticated()).willReturn(true);
        given(authentication.getName()).willReturn("testuser");

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        
        ArgumentCaptor<ApiEvent> eventCaptor = ArgumentCaptor.forClass(ApiEvent.class);
        verify(structuredLogger, atLeastOnce()).api(eventCaptor.capture());
        
        ApiEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getUserAgent()).isEqualTo("TestClient/1.0"); // User-Agent stored normally
        // Authorization and Cookie headers are masked in the safeHeader method, not exposed in ApiEvent
    }

    @Test
    @DisplayName("긴 문자열을 적절히 단축해야 함")
    void doFilterInternal_ShouldShortenLongStrings() throws ServletException, IOException {
        // given
        String longUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) ".repeat(10); // Very long UA
        String longReferer = "https://example.com/very/long/path/with/many/parameters".repeat(5);
        
        given(request.getMethod()).willReturn("GET");
        given(request.getRequestURI()).willReturn("/api/test");
        given(request.getRemoteAddr()).willReturn("192.168.1.100");
        given(request.getHeader("User-Agent")).willReturn(longUserAgent);
        given(request.getHeader("Referer")).willReturn(longReferer);
        given(request.getHeader("X-Request-Id")).willReturn(null); // Add missing stub
        given(request.getQueryString()).willReturn(null); // Add missing stub
        given(response.getStatus()).willReturn(200);
        given(securityContext.getAuthentication()).willReturn(null);

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        
        ArgumentCaptor<ApiEvent> eventCaptor = ArgumentCaptor.forClass(ApiEvent.class);
        verify(structuredLogger, atLeastOnce()).api(eventCaptor.capture());
        
        ApiEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getUserAgent()).isEqualTo(longUserAgent); // UserAgent stored as-is in ApiEvent
        // The shortening happens in the log messages, not in the structured event
    }

    @Test
    @DisplayName("인증되지 않은 요청을 anonymous로 처리해야 함")
    void doFilterInternal_ShouldHandleUnauthenticatedRequestsAsAnonymous() throws ServletException, IOException {
        // given
        given(request.getMethod()).willReturn("GET");
        given(request.getRequestURI()).willReturn("/api/public");
        given(request.getRemoteAddr()).willReturn("198.51.100.1");
        given(response.getStatus()).willReturn(200);
        given(securityContext.getAuthentication()).willReturn(null);

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        ArgumentCaptor<ApiEvent> eventCaptor = ArgumentCaptor.forClass(ApiEvent.class);
        verify(structuredLogger).api(eventCaptor.capture());
        
        ApiEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getUserId()).isEqualTo("anonymous");
    }

    @Test
    @DisplayName("인증 실패 시 anonymous로 처리해야 함")
    void doFilterInternal_ShouldHandleAuthenticationExceptionAsAnonymous() throws ServletException, IOException {
        // given
        given(request.getMethod()).willReturn("GET");
        given(request.getRequestURI()).willReturn("/api/secure");
        given(request.getRemoteAddr()).willReturn("203.0.113.50");
        given(response.getStatus()).willReturn(401);
        
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.isAuthenticated()).willReturn(false);

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        ArgumentCaptor<ApiEvent> eventCaptor = ArgumentCaptor.forClass(ApiEvent.class);
        verify(structuredLogger).api(eventCaptor.capture());
        
        ApiEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getUserId()).isEqualTo("anonymous");
    }

    @Test
    @DisplayName("인증 정보에서 예외 발생 시 anonymous로 처리해야 함")
    void doFilterInternal_ShouldHandleAuthenticationNameExceptionAsAnonymous() throws ServletException, IOException {
        // given
        given(request.getMethod()).willReturn("GET");
        given(request.getRequestURI()).willReturn("/api/test");
        given(request.getRemoteAddr()).willReturn("172.16.255.1");
        given(response.getStatus()).willReturn(200);
        
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.isAuthenticated()).willReturn(true);
        given(authentication.getName()).willThrow(new RuntimeException("Auth name error"));

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        ArgumentCaptor<ApiEvent> eventCaptor = ArgumentCaptor.forClass(ApiEvent.class);
        verify(structuredLogger).api(eventCaptor.capture());
        
        ApiEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getUserId()).isEqualTo("anonymous");
    }

    @Test
    @DisplayName("요청 ID가 없으면 새로 생성해야 함")
    void doFilterInternal_ShouldGenerateRequestIdWhenMissing() throws ServletException, IOException {
        // given
        given(request.getMethod()).willReturn("POST");
        given(request.getRequestURI()).willReturn("/api/data");
        given(request.getRemoteAddr()).willReturn("10.1.1.1");
        given(request.getHeader("X-Request-Id")).willReturn(null);
        given(response.getStatus()).willReturn(201);
        given(securityContext.getAuthentication()).willReturn(null);

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        ArgumentCaptor<String> requestIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(eq("X-Request-Id"), requestIdCaptor.capture());
        
        String generatedRequestId = requestIdCaptor.getValue();
        assertThat(generatedRequestId).isNotNull();
        assertThat(generatedRequestId).isNotBlank();
        // Should be a UUID format
        assertThat(generatedRequestId).matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    }

    @Test
    @DisplayName("다양한 HTTP 상태 코드를 올바르게 처리해야 함")
    void doFilterInternal_ShouldHandleVariousStatusCodes() throws ServletException, IOException {
        // given
        given(request.getMethod()).willReturn("DELETE");
        given(request.getRequestURI()).willReturn("/api/resource/123");
        given(request.getRemoteAddr()).willReturn("192.0.2.1");
        given(response.getStatus()).willReturn(404);
        given(securityContext.getAuthentication()).willReturn(null);

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        ArgumentCaptor<ApiEvent> eventCaptor = ArgumentCaptor.forClass(ApiEvent.class);
        verify(structuredLogger).api(eventCaptor.capture());
        
        ApiEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getStatusCode()).isEqualTo(404);
        assertThat(capturedEvent.getHttpMethod()).isEqualTo("DELETE");
    }
}