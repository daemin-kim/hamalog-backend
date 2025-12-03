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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final String redirectBase;
    private final MemberRepository memberRepository;

    // Security Fix: Define allowed redirect domains to prevent open redirect attacks
    private static final List<String> ALLOWED_REDIRECT_HOSTS = Arrays.asList(
        "localhost",
        "127.0.0.1",
        "49.142.154.182", // Production server IP (frontend)
        "112.72.248.195"  // Production server IP (backend)
    );

    public OAuth2AuthenticationSuccessHandler(
            JwtTokenProvider jwtTokenProvider,
            MemberRepository memberRepository,
            @Value("${hamalog.oauth2.redirect-uri:http://localhost:3000/oauth/kakao}") String redirectBase
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.memberRepository = memberRepository;
        // Validate redirect URI on startup
        if (!isValidRedirectUri(redirectBase)) {
            throw new IllegalStateException("Invalid OAuth2 redirect URI configured: " + redirectBase);
        }
        this.redirectBase = redirectBase;
    }
    
    /**
     * Security Fix: Validate redirect URI to prevent open redirect attacks
     */
    private boolean isValidRedirectUri(String uri) {
        try {
            URL url = new URL(uri);
            String host = url.getHost();
            return ALLOWED_REDIRECT_HOSTS.contains(host);
        } catch (MalformedURLException e) {
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
