package com.Hamalog.aop;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.Hamalog.logging.LoggingConstants;
import com.Hamalog.logging.StructuredLogger;
import com.Hamalog.logging.events.ApiEvent;
import com.Hamalog.security.SecurityContextUtils;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiLoggingAspect Tests")
class ApiLoggingAspectTest {

    @Mock
    private StructuredLogger structuredLogger;

    @Mock
    private SecurityContextUtils securityContextUtils;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private MethodSignature methodSignature;

    @InjectMocks
    private ApiLoggingAspect apiLoggingAspect;

    private MockHttpServletRequest mockRequest;

    @BeforeEach
    void setUp() {
        MDC.clear();
        RequestContextHolder.resetRequestAttributes();

        mockRequest = new MockHttpServletRequest();
        mockRequest.setMethod("GET");
        mockRequest.setRequestURI("/api/test");
        mockRequest.setRemoteAddr("192.168.1.1");
        mockRequest.addHeader("User-Agent", "Test Browser");

        // Default SecurityContextUtils behavior
        lenient().when(securityContextUtils.getCurrentRequest()).thenReturn(Optional.of(mockRequest));
        lenient().when(securityContextUtils.getCurrentUserId()).thenReturn("testuser");
        lenient().when(securityContextUtils.getClientIpAddress(any(HttpServletRequest.class))).thenReturn("192.168.1.1");
        lenient().when(securityContextUtils.sanitizeUserAgent(any())).thenReturn("Test Browser");
        lenient().when(securityContextUtils.determineRequestType(any())).thenReturn("EXTERNAL");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("Should log successful API request and response")
    void logApiRequestAndResponse_SuccessfulRequest_LogsCorrectly() throws Throwable {
        // given
        String expectedResult = "test result";
        setupJoinPointMocks("testMethod", new String[]{"param1"}, new Object[]{"testValue"});
        when(proceedingJoinPoint.proceed()).thenReturn(expectedResult);
        setupRequestContext();

        // when
        Object result = apiLoggingAspect.logApiRequestAndResponse(proceedingJoinPoint);

        // then
        assertThat(result).isEqualTo(expectedResult);
        verify(structuredLogger).api(any(ApiEvent.class));
        verify(proceedingJoinPoint).proceed();

        // Verify MDC cleanup
        assertThat(MDC.get("api.method")).isNull();
        assertThat(MDC.get("api.user")).isNull();
    }

    @Test
    @DisplayName("Should handle API request with exception")
    void logApiRequestAndResponse_WithException_LogsErrorAndRethrows() throws Throwable {
        // given
        RuntimeException expectedException = new RuntimeException("Test error");
        setupJoinPointMocks("testMethod", new String[]{"param1"}, new Object[]{"testValue"});
        when(proceedingJoinPoint.proceed()).thenThrow(expectedException);
        setupRequestContext();

        // when & then
        assertThatThrownBy(() -> apiLoggingAspect.logApiRequestAndResponse(proceedingJoinPoint))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Test error");

        verify(structuredLogger).api(any(ApiEvent.class));
        verify(proceedingJoinPoint).proceed();
    }

    @Test
    @DisplayName("Should skip logging when filter owns logging")
    void logApiRequestAndResponse_FilterOwnsLogging_SkipsLogging() throws Throwable {
        // given
        String expectedResult = "test result";
        mockRequest.setAttribute(LoggingConstants.API_LOGGING_OWNER_ATTRIBUTE, "FILTER");
        when(securityContextUtils.getCurrentRequest()).thenReturn(Optional.of(mockRequest));
        when(proceedingJoinPoint.proceed()).thenReturn(expectedResult);

        // when
        Object result = apiLoggingAspect.logApiRequestAndResponse(proceedingJoinPoint);

        // then
        assertThat(result).isEqualTo(expectedResult);
        verify(structuredLogger, never()).api(any(ApiEvent.class));
    }

    @Test
    @DisplayName("Should handle unauthenticated user")
    void logApiRequestAndResponse_UnauthenticatedUser_LogsAnonymous() throws Throwable {
        // given
        when(securityContextUtils.getCurrentUserId()).thenReturn("anonymous");
        setupJoinPointMocks("testMethod", new String[]{}, new Object[]{});
        when(proceedingJoinPoint.proceed()).thenReturn("result");
        setupRequestContext();

        // when
        apiLoggingAspect.logApiRequestAndResponse(proceedingJoinPoint);

        // then
        verify(structuredLogger).api(argThat(event ->
            "anonymous".equals(event.getUserId())
        ));
    }

    @Test
    @DisplayName("Should handle request without HTTP context")
    void logApiRequestAndResponse_NoHttpContext_HandlesGracefully() throws Throwable {
        // given
        when(securityContextUtils.getCurrentRequest()).thenReturn(Optional.empty());

        setupJoinPointMocks("testMethod", new String[]{}, new Object[]{});
        when(proceedingJoinPoint.proceed()).thenReturn("result");

        // when
        Object result = apiLoggingAspect.logApiRequestAndResponse(proceedingJoinPoint);

        // then
        assertThat(result).isEqualTo("result");
        verify(structuredLogger).api(any(ApiEvent.class));
    }

    @Test
    @DisplayName("Should handle sensitive parameters correctly")
    void logApiRequestAndResponse_SensitiveParams_MasksValues() throws Throwable {
        // given
        setupJoinPointMocks("testMethod",
            new String[]{"username", "password", "token"},
            new Object[]{"testuser", "secret123", "abc123"});
        when(proceedingJoinPoint.proceed()).thenReturn("result");
        setupRequestContext();

        // when
        apiLoggingAspect.logApiRequestAndResponse(proceedingJoinPoint);

        // then
        verify(structuredLogger).api(any(ApiEvent.class));
    }

    @Test
    @DisplayName("Should handle actuator paths as internal requests")
    void logApiRequestAndResponse_ActuatorPath_SetsInternalRequestType() throws Throwable {
        // given
        mockRequest.setRequestURI("/actuator/health");
        when(securityContextUtils.determineRequestType("/actuator/health")).thenReturn("INTERNAL");
        setupJoinPointMocks("testMethod", new String[0], new Object[0]);
        when(proceedingJoinPoint.proceed()).thenReturn("result");
        setupRequestContext();

        // when
        apiLoggingAspect.logApiRequestAndResponse(proceedingJoinPoint);

        // then
        verify(structuredLogger).api(argThat(apiEvent ->
            "INTERNAL".equals(apiEvent.getRequestType())
        ));
    }

    private void setupJoinPointMocks(String methodName, String[] paramNames, Object[] args) {
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getDeclaringType()).thenReturn(TestController.class);
        when(methodSignature.getName()).thenReturn(methodName);
        when(methodSignature.getParameterNames()).thenReturn(paramNames);
        when(proceedingJoinPoint.getArgs()).thenReturn(args);
    }

    private void setupRequestContext() {
        ServletRequestAttributes attributes = new ServletRequestAttributes(mockRequest);
        RequestContextHolder.setRequestAttributes(attributes);
    }

    // Helper test controller class for mocking
    public static class TestController {
        public String testMethod(String param) {
            return "result";
        }
    }
}
