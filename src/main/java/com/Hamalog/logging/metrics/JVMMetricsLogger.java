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
                log.info("JVM_METRICS [HEALTHY]: System operating normally - {}", formatMetrics(metrics));
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
        
        metrics.put("heap_used", heapUsage.getUsed());
        metrics.put("heap_max", heapUsage.getMax());
        metrics.put("heap_committed", heapUsage.getCommitted());
        metrics.put("heap_used_percent", (double) heapUsage.getUsed() / heapUsage.getMax() * 100);
        
        metrics.put("non_heap_used", nonHeapUsage.getUsed());
        metrics.put("non_heap_max", nonHeapUsage.getMax());
        metrics.put("non_heap_committed", nonHeapUsage.getCommitted());
        
        // Thread metrics
        metrics.put("thread_count", threadBean.getThreadCount());
        metrics.put("thread_peak_count", threadBean.getPeakThreadCount());
        metrics.put("thread_daemon_count", threadBean.getDaemonThreadCount());
        metrics.put("thread_total_started", threadBean.getTotalStartedThreadCount());
        
        // GC metrics
        long totalGcTime = 0;
        long totalGcCollections = 0;
        Map<String, Object> gcMetrics = new HashMap<>();
        
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            long collections = gcBean.getCollectionCount();
            long time = gcBean.getCollectionTime();
            totalGcCollections += collections;
            totalGcTime += time;
            
            gcMetrics.put(gcBean.getName() + "_collections", collections);
            gcMetrics.put(gcBean.getName() + "_time_ms", time);
        }
        
        metrics.put("gc_total_collections", totalGcCollections);
        metrics.put("gc_total_time_ms", totalGcTime);
        metrics.put("gc_details", gcMetrics);
        
        // Runtime metrics
        metrics.put("uptime_ms", runtimeBean.getUptime());
        metrics.put("start_time", runtimeBean.getStartTime());
        metrics.put("jvm_name", runtimeBean.getVmName());
        metrics.put("jvm_version", runtimeBean.getVmVersion());
        
        // Class loading metrics
        metrics.put("classes_loaded", classLoadingBean.getLoadedClassCount());
        metrics.put("classes_total_loaded", classLoadingBean.getTotalLoadedClassCount());
        metrics.put("classes_unloaded", classLoadingBean.getUnloadedClassCount());
        
        // OS metrics
        metrics.put("available_processors", osBean.getAvailableProcessors());
        metrics.put("system_load_average", osBean.getSystemLoadAverage());
        
        // Additional OS metrics if available
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            com.sun.management.OperatingSystemMXBean sunOsBean = (com.sun.management.OperatingSystemMXBean) osBean;
            metrics.put("process_cpu_load", sunOsBean.getProcessCpuLoad());
            metrics.put("system_cpu_load", sunOsBean.getSystemCpuLoad());
            metrics.put("free_physical_memory", sunOsBean.getFreePhysicalMemorySize());
            metrics.put("total_physical_memory", sunOsBean.getTotalPhysicalMemorySize());
            metrics.put("free_swap_space", sunOsBean.getFreeSwapSpaceSize());
            metrics.put("total_swap_space", sunOsBean.getTotalSwapSpaceSize());
        }
        
        return metrics;
    }

    /**
     * Determine overall performance level based on metrics
     */
    private String determinePerformanceLevel(Map<String, Object> metrics) {
        double heapUsedPercent = (Double) metrics.get("heap_used_percent");
        int threadCount = (Integer) metrics.get("thread_count");
        long gcTotalTime = (Long) metrics.get("gc_total_time_ms");
        long uptime = (Long) metrics.get("uptime_ms");
        
        // Calculate GC overhead
        double gcOverhead = uptime > 0 ? (double) gcTotalTime / uptime * 100 : 0;
        
        // Get CPU load if available
        Double processCpuLoad = (Double) metrics.get("process_cpu_load");
        
        // Critical conditions
        if (heapUsedPercent > CRITICAL_MEMORY_THRESHOLD * 100 ||
            threadCount > HIGH_THREAD_COUNT_THRESHOLD ||
            gcOverhead > 10 ||
            (processCpuLoad != null && processCpuLoad > HIGH_CPU_THRESHOLD)) {
            return "CRITICAL";
        }
        
        // Degraded conditions
        if (heapUsedPercent > HIGH_MEMORY_THRESHOLD * 100 ||
            threadCount > HIGH_THREAD_COUNT_THRESHOLD * 0.7 ||
            gcOverhead > 5 ||
            (processCpuLoad != null && processCpuLoad > HIGH_CPU_THRESHOLD * 0.7)) {
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
            "Heap: %.1f%% (%d MB / %d MB), Threads: %d, GC: %d collections (%d ms), Uptime: %d min",
            (Double) metrics.get("heap_used_percent"),
            (Long) metrics.get("heap_used") / 1024 / 1024,
            (Long) metrics.get("heap_max") / 1024 / 1024,
            (Integer) metrics.get("thread_count"),
            (Long) metrics.get("gc_total_collections"),
            (Long) metrics.get("gc_total_time_ms"),
            (Long) metrics.get("uptime_ms") / 60000
        );
    }
}