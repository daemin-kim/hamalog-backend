package com.Hamalog.aop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.Hamalog.logging.StructuredLogger;
import com.Hamalog.logging.events.PerformanceEvent;
import com.Hamalog.security.SecurityContextUtils;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("PerformanceMonitoringAspect Tests")
class PerformanceMonitoringAspectTest {

    @Mock
    private StructuredLogger structuredLogger;

    @Mock
    private SecurityContextUtils securityContextUtils;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @InjectMocks
    private PerformanceMonitoringAspect performanceAspect;

    @BeforeEach
    void setUp() {
        // Set up configuration values - 느린 쿼리 임계값을 낮게 설정하여 테스트에서 로깅되도록 함
        ReflectionTestUtils.setField(performanceAspect, "slowThreshold", 0L);  // 0ms: 모든 쿼리 로깅
        ReflectionTestUtils.setField(performanceAspect, "verySlowThreshold", 100L);

        // Configure mock method signature
        lenient().when(joinPoint.getSignature()).thenReturn(methodSignature);
        lenient().when(methodSignature.getDeclaringType()).thenReturn((Class) TestRepository.class);
        lenient().when(methodSignature.getName()).thenReturn("findById");

        // Setup SecurityContextUtils mocks
        lenient().when(securityContextUtils.getCurrentUserId()).thenReturn("testuser");

        // Clear MDC
        MDC.clear();
    }

    @Test
    @DisplayName("Should monitor successful method execution and log performance")
    void monitorPerformance_SuccessfulExecution_ShouldTrackPerformance() throws Throwable {
        // given
        String expectedResult = "success";
        when(joinPoint.proceed()).thenReturn(expectedResult);

        // when
        Object result = performanceAspect.monitorPerformance(joinPoint);

        // then
        assertThat(result).isEqualTo(expectedResult);
        
        // Verify structured logging was called (slowThreshold가 0이므로 모든 쿼리 로깅)
        verify(structuredLogger).performance(any(PerformanceEvent.class));
        
        // Verify method stats were updated
        PerformanceMonitoringAspect.MethodStats stats = performanceAspect.getMethodStats("TestRepository.findById");
        assertThat(stats).isNotNull();
        assertThat(stats.getTotalCalls()).isEqualTo(1);
        assertThat(stats.getSuccessCalls()).isEqualTo(1);
        assertThat(stats.getSuccessRate()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Should monitor failed method execution and log error")
    void monitorPerformance_FailedExecution_ShouldTrackFailure() throws Throwable {
        // given
        RuntimeException exception = new RuntimeException("Test error");
        when(joinPoint.proceed()).thenThrow(exception);

        // when & then
        assertThatThrownBy(() -> performanceAspect.monitorPerformance(joinPoint))
                .isSameAs(exception);
        
        // Verify structured logging was called for errors (errors are always logged)
        ArgumentCaptor<PerformanceEvent> eventCaptor = ArgumentCaptor.forClass(PerformanceEvent.class);
        verify(structuredLogger).performance(eventCaptor.capture());
        
        PerformanceEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.isSuccess()).isFalse();
        assertThat(capturedEvent.getErrorType()).isEqualTo("RuntimeException");
        
        // Verify method stats were updated
        PerformanceMonitoringAspect.MethodStats stats = performanceAspect.getMethodStats("TestRepository.findById");
        assertThat(stats).isNotNull();
        assertThat(stats.getTotalCalls()).isEqualTo(1);
        assertThat(stats.getSuccessCalls()).isEqualTo(0);
        assertThat(stats.getSuccessRate()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should not log fast queries when threshold is high")
    void monitorPerformance_FastQuery_ShouldNotLogWhenThresholdHigh() throws Throwable {
        // given - slowThreshold를 높게 설정
        ReflectionTestUtils.setField(performanceAspect, "slowThreshold", 10000L);
        when(joinPoint.proceed()).thenReturn("result");

        // when
        performanceAspect.monitorPerformance(joinPoint);

        // then - 빠른 쿼리는 로깅되지 않음
        verify(structuredLogger, never()).performance(any(PerformanceEvent.class));

        // But stats are still updated
        PerformanceMonitoringAspect.MethodStats stats = performanceAspect.getMethodStats("TestRepository.findById");
        assertThat(stats).isNotNull();
        assertThat(stats.getTotalCalls()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should categorize performance levels based on thresholds")
    void getPerformanceLevel_DifferentDurations_ShouldCategorizeCorrectly() {
        // Set thresholds for testing
        ReflectionTestUtils.setField(performanceAspect, "slowThreshold", 1000L);
        ReflectionTestUtils.setField(performanceAspect, "verySlowThreshold", 3000L);

        PerformanceMonitoringAspect.PerformanceLevel veryFast =
                (PerformanceMonitoringAspect.PerformanceLevel) ReflectionTestUtils.invokeMethod(
                        performanceAspect, "getPerformanceLevel", 50L);
        assertThat(veryFast).isEqualTo(PerformanceMonitoringAspect.PerformanceLevel.VERY_FAST);

        PerformanceMonitoringAspect.PerformanceLevel fast = 
                (PerformanceMonitoringAspect.PerformanceLevel) ReflectionTestUtils.invokeMethod(
                        performanceAspect, "getPerformanceLevel", 150L);
        assertThat(fast).isEqualTo(PerformanceMonitoringAspect.PerformanceLevel.FAST);

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
        PerformanceMonitoringAspect.MethodStats stats = performanceAspect.getMethodStats("TestRepository.findById");
        assertThat(stats).isNotNull();
        assertThat(stats.getTotalCalls()).isEqualTo(4);
        assertThat(stats.getSuccessCalls()).isEqualTo(3);
        assertThat(stats.getSuccessRate()).isEqualTo(75.0);
    }

    @Test
    @DisplayName("Should clear statistics correctly")
    void clearStats_ShouldRemoveAllStatistics() throws Throwable {
        // given
        when(joinPoint.proceed()).thenReturn("result");
        performanceAspect.monitorPerformance(joinPoint);
        
        assertThat(performanceAspect.getMethodStats("TestRepository.findById")).isNotNull();

        // when
        performanceAspect.clearStats();

        // then
        assertThat(performanceAspect.getMethodStats("TestRepository.findById")).isNull();
        assertThat(performanceAspect.getAllMethodStats()).isEmpty();
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
        assertThat(stats.getAverageDuration()).isEqualTo(162);
        assertThat(stats.getMinDuration()).isEqualTo(50);
        assertThat(stats.getMaxDuration()).isEqualTo(300);
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

    // Helper class for testing - Repository 패턴에 맞춤
    private static class TestRepository {
        public String findById() {
            return "test";
        }
    }
}
