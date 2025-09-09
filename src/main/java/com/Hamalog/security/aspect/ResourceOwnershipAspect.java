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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Field;
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
        Long resourceId = extractResourceId(joinPoint, requireResourceOwnership);
        if (resourceId == null) {
            log.error("[AUTHORIZATION_ERROR] requestId={} | Resource ID parameter '{}' not found", 
                     requestId, requireResourceOwnership.paramName());
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
        
        // 리소스 타입에 따른 소유권 검증
        boolean isOwner = checkOwnership(requireResourceOwnership.resourceType(), resourceId, currentLoginId, requireResourceOwnership.strategy());
        
        if (!isOwner) {
            log.warn("[FORBIDDEN] requestId={} | user={} | resourceType={} | resourceId={} | Access denied", 
                    requestId, currentLoginId, requireResourceOwnership.resourceType().getValue(), resourceId);
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        
        log.debug("[AUTHORIZATION_SUCCESS] requestId={} | user={} | resourceType={} | resourceId={}", 
                 requestId, currentLoginId, requireResourceOwnership.resourceType().getValue(), resourceId);
        
        return joinPoint.proceed();
    }
    
    /**
     * 메서드 파라미터에서 리소스 ID를 추출합니다.
     */
    private Long extractResourceId(ProceedingJoinPoint joinPoint, RequireResourceOwnership annotation) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Parameter[] parameters = signature.getMethod().getParameters();
        Object[] args = joinPoint.getArgs();
        
        String paramName = annotation.paramName();
        RequireResourceOwnership.ParameterSource source = annotation.source();
        
        switch (source) {
            case PATH_VARIABLE:
            case REQUEST_PARAM:
                return extractFromParameters(parameters, args, paramName, source);
                
            case REQUEST_BODY:
                return extractFromRequestBody(args, annotation.bodyField());
                
            default:
                // Fallback to original logic for backward compatibility
                return extractFromParameters(parameters, args, paramName, RequireResourceOwnership.ParameterSource.PATH_VARIABLE);
        }
    }
    
    /**
     * 메서드 파라미터에서 리소스 ID를 추출합니다 (PATH_VARIABLE, REQUEST_PARAM).
     */
    private Long extractFromParameters(Parameter[] parameters, Object[] args, String paramName, RequireResourceOwnership.ParameterSource source) {
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            boolean matches = false;
            
            if (source == RequireResourceOwnership.ParameterSource.PATH_VARIABLE) {
                matches = parameter.getName().equals(paramName) || paramName.equals(getPathVariableName(parameter));
            } else if (source == RequireResourceOwnership.ParameterSource.REQUEST_PARAM) {
                matches = parameter.getName().equals(paramName) || paramName.equals(getRequestParamName(parameter));
            }
            
            if (matches) {
                return convertToLong(args[i]);
            }
        }
        
        return null;
    }
    
    /**
     * 요청 바디에서 리소스 ID를 추출합니다.
     */
    private Long extractFromRequestBody(Object[] args, String fieldName) {
        for (Object arg : args) {
            if (arg != null && !isPrimitiveType(arg.getClass())) {
                try {
                    Field field = findField(arg.getClass(), fieldName);
                    if (field != null) {
                        field.setAccessible(true);
                        Object value = field.get(arg);
                        return convertToLong(value);
                    }
                } catch (Exception e) {
                    log.warn("Failed to extract field '{}' from request body: {}", fieldName, e.getMessage());
                }
            }
        }
        
        return null;
    }
    
    /**
     * 클래스에서 필드를 찾습니다 (상속 포함).
     */
    private Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }
    
    /**
     * 값을 Long으로 변환합니다.
     */
    private Long convertToLong(Object value) {
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                log.warn("Could not parse resource ID from string: {}", value);
                return null;
            }
        } else if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }
    
    /**
     * 기본 타입인지 확인합니다.
     */
    private boolean isPrimitiveType(Class<?> type) {
        return type.isPrimitive() || 
               type == String.class || 
               type == Integer.class || 
               type == Long.class || 
               type == Double.class || 
               type == Boolean.class;
    }
    
    /**
     * @PathVariable 어노테이션의 value를 가져옵니다.
     */
    private String getPathVariableName(Parameter parameter) {
        PathVariable pathVariable =
            parameter.getAnnotation(PathVariable.class);
        return pathVariable != null ? pathVariable.value() : null;
    }
    
    /**
     * @RequestParam 어노테이션의 value를 가져옵니다.
     */
    private String getRequestParamName(Parameter parameter) {
        RequestParam requestParam =
            parameter.getAnnotation(RequestParam.class);
        return requestParam != null ? requestParam.value() : null;
    }
    
    /**
     * 리소스 타입에 따라 소유권을 검증합니다.
     */
    private boolean checkOwnership(RequireResourceOwnership.ResourceType resourceType, Long resourceId, String loginId, RequireResourceOwnership.OwnershipStrategy strategy) {
        switch (resourceType) {
            case MEDICATION_RECORD:
                return checkMedicationRecordOwnership(resourceId, loginId, strategy);
            case MEDICATION_SCHEDULE:
                return checkMedicationScheduleOwnership(resourceId, loginId, strategy);
            case MEDICATION_SCHEDULE_BY_MEMBER:
                return checkMedicationScheduleByMemberOwnership(resourceId, loginId, strategy);
            case MEMBER:
                return checkMemberOwnership(resourceId, loginId, strategy);
            default:
                log.error("Unknown resource type: {}", resourceType);
                return false;
        }
    }
    
    /**
     * 복약 기록 소유권을 검증합니다.
     */
    private boolean checkMedicationRecordOwnership(Long resourceId, String loginId, RequireResourceOwnership.OwnershipStrategy strategy) {
        switch (strategy) {
            case DIRECT:
                return medicationRecordService.isOwnerOfRecord(resourceId, loginId);
            case THROUGH_SCHEDULE:
                // Get medication record first, then check schedule ownership
                try {
                    var record = medicationRecordService.getMedicationRecord(resourceId);
                    return medicationScheduleService.isOwner(record.getMedicationSchedule().getMember().getMemberId(), loginId);
                } catch (Exception e) {
                    log.warn("Failed to validate record ownership through schedule for recordId={}: {}", resourceId, e.getMessage());
                    return false;
                }
            default:
                log.warn("Unsupported ownership strategy '{}' for medication record", strategy);
                return false;
        }
    }
    
    /**
     * 복약 스케줄 소유권을 검증합니다.
     */
    private boolean checkMedicationScheduleOwnership(Long resourceId, String loginId, RequireResourceOwnership.OwnershipStrategy strategy) {
        switch (strategy) {
            case DIRECT:
                // MedicationScheduleService uses memberId-based validation
                // Need to get the schedule first to check ownership through member
                try {
                    var schedule = medicationScheduleService.getMedicationSchedule(resourceId);
                    return medicationScheduleService.isOwner(schedule.getMember().getMemberId(), loginId);
                } catch (Exception e) {
                    log.warn("Failed to validate schedule ownership for scheduleId={}: {}", resourceId, e.getMessage());
                    return false;
                }
            case THROUGH_MEMBER:
                return medicationScheduleService.isOwner(resourceId, loginId);
            default:
                log.warn("Unsupported ownership strategy '{}' for medication schedule", strategy);
                return false;
        }
    }
    
    /**
     * 멤버를 통한 복약 스케줄 소유권을 검증합니다.
     */
    private boolean checkMedicationScheduleByMemberOwnership(Long resourceId, String loginId, RequireResourceOwnership.OwnershipStrategy strategy) {
        // Direct member-based validation for schedule lists
        return medicationScheduleService.isOwner(resourceId, loginId);
    }
    
    /**
     * 멤버 소유권을 검증합니다.
     */
    private boolean checkMemberOwnership(Long resourceId, String loginId, RequireResourceOwnership.OwnershipStrategy strategy) {
        return sideEffectService.isOwner(resourceId, loginId);
    }
}