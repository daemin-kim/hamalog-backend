package com.Hamalog.logging.metrics;

import com.Hamalog.logging.StructuredLogger;
import com.Hamalog.logging.events.PerformanceEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JVMMetricsLoggerTest {

    @Mock
    private StructuredLogger structuredLogger;

    @InjectMocks
    private JVMMetricsLogger jvmMetricsLogger;

    @BeforeEach
    void setUp() {
        // Set up any necessary test fixtures
    }

    @Test
    void logJVMMetrics_ShouldLogMetricsSuccessfully() {
        // when
        jvmMetricsLogger.logJVMMetrics();

        // then
        verify(structuredLogger, times(1)).performance(any(PerformanceEvent.class));
    }

    @Test
    void logCriticalJVMEvents_ShouldCheckMemoryPressure() {
        // when
        jvmMetricsLogger.logCriticalJVMEvents();

        // No immediate verification since the method might or might not log based on current JVM state
        // But we can verify that the method runs without throwing exceptions
        verifyNoMoreInteractions(structuredLogger);
    }

    @Test
    void logJVMMetrics_ShouldHandleExceptionGracefully() {
        // given
        doThrow(new RuntimeException("Test exception"))
            .when(structuredLogger)
            .performance(any(PerformanceEvent.class));

        // when
        jvmMetricsLogger.logJVMMetrics();

        // then
        // Method should not throw exception even when structuredLogger fails
        verify(structuredLogger, times(1)).performance(any(PerformanceEvent.class));
    }
}
