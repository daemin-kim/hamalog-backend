package com.Hamalog.aop;

import com.Hamalog.logging.StructuredLogger;
import com.Hamalog.logging.events.AuditEvent;
import com.Hamalog.logging.events.SecurityEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BusinessAuditAspect Tests")
class BusinessAuditAspectTest {

    @Mock
    private ObjectMapper objectMapper;
    
    @Mock
    private StructuredLogger structuredLogger;
    
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
    
    @Mock
    private ServletRequestAttributes requestAttributes;
    
    @Mock
    private SecurityContext securityContext;
    
    @Mock
    private Authentication authentication;

    @InjectMocks
    private BusinessAuditAspect businessAuditAspect;

    @BeforeEach
    void setUp() {
        MDC.clear();
        
        // Setup basic method signature mocks
        lenient().when(methodSignature.getName()).thenReturn("testMethod");
        lenient().when(methodSignature.getDeclaringType()).thenReturn(BusinessAuditAspectTest.class);
        lenient().when(methodSignature.getParameterNames()).thenReturn(new String[]{"param1", "param2"});
    }

    @Test
    @DisplayName("Should audit successful business operation")
    void auditBusinessOperation_WithSuccessfulOperation_ShouldLogSuccess() throws Throwable {
        // given
        String expectedResult = "success";
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        lenient().when(methodSignature.getMethod()).thenReturn(method);
        lenient().when(method.getName()).thenReturn("createUser");
        when(methodSignature.getName()).thenReturn("createUser");
        when(proceedingJoinPoint.proceed()).thenReturn(expectedResult);
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{"testArg"});

