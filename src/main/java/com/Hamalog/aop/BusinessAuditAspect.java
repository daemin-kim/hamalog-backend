package com.Hamalog.aop;

import com.Hamalog.logging.StructuredLogger;
import com.Hamalog.logging.events.AuditEvent;
import com.Hamalog.logging.events.SecurityEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 비즈니스 감사 로깅을 위한 AOP Aspect
 * 중요한 비즈니스 작업(생성, 수정, 삭제)에 대한 감사 로그를 자동으로 생성합니다.
 */
@Slf4j
@Aspect
@Component
@ConditionalOnProperty(name = "app.aop.audit.enabled", matchIfMissing = true)
@Order(3)
public class BusinessAuditAspect {

    @Autowired(required = false)
    private ObjectMapper objectMapper;
    
    @Autowired
    private StructuredLogger structuredLogger;

    @Pointcut("execution(public * com.Hamalog.service..create*(..))")
    public void createOperations() {}

    @Pointcut("execution(public * com.Hamalog.service..update*(..))")
    public void updateOperations() {}

    @Pointcut("execution(public * com.Hamalog.service..delete*(..))")
    public void deleteOperations() {}

    @Pointcut("execution(public * com.Hamalog.service.auth.AuthService.*(..))")
    public void authOperations() {}

    @Around("createOperations() || updateOperations() || deleteOperations() || authOperations()")
    public Object auditBusinessOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        String operationType = determineOperationType(joinPoint);
        String methodName = getMethodName(joinPoint);
        String userId = getCurrentUserId();
        HttpServletRequest request = getCurrentRequest();
        String userAgent = request != null ? request.getHeader("User-Agent") : "unknown";
        String ipAddress = getClientIpAddress();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        MDC.put("audit.operation", operationType);
        MDC.put("audit.method", methodName);
        MDC.put("audit.userId", userId);
        MDC.put("audit.ipAddress", ipAddress);
        MDC.put("audit.timestamp", timestamp);
        MDC.put("audit.userAgent", sanitizeUserAgent(userAgent));

        String inputParams = getAuditSafeParameters(joinPoint);

