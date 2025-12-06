package com.Hamalog.aop;

import com.Hamalog.logging.StructuredLogger;
import com.Hamalog.logging.events.PerformanceEvent;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

@Slf4j
@Aspect
@Component
@ConditionalOnProperty(name = "app.aop.performance.enabled", matchIfMissing = true)
@Order(2)
public class PerformanceMonitoringAspect {

    @Autowired
    private StructuredLogger structuredLogger;

    @Value("${app.aop.performance.slow-threshold:1000}")
    private long slowThreshold;

    @Value("${app.aop.performance.very-slow-threshold:3000}")
    private long verySlowThreshold;

    // 메서드별 성능 통계 저장
    private final ConcurrentHashMap<String, MethodStats> methodStats = new ConcurrentHashMap<>();

    @Pointcut("execution(public * com.Hamalog.service..*(..))")
    public void allServiceMethods() {}

    @Pointcut("execution(public * com.Hamalog.repository..*(..))")
    public void allRepositoryMethods() {}

    @Around("allServiceMethods() || allRepositoryMethods()")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodKey = getMethodKey(joinPoint);
        long startTime = System.currentTimeMillis();
        long startNanos = System.nanoTime();
        
        // Memory tracking
        Runtime runtime = Runtime.getRuntime();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        // MDC에 성능 모니터링 컨텍스트 추가
        MDC.put("perf.method", methodKey);
        MDC.put("perf.startTime", String.valueOf(startTime));
        MDC.put("perf.memoryBefore", String.valueOf(memoryBefore));

