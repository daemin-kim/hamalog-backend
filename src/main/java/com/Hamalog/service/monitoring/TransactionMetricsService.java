package com.Hamalog.service.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Service for monitoring transaction metrics and database health
 * Provides insights into transaction performance and system health
 */
@Service
@Slf4j
public class TransactionMetricsService {

    private final DataSource dataSource;
    
    // Transaction metrics
    private final LongAdder transactionCount = new LongAdder();
    private final LongAdder successfulTransactions = new LongAdder();
    private final LongAdder failedTransactions = new LongAdder();
    private final LongAdder rollbackCount = new LongAdder();
    
    // Performance metrics
    private final ConcurrentHashMap<String, TransactionStats> methodStats = new ConcurrentHashMap<>();
    private final AtomicLong totalExecutionTime = new AtomicLong();
    private final AtomicLong longestTransaction = new AtomicLong();
    private volatile String longestTransactionMethod = "";
    
    // Connection pool metrics
    private volatile int activeConnections = 0;
    private volatile int idleConnections = 0;
    private volatile boolean databaseHealthy = true;
    private volatile LocalDateTime lastHealthCheck = LocalDateTime.now();

    public TransactionMetricsService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Records transaction execution metrics
     */
    public void recordTransactionExecution(String methodName, long durationMs, boolean successful) {
        transactionCount.increment();
        
        if (successful) {
            successfulTransactions.increment();
        } else {
            failedTransactions.increment();
        }
        
        totalExecutionTime.addAndGet(durationMs);
        
        // Update longest transaction
        if (durationMs > longestTransaction.get()) {
            longestTransaction.set(durationMs);
            longestTransactionMethod = methodName;
        }
        
        // Update method-specific stats
        methodStats.computeIfAbsent(methodName, k -> new TransactionStats())
                  .recordExecution(durationMs, successful);
        
        // Log slow transactions
        if (durationMs > 5000) { // 5 seconds threshold
            log.warn("SLOW_TRANSACTION: {} took {}ms", methodName, durationMs);
        }
    }

    /**
     * Records transaction rollback
     */
    public void recordRollback(String methodName, String reason) {
        rollbackCount.increment();
        log.info("TRANSACTION_ROLLBACK: {} - Reason: {}", methodName, reason);
    }

    /**
     * Gets transaction success rate as percentage
     */
    public double getSuccessRate() {
        long total = transactionCount.sum();
        if (total == 0) return 100.0;
        return (successfulTransactions.sum() * 100.0) / total;
    }

    /**
     * Gets average transaction execution time
     */
    public double getAverageExecutionTime() {
        long total = transactionCount.sum();
        if (total == 0) return 0.0;
        return totalExecutionTime.get() / (double) total;
    }

    /**
     * Gets transaction metrics summary
     */
    public TransactionMetricsSummary getMetricsSummary() {
        return TransactionMetricsSummary.builder()
                .totalTransactions(transactionCount.sum())
                .successfulTransactions(successfulTransactions.sum())
                .failedTransactions(failedTransactions.sum())
                .rollbackCount(rollbackCount.sum())
                .successRate(getSuccessRate())
                .averageExecutionTime(getAverageExecutionTime())
                .longestTransactionDuration(longestTransaction.get())
                .longestTransactionMethod(longestTransactionMethod)
                .activeConnections(activeConnections)
                .idleConnections(idleConnections)
                .databaseHealthy(databaseHealthy)
                .lastHealthCheck(lastHealthCheck)
                .build();
    }

    /**
     * Gets method-specific transaction statistics
     */
    public ConcurrentHashMap<String, TransactionStats> getMethodStats() {
        return new ConcurrentHashMap<>(methodStats);
    }

