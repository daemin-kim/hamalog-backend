package com.Hamalog.logging.metrics;

import com.Hamalog.logging.StructuredLogger;
import com.Hamalog.logging.events.PerformanceEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.management.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Production-ready JVM metrics logger that tracks system health and performance
 * Provides detailed insights into memory usage, GC behavior, thread activity, and CPU utilization
 */
@Slf4j
@Component
public class JVMMetricsLogger {

    @Autowired
    private StructuredLogger structuredLogger;

    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private final RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
    private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    private final List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
    private final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    private final ClassLoadingMXBean classLoadingBean = ManagementFactory.getClassLoadingMXBean();

    // Thresholds for alerting
    private static final double HIGH_MEMORY_THRESHOLD = 0.85; // 85%
    private static final double CRITICAL_MEMORY_THRESHOLD = 0.95; // 95%
    private static final long HIGH_GC_TIME_THRESHOLD = 1000; // 1 second
    private static final int HIGH_THREAD_COUNT_THRESHOLD = 500;
    private static final double HIGH_CPU_THRESHOLD = 0.80; // 80%

    // Previous metrics for calculating deltas
    private long previousGcTime = 0;
    private long previousGcCollections = 0;
    private long previousUptime = 0;

    /**
     * Log comprehensive JVM metrics every minute
     */
    @Scheduled(fixedRate = 60000) // Every 60 seconds
    public void logJVMMetrics() {
        try {
            Map<String, Object> metrics = collectJVMMetrics();
            
            // Determine performance level based on metrics
            String performanceLevel = determinePerformanceLevel(metrics);
            
            // Create performance event
            PerformanceEvent performanceEvent = PerformanceEvent.builder()
                    .operation("JVM_HEALTH_CHECK")
                    .durationMs(0L) // Not applicable for system metrics
                    .durationNanos(0L)
                    .performanceLevel(performanceLevel)
                    .success(true)
                    .userId("SYSTEM")
                    .methodName("collectJVMMetrics")
                    .className("JVMMetricsLogger")
                    .memoryBefore((Long) metrics.get("heap_used"))
                    .memoryAfter((Long) metrics.get("heap_max"))
                    .cpuTime(0L) // Not applicable
                    .build();
            
            structuredLogger.performance(performanceEvent);
            
            // Log with appropriate level based on health status
            if ("CRITICAL".equals(performanceLevel)) {
                log.error("JVM_METRICS [CRITICAL]: High resource usage detected - {}", formatMetrics(metrics));
            } else if ("DEGRADED".equals(performanceLevel)) {
                log.warn("JVM_METRICS [DEGRADED]: Performance degradation detected - {}", formatMetrics(metrics));
            } else {
                log.debug("JVM_METRICS [HEALTHY]: System operating normally - {}", formatMetrics(metrics));
            }
            
        } catch (Exception e) {
            log.error("Failed to collect JVM metrics", e);
        }
    }

    /**
     * Log critical JVM events immediately (high memory usage, long GC, etc.)
     */
    @Scheduled(fixedRate = 15000) // Every 15 seconds for critical monitoring
    public void logCriticalJVMEvents() {
        try {
            // Check memory pressure
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            double heapUsedPercent = (double) heapUsage.getUsed() / heapUsage.getMax();
            
            if (heapUsedPercent > CRITICAL_MEMORY_THRESHOLD) {
                Map<String, Object> alertContext = new HashMap<>();
                alertContext.put("alert_type", "CRITICAL_MEMORY_USAGE");
                alertContext.put("heap_used_percent", heapUsedPercent * 100);
                alertContext.put("heap_used_mb", heapUsage.getUsed() / 1024 / 1024);
                alertContext.put("heap_max_mb", heapUsage.getMax() / 1024 / 1024);
                alertContext.put("severity", "CRITICAL");
                
                structuredLogger.error("Critical memory usage detected", new RuntimeException("Memory threshold exceeded"), alertContext);
                
                log.error("CRITICAL_ALERT: Memory usage at {:.1f}% ({} MB / {} MB)", 
                    heapUsedPercent * 100, 
                    heapUsage.getUsed() / 1024 / 1024, 
                    heapUsage.getMax() / 1024 / 1024);
            }
            
            // Check GC pressure
            long currentGcTime = getTotalGcTime();
            long currentUptime = runtimeBean.getUptime();
            
            if (previousUptime > 0) {
                long gcTimeDelta = currentGcTime - previousGcTime;
                long uptimeDelta = currentUptime - previousUptime;
                
                if (gcTimeDelta > HIGH_GC_TIME_THRESHOLD && uptimeDelta > 0) {
                    double gcPercent = (double) gcTimeDelta / uptimeDelta * 100;
                    
                    Map<String, Object> alertContext = new HashMap<>();
                    alertContext.put("alert_type", "HIGH_GC_PRESSURE");
                    alertContext.put("gc_time_delta_ms", gcTimeDelta);
                    alertContext.put("gc_percent", gcPercent);
                    alertContext.put("severity", gcPercent > 10 ? "CRITICAL" : "HIGH");
                    
                    structuredLogger.error("High GC pressure detected", new RuntimeException("GC time threshold exceeded"), alertContext);
                    
                    log.warn("HIGH_GC_PRESSURE: GC consumed {:.2f}% of time ({} ms in {} ms window)", 
                        gcPercent, gcTimeDelta, uptimeDelta);
                }
            }
            
            previousGcTime = currentGcTime;
            previousUptime = currentUptime;
            
        } catch (Exception e) {
            log.error("Failed to check critical JVM events", e);
        }
    }

