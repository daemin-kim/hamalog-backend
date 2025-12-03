package com.Hamalog.service.auth;

import com.Hamalog.domain.events.member.MemberDeletedEvent;
import com.Hamalog.domain.member.Member;
import com.Hamalog.dto.auth.request.SignupRequest;
import com.Hamalog.dto.auth.response.LoginResponse;
import com.Hamalog.dto.auth.response.TokenRefreshResponse;
import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.logging.SensitiveDataMasker;
import com.Hamalog.repository.member.MemberRepository;
import com.Hamalog.repository.medication.MedicationRecordRepository;
import com.Hamalog.repository.medication.MedicationScheduleRepository;
import com.Hamalog.repository.sideEffect.SideEffectRecordRepository;
import com.Hamalog.security.jwt.JwtTokenProvider;
import com.Hamalog.security.jwt.TokenBlacklistService;
import com.Hamalog.service.security.RefreshTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final MemberRepository memberRepository;
    private final MedicationScheduleRepository medicationScheduleRepository;
    private final MedicationRecordRepository medicationRecordRepository;
    private final SideEffectRecordRepository sideEffectRecordRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(rollbackFor = {Exception.class})
    public void registerMember(SignupRequest request) {
        validateMemberRegistration(request);
        
        Member member = createMemberFromRequest(request);
        memberRepository.save(member);
    }

    public LoginResponse authenticateAndGenerateToken(String loginId, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginId, password)
        );
        
        Member member = memberRepository.findByLoginId(loginId)
            .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // AccessToken 생성
        String accessToken = jwtTokenProvider.createToken(authentication.getName(), member.getMemberId(), null);
        long expiresIn = 900;  // 15분

        // RefreshToken 생성
        var refreshToken = refreshTokenService.createRefreshToken(member.getMemberId());

        log.info("[AUTH] User logged in - loginId: {}, memberId: {}",
            SensitiveDataMasker.maskEmail(loginId),
            SensitiveDataMasker.maskUserId(member.getMemberId()));

        return new LoginResponse(
            accessToken,
            refreshToken.getTokenValue(),
            expiresIn
        );
    }

    public void logoutUser(String token) {
        if (isValidTokenFormat(token)) {
            tokenBlacklistService.blacklistToken(token);
        }
    }

    /**
     * RefreshToken으로 새 AccessToken 발급
     */
    public TokenRefreshResponse refreshAccessToken(String refreshTokenValue) {
        var refreshToken = refreshTokenService.rotateToken(refreshTokenValue);

        // 새 AccessToken 생성
        Member member = memberRepository.findById(refreshToken.getMemberId())
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        String newAccessToken = jwtTokenProvider.createToken(member.getLoginId(), member.getMemberId(), null);

        long expiresIn = 900;  // 15분

        log.debug("[AUTH] Access token refreshed - memberId: {}", refreshToken.getMemberId());

        return new TokenRefreshResponse(
            newAccessToken,
            refreshToken.getTokenValue(),
            expiresIn
        );
    }

    @Transactional(rollbackFor = {Exception.class})
    public void deleteMember(String loginId, String token) {
        // 1. 즉시 토큰 무효화 (트랜잭션 내부에서 처리하여 동시성 문제 방지)
        if (isValidTokenFormat(token)) {
            tokenBlacklistService.blacklistToken(token);
            log.info("[AUTH] Token blacklisted immediately during member deletion - loginId: {}",
                SensitiveDataMasker.maskEmail(loginId));
        }

        // 2. 회원 조회 및 검증
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        Long memberId = member.getMemberId();

        // 3. 관련 데이터 삭제 (cascade 순서 보장)
        deleteMemberRelatedData(memberId);
        
        // 4. 회원 완전 삭제
        memberRepository.delete(member);
        
        log.info("[AUTH] Member deleted successfully - loginId: {}, memberId: {}",
            SensitiveDataMasker.maskEmail(loginId),
            SensitiveDataMasker.maskUserId(memberId));

        // 5. 이벤트 발행 (추가 후처리용)
        eventPublisher.publishEvent(new MemberDeletedEvent(loginId, token, memberId));
    }

    private void deleteMemberRelatedData(Long memberId) {
        sideEffectRecordRepository.deleteByMemberId(memberId);

        var medicationScheduleIds = medicationScheduleRepository.findAllByMember_MemberId(memberId)
                .stream()
                .map(schedule -> schedule.getMedicationScheduleId())
                .toList();

        if (!medicationScheduleIds.isEmpty()) {
            medicationRecordRepository.deleteByScheduleIds(medicationScheduleIds);
        }

        medicationScheduleRepository.deleteByMemberId(memberId);
    }

    private void validateMemberRegistration(SignupRequest request) {
        if (memberRepository.findByLoginId(request.loginId()).isPresent()) {
            throw new CustomException(ErrorCode.DUPLICATE_MEMBER);
        }
    }

    private Member createMemberFromRequest(SignupRequest request) {
        return Member.builder()
                .loginId(request.loginId())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .nickName(request.nickName())
                .phoneNumber(request.phoneNumber())
                .birth(request.birth())
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Transactional
    public LoginResponse processOAuth2Callback(String code) {
        try {
            // 입력값 검증
            if (code == null || code.trim().isEmpty()) {
                log.warn("OAuth2 callback received with null or empty authorization code");
                throw new com.Hamalog.exception.oauth2.OAuth2Exception(ErrorCode.OAUTH2_INVALID_CODE);
            }

            // Get Kakao client registration
            ClientRegistration kakaoRegistration = clientRegistrationRepository.findByRegistrationId("kakao");
            if (kakaoRegistration == null) {
                log.error("Kakao OAuth2 client registration not found");
                throw new com.Hamalog.exception.oauth2.OAuth2Exception(ErrorCode.OAUTH2_CONFIG_ERROR);
            }
            
            // Exchange authorization code for access token
            String accessToken = exchangeCodeForToken(code, kakaoRegistration);
            if (accessToken == null || accessToken.trim().isEmpty()) {
                log.error("Failed to exchange authorization code for access token");
                throw new com.Hamalog.exception.oauth2.OAuth2TokenExchangeException();
            }
            
            // Get user info from Kakao
            JsonNode userInfo = getUserInfoFromKakao(accessToken);
            if (userInfo == null) {
                log.error("Failed to retrieve user info from Kakao");
                throw new com.Hamalog.exception.oauth2.OAuth2Exception(ErrorCode.OAUTH2_USER_INFO_FAILED);
            }
            
            // Process user (create or find existing)
            String loginId = processKakaoUser(userInfo);
            
            // 회원 조회하여 RefreshToken 생성
            Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

            // Generate JWT token
            String jwtToken = jwtTokenProvider.createToken(loginId, member.getMemberId(), null);
            long expiresIn = 900;  // 15분

            // RefreshToken 생성
            var refreshToken = refreshTokenService.createRefreshToken(member.getMemberId());

            log.info("[OAUTH2] User logged in via Kakao - loginId: {}",
                SensitiveDataMasker.maskEmail(loginId));

            return new LoginResponse(jwtToken, refreshToken.getTokenValue(), expiresIn);

        } catch (com.Hamalog.exception.oauth2.OAuth2Exception e) {
            // OAuth2 관련 예외는 그대로 던짐
            throw e;
        } catch (CustomException e) {
            // 기타 커스텀 예외도 그대로 던짐
            throw e;
        } catch (Exception e) {
            // 예상치 못한 예외는 로그 후 일반 오류로 변환
            log.error("Unexpected error during OAuth2 callback processing", e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
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
            
            log.debug("Exchanging Kakao authorization code for access token at: {}", kakaoRegistration.getProviderDetails().getTokenUri());

            // Make token request
            ResponseEntity<String> response = restTemplate.postForEntity(
                    kakaoRegistration.getProviderDetails().getTokenUri(),
                    request,
                    String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode tokenResponse = objectMapper.readTree(response.getBody());

                // access_token 필드 존재 여부 확인
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

        } catch (org.springframework.web.client.ResourceAccessException e) {
            // 네트워크 타임아웃, 연결 실패 등
            log.error("Network error while exchanging authorization code for token", e);
            throw new CustomException(ErrorCode.EXTERNAL_API_TIMEOUT);
        } catch (org.springframework.web.client.HttpClientErrorException |
                 org.springframework.web.client.HttpServerErrorException e) {
            // HTTP 4xx, 5xx 오류
            log.error("HTTP error while exchanging authorization code. Status: {}, Body: {}",
                e.getStatusCode(), e.getResponseBodyAsString());
            throw new CustomException(ErrorCode.EXTERNAL_API_ERROR);
        } catch (Exception e) {
            log.error("Unexpected exception while exchanging authorization code for token", e);
            return null;
        }
    }
    
    private JsonNode getUserInfoFromKakao(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> request = new HttpEntity<>(headers);
            
            log.debug("Fetching user info from Kakao");

            ResponseEntity<String> response = restTemplate.exchange(
                    "https://kapi.kakao.com/v2/user/me",
                    HttpMethod.GET,
                    request,
                    String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode userInfo = objectMapper.readTree(response.getBody());

                // 필수 필드 검증
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

        } catch (org.springframework.web.client.ResourceAccessException e) {
            // 네트워크 타임아웃, 연결 실패 등
            log.error("Network error while fetching user info from Kakao", e);
            throw new CustomException(ErrorCode.EXTERNAL_API_TIMEOUT);
        } catch (org.springframework.web.client.HttpClientErrorException |
                 org.springframework.web.client.HttpServerErrorException e) {
            // HTTP 4xx, 5xx 오류
            log.error("HTTP error while fetching user info. Status: {}, Body: {}",
                e.getStatusCode(), e.getResponseBodyAsString());
            throw new CustomException(ErrorCode.EXTERNAL_API_ERROR);
        } catch (Exception e) {
            log.error("Unexpected exception while fetching user info from Kakao", e);
            return null;
        }
    }
    
    private String processKakaoUser(JsonNode userInfo) {
        try {
            // Extract Kakao ID with validation
            if (!userInfo.has("id")) {
                log.error("Kakao user info missing 'id' field");
                throw new com.Hamalog.exception.oauth2.OAuth2Exception(ErrorCode.OAUTH2_USER_INFO_FAILED);
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
                // Create new member (same logic as KakaoOAuth2UserService)
                String name = (nickname != null && !nickname.isBlank()) ?
                        (nickname.length() > 15 ? nickname.substring(0, 15) : nickname) :
                        "OAuth2_" + kakaoId.toString().substring(0, Math.min(kakaoId.toString().length(), 8));

                String phoneNumber = "01012345678"; // Placeholder as in KakaoOAuth2UserService
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

            return loginId;

        } catch (Exception e) {
            log.error("Error processing Kakao user", e);
            throw new com.Hamalog.exception.oauth2.OAuth2Exception(ErrorCode.OAUTH2_USER_INFO_FAILED);
        }
    }

    private boolean isValidTokenFormat(String token) {
        return token != null && !token.isBlank();
    }
}
