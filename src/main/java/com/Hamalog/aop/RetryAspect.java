package com.Hamalog.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 재시도 로직을 위한 AOP Aspect
 * @Retryable 어노테이션이 적용된 메서드에서 예외 발생 시 자동으로 재시도를 수행합니다.
 */
@Slf4j
@Aspect
@Component
@ConditionalOnProperty(name = "app.aop.retry.enabled", matchIfMissing = true)
public class RetryAspect {

    @Value("${app.retry.default-attempts:3}")
    private int defaultMaxAttempts;

    @Value("${app.retry.default-delay:1000}")
    private long defaultDelay;

    @Value("${app.retry.max-delay:10000}")
    private long maxDelay;

    @Around("@annotation(retryable)")
    public Object retry(ProceedingJoinPoint joinPoint, Retryable retryable) throws Throwable {
        String methodName = getMethodName(joinPoint);
        int maxAttempts = retryable.maxAttempts() > 0 ? retryable.maxAttempts() : defaultMaxAttempts;
        long delay = retryable.delay() > 0 ? retryable.delay() : defaultDelay;
        double backoffMultiplier = retryable.backoffMultiplier();
        boolean randomizeDelay = retryable.randomizeDelay();

        String retryId = java.util.UUID.randomUUID().toString().substring(0, 8);
        MDC.put("retry.id", retryId);
        MDC.put("retry.method", methodName);
        MDC.put("retry.maxAttempts", String.valueOf(maxAttempts));

        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            MDC.put("retry.currentAttempt", String.valueOf(attempt));
            
            try {
                if (attempt > 1) {
                    log.info("RETRY_ATTEMPT: {} | Method: {} | Attempt: {}/{} | Delay: {}ms", 
                            retryId, methodName, attempt, maxAttempts, 
                            attempt > 1 ? getCurrentDelay(delay, attempt, backoffMultiplier) : 0);
                }
                
                Object result = joinPoint.proceed();
                
                if (attempt > 1) {
                    MDC.put("retry.status", "SUCCESS_AFTER_RETRY");
                    log.info("RETRY_SUCCESS: {} | Method: {} | Succeeded on attempt: {}/{}", 
                            retryId, methodName, attempt, maxAttempts);
                } else {
                    MDC.put("retry.status", "SUCCESS_FIRST_TRY");
                }
                
                return result;
                
            } catch (Exception e) {
                lastException = e;
                MDC.put("retry.lastError", e.getClass().getSimpleName());
                MDC.put("retry.lastErrorMessage", e.getMessage());
                
                if (!shouldRetry(e, retryable.retryOn(), retryable.noRetryOn())) {
                    log.error("RETRY_NOT_APPLICABLE: {} | Method: {} | Error: {} | Message: {} | Attempt: {}/{}", 
                             retryId, methodName, e.getClass().getSimpleName(), e.getMessage(), attempt, maxAttempts);
                    MDC.put("retry.status", "NO_RETRY");
                    throw e;
                }
                
                if (attempt == maxAttempts) {
                    log.error("RETRY_EXHAUSTED: {} | Method: {} | Final attempt failed | Error: {} | Message: {} | Attempts: {}/{}", 
                             retryId, methodName, e.getClass().getSimpleName(), e.getMessage(), attempt, maxAttempts, e);
                    MDC.put("retry.status", "EXHAUSTED");
                    break;
                } else {
                    long currentDelay = getCurrentDelay(delay, attempt, backoffMultiplier);
                    if (randomizeDelay) {
                        currentDelay = (long) (currentDelay * (0.5 + Math.random() * 0.5)); // 50%-100% of calculated delay
                    }
                    currentDelay = Math.min(currentDelay, maxDelay);
                    
                    log.warn("RETRY_FAILED_ATTEMPT: {} | Method: {} | Error: {} | Message: {} | Attempt: {}/{} | Next retry in: {}ms", 
                            retryId, methodName, e.getClass().getSimpleName(), e.getMessage(), 
                            attempt, maxAttempts, currentDelay);
                    
                    try {
                        Thread.sleep(currentDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("RETRY_INTERRUPTED: {} | Method: {} | Retry interrupted", retryId, methodName);
                        throw e;
                    }
                }
            }
        }
        
        // 모든 시도가 실패한 경우
        clearRetryContext();
        throw lastException;
    }

    private String getMethodName(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getDeclaringType().getSimpleName() + "." + signature.getName();
    }

    private boolean shouldRetry(Exception e, Class<? extends Throwable>[] retryOn, Class<? extends Throwable>[] noRetryOn) {
        // noRetryOn에 해당하는 예외는 재시도하지 않음
        for (Class<? extends Throwable> noRetryClass : noRetryOn) {
            if (noRetryClass.isInstance(e)) {
                return false;
            }
        }
        
        // retryOn이 명시되어 있으면 해당 예외만 재시도
        if (retryOn.length > 0) {
            for (Class<? extends Throwable> retryClass : retryOn) {
                if (retryClass.isInstance(e)) {
                    return true;
                }
            }
            return false;
        }
        
        // 기본적으로 재시도할 예외 타입들
        return e instanceof DataAccessException ||
               e instanceof ResourceAccessException ||
               e instanceof java.net.SocketTimeoutException ||
               e instanceof java.net.ConnectException ||
               e instanceof java.util.concurrent.TimeoutException ||
               (e instanceof RuntimeException && e.getMessage() != null && 
                (e.getMessage().contains("timeout") || e.getMessage().contains("connection")));
    }

    private long getCurrentDelay(long baseDelay, int attempt, double backoffMultiplier) {
        if (backoffMultiplier <= 1.0) {
            return baseDelay;
        }
        return (long) (baseDelay * Math.pow(backoffMultiplier, attempt - 1));
    }

    private void clearRetryContext() {
        MDC.remove("retry.id");
        MDC.remove("retry.method");
        MDC.remove("retry.maxAttempts");
        MDC.remove("retry.currentAttempt");
        MDC.remove("retry.status");
        MDC.remove("retry.lastError");
        MDC.remove("retry.lastErrorMessage");
    }

    /**
     * 메서드에 재시도 로직을 적용하는 어노테이션
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Retryable {
        
        /**
         * 최대 재시도 횟수 (기본값: 설정된 기본값 사용)
         */
        int maxAttempts() default 0;
        
        /**
         * 재시도 간격 (밀리초, 기본값: 설정된 기본값 사용)
         */
        long delay() default 0;
        
        /**
         * 백오프 승수 (재시도마다 지연시간을 곱할 값, 기본값: 2.0)
         */
        double backoffMultiplier() default 2.0;
        
        /**
         * 지연시간 랜덤화 여부 (기본값: true)
         */
        boolean randomizeDelay() default true;
        
        /**
         * 재시도할 예외 타입들 (명시하지 않으면 기본 예외들에 대해 재시도)
         */
        Class<? extends Throwable>[] retryOn() default {};
        
        /**
         * 재시도하지 않을 예외 타입들
         */
        Class<? extends Throwable>[] noRetryOn() default {};
    }

    /**
     * 서킷 브레이커와 유사한 기능을 제공하는 빠른 실패 메커니즘
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface CircuitBreaker {
        
        /**
         * 실패 임계값 (이 횟수만큼 연속 실패하면 서킷이 열림)
         */
        int failureThreshold() default 5;
        
        /**
         * 서킷이 열린 상태에서 복구 시도까지의 대기시간 (밀리초)
         */
        long recoveryTimeout() default 60000;
        
        /**
         * 서킷 브레이커 이름 (통계를 위한 구분자)
         */
        String name() default "";
    }
}