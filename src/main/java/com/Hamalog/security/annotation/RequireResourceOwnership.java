package com.Hamalog.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireResourceOwnership {
    
    String resourceType();
    
    String paramName();
    
    int errorStatus() default 403;
    
    String errorMessage() default "접근 권한이 없습니다.";
}