package com.Hamalog.controller.oauth2;

import com.Hamalog.domain.member.Member;
import com.Hamalog.dto.auth.response.LoginResponse;
import com.Hamalog.repository.member.MemberRepository;
import com.Hamalog.security.jwt.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Tag(name = "OAuth2 Authentication API", description = "OAuth2 소셜 로그인 관련 API")
@RestController
@RequiredArgsConstructor
public class OAuth2Controller {

    private static final Logger log = LoggerFactory.getLogger(OAuth2Controller.class);
    
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

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
            description = "카카오에서 전송된 authorization code를 처리하여 JWT 토큰을 반환합니다. React Native 앱용."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공 및 JWT 토큰 반환",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "LoginSuccess",
                                    value = "{\n  \"token\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\"\n}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 authorization code",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "InvalidCode",
                                    value = "{\n  \"error\": \"invalid_code\",\n  \"message\": \"유효하지 않은 authorization code입니다.\"\n}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "ServerError",
                                    value = "{\n  \"error\": \"server_error\",\n  \"message\": \"토큰 처리 중 오류가 발생했습니다.\"\n}"
                            )
                    )
            )
    })
    @PostMapping("/api/auth/kakao/callback")
    @Transactional
    public ResponseEntity<LoginResponse> handleKakaoCallback(@RequestParam("code") String code) {
        try {
            log.info("Processing Kakao OAuth2 callback with code: {}", code.substring(0, Math.min(code.length(), 10)) + "...");
            
            // Get Kakao client registration
            ClientRegistration kakaoRegistration = clientRegistrationRepository.findByRegistrationId("kakao");
            if (kakaoRegistration == null) {
                log.error("Kakao client registration not found");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            
            // Exchange authorization code for access token
            String accessToken = exchangeCodeForToken(code, kakaoRegistration);
            if (accessToken == null) {
                log.error("Failed to exchange code for access token");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            
            // Get user info from Kakao
            JsonNode userInfo = getUserInfoFromKakao(accessToken);
            if (userInfo == null) {
                log.error("Failed to get user info from Kakao");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            
            // Process user (create or find existing)
            String loginId = processKakaoUser(userInfo);
            
            // Generate JWT token
            String jwtToken = jwtTokenProvider.createToken(loginId);
            
            log.info("Successfully processed Kakao OAuth2 callback for user: {}", loginId);
            return ResponseEntity.ok(new LoginResponse(jwtToken));
            
        } catch (Exception e) {
            log.error("Error processing Kakao OAuth2 callback", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    private String exchangeCodeForToken(String code, ClientRegistration kakaoRegistration) {
        try {
            // Prepare token request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "authorization_code");
            params.add("client_id", kakaoRegistration.getClientId());
            params.add("client_secret", kakaoRegistration.getClientSecret());
            params.add("redirect_uri", kakaoRegistration.getRedirectUri());
            params.add("code", code);
            
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            
            // Make token request
            ResponseEntity<String> response = restTemplate.postForEntity(
                    kakaoRegistration.getProviderDetails().getTokenUri(),
                    request,
                    String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode tokenResponse = objectMapper.readTree(response.getBody());
                return tokenResponse.get("access_token").asText();
            }
            
            log.error("Token exchange failed with status: {}", response.getStatusCode());
            return null;
            
        } catch (Exception e) {
            log.error("Error exchanging code for token", e);
            return null;
        }
    }
    
    private JsonNode getUserInfoFromKakao(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> request = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://kapi.kakao.com/v2/user/me",
                    HttpMethod.GET,
                    request,
                    String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return objectMapper.readTree(response.getBody());
            }
            
            log.error("User info request failed with status: {}", response.getStatusCode());
            return null;
            
        } catch (Exception e) {
            log.error("Error getting user info from Kakao", e);
            return null;
        }
    }
    
    private String processKakaoUser(JsonNode userInfo) {
        // Extract Kakao ID
        Long kakaoId = userInfo.get("id").asLong();
        
        // Extract user profile information
        JsonNode kakaoAccount = userInfo.get("kakao_account");
        JsonNode profile = kakaoAccount != null ? kakaoAccount.get("profile") : null;
        String nickname = profile != null ? profile.get("nickname").asText(null) : null;
        
        // Create loginId in the same format as KakaoOAuth2UserService
        String loginId = "kakao_" + kakaoId + "@oauth2.internal";
        
        // Check if user already exists
        Optional<Member> existingMember = memberRepository.findByLoginId(loginId);
        
        if (existingMember.isEmpty()) {
            // Create new member (same logic as KakaoOAuth2UserService)
            String name = (nickname != null && !nickname.isBlank()) ? 
                    (nickname.length() > 15 ? nickname.substring(0, 15) : nickname) : 
                    "OAuth2_" + kakaoId.toString().substring(0, Math.min(kakaoId.toString().length(), 8));
            
            String phoneNumber = "0101234567"; // Placeholder as in KakaoOAuth2UserService
            LocalDate birth = LocalDate.of(2000, 1, 1); // Default birth date
            String nickName = (nickname != null && !nickname.isBlank()) ? nickname : "카카오유저";
            
            Member member = Member.builder()
                    .loginId(loginId)
                    .password("{oauth2}") // Mark as OAuth2 account
                    .name(name)
                    .nickName(nickName)
                    .phoneNumber(phoneNumber)
                    .birth(birth)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            memberRepository.save(member);
            log.info("Created new OAuth2 member for Kakao id {} as loginId {}", kakaoId, loginId);
        }
        
        return loginId;
    }
}