    /**
     * Collect comprehensive JVM metrics
     */
    private Map<String, Object> collectJVMMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // Memory metrics
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        
        metrics.put("heapMemoryUsage", formatMemoryUsage(heapUsage));
        metrics.put("nonHeapMemoryUsage", formatMemoryUsage(nonHeapUsage));

        double heapUsedPercent = (double) heapUsage.getUsed() / heapUsage.getMax();
        if (heapUsedPercent > HIGH_MEMORY_THRESHOLD) {
            metrics.put("memoryAlertLevel", heapUsedPercent > CRITICAL_MEMORY_THRESHOLD ? "CRITICAL" : "WARNING");
        }

        // Thread metrics
        metrics.put("threadCount", threadBean.getThreadCount());
        metrics.put("peakThreadCount", threadBean.getPeakThreadCount());
        metrics.put("daemonThreadCount", threadBean.getDaemonThreadCount());

        // GC metrics
        long totalGcTime = 0;
        long totalGcCollections = 0;
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            totalGcTime += gcBean.getCollectionTime();
            totalGcCollections += gcBean.getCollectionCount();
        }
        metrics.put("gcCollectionTime", totalGcTime);
        metrics.put("gcCollectionCount", totalGcCollections);

        // CPU metrics
        metrics.put("cpuLoad", osBean.getSystemLoadAverage());

        // Runtime metrics
        metrics.put("uptime", runtimeBean.getUptime());
        metrics.put("startTime", runtimeBean.getStartTime());

        // Class loading metrics
        metrics.put("loadedClassCount", classLoadingBean.getLoadedClassCount());
        metrics.put("totalLoadedClassCount", classLoadingBean.getTotalLoadedClassCount());
        metrics.put("unloadedClassCount", classLoadingBean.getUnloadedClassCount());

        return metrics;
    }

    private Map<String, Long> formatMemoryUsage(MemoryUsage usage) {
        Map<String, Long> formattedUsage = new HashMap<>();
        formattedUsage.put("init", usage.getInit());
        formattedUsage.put("used", usage.getUsed());
        formattedUsage.put("committed", usage.getCommitted());
        formattedUsage.put("max", usage.getMax());
        return formattedUsage;
    }

    /**
     * Determine overall performance level based on metrics
     */
    private String determinePerformanceLevel(Map<String, Object> metrics) {
        // Check memory usage
        @SuppressWarnings("unchecked")
        Map<String, Long> heapUsage = (Map<String, Long>) metrics.get("heapMemoryUsage");
        double heapUsedPercent = (double) heapUsage.get("used") / heapUsage.get("max");

        if (heapUsedPercent > CRITICAL_MEMORY_THRESHOLD ||
            (Long) metrics.get("gcCollectionTime") > HIGH_GC_TIME_THRESHOLD ||
            (Integer) metrics.get("threadCount") > HIGH_THREAD_COUNT_THRESHOLD) {
            return "CRITICAL";
        }
        
        if (heapUsedPercent > HIGH_MEMORY_THRESHOLD ||
            ((Double) metrics.get("cpuLoad") > HIGH_CPU_THRESHOLD)) {
            return "DEGRADED";
        }
        
        return "HEALTHY";
    }

    /**
     * Get total GC time across all collectors
     */
    private long getTotalGcTime() {
        long totalTime = 0;
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            long time = gcBean.getCollectionTime();
            if (time > 0) {
                totalTime += time;
            }
        }
        return totalTime;
    }

    /**
     * Format metrics for human-readable logging
     */
    private String formatMetrics(Map<String, Object> metrics) {
        return String.format(
            "Memory[Heap: %s, NonHeap: %s], Threads[Active: %d, Peak: %d], " +
            "GC[Collections: %d, Time: %dms], CPU Load: %.2f",
            metrics.get("heapMemoryUsage"),
            metrics.get("nonHeapMemoryUsage"),
            metrics.get("threadCount"),
            metrics.get("peakThreadCount"),
            metrics.get("gcCollectionCount"),
            metrics.get("gcCollectionTime"),
            metrics.get("cpuLoad")
        );
    }
}