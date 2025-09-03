package com.Hamalog.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class ApiLoggingAspect {

    @Pointcut("execution(public * com.Hamalog.controller..*(..))")
    public void allControllerMethods() {}

    @Around("allControllerMethods()")
    public Object logApiRequestAndResponse(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String existingRequestId = MDC.get("requestId");
        boolean putRequestId = false;
        if (existingRequestId == null) {
            MDC.put("requestId", java.util.UUID.randomUUID().toString());
            putRequestId = true;
        }
        String requestId = MDC.get("requestId");

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getDeclaringType().getSimpleName() + "." + signature.getName();
        String params = getParameterInfo(signature, joinPoint.getArgs());
        String user = getAuthenticatedUser();

        // Add additional structured logging context
        MDC.put("api.method", methodName);
        MDC.put("api.user", user);
        
        log.info("API Call: {} | User: {} | Params: {}", methodName, user, params);

        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - startTime;
            
            // Add performance metrics to MDC
            MDC.put("api.duration", String.valueOf(elapsed));
            MDC.put("api.status", "success");
            
            String performanceText = getPerformanceText(elapsed);
            log.info("API Success: {} | User: {} | Time: {}ms {} | Result: {}",
                    methodName, user, elapsed, performanceText, shorten(result));
            return result;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            
            // Add error context to MDC
            MDC.put("api.duration", String.valueOf(elapsed));
            MDC.put("api.status", "error");
            MDC.put("api.errorType", e.getClass().getSimpleName());
            
            log.error("API Error: {} | User: {} | Time: {}ms | Type: {} | Message: {}",
                    methodName, user, elapsed, e.getClass().getSimpleName(), e.getMessage(), e);
            throw e;
        } finally {
            // Clean up MDC context
            MDC.remove("api.method");
            MDC.remove("api.user");
            MDC.remove("api.duration");
            MDC.remove("api.status");
            MDC.remove("api.errorType");
            
            if (putRequestId) {
                MDC.remove("requestId");
            }
        }
    }

    private String getParameterInfo(MethodSignature signature, Object[] args) {
        String[] paramNames = signature.getParameterNames();
        if (paramNames == null || args == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramNames.length; i++) {
            String name = paramNames[i];
            String value = String.valueOf(args[i]);
            if (isSensitive(name)) {
                value = "***";
            }
            sb.append(name).append('=').append(shorten(value)).append(", ");
        }
        return !sb.isEmpty() ? sb.substring(0, sb.length() - 2) : "";
    }

    private boolean isSensitive(String name) {
        String n = name == null ? "" : name.toLowerCase();
        return n.contains("password") || n.contains("token") || n.contains("authorization") || n.contains("secret");
    }

    private String getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            return "anonymous";
        return String.valueOf(auth.getName());
    }

    private String shorten(Object obj) {
        if (obj == null) return "null";
        String s = obj.toString();
        return s.length() > 200 ? s.substring(0, 200) + "..." : s;
    }

    private String getPerformanceText(long elapsed) {
        if (elapsed < 100) return "VERY_FAST"; // Very fast
        if (elapsed < 500) return "FAST"; // Fast
        if (elapsed < 1000) return "MODERATE"; // Moderate
        if (elapsed < 3000) return "SLOW"; // Slow
        return "VERY_SLOW"; // Very slow
    }
}
