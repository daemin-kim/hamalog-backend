package com.Hamalog.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
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

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getDeclaringType().getSimpleName() + "." + signature.getName();
        String params = getParameterInfo(signature, joinPoint.getArgs());
        String user = getAuthenticatedUser();

        log.info("[API 요청] {} | user={} | params={}", methodName, user, params);

        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("[API 응답] {} | user={} | time={}ms | result={}",
                    methodName, user, elapsed, shorten(result));
            return result;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("[API 예외] {} | user={} | time={}ms | error={}",
                    methodName, user, elapsed, e.toString(), e);
            throw e;
        }
    }

    private String getParameterInfo(MethodSignature signature, Object[] args) {
        String[] paramNames = signature.getParameterNames();
        if (paramNames == null || args == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramNames.length; i++) {
            sb.append(paramNames[i]).append('=').append(args[i]).append(", ");
        }
        return !sb.isEmpty() ? sb.substring(0, sb.length() - 2) : "";
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
