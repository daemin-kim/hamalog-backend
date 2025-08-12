package com.Hamalog.security.oauth2;

import com.Hamalog.security.jwt.JwtTokenProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final String redirectBase;

    public OAuth2AuthenticationSuccessHandler(
            JwtTokenProvider jwtTokenProvider,
            @Value("${hamalog.oauth2.redirect-uri:http://localhost:3000/oauth/kakao}") String redirectBase
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.redirectBase = redirectBase;
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
                loginId = "kakao_" + kakaoId;
            }
        } else {
            loginId = authentication.getName();
        }

        String token = jwtTokenProvider.createToken(loginId);

        // Use secure HTTP-only cookie instead of URL parameter for token delivery
        jakarta.servlet.http.Cookie tokenCookie = new jakarta.servlet.http.Cookie("auth_token", token);
        tokenCookie.setHttpOnly(true);
        tokenCookie.setSecure(true); // Use HTTPS in production
        tokenCookie.setPath("/");
        tokenCookie.setMaxAge(3600); // 1 hour
        tokenCookie.setAttribute("SameSite", "Strict");
        response.addCookie(tokenCookie);

        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", redirectBase);
    }
}
