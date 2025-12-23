package com.Hamalog.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.Hamalog.logging.events.ApiEvent;
import com.Hamalog.security.SecurityContextUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@DisplayName("RequestLoggingFilter Tests")
class RequestLoggingFilterTest {

    @Mock
    private StructuredLogger structuredLogger;

    @Mock
    private SecurityContextUtils securityContextUtils;

    @Mock
    private FilterChain filterChain;

    @Mock
    private SecurityContext securityContext;

    private RequestLoggingFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RequestLoggingFilter(structuredLogger, securityContextUtils);
        SecurityContextHolder.setContext(securityContext);
        MDC.clear();

        // Default mock behaviors
        lenient().when(securityContextUtils.getCurrentUserId()).thenReturn("anonymous");
        lenient().when(securityContextUtils.getClientIpAddress(any(HttpServletRequest.class))).thenReturn("192.168.0.1");
        lenient().when(securityContextUtils.sanitizeUserAgent(any())).thenAnswer(inv -> {
            String ua = inv.getArgument(0);
            return ua != null ? ua : "unknown";
        });
        lenient().when(securityContextUtils.determineRequestType(any())).thenReturn("EXTERNAL");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    @DisplayName("정상 요청 정보를 로깅하고 wrapper를 체인에 전달한다")
    void shouldLogSuccessfulRequest() throws Exception {
        MockHttpServletRequest request = baseRequest("GET", "/api/medication/schedules");
        request.addHeader("User-Agent", "Mozilla/5.0");
        request.addHeader("Referer", "https://example.com");
        request.setQueryString("page=1&size=10");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(securityContextUtils.getCurrentUserId()).thenReturn("user123");
        when(securityContextUtils.sanitizeUserAgent("Mozilla/5.0")).thenReturn("Mozilla/5.0");

        doAnswer(invocation -> {
            HttpServletResponse resp = invocation.getArgument(1);
            resp.setStatus(200);
            return null;
        }).when(filterChain).doFilter(eq(request), any(StatusAwareResponseWrapper.class));

        filter.doFilterInternal(request, response, filterChain);

        ArgumentCaptor<ApiEvent> captor = ArgumentCaptor.forClass(ApiEvent.class);
        verify(structuredLogger, org.mockito.Mockito.atLeastOnce()).api(captor.capture());
        ApiEvent event = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(event.getUserId()).isEqualTo("user123");
        assertThat(event.getStatusCode()).isEqualTo(200);
        assertThat(event.getRequestType()).isEqualTo("EXTERNAL");
    }

    @Test
    @DisplayName("예외 발생 시 500 상태로 로깅 후 예외를 전파한다")
    void shouldLogErrorAndRethrow() throws Exception {
        MockHttpServletRequest request = baseRequest("POST", "/api/auth/login");
        request.addHeader("X-Request-Id", "req-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        doThrow(new RuntimeException("boom")).when(filterChain)
                .doFilter(eq(request), any(HttpServletResponse.class));

        assertThatThrownBy(() -> filter.doFilterInternal(request, response, filterChain))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("boom");

        ArgumentCaptor<ApiEvent> captor = ArgumentCaptor.forClass(ApiEvent.class);
        verify(structuredLogger).api(captor.capture());
        assertThat(captor.getAllValues()).hasSize(1).allMatch(ev -> ev.getStatusCode() == 500);
    }

    @Test
    @DisplayName("민감 헤더는 마스킹된다")
    void shouldMaskSensitiveHeaders() throws Exception {
        MockHttpServletRequest request = baseRequest("GET", "/api/users/profile");
        request.addHeader("Authorization", "Bearer secret");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        ArgumentCaptor<ApiEvent> captor = ArgumentCaptor.forClass(ApiEvent.class);
        verify(structuredLogger).api(captor.capture());
        Map<String, Object> parameters = captor.getValue().getParameters();
        Map<?, ?> headers = (Map<?, ?>) parameters.get("headers");
        assertThat(headers.get("Authorization")).isEqualTo("***");
    }

    @Test
    @DisplayName("/actuator 요청은 필터링에서 제외된다")
    void shouldNotFilterActuatorRequest() throws Exception {
        MockHttpServletRequest request = baseRequest("GET", "/actuator/health");

        boolean shouldNotFilter = filter.shouldNotFilter(request);

        assertThat(shouldNotFilter).isTrue();
    }

    @Test
    @DisplayName("응답 상태는 request attribute에 저장된다")
    void shouldStoreStatusOnRequestAttribute() throws Exception {
        MockHttpServletRequest request = baseRequest("DELETE", "/api/resource/1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        doAnswer(invocation -> {
            HttpServletResponse resp = invocation.getArgument(1);
            resp.setStatus(204);
            return null;
        }).when(filterChain).doFilter(eq(request), any(StatusAwareResponseWrapper.class));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(eq(request), any(StatusAwareResponseWrapper.class));
        assertThat(request.getAttribute(LoggingConstants.RESPONSE_STATUS_ATTRIBUTE)).isEqualTo(204);
    }

    private MockHttpServletRequest baseRequest(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        request.setRequestURI(uri);
        request.setRemoteAddr("192.168.0.1");
        return request;
    }
}