    /**
     * Periodic health check for database and connection pool
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void performHealthCheck() {
        try (Connection connection = dataSource.getConnection()) {
            // Test database connectivity
            boolean isValid = connection.isValid(5); // 5 second timeout
            databaseHealthy = isValid;
            
            // Update connection pool metrics
            updateConnectionPoolMetrics();
            
            lastHealthCheck = LocalDateTime.now();
            
            if (!isValid) {
                log.error("DATABASE_HEALTH_CHECK_FAILED: Database connection is not valid");
            }
            
        } catch (SQLException e) {
            databaseHealthy = false;
            lastHealthCheck = LocalDateTime.now();
            log.error("DATABASE_HEALTH_CHECK_ERROR: Failed to check database health", e);
        }
    }

    private void updateConnectionPoolMetrics() {
        try {
            // For HikariCP, we can get pool metrics if available
            if (dataSource instanceof com.zaxxer.hikari.HikariDataSource) {
                com.zaxxer.hikari.HikariDataSource hikariDS = (com.zaxxer.hikari.HikariDataSource) dataSource;
                com.zaxxer.hikari.HikariPoolMXBean poolBean = hikariDS.getHikariPoolMXBean();
                
                if (poolBean != null) {
                    activeConnections = poolBean.getActiveConnections();
                    idleConnections = poolBean.getIdleConnections();
                }
            }
        } catch (Exception e) {
            log.debug("Could not retrieve connection pool metrics: {}", e.getMessage());
        }
    }

    /**
     * Resets all metrics - useful for testing or periodic cleanup
     */
    public void resetMetrics() {
        transactionCount.reset();
        successfulTransactions.reset();
        failedTransactions.reset();
        rollbackCount.reset();
        totalExecutionTime.set(0);
        longestTransaction.set(0);
        longestTransactionMethod = "";
        methodStats.clear();
        
        log.info("Transaction metrics have been reset");
    }

    /**
     * Gets current database health status
     */
    public DatabaseHealthStatus getDatabaseHealthStatus() {
        return DatabaseHealthStatus.builder()
                .healthy(databaseHealthy)
                .lastHealthCheck(lastHealthCheck)
                .transactionSuccessRate(getSuccessRate())
                .averageExecutionTime(getAverageExecutionTime())
                .totalTransactions(transactionCount.sum())
                .activeConnections(activeConnections)
                .idleConnections(idleConnections)
                .build();
    }

    /**
     * Simple health check that returns true if database is healthy
     */
    public boolean isHealthy() {
        return databaseHealthy;
    }

    /**
     * Inner class for method-specific transaction statistics
     */
    public static class TransactionStats {
        private final LongAdder executions = new LongAdder();
        private final LongAdder successCount = new LongAdder();
        private final LongAdder totalDuration = new LongAdder();
        private final AtomicLong maxDuration = new AtomicLong();
        private final AtomicLong minDuration = new AtomicLong(Long.MAX_VALUE);

        public void recordExecution(long durationMs, boolean successful) {
            executions.increment();
            if (successful) {
                successCount.increment();
            }
            totalDuration.add(durationMs);
            
            maxDuration.updateAndGet(current -> Math.max(current, durationMs));
            minDuration.updateAndGet(current -> Math.min(current, durationMs));
        }

        public long getExecutions() { return executions.sum(); }
        public long getSuccessCount() { return successCount.sum(); }
        public double getSuccessRate() {
            long total = executions.sum();
            return total == 0 ? 100.0 : (successCount.sum() * 100.0) / total;
        }
        public double getAverageDuration() {
            long total = executions.sum();
            return total == 0 ? 0.0 : totalDuration.sum() / (double) total;
        }
        public long getMaxDuration() { return maxDuration.get(); }
        public long getMinDuration() { 
            long min = minDuration.get();
            return min == Long.MAX_VALUE ? 0 : min;
        }
    }

    /**
     * Database health status DTO
     */
    public static class DatabaseHealthStatus {
        public final boolean healthy;
        public final LocalDateTime lastHealthCheck;
        public final double transactionSuccessRate;
        public final double averageExecutionTime;
        public final long totalTransactions;
        public final int activeConnections;
        public final int idleConnections;