        try {
            Object result = joinPoint.proceed();
            
            long duration = System.currentTimeMillis() - startTime;
            long durationNanos = System.nanoTime() - startNanos;
            long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
            
            // 성능 통계 업데이트
            updateMethodStats(methodKey, duration, true);
            
            // 성능 레벨 결정 및 로깅
            PerformanceLevel level = getPerformanceLevel(duration);
            logPerformanceResult(methodKey, duration, durationNanos, level, true, null, memoryBefore, memoryAfter);
            
            return result;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            long durationNanos = System.nanoTime() - startNanos;
            long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
            
            // 실패한 경우에도 성능 통계 업데이트
            updateMethodStats(methodKey, duration, false);
            logPerformanceResult(methodKey, duration, durationNanos, 
                               getPerformanceLevel(duration), false, e, memoryBefore, memoryAfter);
            
            throw e;
            
        } finally {
            // MDC 정리
            MDC.remove("perf.method");
            MDC.remove("perf.startTime");
            MDC.remove("perf.memoryBefore");
        }
    }

    private String getMethodKey(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getDeclaringType().getSimpleName() + "." + signature.getName();
    }

    private void updateMethodStats(String methodKey, long duration, boolean success) {
        methodStats.computeIfAbsent(methodKey, k -> new MethodStats()).update(duration, success);
    }

    private PerformanceLevel getPerformanceLevel(long duration) {
        if (duration >= verySlowThreshold) return PerformanceLevel.VERY_SLOW;
        if (duration >= slowThreshold) return PerformanceLevel.SLOW;
        if (duration >= 500) return PerformanceLevel.MODERATE;
        if (duration >= 100) return PerformanceLevel.FAST;
        return PerformanceLevel.VERY_FAST;
    }

    private void logPerformanceResult(String methodKey, long duration, long durationNanos, 
                                    PerformanceLevel level, boolean success, Exception error, 
                                    long memoryBefore, long memoryAfter) {
        
        MethodStats stats = methodStats.get(methodKey);
        long memoryUsed = memoryAfter - memoryBefore;
        
        MDC.put("perf.duration", String.valueOf(duration));
        MDC.put("perf.durationNanos", String.valueOf(durationNanos));
        MDC.put("perf.level", level.name());
        MDC.put("perf.success", String.valueOf(success));
        MDC.put("perf.memoryUsed", String.valueOf(memoryUsed));
        MDC.put("perf.memoryAfter", String.valueOf(memoryAfter));
        
        if (stats != null) {
            MDC.put("perf.callCount", String.valueOf(stats.getTotalCalls()));
            MDC.put("perf.avgDuration", String.valueOf(stats.getAverageDuration()));
            MDC.put("perf.successRate", String.valueOf(stats.getSuccessRate()));
        }

        // Get current user for performance tracking
        String userId = getCurrentUserId();
        
        // Create performance event
        PerformanceEvent performanceEvent = PerformanceEvent.builder()
                .operation(methodKey)
                .durationMs(duration)
                .durationNanos(durationNanos)
                .performanceLevel(level.name())
                .success(success)
                .errorType(error != null ? error.getClass().getSimpleName() : null)
                .userId(userId)
                .methodName(getMethodNameFromKey(methodKey))
                .className(getClassNameFromKey(methodKey))
                .memoryBefore(memoryBefore)
                .memoryAfter(memoryAfter)
                .cpuTime(durationNanos) // Using nano time as CPU time approximation
                .build();
        
        structuredLogger.performance(performanceEvent);

        // MDC 정리
        MDC.remove("perf.duration");
        MDC.remove("perf.durationNanos");
        MDC.remove("perf.level");
        MDC.remove("perf.success");
        MDC.remove("perf.callCount");
        MDC.remove("perf.avgDuration");
        MDC.remove("perf.successRate");
        MDC.remove("perf.memoryUsed");
        MDC.remove("perf.memoryAfter");
    }

    /**
     * Get current authenticated user ID
     */
    private String getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return "anonymous";
            }
            return String.valueOf(auth.getName());
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Extract method name from method key (ClassName.methodName)
     */
    private String getMethodNameFromKey(String methodKey) {
        if (methodKey == null || !methodKey.contains(".")) {
            return methodKey;
        }
        return methodKey.substring(methodKey.lastIndexOf('.') + 1);
    }

    /**
     * Extract class name from method key (ClassName.methodName)
     */
    private String getClassNameFromKey(String methodKey) {
        if (methodKey == null || !methodKey.contains(".")) {
            return "Unknown";
        }
        return methodKey.substring(0, methodKey.lastIndexOf('.'));
    }

    /**
     * 메서드별 성능 통계를 가져옵니다.
     */
    public MethodStats getMethodStats(String methodKey) {
        return methodStats.get(methodKey);
    }

    /**
     * 모든 메서드의 성능 통계를 가져옵니다.
     */
    public ConcurrentHashMap<String, MethodStats> getAllMethodStats() {
        return new ConcurrentHashMap<>(methodStats);
    }

    /**
     * 성능 통계를 초기화합니다.
     */
    public void clearStats() {
        methodStats.clear();
        log.info("Performance statistics cleared");
    }

    /**
     * 성능 레벨 enum
     */
    public enum PerformanceLevel {
        VERY_FAST, FAST, MODERATE, SLOW, VERY_SLOW
    }

    /**
     * 메서드별 성능 통계를 저장하는 클래스
     */
    public static class MethodStats {
        private final LongAdder totalDuration = new LongAdder();
        private final LongAdder totalCalls = new LongAdder();
        private final LongAdder successCalls = new LongAdder();
        private final AtomicLong minDuration = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong maxDuration = new AtomicLong(0);

        public void update(long duration, boolean success) {
            totalDuration.add(duration);
            totalCalls.increment();
            
            if (success) {
                successCalls.increment();
            }
            
            // 최소/최대 시간 업데이트
            minDuration.updateAndGet(current -> Math.min(current, duration));
            maxDuration.updateAndGet(current -> Math.max(current, duration));
        }

        public long getTotalCalls() {
            return totalCalls.sum();
        }

        public long getSuccessCalls() {
            return successCalls.sum();
        }

        public double getSuccessRate() {
            long total = getTotalCalls();
            return total > 0 ? (getSuccessCalls() * 100.0) / total : 0.0;
        }

        public long getAverageDuration() {
            long total = getTotalCalls();
            return total > 0 ? totalDuration.sum() / total : 0;
        }

        public long getMinDuration() {
            long min = minDuration.get();
            return min == Long.MAX_VALUE ? 0 : min;
        }

        public long getMaxDuration() {
            return maxDuration.get();
        }

        public long getTotalDuration() {
            return totalDuration.sum();
        }

        @Override
        public String toString() {
            return String.format("MethodStats{calls=%d, success=%.1f%%, avg=%dms, min=%dms, max=%dms}", 
                               getTotalCalls(), getSuccessRate(), getAverageDuration(), 
                               getMinDuration(), getMaxDuration());
        }
    }
}