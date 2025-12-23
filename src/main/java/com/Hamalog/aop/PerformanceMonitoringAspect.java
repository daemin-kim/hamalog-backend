package com.Hamalog.aop;

import com.Hamalog.logging.StructuredLogger;
import com.Hamalog.logging.events.PerformanceEvent;
import com.Hamalog.security.SecurityContextUtils;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 성능 모니터링 Aspect
 * 서비스 및 리포지토리 레이어의 성능을 측정하고 기록합니다.
 *
 * 중요: 기본적으로 비활성화됩니다. 운영 환경에서 성능 문제 진단 시에만 활성화하세요.
 * ServiceLoggingAspect와 함께 사용할 경우 로그 볼륨이 크게 증가할 수 있습니다.
 *
 * 활성화: app.aop.performance.enabled=true
 */
@Slf4j
@Aspect
@Component
@ConditionalOnProperty(name = "app.aop.performance.enabled", havingValue = "true", matchIfMissing = false)
@Order(2)
public class PerformanceMonitoringAspect {

    private final StructuredLogger structuredLogger;
    private final SecurityContextUtils securityContextUtils;

    @Value("${app.aop.performance.slow-threshold:1000}")
    private long slowThreshold;

    @Value("${app.aop.performance.very-slow-threshold:3000}")
    private long verySlowThreshold;

    // 메서드별 성능 통계 저장
    private final ConcurrentHashMap<String, MethodStats> methodStats = new ConcurrentHashMap<>();

    public PerformanceMonitoringAspect(StructuredLogger structuredLogger,
                                        SecurityContextUtils securityContextUtils) {
        this.structuredLogger = structuredLogger;
        this.securityContextUtils = securityContextUtils;
    }

    // Repository만 모니터링 (Service는 ServiceLoggingAspect에서 처리)
    @Pointcut("execution(public * com.Hamalog.repository..*(..))")
    public void repositoryMethods() {}

    @Around("repositoryMethods()")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodKey = getMethodKey(joinPoint);
        long startTime = System.currentTimeMillis();
        long startNanos = System.nanoTime();

        try {
            Object result = joinPoint.proceed();
            
            long duration = System.currentTimeMillis() - startTime;
            updateMethodStats(methodKey, duration, true);
            
            // 느린 쿼리만 로깅하여 로그 볼륨 감소
            if (duration >= slowThreshold) {
                logPerformanceEvent(methodKey, duration, System.nanoTime() - startNanos, true, null);
            }

            return result;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            updateMethodStats(methodKey, duration, false);
            logPerformanceEvent(methodKey, duration, System.nanoTime() - startNanos, false, e);
            throw e;
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

    private void logPerformanceEvent(String methodKey, long duration, long durationNanos,
                                     boolean success, Exception error) {
        PerformanceLevel level = getPerformanceLevel(duration);
        String userId = securityContextUtils.getCurrentUserId();

        PerformanceEvent performanceEvent = PerformanceEvent.builder()
                .operation(methodKey)
                .durationMs(duration)
                .durationNanos(durationNanos)
                .performanceLevel(level.name())
                .success(success)
                .errorType(error != null ? error.getClass().getSimpleName() : null)
                .userId(userId)
                .methodName(extractMethodName(methodKey))
                .className(extractClassName(methodKey))
                .memoryBefore(0L)
                .memoryAfter(0L)
                .cpuTime(durationNanos)
                .build();
        
        if (level == PerformanceLevel.VERY_SLOW) {
            log.warn("SLOW_QUERY: {} took {}ms", methodKey, duration);
        }

        structuredLogger.performance(performanceEvent);
    }

    private String extractMethodName(String methodKey) {
        if (methodKey == null || !methodKey.contains(".")) return methodKey;
        return methodKey.substring(methodKey.lastIndexOf('.') + 1);
    }

    private String extractClassName(String methodKey) {
        if (methodKey == null || !methodKey.contains(".")) return "Unknown";
        return methodKey.substring(0, methodKey.lastIndexOf('.'));
    }

    public MethodStats getMethodStats(String methodKey) {
        return methodStats.get(methodKey);
    }

    public ConcurrentHashMap<String, MethodStats> getAllMethodStats() {
        return new ConcurrentHashMap<>(methodStats);
    }

    public void clearStats() {
        methodStats.clear();
        log.info("Performance statistics cleared");
    }

    public enum PerformanceLevel {
        VERY_FAST, FAST, MODERATE, SLOW, VERY_SLOW
    }

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
            minDuration.updateAndGet(current -> Math.min(current, duration));
            maxDuration.updateAndGet(current -> Math.max(current, duration));
        }

        public long getTotalCalls() { return totalCalls.sum(); }
        public long getSuccessCalls() { return successCalls.sum(); }
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
        public long getMaxDuration() { return maxDuration.get(); }
        public long getTotalDuration() { return totalDuration.sum(); }

        @Override
        public String toString() {
            return String.format("MethodStats{calls=%d, success=%.1f%%, avg=%dms, min=%dms, max=%dms}", 
                    getTotalCalls(), getSuccessRate(), getAverageDuration(),
                    getMinDuration(), getMaxDuration());
        }
    }
}
