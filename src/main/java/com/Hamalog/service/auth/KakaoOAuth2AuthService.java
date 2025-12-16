package com.Hamalog.service.auth;

import com.Hamalog.domain.member.Member;
import com.Hamalog.dto.auth.response.LoginResponse;
import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.exception.oauth2.OAuth2Exception;
import com.Hamalog.exception.oauth2.OAuth2TokenExchangeException;
import com.Hamalog.logging.SensitiveDataMasker;
import com.Hamalog.repository.member.MemberRepository;
import com.Hamalog.security.jwt.JwtTokenProvider;
import com.Hamalog.service.security.RefreshTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * 카카오 OAuth2 인증 서비스
 * 카카오 OAuth2 콜백 처리, 토큰 교환, 사용자 정보 조회 등의 책임을 담당합니다.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class KakaoOAuth2AuthService {

    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";
    private static final long ACCESS_TOKEN_EXPIRES_IN = 900L;  // 15분

    private final MemberRepository memberRepository;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    /**
     * OAuth2 콜백을 처리하고 JWT 토큰을 발급합니다.
     *
     * @param code 카카오 인증 코드
     * @return 로그인 응답 (AccessToken, RefreshToken, 만료 시간)
     * @throws OAuth2Exception OAuth2 인증 과정에서 오류 발생 시
     * @throws CustomException 기타 오류 발생 시
     */
    @Transactional
    public LoginResponse processOAuth2Callback(String code) {
        try {
            // 입력값 검증
            if (code == null || code.trim().isEmpty()) {
                log.warn("OAuth2 callback received with null or empty authorization code");
                throw new OAuth2Exception(ErrorCode.OAUTH2_INVALID_CODE);
            }

            // Get Kakao client registration
            ClientRegistration kakaoRegistration = clientRegistrationRepository.findByRegistrationId("kakao");
            if (kakaoRegistration == null) {
                log.error("Kakao OAuth2 client registration not found");
                throw new OAuth2Exception(ErrorCode.OAUTH2_CONFIG_ERROR);
            }

            // Exchange authorization code for access token
            String accessToken = exchangeCodeForToken(code, kakaoRegistration);
            if (accessToken == null || accessToken.trim().isEmpty()) {
                log.error("Failed to exchange authorization code for access token");
                throw new OAuth2TokenExchangeException();
            }

            // Get user info from Kakao
            JsonNode userInfo = getUserInfoFromKakao(accessToken);
            if (userInfo == null) {
                log.error("Failed to retrieve user info from Kakao");
                throw new OAuth2Exception(ErrorCode.OAUTH2_USER_INFO_FAILED);
            }

            // Process user (create or find existing)
            String loginId = processKakaoUser(userInfo);

            // 회원 조회하여 RefreshToken 생성
            Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

            // Generate JWT token
            String jwtToken = jwtTokenProvider.createToken(loginId, member.getMemberId(), null);

            // RefreshToken 생성
            var refreshToken = refreshTokenService.createRefreshToken(member.getMemberId());

            log.info("[OAUTH2] User logged in via Kakao - loginId: {}",
                SensitiveDataMasker.maskEmail(loginId));

            return new LoginResponse(jwtToken, refreshToken.getTokenValue(), ACCESS_TOKEN_EXPIRES_IN);

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during OAuth2 callback processing", e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 인증 코드를 액세스 토큰으로 교환합니다.
     */
    private String exchangeCodeForToken(String code, ClientRegistration kakaoRegistration) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "authorization_code");
            params.add("client_id", kakaoRegistration.getClientId());
            params.add("client_secret", kakaoRegistration.getClientSecret());
            params.add("redirect_uri", kakaoRegistration.getRedirectUri());
            params.add("code", code);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            log.debug("Exchanging Kakao authorization code for access token at: {}",
                kakaoRegistration.getProviderDetails().getTokenUri());

            ResponseEntity<String> response = restTemplate.postForEntity(
                    kakaoRegistration.getProviderDetails().getTokenUri(),
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode tokenResponse = objectMapper.readTree(response.getBody());

                if (!tokenResponse.has("access_token")) {
                    log.error("Access token not found in Kakao token response");
                    return null;
                }

                String accessToken = tokenResponse.get("access_token").asText();

                if (accessToken == null || accessToken.trim().isEmpty()) {
                    log.error("Access token is null or empty in Kakao token response");
                    return null;
                }

                log.info("Successfully obtained Kakao access token");
                return accessToken;
            } else {
                log.error("Failed to exchange authorization code. Status: {}, Body: {}",
                    response.getStatusCode(), response.getBody());
                return null;
            }

        } catch (ResourceAccessException e) {
            log.error("Network error while exchanging authorization code for token", e);
            throw new CustomException(ErrorCode.EXTERNAL_API_TIMEOUT);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("HTTP error while exchanging authorization code. Status: {}, Body: {}",
                e.getStatusCode(), e.getResponseBodyAsString());
            throw new CustomException(ErrorCode.EXTERNAL_API_ERROR);
        } catch (Exception e) {
            log.error("Unexpected exception while exchanging authorization code for token", e);
            return null;
        }
    }

    /**
     * 카카오에서 사용자 정보를 가져옵니다.
     */
    private JsonNode getUserInfoFromKakao(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(headers);

            log.debug("Fetching user info from Kakao");

            ResponseEntity<String> response = restTemplate.exchange(
                    KAKAO_USER_INFO_URL,
                    HttpMethod.GET,
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode userInfo = objectMapper.readTree(response.getBody());

                if (!userInfo.has("id")) {
                    log.error("User ID not found in Kakao user info response");
                    return null;
                }

                log.info("Successfully fetched user info from Kakao");
                return userInfo;
            } else {
                log.error("Failed to get user info from Kakao. Status: {}", response.getStatusCode());
                return null;
            }

        } catch (ResourceAccessException e) {
            log.error("Network error while fetching user info from Kakao", e);
            throw new CustomException(ErrorCode.EXTERNAL_API_TIMEOUT);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("HTTP error while fetching user info. Status: {}, Body: {}",
                e.getStatusCode(), e.getResponseBodyAsString());
            throw new CustomException(ErrorCode.EXTERNAL_API_ERROR);
        } catch (Exception e) {
            log.error("Unexpected exception while fetching user info from Kakao", e);
            return null;
        }
    }

    /**
     * 카카오 사용자 정보를 처리하여 회원을 생성하거나 조회합니다.
     */
    private String processKakaoUser(JsonNode userInfo) {
        try {
            if (!userInfo.has("id")) {
                log.error("Kakao user info missing 'id' field");
                throw new OAuth2Exception(ErrorCode.OAUTH2_USER_INFO_FAILED);
            }

            Long kakaoId = userInfo.get("id").asLong();

            // Extract user profile information
            JsonNode kakaoAccount = userInfo.get("kakao_account");
            JsonNode profile = kakaoAccount != null ? kakaoAccount.get("profile") : null;
            String nickname = profile != null && profile.has("nickname") ?
                profile.get("nickname").asText(null) : null;

            // Create loginId in the same format as KakaoOAuth2UserService
            String loginId = "kakao_" + kakaoId + "@oauth2.internal";

            // Check if user already exists
            Optional<Member> existingMember = memberRepository.findByLoginId(loginId);

            if (existingMember.isEmpty()) {
                createNewKakaoMember(kakaoId, loginId, nickname);
            }

            return loginId;

        } catch (OAuth2Exception e) {
            throw e;
        } catch (Exception e) {
            log.error("Error processing Kakao user", e);
            throw new OAuth2Exception(ErrorCode.OAUTH2_USER_INFO_FAILED);
        }
    }

    /**
     * 새 카카오 회원을 생성합니다.
     */
    private void createNewKakaoMember(Long kakaoId, String loginId, String nickname) {
        String name = (nickname != null && !nickname.isBlank()) ?
                (nickname.length() > 15 ? nickname.substring(0, 15) : nickname) :
                "OAuth2_" + kakaoId.toString().substring(0, Math.min(kakaoId.toString().length(), 8));

        String phoneNumber = "01012345678"; // Placeholder for OAuth2 users
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
        log.info("Created new OAuth2 member with loginId: {}", SensitiveDataMasker.maskEmail(loginId));
    }
}
