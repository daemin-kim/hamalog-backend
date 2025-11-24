package com.Hamalog.service.auth;

import com.Hamalog.domain.events.member.MemberDeletedEvent;
import com.Hamalog.domain.member.Member;
import com.Hamalog.domain.medication.MedicationSchedule;
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
import com.Hamalog.service.security.RefreshTokenService;
import com.Hamalog.domain.security.RefreshToken;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private MemberRepository memberRepository;
    
    @Mock
    private MedicationScheduleRepository medicationScheduleRepository;
    
    @Mock
    private MedicationRecordRepository medicationRecordRepository;
    
    @Mock
    private SideEffectRecordRepository sideEffectRecordRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    
    @Mock
    private TokenBlacklistService tokenBlacklistService;
    
    @Mock
    private AuthenticationManager authenticationManager;
    
    @Mock
    private ClientRegistrationRepository clientRegistrationRepository;
    
    @Mock
    private RestTemplate restTemplate;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @Mock
    private ApplicationEventPublisher eventPublisher;
    
    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        // Common setup for tests
    }

    @Test
    @DisplayName("회원 가입이 성공해야 함")
    void registerMember_Success_ShouldRegisterMember() {
        // given
        SignupRequest request = new SignupRequest(
            "test@example.com",
            "password123",
            "홍길동",
            "길동이",
            "01012345678",
            LocalDate.of(1990, 1, 1)
        );
        
        given(memberRepository.findByLoginId(request.loginId())).willReturn(Optional.empty());
        given(passwordEncoder.encode(request.password())).willReturn("encodedPassword");

        // when
        authService.registerMember(request);

        // then
        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());
        
        Member savedMember = memberCaptor.getValue();
        assertThat(savedMember.getLoginId()).isEqualTo(request.loginId());
        assertThat(savedMember.getName()).isEqualTo(request.name());
        assertThat(savedMember.getNickName()).isEqualTo(request.nickName());
        assertThat(savedMember.getPhoneNumber()).isEqualTo(request.phoneNumber());
        assertThat(savedMember.getBirth()).isEqualTo(request.birth());
        assertThat(savedMember.getPassword()).isEqualTo("encodedPassword");
        assertThat(savedMember.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("중복된 로그인 ID로 회원 가입 시 예외가 발생해야 함")
    void registerMember_DuplicateLoginId_ShouldThrowException() {
        // given
        SignupRequest request = new SignupRequest(
            "test@example.com",
            "password123",
            "홍길동",
            "길동이",
            "01012345678",
            LocalDate.of(1990, 1, 1)
        );
        
        Member existingMember = Member.builder().loginId(request.loginId()).build();
        given(memberRepository.findByLoginId(request.loginId())).willReturn(Optional.of(existingMember));

        // when & then
        assertThatThrownBy(() -> authService.registerMember(request))
            .isInstanceOf(CustomException.class)
            .hasMessage(ErrorCode.DUPLICATE_MEMBER.getMessage());

        verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("인증 및 토큰 생성이 성공해야 함")
    void authenticateAndGenerateToken_Success_ShouldReturnToken() {
        // given
        String loginId = "test@example.com";
        String password = "password123";
        String expectedToken = "jwt-token";
        String refreshTokenValue = "refresh-token-value";

        Member member = Member.builder()
            .memberId(1L)
            .loginId(loginId)
            .build();

        RefreshToken refreshToken = RefreshToken.builder()
            .id(1L)
            .memberId(1L)
            .tokenValue(refreshTokenValue)
            .createdAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusDays(7))
            .rotatedAt(LocalDateTime.now())
            .build();

        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .willReturn(authentication);
        given(authentication.getName()).willReturn(loginId);
        given(memberRepository.findByLoginId(loginId)).willReturn(Optional.of(member));
        given(jwtTokenProvider.createToken(loginId)).willReturn(expectedToken);
        given(refreshTokenService.createRefreshToken(1L)).willReturn(refreshToken);

        // when
        LoginResponse response = authService.authenticateAndGenerateToken(loginId, password);

        // then
        assertThat(response.token()).isEqualTo(expectedToken);
        assertThat(response.refreshToken()).isEqualTo(refreshTokenValue);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtTokenProvider).createToken(loginId);
        verify(refreshTokenService).createRefreshToken(1L);
    }

    @Test
    @DisplayName("잘못된 인증 정보로 로그인 시 예외가 발생해야 함")
    void authenticateAndGenerateToken_BadCredentials_ShouldThrowException() {
        // given
        String loginId = "test@example.com";
        String password = "wrongPassword";
        
        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .willThrow(new BadCredentialsException("Bad credentials"));

        // when & then
        assertThatThrownBy(() -> authService.authenticateAndGenerateToken(loginId, password))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessage("Bad credentials");

        verify(jwtTokenProvider, never()).createToken(anyString());
    }

    @Test
    @DisplayName("유효한 토큰으로 로그아웃이 성공해야 함")
    void logoutUser_ValidToken_ShouldBlacklistToken() {
        // given
        String token = "valid-jwt-token";

        // when
        authService.logoutUser(token);

        // then
        verify(tokenBlacklistService).blacklistToken(token);
    }

    @Test
    @DisplayName("null 토큰으로 로그아웃 시 블랙리스트에 추가하지 않아야 함")
    void logoutUser_NullToken_ShouldNotBlacklistToken() {
        // given
        String token = null;

        // when
        authService.logoutUser(token);

        // then
        verify(tokenBlacklistService, never()).blacklistToken(anyString());
    }

    @Test
    @DisplayName("빈 토큰으로 로그아웃 시 블랙리스트에 추가하지 않아야 함")
    void logoutUser_BlankToken_ShouldNotBlacklistToken() {
        // given
        String token = "   ";

        // when
        authService.logoutUser(token);

        // then
        verify(tokenBlacklistService, never()).blacklistToken(anyString());
    }

    @Test
    @DisplayName("회원 삭제가 성공해야 함")
    void deleteMember_Success_ShouldDeleteMemberAndRelatedData() {
        // given
        String loginId = "test@example.com";
        String token = "valid-jwt-token";
        Long memberId = 1L;
        
        Member member = Member.builder()
            .memberId(memberId)
            .loginId(loginId)
            .build();
        
        MedicationSchedule schedule1 = mock(MedicationSchedule.class);
        MedicationSchedule schedule2 = mock(MedicationSchedule.class);
        
        given(schedule1.getMedicationScheduleId()).willReturn(1L);
        given(schedule2.getMedicationScheduleId()).willReturn(2L);
        
        List<MedicationSchedule> schedules = Arrays.asList(schedule1, schedule2);
        
        given(memberRepository.findByLoginId(loginId)).willReturn(Optional.of(member));
        given(medicationScheduleRepository.findAllByMember_MemberId(memberId)).willReturn(schedules);

        // when
        authService.deleteMember(loginId, token);

        // then
        verify(sideEffectRecordRepository).deleteByMemberId(memberId);
        verify(medicationRecordRepository).deleteByScheduleIds(Arrays.asList(1L, 2L));
        verify(medicationScheduleRepository).deleteByMemberId(memberId);
        verify(memberRepository).delete(member);
        
        ArgumentCaptor<MemberDeletedEvent> eventCaptor = ArgumentCaptor.forClass(MemberDeletedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        
        MemberDeletedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getLoginId()).isEqualTo(loginId);
        assertThat(capturedEvent.getToken()).isEqualTo(token);
        assertThat(capturedEvent.getMemberId()).isEqualTo(memberId);
    }

    @Test
    @DisplayName("존재하지 않는 회원 삭제 시 예외가 발생해야 함")
    void deleteMember_MemberNotFound_ShouldThrowException() {
        // given
        String loginId = "nonexistent@example.com";
        String token = "valid-jwt-token";
        
        given(memberRepository.findByLoginId(loginId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.deleteMember(loginId, token))
            .isInstanceOf(CustomException.class)
            .hasMessage(ErrorCode.MEMBER_NOT_FOUND.getMessage());

        verify(memberRepository, never()).delete(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("null 토큰으로 회원 삭제 시 이벤트를 발행하지 않아야 함")
    void deleteMember_NullToken_ShouldNotPublishEvent() {
        // given
        String loginId = "test@example.com";
        String token = null;
        Long memberId = 1L;
        
        Member member = Member.builder()
            .memberId(memberId)
            .loginId(loginId)
            .build();
        
        given(memberRepository.findByLoginId(loginId)).willReturn(Optional.of(member));
        given(medicationScheduleRepository.findAllByMember_MemberId(memberId)).willReturn(Arrays.asList());

        // when
        authService.deleteMember(loginId, token);

        // then
        verify(memberRepository).delete(member);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("OAuth2 콜백 처리가 성공해야 함")
    void processOAuth2Callback_Success_ShouldReturnToken() throws JsonProcessingException {
        // given
        String code = "oauth2-code";
        String accessToken = "kakao-access-token";
        String jwtToken = "jwt-token";
        String refreshTokenValue = "refresh-token-value";
        String loginId = "kakao_12345@oauth2.internal";
        
        ClientRegistration kakaoRegistration = createMockClientRegistration();
        JsonNode userInfo = createMockUserInfo();
        
        Member existingMember = Member.builder()
            .memberId(1L)
            .loginId(loginId)
            .build();

        RefreshToken refreshToken = RefreshToken.builder()
            .id(1L)
            .memberId(1L)
            .tokenValue(refreshTokenValue)
            .createdAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusDays(7))
            .rotatedAt(LocalDateTime.now())
            .build();

        given(clientRegistrationRepository.findByRegistrationId("kakao")).willReturn(kakaoRegistration);
        
        // Mock token exchange
        ResponseEntity<String> tokenResponse = new ResponseEntity<>("{\"access_token\":\"" + accessToken + "\"}", HttpStatus.OK);
        given(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
            .willReturn(tokenResponse);
        given(objectMapper.readTree(tokenResponse.getBody())).willReturn(createMockTokenResponse(accessToken));
        
        // Mock user info retrieval
        ResponseEntity<String> userResponse = new ResponseEntity<>("{\"id\":12345}", HttpStatus.OK);
        given(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
            .willReturn(userResponse);
        given(objectMapper.readTree(userResponse.getBody())).willReturn(userInfo);
        
        // Mock existing user
        given(memberRepository.findByLoginId(loginId)).willReturn(Optional.of(existingMember));
        given(jwtTokenProvider.createToken(loginId)).willReturn(jwtToken);
        given(refreshTokenService.createRefreshToken(1L)).willReturn(refreshToken);

        // when
        LoginResponse response = authService.processOAuth2Callback(code);

        // then
        assertThat(response.token()).isEqualTo(jwtToken);
        assertThat(response.refreshToken()).isEqualTo(refreshTokenValue);
        verify(clientRegistrationRepository).findByRegistrationId("kakao");
        verify(jwtTokenProvider).createToken(loginId);
        verify(refreshTokenService).createRefreshToken(1L);
    }

    @Test
    @DisplayName("OAuth2 콜백 처리 시 클라이언트 등록이 없으면 예외가 발생해야 함")
    void processOAuth2Callback_NoClientRegistration_ShouldThrowException() {
        // given
        String code = "oauth2-code";
        
        given(clientRegistrationRepository.findByRegistrationId("kakao")).willReturn(null);

        // when & then
        assertThatThrownBy(() -> authService.processOAuth2Callback(code))
            .isInstanceOf(com.Hamalog.exception.oauth2.OAuth2Exception.class)
            .hasMessageContaining(ErrorCode.OAUTH2_CONFIG_ERROR.getMessage());
    }

    @Test
    @DisplayName("OAuth2 콜백 처리 시 토큰 교환 실패 시 예외가 발생해야 함")
    void processOAuth2Callback_TokenExchangeFailed_ShouldThrowException() throws JsonProcessingException {
        // given
        String code = "oauth2-code";
        
        ClientRegistration kakaoRegistration = createMockClientRegistration();
        given(clientRegistrationRepository.findByRegistrationId("kakao")).willReturn(kakaoRegistration);
        
        // Mock failed token exchange - 성공 응답이지만 access_token이 없거나 빈 값
        ResponseEntity<String> tokenResponse = new ResponseEntity<>("{}", HttpStatus.OK);
        given(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
            .willReturn(tokenResponse);

        // Mock token response without access_token
        ObjectMapper realObjectMapper = new ObjectMapper();
        JsonNode emptyTokenResponse = realObjectMapper.readTree("{}");
        given(objectMapper.readTree(tokenResponse.getBody())).willReturn(emptyTokenResponse);

        // when & then
        assertThatThrownBy(() -> authService.processOAuth2Callback(code))
            .isInstanceOf(com.Hamalog.exception.oauth2.OAuth2TokenExchangeException.class);
    }

    @Test
    @DisplayName("OAuth2 콜백 처리 시 사용자 정보 조회 실패 시 예외가 발생해야 함")
    void processOAuth2Callback_UserInfoRetrievalFailed_ShouldThrowException() throws JsonProcessingException {
        // given
        String code = "oauth2-code";
        String accessToken = "kakao-access-token";
        
        ClientRegistration kakaoRegistration = createMockClientRegistration();
        given(clientRegistrationRepository.findByRegistrationId("kakao")).willReturn(kakaoRegistration);
        
        // Mock successful token exchange
        ResponseEntity<String> tokenResponse = new ResponseEntity<>("{\"access_token\":\"" + accessToken + "\"}", HttpStatus.OK);
        given(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
            .willReturn(tokenResponse);
        given(objectMapper.readTree(tokenResponse.getBody())).willReturn(createMockTokenResponse(accessToken));
        
        // Mock failed user info retrieval - 응답은 성공이지만 id 필드가 없음
        ResponseEntity<String> userResponse = new ResponseEntity<>("{}", HttpStatus.OK);
        given(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
            .willReturn(userResponse);

        // Mock user info without id field
        ObjectMapper realObjectMapper = new ObjectMapper();
        JsonNode emptyUserInfo = realObjectMapper.readTree("{}");
        given(objectMapper.readTree(userResponse.getBody())).willReturn(emptyUserInfo);

        // when & then
        assertThatThrownBy(() -> authService.processOAuth2Callback(code))
            .isInstanceOf(com.Hamalog.exception.oauth2.OAuth2Exception.class)
            .hasMessageContaining(ErrorCode.OAUTH2_USER_INFO_FAILED.getMessage());
    }

    @Test
    @DisplayName("OAuth2 콜백 처리 시 새로운 사용자 생성이 성공해야 함")
    void processOAuth2Callback_NewUser_ShouldCreateMember() throws JsonProcessingException {
        // given
        String code = "oauth2-code";
        String accessToken = "kakao-access-token";
        String jwtToken = "jwt-token";
        String refreshTokenValue = "refresh-token-value";
        String loginId = "kakao_12345@oauth2.internal";
        
        ClientRegistration kakaoRegistration = createMockClientRegistration();
        JsonNode userInfo = createMockUserInfo();
        
        Member newMember = Member.builder()
            .memberId(1L)
            .loginId(loginId)
            .password("{oauth2}")
            .name("테스트유저")
            .nickName("테스트유저")
            .build();

        RefreshToken refreshToken = RefreshToken.builder()
            .id(1L)
            .memberId(1L)
            .tokenValue(refreshTokenValue)
            .createdAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusDays(7))
            .rotatedAt(LocalDateTime.now())
            .build();

        given(clientRegistrationRepository.findByRegistrationId("kakao")).willReturn(kakaoRegistration);
        
        // Mock token exchange
        ResponseEntity<String> tokenResponse = new ResponseEntity<>("{\"access_token\":\"" + accessToken + "\"}", HttpStatus.OK);
        given(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
            .willReturn(tokenResponse);
        given(objectMapper.readTree(tokenResponse.getBody())).willReturn(createMockTokenResponse(accessToken));
        
        // Mock user info retrieval
        ResponseEntity<String> userResponse = new ResponseEntity<>("{\"id\":12345}", HttpStatus.OK);
        given(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
            .willReturn(userResponse);
        given(objectMapper.readTree(userResponse.getBody())).willReturn(userInfo);
        
        // Mock new user (not existing initially, then return saved member)
        given(memberRepository.findByLoginId(loginId))
            .willReturn(Optional.empty())  // First call - user doesn't exist
            .willReturn(Optional.of(newMember));  // Second call - user exists after save
        given(memberRepository.save(any(Member.class))).willReturn(newMember);
        given(jwtTokenProvider.createToken(loginId)).willReturn(jwtToken);
        given(refreshTokenService.createRefreshToken(1L)).willReturn(refreshToken);

        // when
        LoginResponse response = authService.processOAuth2Callback(code);

        // then
        assertThat(response.token()).isEqualTo(jwtToken);
        assertThat(response.refreshToken()).isEqualTo(refreshTokenValue);

        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());
        
        Member savedMember = memberCaptor.getValue();
        assertThat(savedMember.getLoginId()).isEqualTo(loginId);
        assertThat(savedMember.getPassword()).isEqualTo("{oauth2}");
        assertThat(savedMember.getName()).isEqualTo("테스트유저");
        assertThat(savedMember.getNickName()).isEqualTo("테스트유저");

        verify(refreshTokenService).createRefreshToken(1L);
    }

    @Test
    @DisplayName("OAuth2 콜백 처리 시 예외 발생 시 내부 서버 오류로 변환해야 함")
    void processOAuth2Callback_UnexpectedException_ShouldThrowInternalServerError() {
        // given
        String code = "oauth2-code";
        
        given(clientRegistrationRepository.findByRegistrationId("kakao"))
            .willThrow(new RuntimeException("Unexpected error"));

        // when & then
        assertThatThrownBy(() -> authService.processOAuth2Callback(code))
            .isInstanceOf(CustomException.class)
            .hasMessage(ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
    }

    // Helper methods for creating mock objects
    private ClientRegistration createMockClientRegistration() {
        return ClientRegistration.withRegistrationId("kakao")
            .clientId("kakao-client-id")
            .clientSecret("kakao-client-secret")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("http://localhost:8080/login/oauth2/code/kakao")
            .authorizationUri("https://kauth.kakao.com/oauth/authorize")
            .tokenUri("https://kauth.kakao.com/oauth/token")
            .userInfoUri("https://kapi.kakao.com/v2/user/me")
            .userNameAttributeName("id")
            .clientName("Kakao")
            .build();
    }

    private JsonNode createMockTokenResponse(String accessToken) throws JsonProcessingException {
        String tokenJson = String.format("{\"access_token\":\"%s\",\"token_type\":\"bearer\"}", accessToken);
        ObjectMapper realObjectMapper = new ObjectMapper();
        return realObjectMapper.readTree(tokenJson);
    }

    private JsonNode createMockUserInfo() throws JsonProcessingException {
        String userInfoJson = """
            {
                "id": 12345,
                "kakao_account": {
                    "profile": {
                        "nickname": "테스트유저"
                    }
                }
            }
            """;
        ObjectMapper realObjectMapper = new ObjectMapper();
        return realObjectMapper.readTree(userInfoJson);
    }
}