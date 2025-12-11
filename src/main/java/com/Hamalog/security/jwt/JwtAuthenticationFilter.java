package com.Hamalog.security.jwt;

import com.Hamalog.security.filter.TrustedProxyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final TrustedProxyService trustedProxyService;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, UserDetailsService userDetailsService,
                                   TrustedProxyService trustedProxyService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
        this.trustedProxyService = trustedProxyService;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        String token = resolveToken(request);
        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        String requestUri = request.getRequestURI();
        String method = request.getMethod();

        // Add security context to MDC
        MDC.put("security.clientIp", clientIp);
        MDC.put("security.userAgent", sanitizeForLogging(userAgent, 50));

        try {
            if (token != null) {
                log.debug("[SECURITY] JWT token authentication attempt - method={} uri={} clientIp={}", 
                    method, requestUri, clientIp);
                
                if (jwtTokenProvider.validateToken(token)) {
                    String loginId = jwtTokenProvider.getLoginIdFromToken(token);
                    UserDetails userDetails = userDetailsService.loadUserByUsername(loginId);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    // Add authenticated user to MDC
                    MDC.put("security.authenticatedUser", loginId);
                    
                    log.info("[SECURITY] Authentication successful - user={} method={} uri={} clientIp={} userAgent={}", 
                        loginId, method, requestUri, clientIp, sanitizeForLogging(userAgent, 30));
                } else {
                    log.warn("[SECURITY] JWT token validation failed - method={} uri={} clientIp={} reason=invalid_token", 
                        method, requestUri, clientIp);
                }
            } else if (requiresAuthentication(request)) {
                log.debug("[SECURITY] No JWT token provided for protected resource - method={} uri={} clientIp={}", 
                    method, requestUri, clientIp);
            }
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            log.error("[SECURITY] Authentication error - method={} uri={} clientIp={} error={}", 
                method, requestUri, clientIp, ex.getMessage(), ex);
        } finally {
            try {
                filterChain.doFilter(request, response);
            } finally {
                // Clean up security MDC
                MDC.remove("security.clientIp");
                MDC.remove("security.userAgent");
                MDC.remove("security.authenticatedUser");
            }
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (!trustedProxyService.isTrustedProxy(remoteAddr)) {
            return remoteAddr;
        }

        return trustedProxyService.extractClientIp(request.getHeader("X-Forwarded-For"))
                .or(() -> trustedProxyService.extractSingleIp(request.getHeader("X-Real-IP")))
                .or(() -> trustedProxyService.extractSingleIp(request.getHeader("X-Client-IP")))
                .orElse(remoteAddr);
    }

    private boolean requiresAuthentication(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        
        // Skip authentication for public endpoints
        if (uri.startsWith("/auth/login") ||
            uri.startsWith("/auth/signup") ||
            uri.startsWith("/oauth2/") ||
            uri.startsWith("/actuator/health") ||
            (uri.equals("/") && "GET".equals(method))) {
            return false;
        }
        
        // Most API endpoints require authentication
        return uri.startsWith("/api/") || uri.startsWith("/medication/") || uri.startsWith("/side-effect/");
    }

    /**
     * Sanitizes input strings for safe logging, preventing log injection attacks
     * @param input the input string to sanitize
     * @param maxLength maximum length of the output string
     * @return sanitized string safe for logging
     */
    private String sanitizeForLogging(String input, int maxLength) {
        if (input == null) {
            return "unknown";
        }
        
        // Remove potential log injection characters (newlines, carriage returns, etc.)
        String sanitized = input.replaceAll("[\r\n\t]", "_")
                               .replaceAll("[\\p{Cntrl}]", "_");
        
        // Limit length to prevent log bloat
        if (sanitized.length() > maxLength) {
            sanitized = sanitized.substring(0, maxLength) + "...";
        }
        
        return sanitized;
    }
}
