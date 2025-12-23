package com.Hamalog.logging;

import com.Hamalog.logging.events.ApiEvent;
import com.Hamalog.security.SecurityContextUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * HTTP 요청/응답 로깅 필터
 * 모든 API 요청에 대한 통합 로깅을 담당합니다.
 * AOP 기반 로깅(ApiLoggingAspect)보다 우선하여 실행됩니다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final StructuredLogger structuredLogger;
    private final SecurityContextUtils securityContextUtils;

    public RequestLoggingFilter(StructuredLogger structuredLogger,
                                SecurityContextUtils securityContextUtils) {
        this.structuredLogger = structuredLogger;
        this.securityContextUtils = securityContextUtils;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && path.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();
        boolean requestFailed = false;

        // Correlation/Request ID 설정
        String correlationId = headerOrGenerate(request, LoggingConstants.CORRELATION_ID_HEADER);
        String requestId = headerOrGenerate(request, REQUEST_ID_HEADER);

        boolean putCorrelationId = MDC.get(MDCUtil.CORRELATION_ID) == null;
        boolean putRequestId = MDC.get(MDCUtil.REQUEST_ID) == null;

        if (putCorrelationId) MDC.put(MDCUtil.CORRELATION_ID, correlationId);
        if (putRequestId) MDC.put(MDCUtil.REQUEST_ID, requestId);
        MDC.put("method", request.getMethod());
        MDC.put("path", request.getRequestURI());

        response.setHeader(LoggingConstants.CORRELATION_ID_HEADER, correlationId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        // AOP에서 중복 로깅 방지를 위한 플래그 설정
        request.setAttribute(LoggingConstants.API_LOGGING_OWNER_ATTRIBUTE, "FILTER");

        String user = securityContextUtils.getCurrentUserId();
        String ip = securityContextUtils.getClientIpAddress(request);
        String ua = securityContextUtils.sanitizeUserAgent(request.getHeader("User-Agent"));
        String requestType = securityContextUtils.determineRequestType(request.getRequestURI());

        Map<String, Object> requestParams = new HashMap<>(4);
        if (request.getQueryString() != null) {
            requestParams.put("queryString", request.getQueryString());
        }
        requestParams.put("headers", SensitiveDataMasker.maskHeaders(collectSafeHeaders(request)));

        StatusAwareResponseWrapper statusAwareResponse = wrapResponse(response);

        try {
            log.debug("REQ [{}] {} {} | User: {} | IP: {}",
                    requestType, request.getMethod(), request.getRequestURI(), user, ip);
            filterChain.doFilter(request, statusAwareResponse);

            long took = System.currentTimeMillis() - start;
            int status = resolveStatus(statusAwareResponse, false);
            request.setAttribute(LoggingConstants.RESPONSE_STATUS_ATTRIBUTE, status);

            logStructuredApiOnce(request, buildEvent(request, requestType, user, ip, ua, took, status, requestParams));
        } catch (Exception ex) {
            requestFailed = true;
            long took = System.currentTimeMillis() - start;
            int status = resolveStatus(statusAwareResponse, true);
            request.setAttribute(LoggingConstants.RESPONSE_STATUS_ATTRIBUTE, status);

            logStructuredApiOnce(request, buildEvent(request, requestType, user, ip, ua, took, status, requestParams));

            log.error("ERR [{}] {} {} | User: {} | Status: {} | Error: {}",
                    requestType, request.getMethod(), request.getRequestURI(), user, status, ex.toString());
            throw ex;
        } finally {
            long took = System.currentTimeMillis() - start;
            int status = resolveStatus(statusAwareResponse, requestFailed);
            request.setAttribute(LoggingConstants.RESPONSE_STATUS_ATTRIBUTE, status);
            request.setAttribute(LoggingConstants.API_LOGGING_OWNER_ATTRIBUTE, null);

            log.debug("RES [{}] {} {} | User: {} | Status: {} | Time: {}ms",
                    requestType, request.getMethod(), request.getRequestURI(), user, status, took);

            MDC.remove("method");
            MDC.remove("path");
            if (putRequestId) MDC.remove(MDCUtil.REQUEST_ID);
            if (putCorrelationId) MDC.remove(MDCUtil.CORRELATION_ID);
        }
    }

    private void logStructuredApiOnce(HttpServletRequest request, ApiEvent event) {
        if (request == null) {
            structuredLogger.api(event);
            return;
        }
        Object alreadyLogged = request.getAttribute(LoggingConstants.API_EVENT_LOGGED_ATTRIBUTE);
        if (Boolean.TRUE.equals(alreadyLogged)) {
            return;
        }
        structuredLogger.api(event);
        request.setAttribute(LoggingConstants.API_EVENT_LOGGED_ATTRIBUTE, true);
    }

    private String headerOrGenerate(HttpServletRequest request, String name) {
        String v = request.getHeader(name);
        return (v == null || v.isBlank()) ? UUID.randomUUID().toString() : v;
    }

    private Map<String, String> collectSafeHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        var names = request.getHeaderNames();
        if (names == null) return headers;

        while (names.hasMoreElements()) {
            String header = names.nextElement();
            String lower = header.toLowerCase();
            if (lower.contains("authorization") || lower.contains("cookie") || lower.contains("token")) {
                headers.put(header, "***");
            } else {
                String v = request.getHeader(header);
                headers.put(header, v != null && v.length() > 200 ? v.substring(0, 200) + "..." : v);
            }
        }
        return headers;
    }

    private StatusAwareResponseWrapper wrapResponse(HttpServletResponse response) {
        if (response instanceof StatusAwareResponseWrapper existingWrapper) {
            return existingWrapper;
        }
        return new StatusAwareResponseWrapper(response);
    }

    private int resolveStatus(StatusAwareResponseWrapper responseWrapper, boolean failed) {
        int status = responseWrapper.getStatusCode();
        if (status == 0) {
            return failed ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR : HttpServletResponse.SC_OK;
        }
        return status;
    }

    private ApiEvent buildEvent(HttpServletRequest request, String requestType, String user, String ip, String ua,
                                long took, int status, Map<String, Object> requestParams) {
        return ApiEvent.builder()
                .httpMethod(request.getMethod())
                .path(request.getRequestURI())
                .controller("FILTER")
                .action("HTTP_REQUEST")
                .userId(user)
                .ipAddress(ip)
                .userAgent(ua)
                .durationMs(took)
                .statusCode(status)
                .requestType(requestType)
                .parameters(requestParams)
                .build();
    }
}
