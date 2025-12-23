package com.Hamalog.aop;

import com.Hamalog.logging.LoggingConstants;
import com.Hamalog.logging.SensitiveDataMasker;
import com.Hamalog.logging.StructuredLogger;
import com.Hamalog.logging.events.ApiEvent;
import com.Hamalog.security.SecurityContextUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * API 요청/응답 로깅 Aspect
 *
 * 주의: RequestLoggingFilter와 중복 로깅을 방지하기 위해 기본적으로 비활성화됩니다.
 * Filter 기반 로깅이 더 일찍 실행되어 더 정확한 요청 정보를 캡처할 수 있습니다.
 *
 * 활성화하려면: app.aop.api-logging.enabled=true
 */
@Slf4j
@Aspect
@Component
@Order(1)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.aop.api-logging.enabled", havingValue = "true", matchIfMissing = false)
public class ApiLoggingAspect {

    private final SecurityContextUtils securityContextUtils;
    private final StructuredLogger structuredLogger;

    @Pointcut("execution(public * com.Hamalog.controller..*(..))")
    public void allControllerMethods() {}

    @Around("allControllerMethods()")
    public Object logApiRequestAndResponse(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        HttpServletRequest request = securityContextUtils.getCurrentRequest().orElse(null);

        // Filter가 이미 로깅을 담당하고 있다면 중복 로깅 방지
        boolean filterOwnsLogging = request != null &&
                "FILTER".equals(request.getAttribute(LoggingConstants.API_LOGGING_OWNER_ATTRIBUTE));
        if (filterOwnsLogging) {
            return joinPoint.proceed();
        }

        // requestId 설정
        String existingRequestId = MDC.get("requestId");
        boolean putRequestId = existingRequestId == null;
        if (putRequestId) {
            MDC.put("requestId", java.util.UUID.randomUUID().toString());
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getDeclaringType().getSimpleName() + "." + signature.getName();
        String user = securityContextUtils.getCurrentUserId();
        String httpMethod = request != null ? request.getMethod() : "UNKNOWN";
        String path = request != null ? request.getRequestURI() : "UNKNOWN";
        String ipAddress = securityContextUtils.getClientIpAddress(request);
        String userAgent = securityContextUtils.sanitizeUserAgent(
                request != null ? request.getHeader("User-Agent") : null);
        String requestType = securityContextUtils.determineRequestType(path);

        MDC.put("api.method", methodName);
        MDC.put("api.user", user);
        
        Map<String, Object> parametersMap = createParametersMap(signature, joinPoint.getArgs());
        parametersMap = SensitiveDataMasker.maskParameters(parametersMap);

        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - startTime;
            int statusCode = getActualStatusCode(request);

            logApiEvent(httpMethod, path, signature, user, ipAddress, userAgent,
                       elapsed, statusCode, requestType, parametersMap);
            return result;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            int statusCode = determineErrorStatusCode(request, e);

            logApiEvent(httpMethod, path, signature, user, ipAddress, userAgent,
                       elapsed, statusCode, requestType, parametersMap);
            throw e;
        } finally {
            clearMDC(putRequestId);
        }
    }

    private void logApiEvent(String httpMethod, String path, MethodSignature signature,
                            String user, String ipAddress, String userAgent,
                            long elapsed, int statusCode, String requestType,
                            Map<String, Object> parametersMap) {
        ApiEvent apiEvent = ApiEvent.builder()
                .httpMethod(httpMethod)
                .path(path)
                .controller(signature.getDeclaringType().getSimpleName())
                .action(signature.getName())
                .userId(user)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .durationMs(elapsed)
                .statusCode(statusCode)
                .requestType(requestType)
                .parameters(parametersMap)
                .build();
        structuredLogger.api(apiEvent);
    }

    private void clearMDC(boolean removeRequestId) {
        MDC.remove("api.method");
        MDC.remove("api.user");
        MDC.remove("api.duration");
        MDC.remove("api.status");
        MDC.remove("api.errorType");
        MDC.remove("api.performance");
        if (removeRequestId) {
            MDC.remove("requestId");
        }
    }

    private String shorten(Object obj) {
        if (obj == null) return "null";
        String s = obj.toString();
        return s.length() > 100 ? s.substring(0, 97) + "..." : s;
    }

    private Map<String, Object> createParametersMap(MethodSignature signature, Object[] args) {
        String[] paramNames = signature.getParameterNames();
        int expectedSize = (paramNames != null && args != null) ?
            Math.min(paramNames.length, args.length) : 0;
        Map<String, Object> parametersMap = new HashMap<>(Math.max(expectedSize, 4));
        
        if (paramNames != null && args != null) {
            for (int i = 0; i < Math.min(paramNames.length, args.length); i++) {
                String name = paramNames[i];
                Object value = args[i];
                
                if (isSensitive(name)) {
                    parametersMap.put(name, "***");
                } else if (value != null) {
                    parametersMap.put(name, shorten(value));
                } else {
                    parametersMap.put(name, null);
                }
            }
        }
        return parametersMap;
    }

    private int getActualStatusCode(HttpServletRequest request) {
        if (request != null) {
            Object attr = request.getAttribute(LoggingConstants.RESPONSE_STATUS_ATTRIBUTE);
            if (attr instanceof Integer status) {
                return status;
            }
        }
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletResponse response = attributes.getResponse();
            if (response != null && response.getStatus() > 0) {
                return response.getStatus();
            }
        } catch (IllegalStateException ignored) {}
        return 200;
    }

    private int determineErrorStatusCode(HttpServletRequest request, Exception e) {
        int statusFromRequest = getActualStatusCode(request);
        if (statusFromRequest >= 400) {
            return statusFromRequest;
        }

        String exceptionName = e.getClass().getSimpleName().toLowerCase();
        if (exceptionName.contains("validation") || exceptionName.contains("methodargumentnotvalid")) {
            return 400;
        }
        if (exceptionName.contains("accessdenied") || exceptionName.contains("forbidden")) {
            return 403;
        }
        if (exceptionName.contains("authentication") || exceptionName.contains("unauthorized")) {
            return 401;
        }
        if (exceptionName.contains("notfound")) {
            return 404;
        }
        if (exceptionName.contains("conflict") || exceptionName.contains("duplicate")) {
            return 409;
        }
        return 500;
    }

    private boolean isSensitive(String name) {
        if (name == null) return false;
        String n = name.toLowerCase();
        return n.contains("password") || n.contains("token") ||
               n.contains("authorization") || n.contains("secret");
    }
}