        try {
            Object result = joinPoint.proceed();
            
            String resultSummary = getResultSummary(result);
            MDC.put("audit.status", "SUCCESS");
            MDC.put("audit.result", resultSummary);
            
            AuditEvent successEvent = AuditEvent.builder()
                    .operation(operationType)
                    .entityType("BUSINESS_OPERATION")
                    .entityId(methodName)
                    .userId(userId)
                    .ipAddress(ipAddress)
                    .userAgent(sanitizeUserAgent(userAgent))
                    .status("SUCCESS")
                    .details("Result: " + resultSummary)
                    .build();
            
            structuredLogger.audit(successEvent);
            
            return result;
            
        } catch (Exception e) {
            MDC.put("audit.status", "FAILURE");
            MDC.put("audit.error", e.getClass().getSimpleName());
            MDC.put("audit.errorMessage", e.getMessage());
            
            AuditEvent failureEvent = AuditEvent.builder()
                    .operation(operationType)
                    .entityType("BUSINESS_OPERATION")
                    .entityId(methodName)
                    .userId(userId)
                    .ipAddress(ipAddress)
                    .userAgent(sanitizeUserAgent(userAgent))
                    .status("FAILURE")
                    .details("Error: " + e.getClass().getSimpleName() + " - " + e.getMessage())
                    .build();
            
            structuredLogger.audit(failureEvent);
            
            throw e;
            
        } finally {
            clearAuditContext();
        }
    }

    @AfterReturning(pointcut = "execution(public * com.Hamalog.controller.auth.AuthController.login(..))", returning = "result")
    public void auditLoginSuccess(JoinPoint joinPoint, Object result) {
        String userId = getCurrentUserId();
        String ipAddress = getClientIpAddress();
        HttpServletRequest request = getCurrentRequest();
        String userAgent = request != null ? request.getHeader("User-Agent") : "unknown";
        
        MDC.put("audit.operation", "LOGIN_SUCCESS");
        MDC.put("audit.userId", userId);
        MDC.put("audit.ipAddress", ipAddress);
        MDC.put("audit.timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        SecurityEvent loginEvent = SecurityEvent.builder()
                .eventType("AUTHENTICATION")
                .userId(userId)
                .ipAddress(ipAddress)
                .userAgent(sanitizeUserAgent(userAgent))
                .resource("LOGIN")
                .action("AUTHENTICATE")
                .result("SUCCESS")
                .riskLevel("LOW")
                .details("Successful user authentication")
                .build();
        
        structuredLogger.security(loginEvent);
        
        clearAuditContext();
    }

    @AfterThrowing(pointcut = "execution(public * com.Hamalog.controller.auth.AuthController.login(..))", throwing = "ex")
    public void auditLoginFailure(JoinPoint joinPoint, Exception ex) {
        String ipAddress = getClientIpAddress();
        Object[] args = joinPoint.getArgs();
        String attemptedUser = extractAttemptedUsername(args);
        HttpServletRequest request = getCurrentRequest();
        String userAgent = request != null ? request.getHeader("User-Agent") : "unknown";
        
        MDC.put("audit.operation", "LOGIN_FAILURE");
        MDC.put("audit.attemptedUser", attemptedUser);
        MDC.put("audit.ipAddress", ipAddress);
        MDC.put("audit.timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        MDC.put("audit.error", ex.getClass().getSimpleName());
        
        String riskLevel = determineLoginFailureRiskLevel(ex, attemptedUser);
        
        SecurityEvent loginFailureEvent = SecurityEvent.builder()
                .eventType("AUTHENTICATION_FAILURE")
                .userId(attemptedUser)
                .ipAddress(ipAddress)
                .userAgent(sanitizeUserAgent(userAgent))
                .resource("LOGIN")
                .action("AUTHENTICATE")
                .result("FAILURE")
                .riskLevel(riskLevel)
                .details("Login attempt failed: " + ex.getClass().getSimpleName() + " - " + ex.getMessage())
                .build();
        
        structuredLogger.security(loginFailureEvent);
        
        clearAuditContext();
    }

    @Around("execution(public * com.Hamalog.service.medication.MedicationRecordService.create*(..))")
    public Object auditMedicationRecordChanges(ProceedingJoinPoint joinPoint) throws Throwable {
        return auditDataChange(joinPoint, "MEDICATION_RECORD", "CREATE");
    }

    @Around("execution(public * com.Hamalog.service.medication.MedicationRecordService.update*(..))")
    public Object auditMedicationRecordUpdates(ProceedingJoinPoint joinPoint) throws Throwable {
        return auditDataChange(joinPoint, "MEDICATION_RECORD", "UPDATE");
    }

    @Around("execution(public * com.Hamalog.service.medication.MedicationRecordService.delete*(..))")
    public Object auditMedicationRecordDeletes(ProceedingJoinPoint joinPoint) throws Throwable {
        return auditDataChange(joinPoint, "MEDICATION_RECORD", "DELETE");
    }

    private Object auditDataChange(ProceedingJoinPoint joinPoint, String entityType, String operation) throws Throwable {
        String userId = getCurrentUserId();
        String methodName = getMethodName(joinPoint);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        MDC.put("audit.entityType", entityType);
        MDC.put("audit.operation", operation);
        MDC.put("audit.userId", userId);
        MDC.put("audit.timestamp", timestamp);
        
        try {
            Object result = joinPoint.proceed();
            
            log.info("DATA_AUDIT: {} | Entity: {} | Operation: {} | User: {} | Method: {}", 
                    operation, entityType, operation, userId, methodName);
            
            return result;
            
        } catch (Exception e) {
            log.error("DATA_AUDIT_ERROR: {} | Entity: {} | Operation: {} | User: {} | Method: {} | Error: {}", 
                     operation, entityType, operation, userId, methodName, e.getClass().getSimpleName());
            throw e;
            
        } finally {
            MDC.remove("audit.entityType");
            MDC.remove("audit.operation");
            MDC.remove("audit.userId");
            MDC.remove("audit.timestamp");
        }
    }

    private String determineOperationType(ProceedingJoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName().toLowerCase();
        
        if (methodName.contains("create") || methodName.contains("add") || methodName.contains("insert")) {
            return "CREATE";
        } else if (methodName.contains("update") || methodName.contains("modify") || methodName.contains("edit")) {
            return "UPDATE";
        } else if (methodName.contains("delete") || methodName.contains("remove")) {
            return "DELETE";
        } else if (methodName.contains("login") || methodName.contains("authenticate")) {
            return "LOGIN";
        } else if (methodName.contains("logout")) {
            return "LOGOUT";
        } else {
            return "READ";
        }
    }

    private String getMethodName(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getDeclaringType().getSimpleName() + "." + signature.getName();
    }

    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "anonymous";
        }
        return auth.getName();
    }

    private String getClientIpAddress() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return "unknown";
        }
        
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    private String sanitizeUserAgent(String userAgent) {
        if (userAgent == null) return "unknown";
        // User-Agent 길이 제한 및 특수 문자 제거
        return userAgent.length() > 200 ? userAgent.substring(0, 200) + "..." : userAgent;
    }

    private String getAuditSafeParameters(ProceedingJoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();
            
            if (paramNames == null || args == null) return "{}";
            
            StringBuilder sb = new StringBuilder("{");
            for (int i = 0; i < paramNames.length; i++) {
                String paramName = paramNames[i];
                Object arg = args[i];
                
                if (isSensitiveParameter(paramName)) {
                    sb.append(paramName).append("=***");
                } else {
                    String value = arg == null ? "null" : arg.toString();
                    if (value.length() > 100) {
                        value = value.substring(0, 100) + "...";
                    }
                    sb.append(paramName).append("=").append(value);
                }
                
                if (i < paramNames.length - 1) {
                    sb.append(", ");
                }
            }
            sb.append("}");
            
            return sb.toString();
            
        } catch (Exception e) {
            return "{error_parsing_params}";
        }
    }

    private boolean isSensitiveParameter(String paramName) {
        if (paramName == null) return false;
        String name = paramName.toLowerCase();
        return name.contains("password") || name.contains("token") || name.contains("secret") ||
               name.contains("key") || name.contains("auth") || name.contains("credential");
    }

    private String getResultSummary(Object result) {
        if (result == null) return "null";
        
        try {
            String resultStr = result.toString();
            if (resultStr.length() > 100) {
                return resultStr.substring(0, 100) + "...";
            }
            return resultStr;
        } catch (Exception e) {
            return result.getClass().getSimpleName() + "@" + System.identityHashCode(result);
        }
    }

    private String extractAttemptedUsername(Object[] args) {
        if (args == null || args.length == 0) return "unknown";
        
        for (Object arg : args) {
            if (arg != null) {
                String argStr = arg.toString();
                // LoginRequest 객체나 username 파라미터에서 사용자명 추출 시도
                if (argStr.contains("loginId") || argStr.contains("username")) {
                    // 간단한 파싱 시도 (실제로는 더 정교한 파싱 필요)
                    return "login_attempt";
                }
            }
        }
        return "unknown";
    }

    /**
     * Determine security risk level based on login failure context
     */
    private String determineLoginFailureRiskLevel(Exception ex, String attemptedUser) {
        // High risk indicators
        if (attemptedUser != null && (attemptedUser.equals("admin") || attemptedUser.equals("root") || 
            attemptedUser.equals("administrator") || attemptedUser.contains("admin"))) {
            return "HIGH";
        }
        
        // Medium risk for authentication-related exceptions
        String exceptionName = ex.getClass().getSimpleName().toLowerCase();
        if (exceptionName.contains("badcredentials") || exceptionName.contains("authentication")) {
            return "MEDIUM";
        }
        
        // Low risk for other failures
        return "LOW";
    }

    private void clearAuditContext() {
        MDC.remove("audit.operation");
        MDC.remove("audit.method");
        MDC.remove("audit.userId");
        MDC.remove("audit.ipAddress");
        MDC.remove("audit.timestamp");
        MDC.remove("audit.userAgent");
        MDC.remove("audit.status");
        MDC.remove("audit.result");
        MDC.remove("audit.error");
        MDC.remove("audit.errorMessage");
        MDC.remove("audit.attemptedUser");
    }
}