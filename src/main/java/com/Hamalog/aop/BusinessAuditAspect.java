package com.Hamalog.aop;

import com.Hamalog.logging.StructuredLogger;
import com.Hamalog.logging.events.AuditEvent;
import com.Hamalog.logging.events.SecurityEvent;
import com.Hamalog.security.SecurityContextUtils;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 비즈니스 감사 로깅 Aspect
 * 주요 비즈니스 작업(생성, 수정, 삭제)과 인증 관련 이벤트를 감사 로그로 기록합니다.
 *
 * 참고: ServiceLoggingAspect와의 중복을 피하기 위해, 이 Aspect는
 * "감사가 필요한 중요 작업"만 선별적으로 로깅합니다.
 */
@Slf4j
@Aspect
@Component
@ConditionalOnProperty(name = "app.aop.audit.enabled", matchIfMissing = true)
@Order(3)
@RequiredArgsConstructor
public class BusinessAuditAspect {

    private final StructuredLogger structuredLogger;
    private final SecurityContextUtils securityContextUtils;

    // 인증 관련 작업만 감사 (서비스 레이어의 일반 CRUD는 ServiceLoggingAspect에서 처리)
    @Pointcut("execution(public * com.Hamalog.service.auth.MemberRegistrationService.*(..))")
    public void memberRegistrationOperations() {}

    @Pointcut("execution(public * com.Hamalog.service.auth.AuthenticationService.*(..))")
    public void authenticationOperations() {}

    @Pointcut("execution(public * com.Hamalog.service.auth.MemberDeletionService.*(..))")
    public void memberDeletionOperations() {}

    @Pointcut("memberRegistrationOperations() || authenticationOperations() || memberDeletionOperations()")
    public void authOperations() {}

    @Around("authOperations()")
    public Object auditAuthOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        String operationType = determineOperationType(joinPoint);
        String methodName = getMethodName(joinPoint);
        String userId = securityContextUtils.getCurrentUserId();
        String ipAddress = securityContextUtils.getClientIpAddress();
        String userAgent = securityContextUtils.getCurrentUserAgent();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        setAuditMDC(operationType, methodName, userId, ipAddress, timestamp, userAgent);

        try {
            Object result = joinPoint.proceed();
            
            AuditEvent successEvent = AuditEvent.builder()
                    .operation(operationType)
                    .entityType("AUTH_OPERATION")
                    .entityId(methodName)
                    .userId(userId)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .status("SUCCESS")
                    .details("Result: " + summarize(result))
                    .build();
            
            structuredLogger.audit(successEvent);
            return result;
            
        } catch (Exception e) {
            AuditEvent failureEvent = AuditEvent.builder()
                    .operation(operationType)
                    .entityType("AUTH_OPERATION")
                    .entityId(methodName)
                    .userId(userId)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .status("FAILURE")
                    .details("Error: " + e.getClass().getSimpleName() + " - " + e.getMessage())
                    .build();
            
            structuredLogger.audit(failureEvent);
            throw e;
            
        } finally {
            clearAuditMDC();
        }
    }

    @AfterReturning(pointcut = "execution(public * com.Hamalog.controller.auth.AuthController.login(..))", returning = "result")
    public void auditLoginSuccess(JoinPoint joinPoint, Object result) {
        String userId = securityContextUtils.getCurrentUserId();
        String ipAddress = securityContextUtils.getClientIpAddress();
        String userAgent = securityContextUtils.getCurrentUserAgent();

        SecurityEvent loginEvent = SecurityEvent.builder()
                .eventType("AUTHENTICATION")
                .userId(userId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .resource("LOGIN")
                .action("AUTHENTICATE")
                .result("SUCCESS")
                .riskLevel("LOW")
                .details("Successful user authentication")
                .build();
        
        structuredLogger.security(loginEvent);
    }

    @AfterThrowing(pointcut = "execution(public * com.Hamalog.controller.auth.AuthController.login(..))", throwing = "ex")
    public void auditLoginFailure(JoinPoint joinPoint, Exception ex) {
        String ipAddress = securityContextUtils.getClientIpAddress();
        String userAgent = securityContextUtils.getCurrentUserAgent();
        String attemptedUser = extractAttemptedUsername(joinPoint.getArgs());
        String riskLevel = determineLoginFailureRiskLevel(ex, attemptedUser);
        
        SecurityEvent loginFailureEvent = SecurityEvent.builder()
                .eventType("AUTHENTICATION_FAILURE")
                .userId(attemptedUser)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .resource("LOGIN")
                .action("AUTHENTICATE")
                .result("FAILURE")
                .riskLevel(riskLevel)
                .details("Login attempt failed: " + ex.getClass().getSimpleName())
                .build();
        
        structuredLogger.security(loginFailureEvent);
    }

    private String determineOperationType(ProceedingJoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName().toLowerCase();
        
        if (methodName.contains("create") || methodName.contains("register") || methodName.contains("add")) {
            return "CREATE";
        } else if (methodName.contains("update") || methodName.contains("modify")) {
            return "UPDATE";
        } else if (methodName.contains("delete") || methodName.contains("remove")) {
            return "DELETE";
        } else if (methodName.contains("login") || methodName.contains("authenticate")) {
            return "LOGIN";
        } else if (methodName.contains("logout")) {
            return "LOGOUT";
        }
        return "READ";
    }

    private String getMethodName(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getDeclaringType().getSimpleName() + "." + signature.getName();
    }

    private String summarize(Object result) {
        if (result == null) return "null";
        try {
            String str = result.toString();
            return str.length() > 100 ? str.substring(0, 100) + "..." : str;
        } catch (Exception e) {
            return result.getClass().getSimpleName();
        }
    }

    private String extractAttemptedUsername(Object[] args) {
        if (args == null || args.length == 0) return "unknown";
        for (Object arg : args) {
            if (arg != null && arg.toString().contains("loginId")) {
                return "login_attempt";
            }
        }
        return "unknown";
    }

    private String determineLoginFailureRiskLevel(Exception ex, String attemptedUser) {
        if (attemptedUser != null &&
            (attemptedUser.equalsIgnoreCase("admin") || attemptedUser.equalsIgnoreCase("root"))) {
            return "HIGH";
        }
        String exName = ex.getClass().getSimpleName().toLowerCase();
        if (exName.contains("badcredentials") || exName.contains("authentication")) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private void setAuditMDC(String operation, String method, String userId,
                             String ipAddress, String timestamp, String userAgent) {
        MDC.put("audit.operation", operation);
        MDC.put("audit.method", method);
        MDC.put("audit.userId", userId);
        MDC.put("audit.ipAddress", ipAddress);
        MDC.put("audit.timestamp", timestamp);
        MDC.put("audit.userAgent", userAgent);
    }

    private void clearAuditMDC() {
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
    }
}
