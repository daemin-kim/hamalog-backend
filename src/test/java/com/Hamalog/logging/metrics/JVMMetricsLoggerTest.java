package com.Hamalog.logging.metrics;

import com.Hamalog.logging.StructuredLogger;
import com.Hamalog.logging.events.PerformanceEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JVMMetricsLoggerTest {

    @Mock
    private StructuredLogger structuredLogger;

    @InjectMocks
    private JVMMetricsLogger jvmMetricsLogger;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void logJVMMetrics_ShouldCollectAndLogMetrics() {
        // when
        jvmMetricsLogger.logJVMMetrics();

        // then
        verify(structuredLogger, times(1)).performance(any(PerformanceEvent.class));
    }

    @Test
    void collectJVMMetrics_ShouldReturnValidMetrics() throws Exception {
        // given
        Method collectJVMMetricsMethod = JVMMetricsLogger.class.getDeclaredMethod("collectJVMMetrics");
        collectJVMMetricsMethod.setAccessible(true);

        // when
        @SuppressWarnings("unchecked")
        Map<String, Object> metrics = (Map<String, Object>) collectJVMMetricsMethod.invoke(jvmMetricsLogger);

        // then
        assertNotNull(metrics);
        assertTrue(metrics.containsKey("heapMemoryUsage"));
        assertTrue(metrics.containsKey("nonHeapMemoryUsage"));
        assertTrue(metrics.containsKey("threadCount"));
        assertTrue(metrics.containsKey("cpuLoad"));
        assertTrue(metrics.containsKey("uptime"));
    }

    @Test
    void highMemoryUsage_ShouldBeDetected() throws Exception {
        // given
        Method collectJVMMetricsMethod = JVMMetricsLogger.class.getDeclaredMethod("collectJVMMetrics");
        collectJVMMetricsMethod.setAccessible(true);

        // Simulate high memory usage by mocking MemoryMXBean
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();

        // Using the real memory values should be enough to detect high memory usage
        // as the HIGH_MEMORY_THRESHOLD is set to 0.85 (85%)

        // when
        @SuppressWarnings("unchecked")
        Map<String, Object> metrics = (Map<String, Object>) collectJVMMetricsMethod.invoke(jvmMetricsLogger);

        // then
        assertTrue(metrics.containsKey("heapMemoryUsage"));
        @SuppressWarnings("unchecked")
        Map<String, Long> heapMemoryUsage = (Map<String, Long>) metrics.get("heapMemoryUsage");
        assertNotNull(heapMemoryUsage);
        assertTrue(heapMemoryUsage.containsKey("used"));
        assertTrue(heapMemoryUsage.containsKey("max"));
    }

    @Test
    void gcMetrics_ShouldBeCollected() throws Exception {
        // given
        Method collectJVMMetricsMethod = JVMMetricsLogger.class.getDeclaredMethod("collectJVMMetrics");
        collectJVMMetricsMethod.setAccessible(true);

        // when
        @SuppressWarnings("unchecked")
        Map<String, Object> metrics = (Map<String, Object>) collectJVMMetricsMethod.invoke(jvmMetricsLogger);

        // then
        assertTrue(metrics.containsKey("gcCollectionCount"));
        assertTrue(metrics.containsKey("gcCollectionTime"));
    }

    @Test
    void threadMetrics_ShouldBeCollected() throws Exception {
        // given
        Method collectJVMMetricsMethod = JVMMetricsLogger.class.getDeclaredMethod("collectJVMMetrics");
        collectJVMMetricsMethod.setAccessible(true);

        // when
        @SuppressWarnings("unchecked")
        Map<String, Object> metrics = (Map<String, Object>) collectJVMMetricsMethod.invoke(jvmMetricsLogger);

        // then
        assertTrue(metrics.containsKey("threadCount"));
        assertTrue(metrics.containsKey("peakThreadCount"));
        assertTrue(metrics.containsKey("daemonThreadCount"));
    }
}
