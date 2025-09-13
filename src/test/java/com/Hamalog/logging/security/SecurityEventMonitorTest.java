package com.Hamalog.logging.security;

import com.Hamalog.logging.MDCUtil;
import com.Hamalog.logging.StructuredLogger;
import com.Hamalog.logging.events.SecurityEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityEventMonitor Tests")
class SecurityEventMonitorTest {

    @Mock
    private StructuredLogger structuredLogger;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private SecurityEventMonitor securityEventMonitor;

    private MockHttpServletRequest mockRequest;

    @BeforeEach
    void setUp() {
        // Setup request context
        mockRequest = new MockHttpServletRequest();
        mockRequest.setRemoteAddr("192.168.1.100");
        mockRequest.addHeader("User-Agent", "Test-Agent/1.0");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequest));

        // Setup MDC
        MDC.put(MDCUtil.CORRELATION_ID, "test-correlation-id");
        MDC.put(MDCUtil.IP_ADDRESS, "192.168.1.100");
        MDC.put(MDCUtil.USER_AGENT, "Test-Agent/1.0");

        // Setup Redis mock with lenient stubbing to avoid unnecessary stubbing warnings
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        // Mock Redis increment to return sequential values for failure tracking
        lenient().when(valueOperations.increment(anyString()))
            .thenReturn(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 13L, 14L, 15L);
        lenient().when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
    }

    @Test
    @DisplayName("Should handle authentication success event")
    void handleAuthenticationSuccess_ValidEvent_LogsAndClearsFailures() {
        // given
        Authentication auth = new UsernamePasswordAuthenticationToken("testUser", "password");
        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(auth);

        // when
        securityEventMonitor.handleAuthenticationSuccess(event);

        // then
        ArgumentCaptor<SecurityEvent> eventCaptor = ArgumentCaptor.forClass(SecurityEvent.class);
        verify(structuredLogger).security(eventCaptor.capture());

        SecurityEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getEventType()).isEqualTo("AUTHENTICATION_SUCCESS");
        assertThat(capturedEvent.getUserId()).isEqualTo("testUser");
        assertThat(capturedEvent.getIpAddress()).isEqualTo("192.168.1.100");
        assertThat(capturedEvent.getUserAgent()).isEqualTo("Test-Agent/1.0");
        assertThat(capturedEvent.getResult()).isEqualTo("SUCCESS");
        assertThat(capturedEvent.getRiskLevel()).isEqualTo("LOW");
    }

    @Test
    @DisplayName("Should handle authentication failure event and track failures")
    void handleAuthenticationFailure_FirstFailure_LogsWithLowRisk() {
        // given
        Authentication auth = new UsernamePasswordAuthenticationToken("testUser", "wrongPassword");
        AuthenticationFailureBadCredentialsEvent event = new AuthenticationFailureBadCredentialsEvent(
            auth, new BadCredentialsException("Bad credentials"));

        // when
        securityEventMonitor.handleAuthenticationFailure(event);

        // then
        ArgumentCaptor<SecurityEvent> eventCaptor = ArgumentCaptor.forClass(SecurityEvent.class);
        verify(structuredLogger).security(eventCaptor.capture());

        SecurityEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getEventType()).isEqualTo("AUTHENTICATION_FAILURE");
        assertThat(capturedEvent.getUserId()).isEqualTo("testUser");
        assertThat(capturedEvent.getResult()).isEqualTo("FAILURE");
        assertThat(capturedEvent.getRiskLevel()).isEqualTo("LOW");
        assertThat(capturedEvent.getDetails()).contains("BadCredentialsException").contains("Attempt 1");
    }

    @Test
    @DisplayName("Should escalate risk level on multiple authentication failures")
    void handleAuthenticationFailure_MultipleFailures_EscalatesRiskLevel() {
        // given
        Authentication auth = new UsernamePasswordAuthenticationToken("testUser", "wrongPassword");
        AuthenticationFailureBadCredentialsEvent event = new AuthenticationFailureBadCredentialsEvent(
            auth, new BadCredentialsException("Bad credentials"));

        // when - simulate multiple failures by calling the method multiple times
        for (int i = 0; i < 5; i++) {
            securityEventMonitor.handleAuthenticationFailure(event);
        }

        // then - verify that the risk level escalates to HIGH after multiple failures
        ArgumentCaptor<SecurityEvent> eventCaptor = ArgumentCaptor.forClass(SecurityEvent.class);
        verify(structuredLogger, times(5)).security(eventCaptor.capture());

        SecurityEvent lastEvent = eventCaptor.getValue();
        assertThat(lastEvent.getRiskLevel()).isIn("MEDIUM", "HIGH"); // Should escalate from LOW
        assertThat(lastEvent.getDetails()).contains("Attempt");
    }

    @Test
    @DisplayName("Should detect brute force attack on multiple failures")
    void handleAuthenticationFailure_BruteForce_DetectsAndLogs() {
        // given
        Authentication auth = new UsernamePasswordAuthenticationToken("testUser", "wrongPassword");
        AuthenticationFailureBadCredentialsEvent event = new AuthenticationFailureBadCredentialsEvent(
            auth, new BadCredentialsException("Bad credentials"));

        // when - simulate brute force by calling the method 11 times (above threshold of 10)
        for (int i = 0; i < 11; i++) {
            securityEventMonitor.handleAuthenticationFailure(event);
        }

        // then - verify that the risk level escalates to CRITICAL for brute force
        // Note: 11 authentication failures + 2 brute force detection events = 13 total
        ArgumentCaptor<SecurityEvent> eventCaptor = ArgumentCaptor.forClass(SecurityEvent.class);
        verify(structuredLogger, times(13)).security(eventCaptor.capture());

        SecurityEvent lastEvent = eventCaptor.getValue();
        assertThat(lastEvent.getRiskLevel()).isEqualTo("CRITICAL"); // Should be CRITICAL for brute force
        assertThat(lastEvent.getDetails()).contains("Brute force attack detected");
    }

    @Test
    @DisplayName("Should handle authorization denied event")
    void handleAuthorizationDenied_ValidEvent_LogsAndTracksSuspicious() {
        // given
        Authentication auth = new UsernamePasswordAuthenticationToken("testUser", "password");
        Supplier<Authentication> authSupplier = () -> auth;
        AuthorizationDeniedEvent event = new AuthorizationDeniedEvent(authSupplier, "/admin/users", null);

        // when
        securityEventMonitor.handleAuthorizationDenied(event);

        // then
        ArgumentCaptor<SecurityEvent> eventCaptor = ArgumentCaptor.forClass(SecurityEvent.class);
        verify(structuredLogger).security(eventCaptor.capture());

        SecurityEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getEventType()).isEqualTo("AUTHORIZATION_DENIED");
        assertThat(capturedEvent.getUserId()).isEqualTo("testUser");
        assertThat(capturedEvent.getResult()).isEqualTo("DENIED");
        assertThat(capturedEvent.getRiskLevel()).isEqualTo("MEDIUM");
        assertThat(capturedEvent.getResource()).contains("/admin/users");
    }


    @Test
    @DisplayName("Should handle authentication success with exception gracefully")
    void handleAuthenticationSuccess_WithException_HandlesGracefully() {
        // given
        Authentication auth = new UsernamePasswordAuthenticationToken("testUser", "password");
        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(auth);
        doThrow(new RuntimeException("Logger error")).when(structuredLogger).security(any());

        // when & then - should not throw exception
        securityEventMonitor.handleAuthenticationSuccess(event);
    }

    @Test
    @DisplayName("Should handle authorization denied with null authentication")
    void handleAuthorizationDenied_NullAuth_HandlesGracefully() {
        // given
        Supplier<Authentication> authSupplier = () -> null;
        AuthorizationDeniedEvent event = new AuthorizationDeniedEvent(authSupplier, "/admin/users", null);

        // when
        securityEventMonitor.handleAuthorizationDenied(event);

        // then
        ArgumentCaptor<SecurityEvent> eventCaptor = ArgumentCaptor.forClass(SecurityEvent.class);
        verify(structuredLogger).security(eventCaptor.capture());

        SecurityEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getUserId()).isEqualTo("unknown");
    }

}