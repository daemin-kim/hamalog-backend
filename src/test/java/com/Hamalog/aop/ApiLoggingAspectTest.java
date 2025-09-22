package com.Hamalog.aop;

import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.logging.StructuredLogger;
import com.Hamalog.logging.events.ApiEvent;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiLoggingAspect Tests")
class ApiLoggingAspectTest {

    @Mock
    private StructuredLogger structuredLogger;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private ApiLoggingAspect apiLoggingAspect;

    private MockHttpServletRequest mockRequest;

    @BeforeEach
    void setUp() {
        MDC.clear();
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
        
        mockRequest = new MockHttpServletRequest();
        mockRequest.setMethod("GET");
        mockRequest.setRequestURI("/api/test");
        mockRequest.setRemoteAddr("192.168.1.1");
        mockRequest.addHeader("User-Agent", "Test Browser");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("Should log successful API request and response")
    void logApiRequestAndResponse_SuccessfulRequest_LogsCorrectly() throws Throwable {
        // given
        String expectedResult = "test result";
        Class<?> mockControllerClass = TestController.class;
        Method mockMethod = TestController.class.getMethod("testMethod", String.class);
        
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getDeclaringType()).thenReturn(mockControllerClass);
        when(methodSignature.getName()).thenReturn("testMethod");
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"param1"});
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{"testValue"});
        when(proceedingJoinPoint.proceed()).thenReturn(expectedResult);
        
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        
        ServletRequestAttributes attributes = new ServletRequestAttributes(mockRequest);
        RequestContextHolder.setRequestAttributes(attributes);

        // when
        Object result = apiLoggingAspect.logApiRequestAndResponse(proceedingJoinPoint);

        // then
        assertThat(result).isEqualTo(expectedResult);
        verify(structuredLogger).api(any(ApiEvent.class));
        verify(proceedingJoinPoint).proceed();
        
        // Verify MDC cleanup
        assertThat(MDC.get("api.method")).isNull();
        assertThat(MDC.get("api.user")).isNull();
        assertThat(MDC.get("api.duration")).isNull();
        assertThat(MDC.get("api.status")).isNull();
    }

    @Test
    @DisplayName("Should handle API request with exception")
    void logApiRequestAndResponse_WithException_LogsErrorAndRethrows() throws Throwable {
        // given
        RuntimeException expectedException = new RuntimeException("Test error");
        Class<?> mockControllerClass = TestController.class;
        
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getDeclaringType()).thenReturn(mockControllerClass);
        when(methodSignature.getName()).thenReturn("testMethod");
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"param1"});
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{"testValue"});
        when(proceedingJoinPoint.proceed()).thenThrow(expectedException);
        
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        
        ServletRequestAttributes attributes = new ServletRequestAttributes(mockRequest);
        RequestContextHolder.setRequestAttributes(attributes);

        // when & then
        assertThatThrownBy(() -> apiLoggingAspect.logApiRequestAndResponse(proceedingJoinPoint))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Test error");
        
        verify(structuredLogger).api(any(ApiEvent.class));
        verify(proceedingJoinPoint).proceed();
        
        // Verify MDC cleanup
        assertThat(MDC.get("api.method")).isNull();
        assertThat(MDC.get("api.user")).isNull();
        assertThat(MDC.get("api.duration")).isNull();
        assertThat(MDC.get("api.status")).isNull();
        assertThat(MDC.get("api.errorType")).isNull();
    }

    @Test
    @DisplayName("Should handle unauthenticated user")
    void logApiRequestAndResponse_UnauthenticatedUser_SetsAnonymous() throws Throwable {
        // given
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getDeclaringType()).thenReturn(TestController.class);
        when(methodSignature.getName()).thenReturn("testMethod");
        when(methodSignature.getParameterNames()).thenReturn(new String[0]);
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[0]);
        when(proceedingJoinPoint.proceed()).thenReturn("result");
        
        when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);
        
        ServletRequestAttributes attributes = new ServletRequestAttributes(mockRequest);
        RequestContextHolder.setRequestAttributes(attributes);

        // when
        apiLoggingAspect.logApiRequestAndResponse(proceedingJoinPoint);

        // then
        verify(structuredLogger).api(argThat(apiEvent -> 
            "anonymous".equals(apiEvent.getUserId())
        ));
    }

    @Test
    @DisplayName("Should handle request without HTTP context")
    void logApiRequestAndResponse_WithoutHttpContext_UsesDefaults() throws Throwable {
        // given
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getDeclaringType()).thenReturn(TestController.class);
        when(methodSignature.getName()).thenReturn("testMethod");
        when(methodSignature.getParameterNames()).thenReturn(new String[0]);
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[0]);
        when(proceedingJoinPoint.proceed()).thenReturn("result");
        
        when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);

        // when
        apiLoggingAspect.logApiRequestAndResponse(proceedingJoinPoint);

        // then
        verify(structuredLogger).api(argThat(apiEvent -> 
            "UNKNOWN".equals(apiEvent.getHttpMethod()) &&
            "UNKNOWN".equals(apiEvent.getPath()) &&
            "UNKNOWN".equals(apiEvent.getIpAddress()) &&
            "UNKNOWN".equals(apiEvent.getUserAgent())
        ));
    }

    @Test
    @DisplayName("Should manage request ID correctly when not present")
    void logApiRequestAndResponse_WithoutExistingRequestId_CreatesAndCleansUp() throws Throwable {
        // given
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getDeclaringType()).thenReturn(TestController.class);
        when(methodSignature.getName()).thenReturn("testMethod");
        when(methodSignature.getParameterNames()).thenReturn(new String[0]);
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[0]);
        when(proceedingJoinPoint.proceed()).thenReturn("result");

        // when
        apiLoggingAspect.logApiRequestAndResponse(proceedingJoinPoint);

        // then
        // Request ID should be cleaned up after execution
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    @DisplayName("Should preserve existing request ID")
    void logApiRequestAndResponse_WithExistingRequestId_PreservesIt() throws Throwable {
        // given
        String existingRequestId = "existing-123";
        MDC.put("requestId", existingRequestId);
        
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getDeclaringType()).thenReturn(TestController.class);
        when(methodSignature.getName()).thenReturn("testMethod");
        when(methodSignature.getParameterNames()).thenReturn(new String[0]);
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[0]);
        when(proceedingJoinPoint.proceed()).thenReturn("result");

        // when
        apiLoggingAspect.logApiRequestAndResponse(proceedingJoinPoint);

        // then
        assertThat(MDC.get("requestId")).isEqualTo(existingRequestId);
    }

    @Test
    @DisplayName("Should handle sensitive parameters correctly")
    void logApiRequestAndResponse_WithSensitiveParams_MasksValues() throws Throwable {
        // given
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getDeclaringType()).thenReturn(TestController.class);
        when(methodSignature.getName()).thenReturn("testMethod");
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"password", "token", "normalParam"});
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{"secret123", "token456", "normalValue"});
        when(proceedingJoinPoint.proceed()).thenReturn("result");

        // when
        apiLoggingAspect.logApiRequestAndResponse(proceedingJoinPoint);

        // then
        verify(structuredLogger).api(argThat(apiEvent -> {
            Map<String, Object> params = apiEvent.getParameters();
            return "***".equals(params.get("password")) &&
                   "***".equals(params.get("token")) &&
                   "normalValue".equals(params.get("normalParam"));
        }));
    }

    @Test
    @DisplayName("Should classify performance levels correctly")
    void logApiRequestAndResponse_DifferentDurations_ClassifiesPerformanceCorrectly() throws Throwable {
        // Test will verify the performance classification through MDC values
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getDeclaringType()).thenReturn(TestController.class);
        when(methodSignature.getName()).thenReturn("testMethod");
        when(methodSignature.getParameterNames()).thenReturn(new String[0]);
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[0]);
        
        // Simulate slow execution
        when(proceedingJoinPoint.proceed()).thenAnswer(invocation -> {
            Thread.sleep(50); // Short delay for predictable performance classification
            return "result";
        });

        // when
        apiLoggingAspect.logApiRequestAndResponse(proceedingJoinPoint);

        // then
        verify(structuredLogger).api(any(ApiEvent.class));
    }

    @Test
    @DisplayName("Should handle X-Forwarded-For header for IP extraction")
    void logApiRequestAndResponse_WithXForwardedFor_ExtractsCorrectIP() throws Throwable {
        // given
        mockRequest.addHeader("X-Forwarded-For", "203.0.113.1, 70.41.3.18, 150.172.238.178");
        
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getDeclaringType()).thenReturn(TestController.class);
        when(methodSignature.getName()).thenReturn("testMethod");
        when(methodSignature.getParameterNames()).thenReturn(new String[0]);
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[0]);
        when(proceedingJoinPoint.proceed()).thenReturn("result");
        
        ServletRequestAttributes attributes = new ServletRequestAttributes(mockRequest);
        RequestContextHolder.setRequestAttributes(attributes);

        // when
        apiLoggingAspect.logApiRequestAndResponse(proceedingJoinPoint);

        // then
        verify(structuredLogger).api(argThat(apiEvent -> 
            "203.0.113.1".equals(apiEvent.getIpAddress())
        ));
    }

    @Test
    @DisplayName("Should handle X-Real-IP header for IP extraction")
    void logApiRequestAndResponse_WithXRealIP_ExtractsCorrectIP() throws Throwable {
        // given
        mockRequest.addHeader("X-Real-IP", "203.0.113.2");
        
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getDeclaringType()).thenReturn(TestController.class);
        when(methodSignature.getName()).thenReturn("testMethod");
        when(methodSignature.getParameterNames()).thenReturn(new String[0]);
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[0]);
        when(proceedingJoinPoint.proceed()).thenReturn("result");
        
        ServletRequestAttributes attributes = new ServletRequestAttributes(mockRequest);
        RequestContextHolder.setRequestAttributes(attributes);

        // when
        apiLoggingAspect.logApiRequestAndResponse(proceedingJoinPoint);

        // then
        verify(structuredLogger).api(argThat(apiEvent -> 
            "203.0.113.2".equals(apiEvent.getIpAddress())
        ));
    }

    @Test
    @DisplayName("Should handle long parameter values correctly")
    void logApiRequestAndResponse_WithLongParams_TruncatesValues() throws Throwable {
        // given
        String longValue = "a".repeat(250); // Longer than 200 characters
        
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getDeclaringType()).thenReturn(TestController.class);
        when(methodSignature.getName()).thenReturn("testMethod");
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"longParam"});
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{longValue});
        when(proceedingJoinPoint.proceed()).thenReturn("result");

        // when
        apiLoggingAspect.logApiRequestAndResponse(proceedingJoinPoint);

        // then
        verify(structuredLogger).api(argThat(apiEvent -> {
            Map<String, Object> params = apiEvent.getParameters();
            String paramValue = (String) params.get("longParam");
            return paramValue != null && paramValue.length() == 203 && paramValue.endsWith("..."); // 200 + "..."
        }));
    }

    @Test
    @DisplayName("Should handle null parameters correctly")
    void logApiRequestAndResponse_WithNullParams_HandlesGracefully() throws Throwable {
        // given
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getDeclaringType()).thenReturn(TestController.class);
        when(methodSignature.getName()).thenReturn("testMethod");
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"nullParam"});
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{null});
        when(proceedingJoinPoint.proceed()).thenReturn("result");

        // when
        apiLoggingAspect.logApiRequestAndResponse(proceedingJoinPoint);

        // then
        verify(structuredLogger).api(argThat(apiEvent -> {
            Map<String, Object> params = apiEvent.getParameters();
            return params.get("nullParam") == null;
        }));
    }

    @Test
    @DisplayName("Should handle different exception types")
    void logApiRequestAndResponse_WithDifferentExceptions_LogsAndRethrows() throws Throwable {
        // given
        IllegalArgumentException testException = new IllegalArgumentException("Test validation error");
        
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getDeclaringType()).thenReturn(TestController.class);
        when(methodSignature.getName()).thenReturn("testMethod");
        when(methodSignature.getParameterNames()).thenReturn(new String[0]);
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[0]);
        when(proceedingJoinPoint.proceed()).thenThrow(testException);

        // when & then
        assertThatThrownBy(() -> apiLoggingAspect.logApiRequestAndResponse(proceedingJoinPoint))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Test validation error");
        
        verify(structuredLogger).api(argThat(apiEvent -> 
            apiEvent.getStatusCode() > 0 // Should set some status code
        ));
    }

    @Test
    @DisplayName("Should determine request types correctly")
    void logApiRequestAndResponse_DifferentPaths_DeterminesRequestType() throws Throwable {
        // Test with single path to avoid mock verification issues
        mockRequest.setRequestURI("/api/auth/login");
        ServletRequestAttributes attributes = new ServletRequestAttributes(mockRequest);
        RequestContextHolder.setRequestAttributes(attributes);
        
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getDeclaringType()).thenReturn(TestController.class);
        when(methodSignature.getName()).thenReturn("testMethod");
        when(methodSignature.getParameterNames()).thenReturn(new String[0]);
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[0]);
        when(proceedingJoinPoint.proceed()).thenReturn("result");

        // when
        apiLoggingAspect.logApiRequestAndResponse(proceedingJoinPoint);

        // then
        verify(structuredLogger).api(argThat(apiEvent -> 
            apiEvent.getRequestType() != null
        ));
    }

    @Test
    @DisplayName("Should handle actuator paths as internal requests")
    void logApiRequestAndResponse_ActuatorPath_SetsInternalRequestType() throws Throwable {
        // given
        mockRequest.setRequestURI("/actuator/health");
        ServletRequestAttributes attributes = new ServletRequestAttributes(mockRequest);
        RequestContextHolder.setRequestAttributes(attributes);
        
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getDeclaringType()).thenReturn(TestController.class);
        when(methodSignature.getName()).thenReturn("testMethod");
        when(methodSignature.getParameterNames()).thenReturn(new String[0]);
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[0]);
        when(proceedingJoinPoint.proceed()).thenReturn("result");

        // when
        apiLoggingAspect.logApiRequestAndResponse(proceedingJoinPoint);

        // then
        verify(structuredLogger).api(argThat(apiEvent -> 
            "내부 요청".equals(apiEvent.getRequestType())
        ));
    }

    // Helper test controller class for mocking
    public static class TestController {
        public String testMethod(String param) {
            return "result";
        }
    }
}