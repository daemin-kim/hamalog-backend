package com.Hamalog.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.Hamalog.domain.member.Member;
import com.Hamalog.domain.security.RefreshToken;
import com.Hamalog.dto.auth.response.LoginResponse;
import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.exception.oauth2.OAuth2Exception;
import com.Hamalog.exception.oauth2.OAuth2TokenExchangeException;
import com.Hamalog.repository.member.MemberRepository;
import com.Hamalog.security.jwt.JwtTokenProvider;
import com.Hamalog.service.security.RefreshTokenService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("KakaoOAuth2AuthService 테스트")
class KakaoOAuth2AuthServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ClientRegistrationRepository clientRegistrationRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private KakaoOAuth2AuthService kakaoOAuth2AuthService;

    private ClientRegistration kakaoRegistration;
    private Member testMember;
    private RefreshToken testRefreshToken;
    private ObjectMapper realObjectMapper;

    @BeforeEach
    void setUp() {
        realObjectMapper = new ObjectMapper();

        kakaoRegistration = ClientRegistration.withRegistrationId("kakao")
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

        testMember = Member.builder()
            .memberId(1L)
            .loginId("kakao_12345@oauth2.internal")
            .password("{oauth2}")
            .name("테스트유저")
            .build();

        testRefreshToken = RefreshToken.builder()
            .id(1L)
            .memberId(1L)
            .tokenValue("refresh-token-value")
            .createdAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusDays(7))
            .rotatedAt(LocalDateTime.now())
            .build();
    }

    @Nested
    @DisplayName("OAuth2 콜백 처리 테스트")
    class ProcessOAuth2CallbackTest {

        @Test
        @DisplayName("OAuth2 콜백 처리 성공 - 기존 회원")
        void processOAuth2Callback_ExistingUser_Success() throws JsonProcessingException {
            // given
            String code = "valid-auth-code";
            String accessToken = "kakao-access-token";
            String jwtToken = "jwt-token";
            String loginId = "kakao_12345@oauth2.internal";

            given(clientRegistrationRepository.findByRegistrationId("kakao"))
                .willReturn(kakaoRegistration);

            // Mock token exchange
            String tokenResponseBody = "{\"access_token\":\"" + accessToken + "\"}";
            ResponseEntity<String> tokenResponse = new ResponseEntity<>(tokenResponseBody, HttpStatus.OK);
            given(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .willReturn(tokenResponse);
            given(objectMapper.readTree(tokenResponseBody))
                .willReturn(realObjectMapper.readTree(tokenResponseBody));

            // Mock user info retrieval
            String userInfoBody = "{\"id\":12345,\"kakao_account\":{\"profile\":{\"nickname\":\"테스트유저\"}}}";
            ResponseEntity<String> userInfoResponse = new ResponseEntity<>(userInfoBody, HttpStatus.OK);
            given(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .willReturn(userInfoResponse);
            given(objectMapper.readTree(userInfoBody))
                .willReturn(realObjectMapper.readTree(userInfoBody));

            // Mock existing member
            given(memberRepository.findByLoginId(loginId))
                .willReturn(Optional.of(testMember));
            given(jwtTokenProvider.createToken(loginId, testMember.getMemberId(), null))
                .willReturn(jwtToken);
            given(refreshTokenService.createRefreshToken(testMember.getMemberId()))
                .willReturn(testRefreshToken);

            // when
            LoginResponse response = kakaoOAuth2AuthService.processOAuth2Callback(code);

            // then
            assertThat(response.token()).isEqualTo(jwtToken);
            assertThat(response.refreshToken()).isEqualTo(testRefreshToken.getTokenValue());
            assertThat(response.expiresIn()).isEqualTo(900L);

            verify(clientRegistrationRepository).findByRegistrationId("kakao");
            verify(jwtTokenProvider).createToken(loginId, testMember.getMemberId(), null);
            verify(refreshTokenService).createRefreshToken(testMember.getMemberId());
        }

        @Test
        @DisplayName("OAuth2 콜백 처리 성공 - 새 회원 생성")
        void processOAuth2Callback_NewUser_Success() throws JsonProcessingException {
            // given
            String code = "valid-auth-code";
            String accessToken = "kakao-access-token";
            String jwtToken = "jwt-token";
            String loginId = "kakao_12345@oauth2.internal";

            given(clientRegistrationRepository.findByRegistrationId("kakao"))
                .willReturn(kakaoRegistration);

            // Mock token exchange
            String tokenResponseBody = "{\"access_token\":\"" + accessToken + "\"}";
            ResponseEntity<String> tokenResponse = new ResponseEntity<>(tokenResponseBody, HttpStatus.OK);
            given(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .willReturn(tokenResponse);
            given(objectMapper.readTree(tokenResponseBody))
                .willReturn(realObjectMapper.readTree(tokenResponseBody));

            // Mock user info retrieval
            String userInfoBody = "{\"id\":12345,\"kakao_account\":{\"profile\":{\"nickname\":\"테스트유저\"}}}";
            ResponseEntity<String> userInfoResponse = new ResponseEntity<>(userInfoBody, HttpStatus.OK);
            given(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .willReturn(userInfoResponse);
            given(objectMapper.readTree(userInfoBody))
                .willReturn(realObjectMapper.readTree(userInfoBody));

            // Mock new member - first call returns empty, save returns member, second call returns member
            given(memberRepository.findByLoginId(loginId))
                .willReturn(Optional.empty())
                .willReturn(Optional.of(testMember));
            given(memberRepository.save(any(Member.class))).willReturn(testMember);
            given(jwtTokenProvider.createToken(loginId, testMember.getMemberId(), null))
                .willReturn(jwtToken);
            given(refreshTokenService.createRefreshToken(testMember.getMemberId()))
                .willReturn(testRefreshToken);

            // when
            LoginResponse response = kakaoOAuth2AuthService.processOAuth2Callback(code);

            // then
            assertThat(response.token()).isEqualTo(jwtToken);

            ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
            verify(memberRepository).save(memberCaptor.capture());

            Member savedMember = memberCaptor.getValue();
            assertThat(savedMember.getLoginId()).isEqualTo(loginId);
            assertThat(savedMember.getPassword()).isEqualTo("{oauth2}");
            assertThat(savedMember.getName()).isEqualTo("테스트유저");
        }

        @Test
        @DisplayName("OAuth2 콜백 실패 - null 인증 코드")
        void processOAuth2Callback_NullCode_ThrowsException() {
            // given
            String code = null;

            // when & then
            assertThatThrownBy(() -> kakaoOAuth2AuthService.processOAuth2Callback(code))
                .isInstanceOf(OAuth2Exception.class)
                .hasMessageContaining(ErrorCode.OAUTH2_INVALID_CODE.getMessage());
        }

        @Test
        @DisplayName("OAuth2 콜백 실패 - 빈 인증 코드")
        void processOAuth2Callback_EmptyCode_ThrowsException() {
            // given
            String code = "";

            // when & then
            assertThatThrownBy(() -> kakaoOAuth2AuthService.processOAuth2Callback(code))
                .isInstanceOf(OAuth2Exception.class)
                .hasMessageContaining(ErrorCode.OAUTH2_INVALID_CODE.getMessage());
        }

        @Test
        @DisplayName("OAuth2 콜백 실패 - 카카오 클라이언트 설정 없음")
        void processOAuth2Callback_NoClientRegistration_ThrowsException() {
            // given
            String code = "valid-auth-code";

            given(clientRegistrationRepository.findByRegistrationId("kakao"))
                .willReturn(null);

            // when & then
            assertThatThrownBy(() -> kakaoOAuth2AuthService.processOAuth2Callback(code))
                .isInstanceOf(OAuth2Exception.class)
                .hasMessageContaining(ErrorCode.OAUTH2_CONFIG_ERROR.getMessage());
        }

        @Test
        @DisplayName("OAuth2 콜백 실패 - 토큰 교환 실패")
        void processOAuth2Callback_TokenExchangeFailed_ThrowsException() throws JsonProcessingException {
            // given
            String code = "valid-auth-code";

            given(clientRegistrationRepository.findByRegistrationId("kakao"))
                .willReturn(kakaoRegistration);

            // Mock failed token exchange - no access_token in response
            String tokenResponseBody = "{}";
            ResponseEntity<String> tokenResponse = new ResponseEntity<>(tokenResponseBody, HttpStatus.OK);
            given(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .willReturn(tokenResponse);
            given(objectMapper.readTree(tokenResponseBody))
                .willReturn(realObjectMapper.readTree(tokenResponseBody));

            // when & then
            assertThatThrownBy(() -> kakaoOAuth2AuthService.processOAuth2Callback(code))
                .isInstanceOf(OAuth2TokenExchangeException.class);
        }

        @Test
        @DisplayName("OAuth2 콜백 실패 - 사용자 정보 조회 실패")
        void processOAuth2Callback_UserInfoFailed_ThrowsException() throws JsonProcessingException {
            // given
            String code = "valid-auth-code";
            String accessToken = "kakao-access-token";

            given(clientRegistrationRepository.findByRegistrationId("kakao"))
                .willReturn(kakaoRegistration);

            // Mock token exchange
            String tokenResponseBody = "{\"access_token\":\"" + accessToken + "\"}";
            ResponseEntity<String> tokenResponse = new ResponseEntity<>(tokenResponseBody, HttpStatus.OK);
            given(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .willReturn(tokenResponse);
            given(objectMapper.readTree(tokenResponseBody))
                .willReturn(realObjectMapper.readTree(tokenResponseBody));

            // Mock failed user info - no id in response
            String userInfoBody = "{}";
            ResponseEntity<String> userInfoResponse = new ResponseEntity<>(userInfoBody, HttpStatus.OK);
            given(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .willReturn(userInfoResponse);
            given(objectMapper.readTree(userInfoBody))
                .willReturn(realObjectMapper.readTree(userInfoBody));

            // when & then
            assertThatThrownBy(() -> kakaoOAuth2AuthService.processOAuth2Callback(code))
                .isInstanceOf(OAuth2Exception.class)
                .hasMessageContaining(ErrorCode.OAUTH2_USER_INFO_FAILED.getMessage());
        }

        @Test
        @DisplayName("OAuth2 콜백 실패 - 네트워크 오류 시 예외 발생")
        void processOAuth2Callback_NetworkError_ThrowsException() {
            // given
            String code = "valid-auth-code";

            given(clientRegistrationRepository.findByRegistrationId("kakao"))
                .willReturn(kakaoRegistration);

            // Mock network error
            given(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .willThrow(new ResourceAccessException("Connection timeout"));

            // when & then
            assertThatThrownBy(() -> kakaoOAuth2AuthService.processOAuth2Callback(code))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.EXTERNAL_API_TIMEOUT.getMessage());
        }

        @Test
        @DisplayName("OAuth2 콜백 실패 - 예상치 못한 예외 시 INTERNAL_SERVER_ERROR")
        void processOAuth2Callback_UnexpectedException_ThrowsInternalServerError() {
            // given
            String code = "valid-auth-code";

            given(clientRegistrationRepository.findByRegistrationId("kakao"))
                .willThrow(new RuntimeException("Unexpected error"));

            // when & then
            assertThatThrownBy(() -> kakaoOAuth2AuthService.processOAuth2Callback(code))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
        }
    }

    @Nested
    @DisplayName("새 회원 생성 테스트")
    class NewMemberCreationTest {

        @Test
        @DisplayName("닉네임이 있는 경우 이름으로 사용된다")
        void newMember_WithNickname_UsesNicknameAsName() throws JsonProcessingException {
            // given
            String code = "valid-auth-code";
            String accessToken = "kakao-access-token";
            String loginId = "kakao_12345@oauth2.internal";

            given(clientRegistrationRepository.findByRegistrationId("kakao"))
                .willReturn(kakaoRegistration);

            // Mock token exchange
            String tokenResponseBody = "{\"access_token\":\"" + accessToken + "\"}";
            ResponseEntity<String> tokenResponse = new ResponseEntity<>(tokenResponseBody, HttpStatus.OK);
            given(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .willReturn(tokenResponse);
            given(objectMapper.readTree(tokenResponseBody))
                .willReturn(realObjectMapper.readTree(tokenResponseBody));

            // Mock user info with nickname
            String userInfoBody = "{\"id\":12345,\"kakao_account\":{\"profile\":{\"nickname\":\"카카오닉네임\"}}}";
            ResponseEntity<String> userInfoResponse = new ResponseEntity<>(userInfoBody, HttpStatus.OK);
            given(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .willReturn(userInfoResponse);
            given(objectMapper.readTree(userInfoBody))
                .willReturn(realObjectMapper.readTree(userInfoBody));

            given(memberRepository.findByLoginId(loginId))
                .willReturn(Optional.empty())
                .willReturn(Optional.of(testMember));
            given(memberRepository.save(any(Member.class))).willReturn(testMember);
            given(jwtTokenProvider.createToken(anyString(), anyLong(), any()))
                .willReturn("jwt-token");
            given(refreshTokenService.createRefreshToken(anyLong()))
                .willReturn(testRefreshToken);

            // when
            kakaoOAuth2AuthService.processOAuth2Callback(code);

            // then
            ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
            verify(memberRepository).save(memberCaptor.capture());

            Member savedMember = memberCaptor.getValue();
            assertThat(savedMember.getName()).isEqualTo("카카오닉네임");
            assertThat(savedMember.getNickName()).isEqualTo("카카오닉네임");
        }

        @Test
        @DisplayName("닉네임이 없는 경우 기본 이름이 사용된다")
        void newMember_WithoutNickname_UsesDefaultName() throws JsonProcessingException {
            // given
            String code = "valid-auth-code";
            String accessToken = "kakao-access-token";
            String loginId = "kakao_12345@oauth2.internal";

            given(clientRegistrationRepository.findByRegistrationId("kakao"))
                .willReturn(kakaoRegistration);

            // Mock token exchange
            String tokenResponseBody = "{\"access_token\":\"" + accessToken + "\"}";
            ResponseEntity<String> tokenResponse = new ResponseEntity<>(tokenResponseBody, HttpStatus.OK);
            given(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .willReturn(tokenResponse);
            given(objectMapper.readTree(tokenResponseBody))
                .willReturn(realObjectMapper.readTree(tokenResponseBody));

            // Mock user info without nickname
            String userInfoBody = "{\"id\":12345}";
            ResponseEntity<String> userInfoResponse = new ResponseEntity<>(userInfoBody, HttpStatus.OK);
            given(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .willReturn(userInfoResponse);
            given(objectMapper.readTree(userInfoBody))
                .willReturn(realObjectMapper.readTree(userInfoBody));

            given(memberRepository.findByLoginId(loginId))
                .willReturn(Optional.empty())
                .willReturn(Optional.of(testMember));
            given(memberRepository.save(any(Member.class))).willReturn(testMember);
            given(jwtTokenProvider.createToken(anyString(), anyLong(), any()))
                .willReturn("jwt-token");
            given(refreshTokenService.createRefreshToken(anyLong()))
                .willReturn(testRefreshToken);

            // when
            kakaoOAuth2AuthService.processOAuth2Callback(code);

            // then
            ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
            verify(memberRepository).save(memberCaptor.capture());

            Member savedMember = memberCaptor.getValue();
            assertThat(savedMember.getName()).startsWith("OAuth2_");
            assertThat(savedMember.getNickName()).isEqualTo("카카오유저");
        }
    }
}
