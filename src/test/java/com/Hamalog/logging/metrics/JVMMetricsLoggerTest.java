package com.Hamalog.logging.metrics;

import com.Hamalog.logging.StructuredLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JVMMetricsLoggerTest {

    @Mock
    private StructuredLogger structuredLogger;

    @InjectMocks
    private JVMMetricsLogger jvmMetricsLogger;

    private MemoryMXBean memoryBean;

    @BeforeEach
    void setUp() {
        memoryBean = ManagementFactory.getMemoryMXBean();
    }

    @Test
    void logJVMMetrics_ShouldLogMetricsSuccessfully() {
        // When
        jvmMetricsLogger.logJVMMetrics();

        // Then
        verify(structuredLogger, times(1)).performance(any());
    }

    @Test
    void logCriticalJVMEvents_WithNormalMemoryUsage_ShouldNotLogCriticalEvent() {
        // Given
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();

        // When
        jvmMetricsLogger.logCriticalJVMEvents();

        // Then - 위험한 수준이 아닐 경우 critical 이벤트가 기록되지 않아야 함
        verify(structuredLogger, never()).performance(argThat(event ->
            event.getPerformanceLevel().equals("CRITICAL")));
    }

    @Test
    void checkAllMXBeansAreInitialized() {
        // Given & When
        Object runtimeBean = ReflectionTestUtils.getField(jvmMetricsLogger, "runtimeBean");
        Object threadBean = ReflectionTestUtils.getField(jvmMetricsLogger, "threadBean");
        Object gcBeans = ReflectionTestUtils.getField(jvmMetricsLogger, "gcBeans");
        Object osBean = ReflectionTestUtils.getField(jvmMetricsLogger, "osBean");
        Object classLoadingBean = ReflectionTestUtils.getField(jvmMetricsLogger, "classLoadingBean");

        // Then
        assert runtimeBean != null : "RuntimeMXBean should be initialized";
        assert threadBean != null : "ThreadMXBean should be initialized";
        assert gcBeans != null : "GarbageCollectorMXBeans should be initialized";
        assert osBean != null : "OperatingSystemMXBean should be initialized";
        assert classLoadingBean != null : "ClassLoadingMXBean should be initialized";
    }
}
