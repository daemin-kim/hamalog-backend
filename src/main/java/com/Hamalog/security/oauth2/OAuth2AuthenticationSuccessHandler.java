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
                // Fallback to name
                loginId = authentication.getName();
            } else {
                long kakaoId = ((Number) idObj).longValue();
                loginId = "kakao_" + kakaoId;
            }
        } else {
            loginId = authentication.getName();
        }

        String token = jwtTokenProvider.createToken(loginId);

        String target = redirectBase;
        // append token as query param
        String sep = target.contains("?") ? "&" : "?";
        String url = target + sep + "token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);

        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", url);
    }
}
