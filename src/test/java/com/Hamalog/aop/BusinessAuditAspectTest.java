package com.Hamalog.aop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.Hamalog.logging.StructuredLogger;
import com.Hamalog.logging.events.AuditEvent;
import com.Hamalog.logging.events.SecurityEvent;
import com.Hamalog.security.SecurityContextUtils;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

@ExtendWith(MockitoExtension.class)
@DisplayName("BusinessAuditAspect Tests")
class BusinessAuditAspectTest {

    @Mock
    private StructuredLogger structuredLogger;
    
    @Mock
    private SecurityContextUtils securityContextUtils;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;
    
    @Mock
    private JoinPoint joinPoint;
    
    @Mock
    private MethodSignature methodSignature;
    
    @Mock
    private Method method;
    
    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private BusinessAuditAspect businessAuditAspect;

    @BeforeEach
    void setUp() {
        MDC.clear();
        
        // Setup basic method signature mocks
        lenient().when(methodSignature.getName()).thenReturn("testMethod");
        lenient().when(methodSignature.getDeclaringType()).thenReturn(BusinessAuditAspectTest.class);
        lenient().when(methodSignature.getParameterNames()).thenReturn(new String[]{"param1", "param2"});

        // Setup SecurityContextUtils mocks
        lenient().when(securityContextUtils.getCurrentUserId()).thenReturn("testUser");
        lenient().when(securityContextUtils.getClientIpAddress()).thenReturn("127.0.0.1");
        lenient().when(securityContextUtils.getCurrentUserAgent()).thenReturn("Mozilla/5.0 TestBrowser");
    }

    @Test
    @DisplayName("Should audit successful auth operation")
    void auditAuthOperation_WithSuccessfulOperation_ShouldLogSuccess() throws Throwable {
        // given
        String expectedResult = "success";
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getName()).thenReturn("registerMember");
        when(proceedingJoinPoint.proceed()).thenReturn(expectedResult);

        // when
        Object result = businessAuditAspect.auditAuthOperation(proceedingJoinPoint);

        // then
        assertThat(result).isEqualTo(expectedResult);
        verify(structuredLogger).audit(any(AuditEvent.class));
        verify(proceedingJoinPoint).proceed();
    }

    @Test
    @DisplayName("Should audit failed auth operation")
    void auditAuthOperation_WithFailedOperation_ShouldLogFailure() throws Throwable {
        // given
        RuntimeException expectedException = new RuntimeException("Test error");
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getName()).thenReturn("registerMember");
        when(proceedingJoinPoint.proceed()).thenThrow(expectedException);

        // when & then
        assertThatThrownBy(() -> businessAuditAspect.auditAuthOperation(proceedingJoinPoint))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Test error");

        verify(structuredLogger).audit(any(AuditEvent.class));
        verify(proceedingJoinPoint).proceed();
    }

    @Test
    @DisplayName("Should audit login success")
    void auditLoginSuccess_WithValidLogin_ShouldLogSecurityEvent() {
        // given
        Object loginResult = "loginSuccess";
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{"testUser", "password"});

        // when
        businessAuditAspect.auditLoginSuccess(joinPoint, loginResult);

        // then
        verify(structuredLogger).security(any(SecurityEvent.class));
    }

    @Test
    @DisplayName("Should audit login failure")
    void auditLoginFailure_WithFailedException_ShouldLogSecurityEvent() {
        // given
        Exception loginException = new RuntimeException("Invalid credentials");
        when(joinPoint.getArgs()).thenReturn(new Object[]{"testUser", "wrongPassword"});

        // when
        businessAuditAspect.auditLoginFailure(joinPoint, loginException);

        // then
        verify(structuredLogger).security(any(SecurityEvent.class));
    }

    @Test
    @DisplayName("Should determine CREATE operation type correctly")
    void auditAuthOperation_WithCreateMethod_ShouldIdentifyAsCreate() throws Throwable {
        // given
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getName()).thenReturn("createMember");
        when(proceedingJoinPoint.proceed()).thenReturn("created");

        // when
        businessAuditAspect.auditAuthOperation(proceedingJoinPoint);

        // then
        verify(structuredLogger).audit(argThat(event ->
            event.getOperation().equals("CREATE")
        ));
    }

    @Test
    @DisplayName("Should determine DELETE operation type correctly")
    void auditAuthOperation_WithDeleteMethod_ShouldIdentifyAsDelete() throws Throwable {
        // given
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getName()).thenReturn("deleteMember");
        when(proceedingJoinPoint.proceed()).thenReturn(null);

        // when
        businessAuditAspect.auditAuthOperation(proceedingJoinPoint);

        // then
        verify(structuredLogger).audit(argThat(event ->
            event.getOperation().equals("DELETE")
        ));
    }

    @Test
    @DisplayName("Should determine LOGIN operation type correctly")
    void auditAuthOperation_WithLoginMethod_ShouldIdentifyAsLogin() throws Throwable {
        // given
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getName()).thenReturn("authenticateAndGenerateToken");
        when(proceedingJoinPoint.proceed()).thenReturn("token");

        // when
        businessAuditAspect.auditAuthOperation(proceedingJoinPoint);

        // then
        verify(structuredLogger).audit(argThat(event ->
            event.getOperation().equals("LOGIN")
        ));
    }
}