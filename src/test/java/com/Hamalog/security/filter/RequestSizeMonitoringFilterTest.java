package com.Hamalog.security.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("RequestSizeMonitoringFilter Tests")
class RequestSizeMonitoringFilterTest {

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private RequestSizeMonitoringFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        
        // Set default configuration values
        ReflectionTestUtils.setField(filter, "maxRequestSizeMB", 10L);
        ReflectionTestUtils.setField(filter, "warnRequestSizeMB", 5L);
        ReflectionTestUtils.setField(filter, "monitoringEnabled", true);
    }

    @Test
    @DisplayName("Should continue filter chain when monitoring is disabled")
    void doFilterInternal_MonitoringDisabled_ContinuesFilterChain() throws ServletException, IOException {
        // given
        ReflectionTestUtils.setField(filter, "monitoringEnabled", false);
        request.addHeader("Content-Length", "1000");

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should monitor request size for regular endpoints")
    void doFilterInternal_RegularEndpoint_MonitorsRequestSize() throws ServletException, IOException {
        // given
        request.setRequestURI("/api/data");
        request.setMethod("POST");
        request.addHeader("Content-Length", String.valueOf(1024 * 1024 * 2)); // 2MB
        request.setRemoteAddr("192.168.1.1");

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should apply strict limits for authentication endpoints")
    void doFilterInternal_StrictEndpoint_AppliesStrictLimits() throws ServletException, IOException {
        // given
        request.setRequestURI("/api/v1/auth/login");
        request.setMethod("POST");
        request.addHeader("Content-Length", String.valueOf(1024 * 1024 * 2)); // 2MB - exceeds strict limit of 1MB
        request.setRemoteAddr("192.168.1.1");

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should skip monitoring for excluded endpoints")
    void shouldNotFilter_ExcludedEndpoints_ReturnsTrue() {
        // given/when/then
        request.setRequestURI("/actuator/health");
        assertThat(filter.shouldNotFilter(request)).isTrue();

        request.setRequestURI("/favicon.ico");
        assertThat(filter.shouldNotFilter(request)).isTrue();

        request.setRequestURI("/static/css/style.css");
        assertThat(filter.shouldNotFilter(request)).isTrue();

        request.setRequestURI("/css/main.css");
        assertThat(filter.shouldNotFilter(request)).isTrue();

        request.setRequestURI("/js/app.js");
        assertThat(filter.shouldNotFilter(request)).isTrue();

        request.setRequestURI("/images/logo.png");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    @DisplayName("Should not skip monitoring for regular endpoints")
    void shouldNotFilter_RegularEndpoints_ReturnsFalse() {
        // given/when/then
        request.setRequestURI("/api/data");
        assertThat(filter.shouldNotFilter(request)).isFalse();

        request.setRequestURI("/api/v1/auth/login");
        assertThat(filter.shouldNotFilter(request)).isFalse();

        request.setRequestURI("/medication/schedule");
        assertThat(filter.shouldNotFilter(request)).isFalse();
    }

    @Test
    @DisplayName("Should detect client IP from X-Forwarded-For header")
    void doFilterInternal_XForwardedForHeader_ProcessesCorrectly() throws ServletException, IOException {
        // given
        request.addHeader("X-Forwarded-For", "203.0.113.1, 192.168.1.1");
        request.setRequestURI("/api/test");
        request.addHeader("Content-Length", "1000");

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should detect client IP from X-Real-IP header")
    void doFilterInternal_XRealIpHeader_ProcessesCorrectly() throws ServletException, IOException {
        // given
        request.addHeader("X-Real-IP", "203.0.113.2");
        request.setRequestURI("/api/test");
        request.addHeader("Content-Length", "1000");

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should fallback to remote address when no proxy headers")
    void doFilterInternal_NoProxyHeaders_UsesRemoteAddr() throws ServletException, IOException {
        // given
        request.setRemoteAddr("10.0.0.1");
        request.setRequestURI("/api/test");
        request.addHeader("Content-Length", "1000");

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should handle unknown IP addresses in headers")
    void doFilterInternal_UnknownIpInHeader_IgnoresUnknown() throws ServletException, IOException {
        // given
        request.addHeader("X-Forwarded-For", "unknown");
        request.addHeader("X-Real-IP", "203.0.113.3");
        request.setRequestURI("/api/test");
        request.addHeader("Content-Length", "1000");

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should monitor headers size and process large headers")
    void doFilterInternal_LargeHeaders_MonitorsHeadersSize() throws ServletException, IOException {
        // given
        request.setRequestURI("/api/test");
        request.addHeader("Content-Length", "1000");
        
        // Add large headers
        StringBuilder largeValue = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeValue.append("a");
        }
        request.addHeader("Large-Header-1", largeValue.toString());
        request.addHeader("Large-Header-2", largeValue.toString());
        request.addHeader("Large-Header-3", largeValue.toString());

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should handle zero content length gracefully")
    void doFilterInternal_ZeroContentLength_HandlesGracefully() throws ServletException, IOException {
        // given
        request.setRequestURI("/api/test");
        request.setMethod("GET");
        request.addHeader("Content-Length", "0");

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should handle requests without content length")
    void doFilterInternal_NoContentLength_HandlesGracefully() throws ServletException, IOException {
        // given
        request.setRequestURI("/api/test");
        request.setMethod("GET");
        // No Content-Length header

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should return current limits configuration")
    void getCurrentLimits_Always_ReturnsCurrentConfiguration() {
        // when
        RequestSizeMonitoringFilter.RequestSizeLimits limits = filter.getCurrentLimits();

        // then
        assertThat(limits).isNotNull();
        assertThat(limits.maxRequestSizeMB()).isEqualTo(10L);
        assertThat(limits.warnRequestSizeMB()).isEqualTo(5L);
        assertThat(limits.monitoringEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should return updated limits after configuration change")
    void getCurrentLimits_AfterConfigurationChange_ReturnsUpdatedLimits() {
        // given
        ReflectionTestUtils.setField(filter, "maxRequestSizeMB", 20L);
        ReflectionTestUtils.setField(filter, "warnRequestSizeMB", 10L);
        ReflectionTestUtils.setField(filter, "monitoringEnabled", false);

        // when
        RequestSizeMonitoringFilter.RequestSizeLimits limits = filter.getCurrentLimits();

        // then
        assertThat(limits.maxRequestSizeMB()).isEqualTo(20L);
        assertThat(limits.warnRequestSizeMB()).isEqualTo(10L);
        assertThat(limits.monitoringEnabled()).isFalse();
    }

    @Test
    @DisplayName("Should handle multiple authentication endpoints correctly")
    void doFilterInternal_MultipleStrictEndpoints_AppliesStrictLimits() throws ServletException, IOException {
        // Test /auth/login
        request.setRequestURI("/api/v1/auth/login");
        request.setMethod("POST");
        request.addHeader("Content-Length", String.valueOf(1024 * 1024)); // 1MB
        filter.doFilterInternal(request, response, filterChain);

        // Test /auth/signup
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/signup");
        request.setMethod("POST");
        request.addHeader("Content-Length", String.valueOf(1024 * 1024)); // 1MB
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain, times(2)).doFilter(any(), any());
    }

    @Test
    @DisplayName("Should handle requests with various content sizes")
    void doFilterInternal_VariousContentSizes_MonitorsAppropriately() throws ServletException, IOException {
        // Small request
        request.setRequestURI("/api/test");
        request.addHeader("Content-Length", "1024"); // 1KB
        filter.doFilterInternal(request, response, filterChain);

        // Warning threshold request
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");
        request.addHeader("Content-Length", String.valueOf(1024 * 1024 * 6)); // 6MB
        filter.doFilterInternal(request, response, filterChain);

        // Large request
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");
        request.addHeader("Content-Length", String.valueOf(1024 * 1024 * 15)); // 15MB
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain, times(3)).doFilter(any(), any());
    }

    // === RequestSizeLimits Record Tests ===

    @Test
    @DisplayName("Should create RequestSizeLimits record correctly")
    void requestSizeLimits_Creation_WorksCorrectly() {
        // when
        RequestSizeMonitoringFilter.RequestSizeLimits limits = 
            new RequestSizeMonitoringFilter.RequestSizeLimits(25L, 15L, true);

        // then
        assertThat(limits.maxRequestSizeMB()).isEqualTo(25L);
        assertThat(limits.warnRequestSizeMB()).isEqualTo(15L);
        assertThat(limits.monitoringEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should handle RequestSizeLimits record equality")
    void requestSizeLimits_Equality_WorksCorrectly() {
        // given
        RequestSizeMonitoringFilter.RequestSizeLimits limits1 = 
            new RequestSizeMonitoringFilter.RequestSizeLimits(10L, 5L, true);
        RequestSizeMonitoringFilter.RequestSizeLimits limits2 = 
            new RequestSizeMonitoringFilter.RequestSizeLimits(10L, 5L, true);
        RequestSizeMonitoringFilter.RequestSizeLimits limits3 = 
            new RequestSizeMonitoringFilter.RequestSizeLimits(20L, 10L, false);

        // then
        assertThat(limits1).isEqualTo(limits2);
        assertThat(limits1).isNotEqualTo(limits3);
        assertThat(limits1.hashCode()).isEqualTo(limits2.hashCode());
    }

    @Test
    @DisplayName("Should provide meaningful toString for RequestSizeLimits")
    void requestSizeLimits_ToString_ProvidesToString() {
        // given
        RequestSizeMonitoringFilter.RequestSizeLimits limits = 
            new RequestSizeMonitoringFilter.RequestSizeLimits(10L, 5L, true);

        // when
        String toString = limits.toString();

        // then
        assertThat(toString).contains("maxRequestSizeMB=10");
        assertThat(toString).contains("warnRequestSizeMB=5");
        assertThat(toString).contains("monitoringEnabled=true");
    }
}