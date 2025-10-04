package com.Hamalog.aop;

import com.Hamalog.logging.StructuredLogger;
import com.Hamalog.logging.events.PerformanceEvent;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
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
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PerformanceMonitoringAspect Tests")
class PerformanceMonitoringAspectTest {

    @Mock
    private StructuredLogger structuredLogger;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private PerformanceMonitoringAspect performanceAspect;

    @BeforeEach
    void setUp() {
        // Set up configuration values
        ReflectionTestUtils.setField(performanceAspect, "slowThreshold", 1000L);
        ReflectionTestUtils.setField(performanceAspect, "verySlowThreshold", 3000L);

        // Configure mock method signature
        lenient().when(joinPoint.getSignature()).thenReturn(methodSignature);
        lenient().when(methodSignature.getDeclaringType()).thenReturn((Class) TestService.class);
        lenient().when(methodSignature.getName()).thenReturn("testMethod");

        // Clear MDC and reset security context
        MDC.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should monitor successful method execution")
    void monitorPerformance_SuccessfulExecution_ShouldTrackPerformance() throws Throwable {
        // given
        String expectedResult = "success";
        when(joinPoint.proceed()).thenReturn(expectedResult);

        // when
        Object result = performanceAspect.monitorPerformance(joinPoint);

        // then
        assertThat(result).isEqualTo(expectedResult);
        
        // Verify structured logging was called
        verify(structuredLogger).performance(any(PerformanceEvent.class));
        
        // Verify method stats were updated
        PerformanceMonitoringAspect.MethodStats stats = performanceAspect.getMethodStats("TestService.testMethod");
        assertThat(stats).isNotNull();
        assertThat(stats.getTotalCalls()).isEqualTo(1);
        assertThat(stats.getSuccessCalls()).isEqualTo(1);
        assertThat(stats.getSuccessRate()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Should monitor failed method execution")
    void monitorPerformance_FailedExecution_ShouldTrackFailure() throws Throwable {
        // given
        RuntimeException exception = new RuntimeException("Test error");
        when(joinPoint.proceed()).thenThrow(exception);

        // when & then
        assertThatThrownBy(() -> performanceAspect.monitorPerformance(joinPoint))
                .isSameAs(exception);
        
        // Verify structured logging was called
        ArgumentCaptor<PerformanceEvent> eventCaptor = ArgumentCaptor.forClass(PerformanceEvent.class);
        verify(structuredLogger).performance(eventCaptor.capture());
        
        PerformanceEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.isSuccess()).isFalse();
        assertThat(capturedEvent.getErrorType()).isEqualTo("RuntimeException");
        
        // Verify method stats were updated
        PerformanceMonitoringAspect.MethodStats stats = performanceAspect.getMethodStats("TestService.testMethod");
        assertThat(stats).isNotNull();
        assertThat(stats.getTotalCalls()).isEqualTo(1);
        assertThat(stats.getSuccessCalls()).isEqualTo(0);
        assertThat(stats.getSuccessRate()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should categorize performance levels correctly")
    void monitorPerformance_DifferentDurations_ShouldCategorizeCorrectly() throws Throwable {
        // given
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            Thread.sleep(50); // VERY_FAST (< 100ms)
            return "result";
        });

        // when
        performanceAspect.monitorPerformance(joinPoint);

        // then
        ArgumentCaptor<PerformanceEvent> eventCaptor = ArgumentCaptor.forClass(PerformanceEvent.class);
        verify(structuredLogger).performance(eventCaptor.capture());
        
        PerformanceEvent event = eventCaptor.getValue();
        // Should be VERY_FAST or FAST depending on actual timing
        assertThat(event.getPerformanceLevel()).isIn("VERY_FAST", "FAST");
    }

    @Test
    @DisplayName("Should handle authenticated user correctly")
    void monitorPerformance_WithAuthenticatedUser_ShouldCaptureUserId() throws Throwable {
        // given
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        when(joinPoint.proceed()).thenReturn("result");

        // when
        performanceAspect.monitorPerformance(joinPoint);

        // then
        ArgumentCaptor<PerformanceEvent> eventCaptor = ArgumentCaptor.forClass(PerformanceEvent.class);
        verify(structuredLogger).performance(eventCaptor.capture());
        
        PerformanceEvent event = eventCaptor.getValue();
        assertThat(event.getUserId()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should handle anonymous user correctly")
    void monitorPerformance_WithAnonymousUser_ShouldUseAnonymous() throws Throwable {
        // given
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(null);
        when(joinPoint.proceed()).thenReturn("result");

        // when
        performanceAspect.monitorPerformance(joinPoint);

        // then
        ArgumentCaptor<PerformanceEvent> eventCaptor = ArgumentCaptor.forClass(PerformanceEvent.class);
        verify(structuredLogger).performance(eventCaptor.capture());
        
        PerformanceEvent event = eventCaptor.getValue();
        assertThat(event.getUserId()).isEqualTo("anonymous");
    }

    @Test
    @DisplayName("Should handle security context exception")
    void monitorPerformance_WithSecurityException_ShouldUseUnknown() throws Throwable {
        // given
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenThrow(new RuntimeException("Security error"));
        when(joinPoint.proceed()).thenReturn("result");

        // when
        performanceAspect.monitorPerformance(joinPoint);

        // then
        ArgumentCaptor<PerformanceEvent> eventCaptor = ArgumentCaptor.forClass(PerformanceEvent.class);
        verify(structuredLogger).performance(eventCaptor.capture());
        
        PerformanceEvent event = eventCaptor.getValue();
        assertThat(event.getUserId()).isEqualTo("unknown");
    }

    @Test
    @DisplayName("Should extract method name correctly")
    void getMethodNameFromKey_ValidKey_ShouldExtractMethodName() {
        // Test via reflection since method is private
        String result = (String) ReflectionTestUtils.invokeMethod(performanceAspect, 
                "getMethodNameFromKey", "TestService.testMethod");
        assertThat(result).isEqualTo("testMethod");
    }

    @Test
    @DisplayName("Should extract class name correctly")
    void getClassNameFromKey_ValidKey_ShouldExtractClassName() {
        // Test via reflection since method is private
        String result = (String) ReflectionTestUtils.invokeMethod(performanceAspect, 
                "getClassNameFromKey", "TestService.testMethod");
        assertThat(result).isEqualTo("TestService");
    }

    @Test
    @DisplayName("Should handle invalid method key gracefully")
    void getMethodNameFromKey_InvalidKey_ShouldReturnOriginal() {
        String result = (String) ReflectionTestUtils.invokeMethod(performanceAspect, 
                "getMethodNameFromKey", "invalidkey");
        assertThat(result).isEqualTo("invalidkey");
    }

    @Test
    @DisplayName("Should categorize performance levels based on thresholds")
    void getPerformanceLevel_DifferentDurations_ShouldCategorizeCorrectly() {
        // Test via reflection since method is private
        PerformanceMonitoringAspect.PerformanceLevel veryFast = 
                (PerformanceMonitoringAspect.PerformanceLevel) ReflectionTestUtils.invokeMethod(
                        performanceAspect, "getPerformanceLevel", 50L);
        assertThat(veryFast).isEqualTo(PerformanceMonitoringAspect.PerformanceLevel.VERY_FAST);

        PerformanceMonitoringAspect.PerformanceLevel fast = 
                (PerformanceMonitoringAspect.PerformanceLevel) ReflectionTestUtils.invokeMethod(
                        performanceAspect, "getPerformanceLevel", 150L);
        assertThat(fast).isEqualTo(PerformanceMonitoringAspect.PerformanceLevel.FAST);

        PerformanceMonitoringAspect.PerformanceLevel moderate = 
                (PerformanceMonitoringAspect.PerformanceLevel) ReflectionTestUtils.invokeMethod(
                        performanceAspect, "getPerformanceLevel", 750L);
        assertThat(moderate).isEqualTo(PerformanceMonitoringAspect.PerformanceLevel.MODERATE);

        PerformanceMonitoringAspect.PerformanceLevel slow = 
                (PerformanceMonitoringAspect.PerformanceLevel) ReflectionTestUtils.invokeMethod(
                        performanceAspect, "getPerformanceLevel", 1500L);
        assertThat(slow).isEqualTo(PerformanceMonitoringAspect.PerformanceLevel.SLOW);

        PerformanceMonitoringAspect.PerformanceLevel verySlow = 
                (PerformanceMonitoringAspect.PerformanceLevel) ReflectionTestUtils.invokeMethod(
                        performanceAspect, "getPerformanceLevel", 5000L);
        assertThat(verySlow).isEqualTo(PerformanceMonitoringAspect.PerformanceLevel.VERY_SLOW);
    }

    @Test
    @DisplayName("Should accumulate statistics over multiple calls")
    void monitorPerformance_MultipleCalls_ShouldAccumulateStats() throws Throwable {
        // given
        when(joinPoint.proceed())
                .thenReturn("result1")
                .thenReturn("result2")
                .thenThrow(new RuntimeException("error"))
                .thenReturn("result3");

        // when
        performanceAspect.monitorPerformance(joinPoint);
        performanceAspect.monitorPerformance(joinPoint);
        
        try {
            performanceAspect.monitorPerformance(joinPoint);
        } catch (RuntimeException ignored) {}
        
        performanceAspect.monitorPerformance(joinPoint);

        // then
        PerformanceMonitoringAspect.MethodStats stats = performanceAspect.getMethodStats("TestService.testMethod");
        assertThat(stats).isNotNull();
        assertThat(stats.getTotalCalls()).isEqualTo(4);
        assertThat(stats.getSuccessCalls()).isEqualTo(3);
        assertThat(stats.getSuccessRate()).isEqualTo(75.0);
        assertThat(stats.getAverageDuration()).isGreaterThanOrEqualTo(0);
        assertThat(stats.getMinDuration()).isGreaterThanOrEqualTo(0);
        assertThat(stats.getMaxDuration()).isGreaterThanOrEqualTo(stats.getMinDuration());
    }

    @Test
    @DisplayName("Should clear statistics correctly")
    void clearStats_ShouldRemoveAllStatistics() throws Throwable {
        // given
        when(joinPoint.proceed()).thenReturn("result");
        performanceAspect.monitorPerformance(joinPoint);
        
        assertThat(performanceAspect.getMethodStats("TestService.testMethod")).isNotNull();

        // when
        performanceAspect.clearStats();

        // then
        assertThat(performanceAspect.getMethodStats("TestService.testMethod")).isNull();
        assertThat(performanceAspect.getAllMethodStats()).isEmpty();
    }

    @Test
    @DisplayName("Should return all method statistics")
    void getAllMethodStats_WithMultipleMethods_ShouldReturnAllStats() throws Throwable {
        // given
        when(joinPoint.proceed()).thenReturn("result");
        when(methodSignature.getName()).thenReturn("method1").thenReturn("method2");
        
        // when
        performanceAspect.monitorPerformance(joinPoint);
        performanceAspect.monitorPerformance(joinPoint);

        // then
        var allStats = performanceAspect.getAllMethodStats();
        assertThat(allStats).hasSize(2);
        assertThat(allStats).containsKeys("TestService.method1", "TestService.method2");
    }

    @Test
    @DisplayName("Should create comprehensive performance event")
    void monitorPerformance_ShouldCreateCompletePerformanceEvent() throws Throwable {
        // given
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        when(joinPoint.proceed()).thenReturn("result");

        // when
        performanceAspect.monitorPerformance(joinPoint);

        // then
        ArgumentCaptor<PerformanceEvent> eventCaptor = ArgumentCaptor.forClass(PerformanceEvent.class);
        verify(structuredLogger).performance(eventCaptor.capture());
        
        PerformanceEvent event = eventCaptor.getValue();
        assertThat(event.getOperation()).isEqualTo("TestService.testMethod");
        assertThat(event.getDurationMs()).isGreaterThanOrEqualTo(0);
        assertThat(event.getDurationNanos()).isGreaterThanOrEqualTo(0);
        assertThat(event.getPerformanceLevel()).isNotNull();
        assertThat(event.isSuccess()).isTrue();
        assertThat(event.getErrorType()).isNull();
        assertThat(event.getUserId()).isEqualTo("testuser");
        assertThat(event.getMethodName()).isEqualTo("testMethod");
        assertThat(event.getClassName()).isEqualTo("TestService");
        assertThat(event.getMemoryBefore()).isGreaterThanOrEqualTo(0);
        assertThat(event.getMemoryAfter()).isGreaterThanOrEqualTo(0);
        assertThat(event.getCpuTime()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("MethodStats should handle concurrent updates correctly")
    void methodStats_ConcurrentUpdates_ShouldHandleCorrectly() {
        // given
        PerformanceMonitoringAspect.MethodStats stats = new PerformanceMonitoringAspect.MethodStats();

        // when
        stats.update(100, true);
        stats.update(200, true);
        stats.update(300, false);
        stats.update(50, true);

        // then
        assertThat(stats.getTotalCalls()).isEqualTo(4);
        assertThat(stats.getSuccessCalls()).isEqualTo(3);
        assertThat(stats.getSuccessRate()).isEqualTo(75.0);
        assertThat(stats.getAverageDuration()).isEqualTo(162); // (100+200+300+50)/4
        assertThat(stats.getMinDuration()).isEqualTo(50);
        assertThat(stats.getMaxDuration()).isEqualTo(300);
        assertThat(stats.getTotalDuration()).isEqualTo(650);
    }

    @Test
    @DisplayName("MethodStats should handle empty state correctly")
    void methodStats_EmptyState_ShouldReturnCorrectDefaults() {
        // given
        PerformanceMonitoringAspect.MethodStats stats = new PerformanceMonitoringAspect.MethodStats();

        // then
        assertThat(stats.getTotalCalls()).isEqualTo(0);
        assertThat(stats.getSuccessCalls()).isEqualTo(0);
        assertThat(stats.getSuccessRate()).isEqualTo(0.0);
        assertThat(stats.getAverageDuration()).isEqualTo(0);
        assertThat(stats.getMinDuration()).isEqualTo(0);
        assertThat(stats.getMaxDuration()).isEqualTo(0);
        assertThat(stats.getTotalDuration()).isEqualTo(0);
    }

    @Test
    @DisplayName("MethodStats toString should format correctly")
    void methodStats_ToString_ShouldFormatCorrectly() {
        // given
        PerformanceMonitoringAspect.MethodStats stats = new PerformanceMonitoringAspect.MethodStats();
        stats.update(100, true);
        stats.update(200, false);

        // when
        String result = stats.toString();

        // then
        assertThat(result).contains("calls=2");
        assertThat(result).contains("success=50.0%");
        assertThat(result).contains("avg=150ms");
        assertThat(result).contains("min=100ms");
        assertThat(result).contains("max=200ms");
    }

    // Helper class for testing
    private static class TestService {
        public String testMethod() {
            return "test";
        }
        
        public String method1() {
            return "method1";
        }
        
        public String method2() {
            return "method2";
        }
    }
}