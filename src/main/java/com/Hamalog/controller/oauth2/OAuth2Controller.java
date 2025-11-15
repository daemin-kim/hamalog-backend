package com.Hamalog.controller.oauth2;

import com.Hamalog.dto.auth.response.LoginResponse;
import com.Hamalog.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Tag(name = "OAuth2 Authentication API", description = "OAuth2 소셜 로그인 관련 API")
@RestController
@RequiredArgsConstructor
public class OAuth2Controller {

    private static final Logger log = LoggerFactory.getLogger(OAuth2Controller.class);
    
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final AuthService authService;

    @Value("${hamalog.oauth2.rn-app-redirect-scheme:hamalog-rn}")
    private String rnAppRedirectScheme;

    @Operation(
            summary = "카카오 OAuth2 로그인 시작",
            description = "카카오 OAuth2 인증 과정을 시작합니다. 사용자를 카카오 인증 서버로 리디렉션합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "302",
                    description = "카카오 인증 서버로 리디렉션",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "OAuth2 설정 오류",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "OAuth2ConfigError",
                                    value = "{\n  \"error\": \"OAuth2 configuration error\",\n  \"message\": \"카카오 OAuth2 설정을 확인해주세요.\"\n}"
                            )
                    )
            )
    })
    @GetMapping("/oauth2/auth/kakao")
    public void kakaoAuth(HttpServletResponse response) throws IOException {
        try {
            // Get Kakao client registration
            ClientRegistration kakaoRegistration = clientRegistrationRepository.findByRegistrationId("kakao");
            
            if (kakaoRegistration == null) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "카카오 OAuth2 설정이 없습니다.");
                return;
            }
            
            // Generate state parameter for security
            String state = UUID.randomUUID().toString();
            
            // Build authorization URL
            String authorizationUri = UriComponentsBuilder
                    .fromUriString(kakaoRegistration.getProviderDetails().getAuthorizationUri())
                    .queryParam(OAuth2ParameterNames.RESPONSE_TYPE, "code")
                    .queryParam(OAuth2ParameterNames.CLIENT_ID, kakaoRegistration.getClientId())
                    .queryParam(OAuth2ParameterNames.SCOPE, String.join(" ", kakaoRegistration.getScopes()))
                    .queryParam(OAuth2ParameterNames.STATE, state)
                    .queryParam(OAuth2ParameterNames.REDIRECT_URI, kakaoRegistration.getRedirectUri())
                    .build()
                    .toUriString();
            
            // Redirect to Kakao authorization server
            response.sendRedirect(authorizationUri);
            
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "OAuth2 인증 초기화 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Operation(
            summary = "카카오 OAuth2 콜백 처리",
            description = "카카오에서 전송된 authorization code를 처리하여 JWT 토큰으로 교환하고 RN 앱으로 리다이렉트합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "302",
                    description = "RN 앱으로 리다이렉트 (JWT 토큰 포함)",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 authorization code",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content
            )
    })
    @GetMapping("/oauth2/auth/kakao/callback")
    public void handleKakaoCallback(
            @RequestParam("code") String code,
            @RequestParam(value = "state", required = false) String state,
            HttpServletResponse response) throws IOException {
        try {
            // TODO: State parameter validation should be implemented in future versions
            // for preventing CSRF attacks
            log.info("Processing Kakao OAuth2 callback with code: {}", code.substring(0, Math.min(code.length(), 10)) + "...");
            if (state != null) {
                log.debug("State parameter received (validation not implemented yet)");
            }

            // Process OAuth2 callback and get JWT token
            LoginResponse loginResponse = authService.processOAuth2Callback(code);
            String jwtToken = loginResponse.token();

            // Redirect to RN app with JWT token
            String redirectUri = String.format("%s://auth?token=%s",
                    rnAppRedirectScheme,
                    URLEncoder.encode(jwtToken, StandardCharsets.UTF_8));

            log.info("Redirecting to RN app: {}://auth?token=***", rnAppRedirectScheme);
            response.sendRedirect(redirectUri);

        } catch (Exception e) {
            log.error("Error processing Kakao OAuth2 callback", e);
            // Redirect to RN app with error information
            String redirectUri = String.format("%s://auth?error=%s",
                    rnAppRedirectScheme,
                    URLEncoder.encode("TOKEN_EXCHANGE_FAILED", StandardCharsets.UTF_8));
            response.sendRedirect(redirectUri);
        }
    }
}