package com.Hamalog.aop;

import com.Hamalog.logging.StructuredLogger;
import com.Hamalog.logging.events.BusinessEvent;
import com.Hamalog.security.SecurityContextUtils;
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
 *
 * 참고: 기본값으로 비활성화됩니다(matchIfMissing = false).
 * 필요 시 app.aop.service-logging.enabled=true로 활성화하세요.
 * 활성화 시 BusinessAuditAspect와의 중복 로깅에 주의하세요.
 */
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.aop.service-logging.enabled", havingValue = "true", matchIfMissing = false)
@Order(3)
public class ServiceLoggingAspect {

    private final StructuredLogger structuredLogger;
    private final SecurityContextUtils securityContextUtils;

    @Pointcut("execution(public * com.Hamalog.service..*(..)) " +
              "&& !execution(public * com.Hamalog.service.auth..*(..))")
    public void nonAuthServiceMethods() {}

    @Around("nonAuthServiceMethods()")
    public Object logServiceCall(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        String userId = securityContextUtils.getCurrentUserId();

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();

            long durationMs = System.currentTimeMillis() - start;
            Map<String, Object> successMeta = createMetadata(className, methodName, userId, durationMs);

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
            Map<String, Object> errorMeta = createMetadata(className, methodName, userId, durationMs);
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

    private Map<String, Object> createMetadata(String className, String methodName,
                                                String userId, long durationMs) {
        Map<String, Object> metadata = new HashMap<>(6);
        metadata.put("service_class", className);
        metadata.put("service_method", methodName);
        metadata.put("user_id", userId);
        metadata.put("duration_ms", durationMs);
        return metadata;
    }
}
