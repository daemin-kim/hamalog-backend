package com.Hamalog.security;

import com.Hamalog.security.filter.TrustedProxyService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 보안 컨텍스트 관련 공통 유틸리티 클래스
 * 여러 AOP Aspect와 Filter에서 중복 사용되던 메서드들을 중앙화합니다.
 */
@Component
@RequiredArgsConstructor
public class SecurityContextUtils {

    private final TrustedProxyService trustedProxyService;

    /**
     * 현재 인증된 사용자 ID를 반환합니다.
     *
     * @return 인증된 사용자 ID, 미인증 시 "anonymous"
     */
    public String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "anonymous";
        }
        String name = auth.getName();
        return "anonymousUser".equals(name) ? "anonymous" : name;
    }

    /**
     * 현재 HTTP 요청을 반환합니다.
     *
     * @return 현재 HttpServletRequest, 없으면 empty
     */
    public Optional<HttpServletRequest> getCurrentRequest() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            return Optional.ofNullable(attributes.getRequest());
        } catch (IllegalStateException e) {
            return Optional.empty();
        }
    }

    /**
     * 클라이언트 IP 주소를 반환합니다.
     * 프록시 헤더를 고려하여 실제 클라이언트 IP를 추출합니다.
     *
     * @param request HTTP 요청
     * @return 클라이언트 IP 주소, 알 수 없으면 "unknown"
     */
    public String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        return trustedProxyService.resolveClientIp(request).orElse("unknown");
    }

    /**
     * 현재 요청의 클라이언트 IP 주소를 반환합니다.
     *
     * @return 클라이언트 IP 주소, 알 수 없으면 "unknown"
     */
    public String getClientIpAddress() {
        return getCurrentRequest()
                .map(this::getClientIpAddress)
                .orElse("unknown");
    }

    /**
     * User-Agent 헤더를 안전하게 정제합니다.
     * 길이 제한 및 잠재적 악성 문자를 제거합니다.
     *
     * @param userAgent 원본 User-Agent 문자열
     * @return 정제된 User-Agent 문자열
     */
    public String sanitizeUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "unknown";
        }
        // 길이 제한 (200자) 및 개행 문자 제거
        String sanitized = userAgent.replaceAll("[\\r\\n]", "");
        return sanitized.length() > 200 ? sanitized.substring(0, 200) + "..." : sanitized;
    }

    /**
     * 현재 요청의 User-Agent를 반환합니다.
     *
     * @return User-Agent 문자열
     */
    public String getCurrentUserAgent() {
        return getCurrentRequest()
                .map(req -> sanitizeUserAgent(req.getHeader("User-Agent")))
                .orElse("unknown");
    }

    /**
     * 요청 경로를 기반으로 요청 타입을 결정합니다.
     *
     * @param path 요청 경로
     * @return 요청 타입 (INTERNAL 또는 EXTERNAL)
     */
    public String determineRequestType(String path) {
        if (path == null) {
            return "EXTERNAL";
        }
        // actuator 엔드포인트는 내부 요청으로 분류
        if (path.startsWith("/actuator/")) {
            return "INTERNAL";
        }
        return "EXTERNAL";
    }
}
