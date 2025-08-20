package com.Hamalog.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 리소스 소유권 검증을 위한 어노테이션
 * 컨트롤러 메서드에 적용하여 현재 인증된 사용자가 해당 리소스의 소유자인지 검증합니다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireResourceOwnership {
    
    /**
     * 리소스 타입을 지정합니다.
     * 예: "medication-record", "medication-schedule", "member"
     */
    String resourceType();
    
    /**
     * 리소스 ID를 포함하는 파라미터 이름을 지정합니다.
     * 예: "medicationRecordId", "medicationScheduleId", "memberId"
     */
    String paramName();
    
    /**
     * 소유권 검증 실패 시 반환할 HTTP 상태 코드
     * 기본값은 403 (FORBIDDEN)
     */
    int errorStatus() default 403;
    
    /**
     * 소유권 검증 실패 시 반환할 에러 메시지
     */
    String errorMessage() default "접근 권한이 없습니다.";
}