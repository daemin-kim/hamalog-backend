package com.Hamalog.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class ApiLoggingAspect {

    @Pointcut("execution(public * com.Hamalog.controller..*(..))")
    public void allControllerMethods() {}

    @Before("allControllerMethods()")
    public void logApiCall(JoinPoint joinPoint) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        String methodName = methodSignature.getDeclaringType().getSimpleName() + "." + methodSignature.getName();

        Object[] args = joinPoint.getArgs();

        log.info("API 호출: {}", methodName);
        log.debug("파라미터: {}", Arrays.toString(args));
    }

    @Around("allControllerMethods()")
    public Object logApiRequestAndResponse(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getDeclaringType().getSimpleName() + "." + signature.getName();

        // 파라미터명/값 출력
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        StringBuilder params = new StringBuilder();
        for (int i = 0; i < paramNames.length; i++) {
            params.append(paramNames[i]).append("=").append(args[i]).append(", ");
        }

        // 요청자 정보
        String user = "anonymous";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            user = String.valueOf(auth.getPrincipal());
        }

        // 요청 로그
        log.info("[API 요청] {} | user={} | params=[{}]", methodName, user, params);

        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("[API 응답] {} | user={} | time={}ms | result={}", methodName, user, elapsed, shorten(result));
            return result;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("[API 예외] {} | user={} | time={}ms | error={}", methodName, user, elapsed, e.toString(), e);
            throw e;
        }
    }

    // JSON 직렬화 또는 toString() 등으로 반환 데이터를 간단하게 처리(너무 길면 자름)
    private String shorten(Object obj) {
        if (obj == null) return "null";
        String s = obj.toString();
        return s.length() > 200 ? s.substring(0, 200) + "..." : s;
    }

}
