package com.Hamalog.aop;

import com.Hamalog.logging.StructuredLogger;
import com.Hamalog.logging.events.ApiEvent;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Aspect
@Component
public class ApiLoggingAspect {

    @Autowired
    private StructuredLogger structuredLogger;

    @Pointcut("execution(public * com.Hamalog.controller..*(..))")
    public void allControllerMethods() {}

    @Around("allControllerMethods()")
    public Object logApiRequestAndResponse(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String existingRequestId = MDC.get("requestId");
        boolean putRequestId = false;
        if (existingRequestId == null) {
            MDC.put("requestId", java.util.UUID.randomUUID().toString());
            putRequestId = true;
        }
        String requestId = MDC.get("requestId");

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getDeclaringType().getSimpleName() + "." + signature.getName();
        String params = getParameterInfo(signature, joinPoint.getArgs());
        String user = getAuthenticatedUser();

        HttpServletRequest request = getCurrentRequest();
        String httpMethod = request != null ? request.getMethod() : "UNKNOWN";
        String path = request != null ? request.getRequestURI() : "UNKNOWN";
        String ipAddress = request != null ? getClientIpAddress(request) : "UNKNOWN";
        String userAgent = request != null ? request.getHeader("User-Agent") : "UNKNOWN";

        MDC.put("api.method", methodName);
        MDC.put("api.user", user);
        
        Map<String, Object> parametersMap = createParametersMap(signature, joinPoint.getArgs());

        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - startTime;
            
            MDC.put("api.duration", String.valueOf(elapsed));
            MDC.put("api.status", "success");
            
            ApiEvent apiEvent = ApiEvent.builder()
                    .httpMethod(httpMethod)
                    .path(path)
                    .controller(signature.getDeclaringType().getSimpleName())
                    .action(signature.getName())
                    .userId(user)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .durationMs(elapsed)
                    .statusCode(200) // Assuming success is 200, could be enhanced to get actual status
                    .parameters(parametersMap)
                    .build();
            
            structuredLogger.api(apiEvent);
            
            return result;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            
            MDC.put("api.duration", String.valueOf(elapsed));
            MDC.put("api.status", "error");
            MDC.put("api.errorType", e.getClass().getSimpleName());
            
            ApiEvent apiErrorEvent = ApiEvent.builder()
                    .httpMethod(httpMethod)
                    .path(path)
                    .controller(signature.getDeclaringType().getSimpleName())
                    .action(signature.getName())
                    .userId(user)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .durationMs(elapsed)
                    .statusCode(500) // Assuming error is 500, could be enhanced
                    .parameters(parametersMap)
                    .build();
            
            structuredLogger.api(apiErrorEvent);
            
            throw e;
        } finally {
            MDC.remove("api.method");
            MDC.remove("api.user");
            MDC.remove("api.duration");
            MDC.remove("api.status");
            MDC.remove("api.errorType");
            
            if (putRequestId) {
                MDC.remove("requestId");
            }
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
        return String.valueOf(auth.getName());
    }

    private String shorten(Object obj) {
        if (obj == null) return "null";
        String s = obj.toString();
        return s.length() > 200 ? s.substring(0, 200) + "..." : s;
    }

    private String getPerformanceText(long elapsed) {
        if (elapsed < 100) return "VERY_FAST";
        if (elapsed < 500) return "FAST";
        if (elapsed < 1000) return "MODERATE";
        if (elapsed < 3000) return "SLOW";
        return "VERY_SLOW";
    }
    
    /**
     * Get current HTTP request
     */
    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            return attributes.getRequest();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        if (request == null) return "unknown";
        
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0];
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * Create parameters map for structured logging
     */
    private Map<String, Object> createParametersMap(MethodSignature signature, Object[] args) {
        Map<String, Object> parametersMap = new HashMap<>();
        String[] paramNames = signature.getParameterNames();
        
        if (paramNames != null && args != null) {
            for (int i = 0; i < Math.min(paramNames.length, args.length); i++) {
                String name = paramNames[i];
                Object value = args[i];
                
                if (isSensitive(name)) {
                    parametersMap.put(name, "***");
                } else if (value != null) {
                    String stringValue = shorten(value);
                    parametersMap.put(name, stringValue);
                } else {
                    parametersMap.put(name, null);
                }
            }
        }
        
        return parametersMap;
    }

    /**
     * Security Fix: Sanitize API response results to prevent sensitive data exposure in logs
     */
    private String sanitizeResult(Object result) {
        if (result == null) return "null";
        
        String className = result.getClass().getSimpleName();
        
        if (result instanceof java.util.Collection) {
            int size = ((java.util.Collection<?>) result).size();
            return String.format("[Collection<%s> size=%d]", className, size);
        }
        
        return String.format("[%s response - details hidden for security]", className);
    }
}