        private DatabaseHealthStatus(Builder builder) {
            this.healthy = builder.healthy;
            this.lastHealthCheck = builder.lastHealthCheck;
            this.transactionSuccessRate = builder.transactionSuccessRate;
            this.averageExecutionTime = builder.averageExecutionTime;
            this.totalTransactions = builder.totalTransactions;
            this.activeConnections = builder.activeConnections;
            this.idleConnections = builder.idleConnections;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private boolean healthy;
            private LocalDateTime lastHealthCheck;
            private double transactionSuccessRate;
            private double averageExecutionTime;
            private long totalTransactions;
            private int activeConnections;
            private int idleConnections;

            public Builder healthy(boolean healthy) {
                this.healthy = healthy;
                return this;
            }

            public Builder lastHealthCheck(LocalDateTime lastHealthCheck) {
                this.lastHealthCheck = lastHealthCheck;
                return this;
            }

            public Builder transactionSuccessRate(double transactionSuccessRate) {
                this.transactionSuccessRate = transactionSuccessRate;
                return this;
            }

            public Builder averageExecutionTime(double averageExecutionTime) {
                this.averageExecutionTime = averageExecutionTime;
                return this;
            }

            public Builder totalTransactions(long totalTransactions) {
                this.totalTransactions = totalTransactions;
                return this;
            }

            public Builder activeConnections(int activeConnections) {
                this.activeConnections = activeConnections;
                return this;
            }

            public Builder idleConnections(int idleConnections) {
                this.idleConnections = idleConnections;
                return this;
            }

            public DatabaseHealthStatus build() {
                return new DatabaseHealthStatus(this);
            }
        }
    }

    /**
     * Summary DTO for transaction metrics
     */
    public static class TransactionMetricsSummary {
        public final long totalTransactions;
        public final long successfulTransactions;
        public final long failedTransactions;
        public final long rollbackCount;
        public final double successRate;
        public final double averageExecutionTime;
        public final long longestTransactionDuration;
        public final String longestTransactionMethod;
        public final int activeConnections;
        public final int idleConnections;
        public final boolean databaseHealthy;
        public final LocalDateTime lastHealthCheck;

        private TransactionMetricsSummary(Builder builder) {
            this.totalTransactions = builder.totalTransactions;
            this.successfulTransactions = builder.successfulTransactions;
            this.failedTransactions = builder.failedTransactions;
            this.rollbackCount = builder.rollbackCount;
            this.successRate = builder.successRate;
            this.averageExecutionTime = builder.averageExecutionTime;
            this.longestTransactionDuration = builder.longestTransactionDuration;
            this.longestTransactionMethod = builder.longestTransactionMethod;
            this.activeConnections = builder.activeConnections;
            this.idleConnections = builder.idleConnections;
            this.databaseHealthy = builder.databaseHealthy;
            this.lastHealthCheck = builder.lastHealthCheck;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private long totalTransactions;
            private long successfulTransactions;
            private long failedTransactions;
            private long rollbackCount;
            private double successRate;
            private double averageExecutionTime;
            private long longestTransactionDuration;
            private String longestTransactionMethod;
            private int activeConnections;
            private int idleConnections;
            private boolean databaseHealthy;
            private LocalDateTime lastHealthCheck;

            public Builder totalTransactions(long totalTransactions) {
                this.totalTransactions = totalTransactions;
                return this;
            }

            public Builder successfulTransactions(long successfulTransactions) {
                this.successfulTransactions = successfulTransactions;
                return this;
            }

            public Builder failedTransactions(long failedTransactions) {
                this.failedTransactions = failedTransactions;
                return this;
            }

            public Builder rollbackCount(long rollbackCount) {
                this.rollbackCount = rollbackCount;
                return this;
            }

            public Builder successRate(double successRate) {
                this.successRate = successRate;
                return this;
            }

            public Builder averageExecutionTime(double averageExecutionTime) {
                this.averageExecutionTime = averageExecutionTime;
                return this;
            }

            public Builder longestTransactionDuration(long longestTransactionDuration) {
                this.longestTransactionDuration = longestTransactionDuration;
                return this;
            }

            public Builder longestTransactionMethod(String longestTransactionMethod) {
                this.longestTransactionMethod = longestTransactionMethod;
                return this;
            }

            public Builder activeConnections(int activeConnections) {
                this.activeConnections = activeConnections;
                return this;
            }

            public Builder idleConnections(int idleConnections) {
                this.idleConnections = idleConnections;
                return this;
            }

            public Builder databaseHealthy(boolean databaseHealthy) {
                this.databaseHealthy = databaseHealthy;
                return this;
            }

            public Builder lastHealthCheck(LocalDateTime lastHealthCheck) {
                this.lastHealthCheck = lastHealthCheck;
                return this;
            }

            public TransactionMetricsSummary build() {
                return new TransactionMetricsSummary(this);
            }
        }
    }
}