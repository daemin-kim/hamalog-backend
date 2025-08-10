package com.Hamalog.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Aspect
@Component
public class ApiLoggingAspect {

    @Pointcut("execution(public * com.Hamalog.controller..*(..))")
    public void allControllerMethods() {}

    @Around("allControllerMethods()")
    public Object logApiRequestAndResponse(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getDeclaringType().getSimpleName() + "." + signature.getName();
        String params = getParameterInfo(signature, joinPoint.getArgs());
        String user = getAuthenticatedUser();

        log.info("[API 요청] {} | requestId={} | user={} | params={}", methodName, requestId, user, params);

        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("[API 응답] {} | requestId={} | user={} | time={}ms | result={}",
                    methodName, requestId, user, elapsed, shorten(result));
            return result;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("[API 예외] {} | requestId={} | user={} | time={}ms | error={}",
                    methodName, requestId, user, elapsed, e.toString(), e);
            throw e;
        } finally {
            MDC.remove("requestId");
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
        return String.valueOf(auth.getPrincipal());
    }

    private String shorten(Object obj) {
        if (obj == null) return "null";
        String s = obj.toString();
        return s.length() > 200 ? s.substring(0, 200) + "..." : s;
    }
}
