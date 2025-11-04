package com.Hamalog.logging.metrics;

import com.Hamalog.logging.StructuredLogger;
import com.Hamalog.logging.events.PerformanceEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JVMMetricsLoggerTest {

    @Mock
    private StructuredLogger structuredLogger;

    @InjectMocks
    private JVMMetricsLogger jvmMetricsLogger;

    @Test
    void logJVMMetrics_ShouldLogMetricsSuccessfully() {
        // given
        doNothing().when(structuredLogger).performance(any(PerformanceEvent.class));

        // when
        jvmMetricsLogger.logJVMMetrics();

        // then
        verify(structuredLogger, times(1)).performance(any(PerformanceEvent.class));
    }

    @Test
    void logCriticalJVMEvents_ShouldExecuteWithoutException() {
        // when & then
        // 예외가 발생하지 않고 정상적으로 실행되는지 확인
        assertDoesNotThrow(() -> jvmMetricsLogger.logCriticalJVMEvents());
    }
}
