package com.Hamalog.aop;

import com.Hamalog.logging.StructuredLogger;
import com.Hamalog.logging.events.BusinessEvent;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 서비스 레이어 공통 구조화 로깅 Aspect
 * - StructuredLogger를 통해 서비스 호출의 입력/출력을 일관되게 로깅
 * - 민감정보는 기존 마스킹 유틸(SensitiveDataMasker)을 통해 처리됨
 */
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.aop.service-logging.enabled", matchIfMissing = true)
@Order(3)
public class ServiceLoggingAspect {

    private final StructuredLogger structuredLogger;

    @Pointcut("execution(public * com.Hamalog.service..*(..))")
    public void allServiceMethods() {}

    @Around("allServiceMethods()")
    public Object logServiceCall(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();

        Map<String, Object> metadata = new HashMap<>(8);
        metadata.put("service_class", className);
        metadata.put("service_method", methodName);
        metadata.put("arg_count", joinPoint.getArgs() != null ? joinPoint.getArgs().length : 0);

        structuredLogger.business(BusinessEvent.builder()
            .eventType("SERVICE_CALL")
            .entity(className)
            .action(methodName)
            .result("START")
            .metadata(metadata)
            .build());

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();

            long durationMs = System.currentTimeMillis() - start;
            Map<String, Object> successMeta = new HashMap<>(metadata);
            successMeta.put("duration_ms", durationMs);

            structuredLogger.business(BusinessEvent.builder()
                .eventType("SERVICE_CALL")
                .entity(className)
                .action(methodName)
                .result("SUCCESS")
                .metadata(successMeta)
                .build());

            return result;
        } catch (Throwable t) {
            long durationMs = System.currentTimeMillis() - start;
            Map<String, Object> errorMeta = new HashMap<>(metadata);
            errorMeta.put("duration_ms", durationMs);
            errorMeta.put("error_type", t.getClass().getSimpleName());

            structuredLogger.business(BusinessEvent.builder()
                .eventType("SERVICE_CALL")
                .entity(className)
                .action(methodName)
                .result("ERROR")
                .metadata(errorMeta)
                .build());
            throw t;
        }
    }
}
