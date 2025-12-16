package com.Hamalog.service.monitoring;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionMetricsServiceTest {

    @Mock
    private DataSource dataSource;

    private TransactionMetricsService transactionMetricsService;

    @BeforeEach
    void setUp() {
        transactionMetricsService = new TransactionMetricsService(dataSource);
    }

    @Test
    void recordTransactionExecution_WhenSuccessful_ShouldIncrementSuccessfulTransactions() {
        // given
        String methodName = "testMethod";
        long duration = 100L;
        boolean successful = true;

        // when
        transactionMetricsService.recordTransactionExecution(methodName, duration, successful);

        // then
        assertThat(transactionMetricsService.getSuccessRate()).isEqualTo(100.0);
        assertThat(transactionMetricsService.getAverageExecutionTime()).isEqualTo(duration);
    }

    @Test
    void recordTransactionExecution_WhenFailed_ShouldIncrementFailedTransactions() {
        // given
        String methodName = "testMethod";
        long duration = 100L;
        // when
        transactionMetricsService.recordTransactionExecution(methodName, duration, false);

        // then
        assertThat(transactionMetricsService.getSuccessRate()).isEqualTo(0.0);
        assertThat(transactionMetricsService.getAverageExecutionTime()).isEqualTo(duration);
    }

    @Test
    void recordRollback_ShouldIncrementRollbackCount() {
        // given
        String methodName = "testMethod";
        String reason = "Test rollback reason";

        // when
        transactionMetricsService.recordRollback(methodName, reason);
        transactionMetricsService.recordTransactionExecution(methodName, 100L, false);

        // then
        assertThat(transactionMetricsService.getSuccessRate()).isEqualTo(0.0);
    }

    @Test
    void getAverageExecutionTime_WithMultipleTransactions_ShouldCalculateCorrectly() {
        // given
        String methodName = "testMethod";

        // when
        transactionMetricsService.recordTransactionExecution(methodName, 100L, true);
        transactionMetricsService.recordTransactionExecution(methodName, 200L, true);
        transactionMetricsService.recordTransactionExecution(methodName, 300L, true);

        // then
        assertThat(transactionMetricsService.getAverageExecutionTime()).isEqualTo(200.0);
    }

    @Test
    void getSuccessRate_WithMixedTransactions_ShouldCalculateCorrectly() {
        // given
        String methodName = "testMethod";

        // when
        transactionMetricsService.recordTransactionExecution(methodName, 100L, true);
        transactionMetricsService.recordTransactionExecution(methodName, 200L, false);
        transactionMetricsService.recordTransactionExecution(methodName, 300L, true);
        transactionMetricsService.recordTransactionExecution(methodName, 400L, true);

        // then
        assertThat(transactionMetricsService.getSuccessRate()).isEqualTo(75.0);
    }
}
