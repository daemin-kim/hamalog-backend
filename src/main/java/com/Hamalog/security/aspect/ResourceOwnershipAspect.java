package com.Hamalog.security.aspect;

import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.security.annotation.RequireResourceOwnership;
import com.Hamalog.service.medication.MedicationRecordService;
import com.Hamalog.service.medication.MedicationScheduleService;
import com.Hamalog.service.sideEffect.SideEffectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Parameter;

/**
 * 리소스 소유권 검증을 위한 AOP Aspect
 * @RequireResourceOwnership 어노테이션이 적용된 메서드에서 자동으로 소유권을 검증합니다.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ResourceOwnershipAspect {
    
    private final MedicationRecordService medicationRecordService;
    private final MedicationScheduleService medicationScheduleService;
    private final SideEffectService sideEffectService;
    
    @Around("@annotation(requireResourceOwnership)")
    public Object checkResourceOwnership(ProceedingJoinPoint joinPoint, RequireResourceOwnership requireResourceOwnership) throws Throwable {
        String requestId = MDC.get("requestId");
        
        // 현재 인증된 사용자 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("[UNAUTHORIZED] requestId={} | No authenticated user found", requestId);
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        
        String currentLoginId = authentication.getName();
        
        // 메서드 파라미터에서 리소스 ID 추출
        Long resourceId = extractResourceId(joinPoint, requireResourceOwnership.paramName());
        if (resourceId == null) {
            log.error("[AUTHORIZATION_ERROR] requestId={} | Resource ID parameter '{}' not found", 
                     requestId, requireResourceOwnership.paramName());
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
        
        // 리소스 타입에 따른 소유권 검증
        boolean isOwner = checkOwnership(requireResourceOwnership.resourceType(), resourceId, currentLoginId);
        
        if (!isOwner) {
            log.warn("[FORBIDDEN] requestId={} | user={} | resourceType={} | resourceId={} | Access denied", 
                    requestId, currentLoginId, requireResourceOwnership.resourceType(), resourceId);
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        
        log.debug("[AUTHORIZATION_SUCCESS] requestId={} | user={} | resourceType={} | resourceId={}", 
                 requestId, currentLoginId, requireResourceOwnership.resourceType(), resourceId);
        
        return joinPoint.proceed();
    }
    
    /**
     * 메서드 파라미터에서 리소스 ID를 추출합니다.
     */
    private Long extractResourceId(ProceedingJoinPoint joinPoint, String paramName) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Parameter[] parameters = signature.getMethod().getParameters();
        Object[] args = joinPoint.getArgs();
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            
            // @PathVariable이나 @RequestParam의 value와 매칭하거나 파라미터 이름과 직접 매칭
            if (parameter.getName().equals(paramName) || 
                paramName.equals(getPathVariableName(parameter)) ||
                paramName.equals(getRequestParamName(parameter))) {
                
                Object arg = args[i];
                if (arg instanceof Long) {
                    return (Long) arg;
                } else if (arg instanceof String) {
                    try {
                        return Long.parseLong((String) arg);
                    } catch (NumberFormatException e) {
                        log.warn("Could not parse resource ID from string: {}", arg);
                        return null;
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * @PathVariable 어노테이션의 value를 가져옵니다.
     */
    private String getPathVariableName(Parameter parameter) {
        org.springframework.web.bind.annotation.PathVariable pathVariable = 
            parameter.getAnnotation(org.springframework.web.bind.annotation.PathVariable.class);
        return pathVariable != null ? pathVariable.value() : null;
    }
    
    /**
     * @RequestParam 어노테이션의 value를 가져옵니다.
     */
    private String getRequestParamName(Parameter parameter) {
        org.springframework.web.bind.annotation.RequestParam requestParam = 
            parameter.getAnnotation(org.springframework.web.bind.annotation.RequestParam.class);
        return requestParam != null ? requestParam.value() : null;
    }
    
    /**
     * 리소스 타입에 따라 소유권을 검증합니다.
     */
    private boolean checkOwnership(String resourceType, Long resourceId, String loginId) {
        switch (resourceType) {
            case "medication-record":
                return medicationRecordService.isOwnerOfRecord(resourceId, loginId);
            case "medication-schedule":
                // MedicationScheduleService uses memberId-based validation
                // Need to get the schedule first to check ownership through member
                try {
                    var schedule = medicationScheduleService.getMedicationSchedule(resourceId);
                    return medicationScheduleService.isOwner(schedule.getMember().getMemberId(), loginId);
                } catch (Exception e) {
                    log.warn("Failed to validate schedule ownership for scheduleId={}: {}", resourceId, e.getMessage());
                    return false;
                }
            case "medication-schedule-by-member":
                // Direct member-based validation for schedule lists
                return medicationScheduleService.isOwner(resourceId, loginId);
            case "member":
                return sideEffectService.isOwner(resourceId, loginId);
            default:
                log.error("Unknown resource type: {}", resourceType);
                return false;
        }
    }
}