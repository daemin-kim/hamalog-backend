package com.Hamalog.security.filter;

import com.Hamalog.security.csrf.CsrfTokenProvider;
import com.Hamalog.security.jwt.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * CSRF 토큰 검증 필터
 * SPA 환경에서 상태 변경 작업에 대한 CSRF 보호 제공
 */
@Component
public class CsrfValidationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CsrfValidationFilter.class);
    
    private static final String CSRF_TOKEN_HEADER = "X-CSRF-TOKEN";
    private static final String JWT_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    
    // CSRF 검증을 건너뛸 경로들
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
        "/auth/login",
        "/auth/signup", 
        "/auth/csrf-token",
        "/oauth2/",
        "/v3/api-docs",
        "/swagger-ui",
        "/error",
        "/actuator"
    );
    
    // CSRF 검증이 필요한 HTTP 메소드
    private static final List<String> CSRF_REQUIRED_METHODS = Arrays.asList(
        "POST", "PUT", "PATCH", "DELETE"
    );

    private final CsrfTokenProvider csrfTokenProvider;
    private final JwtTokenProvider jwtTokenProvider;

    public CsrfValidationFilter(CsrfTokenProvider csrfTokenProvider, JwtTokenProvider jwtTokenProvider) {
        this.csrfTokenProvider = csrfTokenProvider;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String method = request.getMethod();
        String requestUri = request.getRequestURI();
        
        // CSRF 검증이 필요하지 않은 경우 건너뛰기
        if (!isCsrfValidationRequired(method, requestUri)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // JWT 토큰에서 사용자 ID 추출
        String userId = extractUserIdFromJwt(request);
        if (userId == null) {
            log.warn("CSRF 검증 실패: JWT 토큰이 없거나 유효하지 않음. URI: {}", requestUri);
            sendCsrfError(response, "인증이 필요합니다");
            return;
        }
        
        // CSRF 토큰 검증
        String csrfToken = request.getHeader(CSRF_TOKEN_HEADER);
        if (!csrfTokenProvider.validateToken(userId, csrfToken)) {
            log.warn("CSRF 검증 실패: 토큰이 유효하지 않음. User: {}, URI: {}", userId, requestUri);
            sendCsrfError(response, "CSRF 토큰이 유효하지 않습니다");
            return;
        }
        
        log.debug("CSRF 검증 성공. User: {}, URI: {}", userId, requestUri);
        filterChain.doFilter(request, response);
    }

    /**
     * CSRF 검증이 필요한지 확인
     */
    private boolean isCsrfValidationRequired(String method, String requestUri) {
        // GET, HEAD, OPTIONS 등은 검증하지 않음
        if (!CSRF_REQUIRED_METHODS.contains(method)) {
            return false;
        }
        
        // 제외 경로 확인
        for (String excludedPath : EXCLUDED_PATHS) {
            if (requestUri.startsWith(excludedPath)) {
                return false;
            }
        }
        
        return true;
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
     * CSRF 에러 응답 전송
     */
    private void sendCsrfError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        
        String jsonResponse = String.format(
            "{\"error\":\"CSRF_TOKEN_INVALID\",\"message\":\"%s\",\"timestamp\":\"%s\"}", 
            message, 
            java.time.Instant.now().toString()
        );
        
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
}