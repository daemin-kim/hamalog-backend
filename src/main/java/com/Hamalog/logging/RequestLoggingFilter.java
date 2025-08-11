package com.Hamalog.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();

        String requestId = headerOrGenerate(request, REQUEST_ID_HEADER);
        boolean putRequestId = false;
        if (MDC.get("requestId") == null) {
            MDC.put("requestId", requestId);
            putRequestId = true;
        }
        MDC.put("method", request.getMethod());
        MDC.put("path", request.getRequestURI());

        response.setHeader(REQUEST_ID_HEADER, requestId);

        String user = currentPrincipal();
        String ip = request.getRemoteAddr();
        String ua = safeHeader(request, "User-Agent");
        String referer = safeHeader(request, "Referer");

        try {
            log.info("[REQ] method={} path={} user={} ip={} ua={} referer={}", request.getMethod(), request.getRequestURI(), user, ip, shorten(ua), shorten(referer));
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            log.error("[ERR] method={} path={} user={} status=500 message={}", request.getMethod(), request.getRequestURI(), user, ex.toString(), ex);
            throw ex;
        } finally {
            long took = System.currentTimeMillis() - start;
            int status = response.getStatus();
            log.info("[RES] method={} path={} user={} status={} time={}ms", request.getMethod(), request.getRequestURI(), user, status, took);
            MDC.remove("method");
            MDC.remove("path");
            if (putRequestId) {
                MDC.remove("requestId");
            }
        }
    }

    private String currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return "anonymous";
        try {
            return String.valueOf(auth.getName());
        } catch (Exception e) {
            return "anonymous";
        }
    }

    private String headerOrGenerate(HttpServletRequest request, String name) {
        String v = request.getHeader(name);
        if (v == null || v.isBlank()) return UUID.randomUUID().toString();
        return v;
    }

    private String safeHeader(HttpServletRequest request, String name) {
        if ("Authorization".equalsIgnoreCase(name) || "Cookie".equalsIgnoreCase(name)) {
            return "***";
        }
        String v = request.getHeader(name);
        return v == null ? "" : v;
    }

    private String shorten(String s) {
        if (s == null) return "";
        return s.length() > 200 ? s.substring(0, 200) + "..." : s;
    }
}
