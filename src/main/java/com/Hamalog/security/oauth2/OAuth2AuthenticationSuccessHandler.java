package com.Hamalog.security.oauth2;

import com.Hamalog.repository.member.MemberRepository;
import com.Hamalog.security.jwt.JwtTokenProvider;
import com.Hamalog.domain.member.Member;
import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final String redirectBase;
    private final MemberRepository memberRepository;
    private final Set<String> allowedRedirectHosts;

    // 기본 허용 호스트 (개발 환경)
    private static final Set<String> DEFAULT_ALLOWED_HOSTS = Set.of("localhost", "127.0.0.1");

    public OAuth2AuthenticationSuccessHandler(
            JwtTokenProvider jwtTokenProvider,
            MemberRepository memberRepository,
            @Value("${hamalog.oauth2.redirect-uri:http://localhost:3000/oauth/kakao}") String redirectBase,
            @Value("${hamalog.cors.allowed-origins:}") String allowedOrigins
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.memberRepository = memberRepository;

        // 허용된 origins에서 호스트 추출
        this.allowedRedirectHosts = extractHostsFromOrigins(allowedOrigins);
        log.info("[OAUTH2] Allowed redirect hosts: {}", allowedRedirectHosts);

        // Validate redirect URI on startup
        if (!isValidRedirectUri(redirectBase)) {
            throw new IllegalStateException("Invalid OAuth2 redirect URI configured: " + redirectBase +
                ". Allowed hosts: " + allowedRedirectHosts);
        }
        this.redirectBase = redirectBase;
    }
    
    /**
     * CORS 허용 origins에서 호스트 목록 추출
     */
    private Set<String> extractHostsFromOrigins(String allowedOrigins) {
        Set<String> hosts = new HashSet<>(DEFAULT_ALLOWED_HOSTS);

        if (allowedOrigins != null && !allowedOrigins.isBlank()) {
            for (String origin : allowedOrigins.split(",")) {
                try {
                    String trimmed = origin.trim();
                    if (!trimmed.isEmpty()) {
                        URI uri = URI.create(trimmed);
                        String host = uri.getHost();
                        if (host != null) {
                            hosts.add(host);
                        }
                    }
                } catch (IllegalArgumentException e) {
                    log.warn("[OAUTH2] Invalid origin in allowed-origins: {}", origin);
                }
            }
        }

        return hosts;
    }

    /**
     * Security Fix: Validate redirect URI to prevent open redirect attacks
     */
    private boolean isValidRedirectUri(String uri) {
        try {
            URI parsed = URI.create(uri);
            String host = parsed.getHost();
            return host != null && allowedRedirectHosts.contains(host);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        Object principal = authentication.getPrincipal();
        String loginId;
        if (principal instanceof OAuth2User oAuth2User) {
            Object idObj = oAuth2User.getAttributes().get("id");
            if (idObj == null) {
                loginId = authentication.getName();
            } else {
                long kakaoId = ((Number) idObj).longValue();
                loginId = "kakao_" + kakaoId + "@oauth2.internal";
            }
        } else {
            loginId = authentication.getName();
        }

        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        String token = jwtTokenProvider.createToken(loginId, member.getMemberId(), null);

        Cookie tokenCookie = new Cookie("auth_token", token);
        tokenCookie.setHttpOnly(true);
        tokenCookie.setSecure(true);
        tokenCookie.setPath("/");
        tokenCookie.setMaxAge(3600);
        tokenCookie.setAttribute("SameSite", "Strict");
        response.addCookie(tokenCookie);

        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", redirectBase);
    }
}
