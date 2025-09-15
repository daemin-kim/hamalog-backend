package com.Hamalog.service.auth;

import com.Hamalog.domain.events.member.MemberDeletedEvent;
import com.Hamalog.domain.member.Member;
import com.Hamalog.domain.medication.MedicationRecord;
import com.Hamalog.domain.medication.MedicationSchedule;
import com.Hamalog.domain.sideEffect.SideEffectRecord;
import com.Hamalog.dto.auth.request.SignupRequest;
import com.Hamalog.dto.auth.response.LoginResponse;
import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.repository.member.MemberRepository;
import com.Hamalog.repository.medication.MedicationRecordRepository;
import com.Hamalog.repository.medication.MedicationScheduleRepository;
import com.Hamalog.repository.sideEffect.SideEffectRecordRepository;
import com.Hamalog.security.jwt.JwtTokenProvider;
import com.Hamalog.security.jwt.TokenBlacklistService;
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
        
        String token = jwtTokenProvider.createToken(authentication.getName());
        return new LoginResponse(token);
    }

    public void logoutUser(String token) {
        if (isValidTokenFormat(token)) {
            tokenBlacklistService.blacklistToken(token);
        }
    }

    @Transactional(rollbackFor = {Exception.class})
    public void deleteMember(String loginId, String token) {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        Long memberId = member.getMemberId();
        deleteMemberRelatedData(memberId);
        
        memberRepository.delete(member);
        
        // Publish event to handle Redis operations after transaction completion
        if (isValidTokenFormat(token)) {
            eventPublisher.publishEvent(new MemberDeletedEvent(loginId, token, memberId));
        }
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
            // Get Kakao client registration
            ClientRegistration kakaoRegistration = clientRegistrationRepository.findByRegistrationId("kakao");
            if (kakaoRegistration == null) {
                throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
            
            // Exchange authorization code for access token
            String accessToken = exchangeCodeForToken(code, kakaoRegistration);
            if (accessToken == null) {
                throw new CustomException(ErrorCode.BAD_REQUEST);
            }
            
            // Get user info from Kakao
            JsonNode userInfo = getUserInfoFromKakao(accessToken);
            if (userInfo == null) {
                throw new CustomException(ErrorCode.BAD_REQUEST);
            }
            
            // Process user (create or find existing)
            String loginId = processKakaoUser(userInfo);
            
            // Generate JWT token
            String jwtToken = jwtTokenProvider.createToken(loginId);
            
            return new LoginResponse(jwtToken);
            
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
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
            
            return null;
            
        } catch (Exception e) {
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
            
            return null;
            
        } catch (Exception e) {
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
        }
        
        return loginId;
    }

    private boolean isValidTokenFormat(String token) {
        return token != null && !token.isBlank();
    }
}