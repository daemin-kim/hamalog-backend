package com.Hamalog.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
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
}
