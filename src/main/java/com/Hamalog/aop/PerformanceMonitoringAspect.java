package com.Hamalog.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 성능 모니터링을 위한 AOP Aspect
 * 메서드 실행 시간, 호출 횟수, 평균 응답 시간 등을 추적하고 모니터링합니다.
 */
@Slf4j
@Aspect
@Component
@ConditionalOnProperty(name = "app.aop.performance.enabled", matchIfMissing = true)
public class PerformanceMonitoringAspect {

    @Value("${app.monitoring.performance.slow-threshold:1000}")
    private long slowThreshold;

    @Value("${app.monitoring.performance.very-slow-threshold:3000}")
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
        
        // MDC에 성능 모니터링 컨텍스트 추가
        MDC.put("perf.method", methodKey);
        MDC.put("perf.startTime", String.valueOf(startTime));

        try {
            Object result = joinPoint.proceed();
            
            long duration = System.currentTimeMillis() - startTime;
            long durationNanos = System.nanoTime() - startNanos;
            
            // 성능 통계 업데이트
            updateMethodStats(methodKey, duration, true);
            
            // 성능 레벨 결정 및 로깅
            PerformanceLevel level = getPerformanceLevel(duration);
            logPerformanceResult(methodKey, duration, durationNanos, level, true, null);
            
            return result;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            long durationNanos = System.nanoTime() - startNanos;
            
            // 실패한 경우에도 성능 통계 업데이트
            updateMethodStats(methodKey, duration, false);
            logPerformanceResult(methodKey, duration, durationNanos, 
                               getPerformanceLevel(duration), false, e);
            
            throw e;
            
        } finally {
            // MDC 정리
            MDC.remove("perf.method");
            MDC.remove("perf.startTime");
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
                                    PerformanceLevel level, boolean success, Exception error) {
        
        MethodStats stats = methodStats.get(methodKey);
        
        MDC.put("perf.duration", String.valueOf(duration));
        MDC.put("perf.durationNanos", String.valueOf(durationNanos));
        MDC.put("perf.level", level.name());
        MDC.put("perf.success", String.valueOf(success));
        
        if (stats != null) {
            MDC.put("perf.callCount", String.valueOf(stats.getTotalCalls()));
            MDC.put("perf.avgDuration", String.valueOf(stats.getAverageDuration()));
            MDC.put("perf.successRate", String.valueOf(stats.getSuccessRate()));
        }

        if (success) {
            if (level.ordinal() >= PerformanceLevel.SLOW.ordinal()) {
                log.warn("SLOW_PERFORMANCE: {} | {}ms | {} | Avg: {}ms | Calls: {} | Success: {}%", 
                        methodKey, duration, level, 
                        stats != null ? stats.getAverageDuration() : "N/A",
                        stats != null ? stats.getTotalCalls() : "N/A",
                        stats != null ? String.format("%.1f", stats.getSuccessRate()) : "N/A");
            } else {
                log.debug("PERFORMANCE: {} | {}ms | {} | Avg: {}ms", 
                         methodKey, duration, level, 
                         stats != null ? stats.getAverageDuration() : "N/A");
            }
        } else {
            log.error("PERFORMANCE_ERROR: {} | {}ms | {} | Error: {} | Avg: {}ms | Success: {}%", 
                     methodKey, duration, level, error.getClass().getSimpleName(),
                     stats != null ? stats.getAverageDuration() : "N/A",
                     stats != null ? String.format("%.1f", stats.getSuccessRate()) : "N/A");
        }

        // MDC 정리
        MDC.remove("perf.duration");
        MDC.remove("perf.durationNanos");
        MDC.remove("perf.level");
        MDC.remove("perf.success");
        MDC.remove("perf.callCount");
        MDC.remove("perf.avgDuration");
        MDC.remove("perf.successRate");
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