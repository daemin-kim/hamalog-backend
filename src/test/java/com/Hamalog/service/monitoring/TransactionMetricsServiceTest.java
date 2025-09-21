package com.Hamalog.service.monitoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionMetricsService Tests")
class TransactionMetricsServiceTest {

    @Mock
    private DataSource mockDataSource;

    @Mock
    private Connection mockConnection;

    private TransactionMetricsService transactionMetricsService;

    @BeforeEach
    void setUp() {
        transactionMetricsService = new TransactionMetricsService(mockDataSource);
    }

    @Test
    @DisplayName("Should record successful transaction execution")
    void recordTransactionExecution_SuccessfulTransaction_RecordsCorrectly() {
        // given
        String methodName = "testMethod";
        long duration = 100L;

        // when
        transactionMetricsService.recordTransactionExecution(methodName, duration, true);

        // then
        TransactionMetricsService.TransactionMetricsSummary summary = transactionMetricsService.getMetricsSummary();
        assertThat(summary.totalTransactions).isEqualTo(1L);
        assertThat(summary.successfulTransactions).isEqualTo(1L);
        assertThat(summary.failedTransactions).isEqualTo(0L);
        assertThat(summary.successRate).isEqualTo(100.0);
        assertThat(summary.averageExecutionTime).isEqualTo(100.0);
        assertThat(summary.longestTransactionDuration).isEqualTo(100L);
        assertThat(summary.longestTransactionMethod).isEqualTo(methodName);
    }

    @Test
    @DisplayName("Should record failed transaction execution")
    void recordTransactionExecution_FailedTransaction_RecordsCorrectly() {
        // given
        String methodName = "testMethod";
        long duration = 200L;

        // when
        transactionMetricsService.recordTransactionExecution(methodName, duration, false);

        // then
        TransactionMetricsService.TransactionMetricsSummary summary = transactionMetricsService.getMetricsSummary();
        assertThat(summary.totalTransactions).isEqualTo(1L);
        assertThat(summary.successfulTransactions).isEqualTo(0L);
        assertThat(summary.failedTransactions).isEqualTo(1L);
        assertThat(summary.successRate).isEqualTo(0.0);
        assertThat(summary.averageExecutionTime).isEqualTo(200.0);
    }

    @Test
    @DisplayName("Should update longest transaction when new transaction is longer")
    void recordTransactionExecution_LongerTransaction_UpdatesLongest() {
        // given
        transactionMetricsService.recordTransactionExecution("method1", 100L, true);

        // when
        transactionMetricsService.recordTransactionExecution("method2", 300L, true);

        // then
        TransactionMetricsService.TransactionMetricsSummary summary = transactionMetricsService.getMetricsSummary();
        assertThat(summary.longestTransactionDuration).isEqualTo(300L);
        assertThat(summary.longestTransactionMethod).isEqualTo("method2");
    }

    @Test
    @DisplayName("Should not update longest transaction when new transaction is shorter")
    void recordTransactionExecution_ShorterTransaction_DoesNotUpdateLongest() {
        // given
        transactionMetricsService.recordTransactionExecution("method1", 300L, true);

        // when
        transactionMetricsService.recordTransactionExecution("method2", 100L, true);

        // then
        TransactionMetricsService.TransactionMetricsSummary summary = transactionMetricsService.getMetricsSummary();
        assertThat(summary.longestTransactionDuration).isEqualTo(300L);
        assertThat(summary.longestTransactionMethod).isEqualTo("method1");
    }

