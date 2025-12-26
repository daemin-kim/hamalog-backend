package com.Hamalog.logging.metrics;

import com.Hamalog.logging.StructuredLogger;
import com.Hamalog.logging.events.PerformanceEvent;
import java.lang.management.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
    private static final long BYTES_IN_MB = 1024L * 1024L;

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
                    .memoryBefore((Long) metrics.get("heapUsedBytes"))
                    .memoryAfter((Long) metrics.get("heapMaxBytes"))
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
                alertContext.put("heap_used_mb", toMegabytes(heapUsage.getUsed()));
                alertContext.put("heap_max_mb", toMegabytes(heapUsage.getMax()));
                alertContext.put("severity", "CRITICAL");
                
                structuredLogger.error("Critical memory usage detected", new RuntimeException("Memory threshold exceeded"), alertContext);
                
                log.error(
                    "CRITICAL_ALERT: Heap usage {}% ({} MB / {} MB)",
                    String.format("%.1f", heapUsedPercent * 100),
                    toMegabytes(heapUsage.getUsed()),
                    toMegabytes(heapUsage.getMax())
                );
            }
            
            // Check GC pressure
            long currentGcTime = getTotalGcTime();
            long currentUptime = runtimeBean.getUptime();
            
            if (previousUptime > 0) {
                long gcTimeDelta = currentGcTime - previousGcTime;
                long uptimeDelta = currentUptime - previousUptime;
                
                if (gcTimeDelta > HIGH_GC_TIME_THRESHOLD && uptimeDelta > 0) {
                    double gcPercent = percent(gcTimeDelta, uptimeDelta);

                    Map<String, Object> alertContext = new HashMap<>();
                    alertContext.put("alert_type", "HIGH_GC_PRESSURE");
                    alertContext.put("gc_time_delta_ms", gcTimeDelta);
                    alertContext.put("gc_percent", gcPercent);
                    alertContext.put("severity", gcPercent > 10 ? "CRITICAL" : "HIGH");
                    
                    structuredLogger.error("High GC pressure detected", new RuntimeException("GC time threshold exceeded"), alertContext);
                    
                    log.warn(
                        "HIGH_GC_PRESSURE: GC consumed {}% of time ({} ms in {} ms window)",
                        String.format("%.2f", gcPercent),
                        gcTimeDelta,
                        uptimeDelta
                    );
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
        metrics.put("heapUsedBytes", heapUsage.getUsed());
        metrics.put("heapMaxBytes", heapUsage.getMax());
        metrics.put("heapUsedPercent", percent(heapUsage.getUsed(), heapUsage.getMax()));
        metrics.put("nonHeapUsedBytes", nonHeapUsage.getUsed());
        metrics.put("nonHeapMaxBytes", nonHeapUsage.getMax());
        metrics.put("nonHeapUsedPercent", percent(nonHeapUsage.getUsed(), nonHeapUsage.getMax()));

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
        @SuppressWarnings("unchecked")
        Map<String, Long> heapUsage = (Map<String, Long>) metrics.get("heapMemoryUsage");
        @SuppressWarnings("unchecked")
        Map<String, Long> nonHeapUsage = (Map<String, Long>) metrics.get("nonHeapMemoryUsage");

        double heapUsedPercent = percent(heapUsage.get("used"), heapUsage.get("max"));
        double nonHeapUsedPercent = percent(nonHeapUsage.get("used"), nonHeapUsage.get("max"));

        return String.format(
            "Heap: used=%d MB / %d MB (%.1f%%), NonHeap: used=%d MB / %d MB (%.1f%%), " +
            "Threads: active=%d, peak=%d, daemon=%d, GC: collections=%d, time=%d ms, " +
            "Uptime=%d s, CPU load(avg1m)=%.2f",
            toMegabytes(heapUsage.get("used")),
            toMegabytes(heapUsage.get("max")),
            heapUsedPercent,
            toMegabytes(nonHeapUsage.get("used")),
            toMegabytes(nonHeapUsage.get("max")),
            nonHeapUsedPercent,
            metrics.get("threadCount"),
            metrics.get("peakThreadCount"),
            metrics.get("daemonThreadCount"),
            metrics.get("gcCollectionCount"),
            metrics.get("gcCollectionTime"),
            (Long) metrics.get("uptime") / 1000,
            metrics.get("cpuLoad")
        );
    }

    private double percent(long used, long max) {
        if (max <= 0) {
            return 0.0;
        }
        return ((double) used / max) * 100;
    }

    private long toMegabytes(long bytes) {
        if (bytes <= 0) {
            return 0;
        }
        return bytes / BYTES_IN_MB;
    }
}