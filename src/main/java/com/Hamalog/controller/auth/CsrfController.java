package com.Hamalog.controller.auth;

import com.Hamalog.security.csrf.CsrfTokenProvider;
import com.Hamalog.security.filter.TrustedProxyService;
import com.Hamalog.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CSRF 토큰 관리 컨트롤러
 * SPA 클라이언트가 CSRF 토큰을 요청할 수 있는 엔드포인트 제공
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class CsrfController {

    private static final String JWT_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final CsrfTokenProvider csrfTokenProvider;
    private final JwtTokenProvider jwtTokenProvider;
    private final TrustedProxyService trustedProxyService;


    /**
     * CSRF 토큰 발급
     * 인증된 사용자에게 CSRF 토큰을 제공
     * 
     * @param request HTTP 요청
     * @return CSRF 토큰과 메타데이터
     */
    @GetMapping("/csrf-token")
    public ResponseEntity<Map<String, Object>> getCsrfToken(HttpServletRequest request) {
        try {
            // JWT 토큰에서 사용자 ID 추출
            String userId = extractUserIdFromJwt(request);
            if (userId == null) {
                log.warn("CSRF 토큰 요청 실패: 유효한 JWT 토큰이 없음. IP: {}", getClientIp(request));
                return ResponseEntity.status(401)
                    .body(createErrorResponse("UNAUTHORIZED", "인증이 필요합니다"));
            }

            // CSRF 토큰 생성
            String csrfToken = csrfTokenProvider.generateToken(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("csrfToken", csrfToken);
            response.put("headerName", "X-CSRF-TOKEN");
            response.put("expiryMinutes", 60);
            response.put("storage", csrfTokenProvider.isFallbackActive() ? "fallback" : "redis");
            response.put("timestamp", java.time.Instant.now().toString());
            
            log.info("CSRF 토큰 발급 완료. User: {}, IP: {}", userId, getClientIp(request));
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("CSRF 토큰 발급 중 오류 발생", e);
            log.error("요청자 IP: {}", getClientIp(request));
            return ResponseEntity.status(500)
                .body(createErrorResponse("INTERNAL_ERROR", "토큰 발급 중 오류가 발생했습니다"));
        }
    }

    /**
     * CSRF 토큰 검증 상태 확인 (개발/디버깅용)
     * 
     * @param request HTTP 요청
     * @return 토큰 검증 상태
     */
    @GetMapping("/csrf-status")
    public ResponseEntity<Map<String, Object>> getCsrfStatus(HttpServletRequest request) {
        try {
            String userId = extractUserIdFromJwt(request);
            if (userId == null) {
                return ResponseEntity.status(401)
                    .body(createErrorResponse("UNAUTHORIZED", "인증이 필요합니다"));
            }

            String csrfToken = request.getHeader("X-CSRF-TOKEN");
            boolean isValid = csrfTokenProvider.validateToken(userId, csrfToken);
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("csrfTokenPresent", StringUtils.hasText(csrfToken));
            response.put("csrfTokenValid", isValid);
            response.put("storage", csrfTokenProvider.isFallbackActive() ? "fallback" : "redis");
            response.put("timestamp", java.time.Instant.now().toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("CSRF 상태 확인 중 오류 발생", e);
            log.error("요청자 IP: {}", getClientIp(request));
            return ResponseEntity.status(500)
                .body(createErrorResponse("INTERNAL_ERROR", "상태 확인 중 오류가 발생했습니다"));
        }
    }

    /**
     * JWT 토큰에서 사용자 ID 추출
     */
    private String extractUserIdFromJwt(HttpServletRequest request) {
        String authHeader = request.getHeader(JWT_HEADER);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        
        String token = authHeader.substring(BEARER_PREFIX.length());
        try {
            if (jwtTokenProvider.validateToken(token)) {
                return jwtTokenProvider.getLoginIdFromToken(token);
            }
        } catch (Exception e) {
            log.debug("JWT 토큰 추출 실패: {}", e.getMessage());
        }
        
        return null;
    }

    /**
     * 클라이언트 IP 주소 추출
     */
    private String getClientIp(HttpServletRequest request) {
        String clientIp = trustedProxyService.resolveClientIp(request)
                .orElseGet(() -> !StringUtils.hasText(request.getRemoteAddr()) ? "unknown" : request.getRemoteAddr());
        if (!StringUtils.hasText(clientIp)) {
            throw new IllegalStateException("Unable to resolve client IP");
        }
        return clientIp;
    }

    /**
     * 에러 응답 생성
     */
    private Map<String, Object> createErrorResponse(String errorCode, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", errorCode);
        error.put("message", message);
        error.put("timestamp", java.time.Instant.now().toString());
        return error;
    }
}