    @Test
    @DisplayName("Should record multiple transactions correctly")
    void recordTransactionExecution_MultipleTransactions_CalculatesCorrectly() {
        // given & when
        transactionMetricsService.recordTransactionExecution("method1", 100L, true);
        transactionMetricsService.recordTransactionExecution("method2", 200L, false);
        transactionMetricsService.recordTransactionExecution("method3", 150L, true);

        // then
        TransactionMetricsService.TransactionMetricsSummary summary = transactionMetricsService.getMetricsSummary();
        assertThat(summary.totalTransactions).isEqualTo(3L);
        assertThat(summary.successfulTransactions).isEqualTo(2L);
        assertThat(summary.failedTransactions).isEqualTo(1L);
        assertThat(summary.successRate).isCloseTo(66.67, within(0.01)); // 2/3 * 100
        assertThat(summary.averageExecutionTime).isEqualTo(150.0); // (100+200+150)/3
    }

    @Test
    @DisplayName("Should record rollback correctly")
    void recordRollback_ValidInput_RecordsRollback() {
        // given
        String methodName = "testMethod";
        String reason = "Constraint violation";

        // when
        transactionMetricsService.recordRollback(methodName, reason);

        // then
        TransactionMetricsService.TransactionMetricsSummary summary = transactionMetricsService.getMetricsSummary();
        assertThat(summary.rollbackCount).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should return 100% success rate when no transactions recorded")
    void getSuccessRate_NoTransactions_Returns100Percent() {
        // when
        double successRate = transactionMetricsService.getSuccessRate();

        // then
        assertThat(successRate).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Should return 0 average execution time when no transactions recorded")
    void getAverageExecutionTime_NoTransactions_ReturnsZero() {
        // when
        double averageTime = transactionMetricsService.getAverageExecutionTime();

        // then
        assertThat(averageTime).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should track method-specific statistics")
    void recordTransactionExecution_SameMethod_TracksMethodStats() {
        // given
        String methodName = "testMethod";

        // when
        transactionMetricsService.recordTransactionExecution(methodName, 100L, true);
        transactionMetricsService.recordTransactionExecution(methodName, 200L, false);
        transactionMetricsService.recordTransactionExecution(methodName, 150L, true);

        // then
        var methodStats = transactionMetricsService.getMethodStats();
        assertThat(methodStats).containsKey(methodName);
        
        TransactionMetricsService.TransactionStats stats = methodStats.get(methodName);
        assertThat(stats.getExecutions()).isEqualTo(3);
        assertThat(stats.getSuccessCount()).isEqualTo(2);
        assertThat(stats.getSuccessRate()).isCloseTo(66.67, within(0.01));
        assertThat(stats.getAverageDuration()).isEqualTo(150.0);
        assertThat(stats.getMaxDuration()).isEqualTo(200L);
        assertThat(stats.getMinDuration()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Should handle concurrent transaction recording")
    void recordTransactionExecution_ConcurrentAccess_HandlesCorrectly() throws InterruptedException {
        // given
        int threadCount = 10;
        int transactionsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < transactionsPerThread; j++) {
                        transactionMetricsService.recordTransactionExecution(
                                "method" + threadId, 
                                100L + j, 
                                j % 2 == 0 // alternate success/failure
                        );
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // then
        TransactionMetricsService.TransactionMetricsSummary summary = transactionMetricsService.getMetricsSummary();
        assertThat(summary.totalTransactions).isEqualTo(threadCount * transactionsPerThread);
        assertThat(summary.successfulTransactions).isEqualTo(threadCount * (transactionsPerThread / 2));
        assertThat(summary.failedTransactions).isEqualTo(threadCount * (transactionsPerThread / 2));
    }

    @Test
    @DisplayName("Should perform health check successfully")
    void performHealthCheck_HealthyDatabase_UpdatesHealthStatus() throws SQLException {
        // given
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.isValid(anyInt())).thenReturn(true);
        doNothing().when(mockConnection).close();

        // when
        transactionMetricsService.performHealthCheck();

        // then
        TransactionMetricsService.DatabaseHealthStatus healthStatus = transactionMetricsService.getDatabaseHealthStatus();
        assertThat(healthStatus.healthy).isTrue();
        assertThat(healthStatus.lastHealthCheck).isNotNull();
        
        verify(mockConnection).isValid(5);
        verify(mockConnection).close();
    }

    @Test
    @DisplayName("Should handle database connection failure during health check")
    void performHealthCheck_DatabaseConnectionFailure_UpdatesHealthStatus() throws SQLException {
        // given
        when(mockDataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        // when
        transactionMetricsService.performHealthCheck();

        // then
        TransactionMetricsService.DatabaseHealthStatus healthStatus = transactionMetricsService.getDatabaseHealthStatus();
        assertThat(healthStatus.healthy).isFalse();
        assertThat(healthStatus.lastHealthCheck).isNotNull();
    }

    @Test
    @DisplayName("Should handle invalid connection during health check")
    void performHealthCheck_InvalidConnection_UpdatesHealthStatus() throws SQLException {
        // given
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.isValid(anyInt())).thenReturn(false);
        doNothing().when(mockConnection).close();

        // when
        transactionMetricsService.performHealthCheck();

        // then
        TransactionMetricsService.DatabaseHealthStatus healthStatus = transactionMetricsService.getDatabaseHealthStatus();
        assertThat(healthStatus.healthy).isFalse();
        
        verify(mockConnection).isValid(5);
        verify(mockConnection).close();
    }

    @Test
    @DisplayName("Should reset metrics correctly")
    void resetMetrics_Called_ResetsAllMetrics() {
        // given
        transactionMetricsService.recordTransactionExecution("method1", 100L, true);
        transactionMetricsService.recordTransactionExecution("method2", 200L, false);
        transactionMetricsService.recordRollback("method1", "Test rollback");

        // when
        transactionMetricsService.resetMetrics();

        // then
        TransactionMetricsService.TransactionMetricsSummary summary = transactionMetricsService.getMetricsSummary();
        assertThat(summary.totalTransactions).isEqualTo(0L);
        assertThat(summary.successfulTransactions).isEqualTo(0L);
        assertThat(summary.failedTransactions).isEqualTo(0L);
        assertThat(summary.rollbackCount).isEqualTo(0L);
        assertThat(summary.longestTransactionDuration).isEqualTo(0L);
        assertThat(summary.longestTransactionMethod).isEmpty();
        assertThat(transactionMetricsService.getMethodStats()).isEmpty();
    }

    @Test
    @DisplayName("Should return correct health status")
    void isHealthy_VariousStates_ReturnsCorrectStatus() throws SQLException {
        // Test healthy state
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.isValid(anyInt())).thenReturn(true);
        doNothing().when(mockConnection).close();
        
        transactionMetricsService.performHealthCheck();
        assertThat(transactionMetricsService.isHealthy()).isTrue();

        // Test unhealthy state
        when(mockDataSource.getConnection()).thenThrow(new SQLException("Connection failed"));
        transactionMetricsService.performHealthCheck();
        assertThat(transactionMetricsService.isHealthy()).isFalse();
    }

    @Test
    @DisplayName("Should build TransactionStats correctly")
    void transactionStats_RecordExecutions_CalculatesCorrectly() {
        // given
        TransactionMetricsService.TransactionStats stats = new TransactionMetricsService.TransactionStats();

        // when
        stats.recordExecution(100L, true);
        stats.recordExecution(200L, false);
        stats.recordExecution(150L, true);
        stats.recordExecution(50L, true);

        // then
        assertThat(stats.getExecutions()).isEqualTo(4);
        assertThat(stats.getSuccessCount()).isEqualTo(3);
        assertThat(stats.getSuccessRate()).isEqualTo(75.0);
        assertThat(stats.getAverageDuration()).isEqualTo(125.0); // (100+200+150+50)/4
        assertThat(stats.getMaxDuration()).isEqualTo(200L);
        assertThat(stats.getMinDuration()).isEqualTo(50L);
    }

    @Test
    @DisplayName("Should handle zero executions in TransactionStats")
    void transactionStats_ZeroExecutions_ReturnsCorrectDefaults() {
        // given
        TransactionMetricsService.TransactionStats stats = new TransactionMetricsService.TransactionStats();

        // then
        assertThat(stats.getExecutions()).isEqualTo(0);
        assertThat(stats.getSuccessCount()).isEqualTo(0);
        assertThat(stats.getSuccessRate()).isEqualTo(100.0);
        assertThat(stats.getAverageDuration()).isEqualTo(0.0);
        assertThat(stats.getMaxDuration()).isEqualTo(0L);
        assertThat(stats.getMinDuration()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Should build DatabaseHealthStatus with builder pattern")
    void databaseHealthStatus_Builder_BuildsCorrectly() {
        // given
        LocalDateTime now = LocalDateTime.now();

        // when
        TransactionMetricsService.DatabaseHealthStatus status = TransactionMetricsService.DatabaseHealthStatus.builder()
                .healthy(true)
                .lastHealthCheck(now)
                .transactionSuccessRate(95.5)
                .averageExecutionTime(150.0)
                .totalTransactions(1000L)
                .activeConnections(10)
                .idleConnections(5)
                .build();

        // then
        assertThat(status.healthy).isTrue();
        assertThat(status.lastHealthCheck).isEqualTo(now);
        assertThat(status.transactionSuccessRate).isEqualTo(95.5);
        assertThat(status.averageExecutionTime).isEqualTo(150.0);
        assertThat(status.totalTransactions).isEqualTo(1000L);
        assertThat(status.activeConnections).isEqualTo(10);
        assertThat(status.idleConnections).isEqualTo(5);
    }

    @Test
    @DisplayName("Should build TransactionMetricsSummary with builder pattern")
    void transactionMetricsSummary_Builder_BuildsCorrectly() {
        // given
        LocalDateTime now = LocalDateTime.now();

        // when
        TransactionMetricsService.TransactionMetricsSummary summary = TransactionMetricsService.TransactionMetricsSummary.builder()
                .totalTransactions(1000L)
                .successfulTransactions(950L)
                .failedTransactions(50L)
                .rollbackCount(10L)
                .successRate(95.0)
                .averageExecutionTime(150.0)
                .longestTransactionDuration(5000L)
                .longestTransactionMethod("slowMethod")
                .activeConnections(10)
                .idleConnections(5)
                .databaseHealthy(true)
                .lastHealthCheck(now)
                .build();

        // then
        assertThat(summary.totalTransactions).isEqualTo(1000L);
        assertThat(summary.successfulTransactions).isEqualTo(950L);
        assertThat(summary.failedTransactions).isEqualTo(50L);
        assertThat(summary.rollbackCount).isEqualTo(10L);
        assertThat(summary.successRate).isEqualTo(95.0);
        assertThat(summary.averageExecutionTime).isEqualTo(150.0);
        assertThat(summary.longestTransactionDuration).isEqualTo(5000L);
        assertThat(summary.longestTransactionMethod).isEqualTo("slowMethod");
        assertThat(summary.activeConnections).isEqualTo(10);
        assertThat(summary.idleConnections).isEqualTo(5);
        assertThat(summary.databaseHealthy).isTrue();
        assertThat(summary.lastHealthCheck).isEqualTo(now);
    }

    @Test
    @DisplayName("Should handle single successful transaction in TransactionStats")
    void transactionStats_SingleSuccessfulTransaction_CalculatesCorrectly() {
        // given
        TransactionMetricsService.TransactionStats stats = new TransactionMetricsService.TransactionStats();

        // when
        stats.recordExecution(100L, true);

        // then
        assertThat(stats.getExecutions()).isEqualTo(1);
        assertThat(stats.getSuccessCount()).isEqualTo(1);
        assertThat(stats.getSuccessRate()).isEqualTo(100.0);
        assertThat(stats.getAverageDuration()).isEqualTo(100.0);
        assertThat(stats.getMaxDuration()).isEqualTo(100L);
        assertThat(stats.getMinDuration()).isEqualTo(100L);
    }

}