        try (MockedStatic<SecurityContextHolder> securityMock = mockStatic(SecurityContextHolder.class);
             MockedStatic<RequestContextHolder> requestMock = mockStatic(RequestContextHolder.class)) {
            
            setupSecurityContext(securityMock, "testUser");
            setupRequestContext(requestMock);

            // when
            Object result = businessAuditAspect.auditBusinessOperation(proceedingJoinPoint);

            // then
            assertThat(result).isEqualTo(expectedResult);
            verify(structuredLogger).audit(any(AuditEvent.class));
            verify(proceedingJoinPoint).proceed();
        }
    }

    @Test
    @DisplayName("Should audit failed business operation")
    void auditBusinessOperation_WithFailedOperation_ShouldLogFailure() throws Throwable {
        // given
        RuntimeException expectedException = new RuntimeException("Test error");
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        lenient().when(methodSignature.getMethod()).thenReturn(method);
        lenient().when(method.getName()).thenReturn("createUser");
        when(methodSignature.getName()).thenReturn("createUser");
        when(proceedingJoinPoint.proceed()).thenThrow(expectedException);
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{"testArg"});

        try (MockedStatic<SecurityContextHolder> securityMock = mockStatic(SecurityContextHolder.class);
             MockedStatic<RequestContextHolder> requestMock = mockStatic(RequestContextHolder.class)) {
            
            setupSecurityContext(securityMock, "testUser");
            setupRequestContext(requestMock);

            // when & then
            assertThatThrownBy(() -> businessAuditAspect.auditBusinessOperation(proceedingJoinPoint))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Test error");
            
            verify(structuredLogger).audit(any(AuditEvent.class));
            verify(proceedingJoinPoint).proceed();
        }
    }

    @Test
    @DisplayName("Should audit login success")
    void auditLoginSuccess_WithValidLogin_ShouldLogSecurityEvent() {
        // given
        Object loginResult = "loginSuccess";
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{"testUser", "password"});

        try (MockedStatic<SecurityContextHolder> securityMock = mockStatic(SecurityContextHolder.class);
             MockedStatic<RequestContextHolder> requestMock = mockStatic(RequestContextHolder.class)) {
            
            setupSecurityContext(securityMock, "testUser");
            setupRequestContext(requestMock);

            // when
            businessAuditAspect.auditLoginSuccess(joinPoint, loginResult);

            // then
            verify(structuredLogger).security(any(SecurityEvent.class));
        }
    }

    @Test
    @DisplayName("Should audit login failure")
    void auditLoginFailure_WithFailedException_ShouldLogSecurityEvent() {
        // given
        Exception loginException = new RuntimeException("Invalid credentials");
        when(joinPoint.getArgs()).thenReturn(new Object[]{"testUser", "wrongPassword"});

        try (MockedStatic<SecurityContextHolder> securityMock = mockStatic(SecurityContextHolder.class);
             MockedStatic<RequestContextHolder> requestMock = mockStatic(RequestContextHolder.class)) {
            
            setupSecurityContext(securityMock, null); // No authenticated user on failure
            setupRequestContext(requestMock);

            // when
            businessAuditAspect.auditLoginFailure(joinPoint, loginException);

            // then
            verify(structuredLogger).security(any(SecurityEvent.class));
        }
    }

    @Test
    @DisplayName("Should audit medication record changes")
    void auditMedicationRecordChanges_WithValidOperation_ShouldDelegate() throws Throwable {
        // given
        String expectedResult = "recordUpdated";
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        lenient().when(methodSignature.getMethod()).thenReturn(method);
        lenient().when(method.getName()).thenReturn("updateMedicationRecord");
        when(proceedingJoinPoint.proceed()).thenReturn(expectedResult);
        lenient().when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{1L, "updateData"});

        try (MockedStatic<SecurityContextHolder> securityMock = mockStatic(SecurityContextHolder.class);
             MockedStatic<RequestContextHolder> requestMock = mockStatic(RequestContextHolder.class)) {
            
            setupSecurityContext(securityMock, "testUser");
            setupRequestContext(requestMock);

            // when
            Object result = businessAuditAspect.auditMedicationRecordChanges(proceedingJoinPoint);

            // then
            assertThat(result).isEqualTo(expectedResult);
        }
    }

    @Test
    @DisplayName("Should audit medication record updates")
    void auditMedicationRecordUpdates_WithValidOperation_ShouldDelegate() throws Throwable {
        // given
        String expectedResult = "recordUpdated";
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        lenient().when(methodSignature.getMethod()).thenReturn(method);
        lenient().when(method.getName()).thenReturn("updateMedicationRecord");
        when(proceedingJoinPoint.proceed()).thenReturn(expectedResult);
        lenient().when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{1L, "updateData"});

        try (MockedStatic<SecurityContextHolder> securityMock = mockStatic(SecurityContextHolder.class);
             MockedStatic<RequestContextHolder> requestMock = mockStatic(RequestContextHolder.class)) {
            
            setupSecurityContext(securityMock, "testUser");
            setupRequestContext(requestMock);

            // when
            Object result = businessAuditAspect.auditMedicationRecordUpdates(proceedingJoinPoint);

            // then
            assertThat(result).isEqualTo(expectedResult);
        }
    }

    @Test
    @DisplayName("Should audit medication record deletes")
    void auditMedicationRecordDeletes_WithValidOperation_ShouldDelegate() throws Throwable {
        // given
        String expectedResult = "recordDeleted";
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        lenient().when(methodSignature.getMethod()).thenReturn(method);
        lenient().when(method.getName()).thenReturn("deleteMedicationRecord");
        when(proceedingJoinPoint.proceed()).thenReturn(expectedResult);
        lenient().when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{1L});

        try (MockedStatic<SecurityContextHolder> securityMock = mockStatic(SecurityContextHolder.class);
             MockedStatic<RequestContextHolder> requestMock = mockStatic(RequestContextHolder.class)) {
            
            setupSecurityContext(securityMock, "testUser");
            setupRequestContext(requestMock);

            // when
            Object result = businessAuditAspect.auditMedicationRecordDeletes(proceedingJoinPoint);

            // then
            assertThat(result).isEqualTo(expectedResult);
        }
    }

    private void setupSecurityContext(MockedStatic<SecurityContextHolder> securityMock, String username) {
        securityMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
        if (username != null) {
            lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
            lenient().when(authentication.getName()).thenReturn(username);
        } else {
            lenient().when(securityContext.getAuthentication()).thenReturn(null);
        }
    }

    private void setupRequestContext(MockedStatic<RequestContextHolder> requestMock) {
        requestMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributes);
        lenient().when(requestAttributes.getRequest()).thenReturn(request);
        lenient().when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 TestBrowser");
        lenient().when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        lenient().when(request.getHeader("X-Real-IP")).thenReturn(null);
        lenient().when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    }
}