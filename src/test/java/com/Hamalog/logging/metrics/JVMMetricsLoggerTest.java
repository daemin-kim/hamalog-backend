package com.Hamalog.logging.metrics;

import com.Hamalog.logging.StructuredLogger;
import com.Hamalog.logging.events.PerformanceEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
        // 필요한 경우 추가 설정
    }

    @Test
    @DisplayName("JVM 메트릭 로깅이 정상적으로 동작해야 한다")
    void logJVMMetrics_ShouldLogMetricsSuccessfully() {
        // when
        jvmMetricsLogger.logJVMMetrics();

        // then
        verify(structuredLogger, times(1)).performance(any(PerformanceEvent.class));
    }

    @Test
    @DisplayName("예외 발생 시에도 애플리케이션이 계속 실행되어야 한다")
    void logJVMMetrics_ShouldHandleExceptionsGracefully() {
        // given
        doThrow(new RuntimeException("Test exception"))
            .when(structuredLogger)
            .performance(any(PerformanceEvent.class));

        // when
        jvmMetricsLogger.logJVMMetrics();

        // then
        verify(structuredLogger, times(1)).performance(any(PerformanceEvent.class));
        // 예외가 발생해도 메소드가 정상적으로 종료되어야 함
    }
}
