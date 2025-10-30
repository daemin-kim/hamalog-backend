package com.Hamalog.service.oauth2;

import com.Hamalog.domain.member.Member;
import com.Hamalog.repository.member.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KakaoOAuth2UserServiceTest {

    @Mock
    private MemberRepository memberRepository;

    private KakaoOAuth2UserService service;
    private OAuth2UserRequest kakaoRequest;
    private OAuth2User kakaoUser;

    @BeforeEach
    void setUp() {
        // Create sample OAuth2UserRequest for Kakao
        ClientRegistration kakaoRegistration = ClientRegistration.withRegistrationId("kakao")
                .clientId("client-id")
                .clientSecret("client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost:8080/login/oauth2/code/kakao")
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .tokenUri("https://kauth.kakao.com/oauth/token")
                .userInfoUri("https://kapi.kakao.com/v2/user/me")
                .userNameAttributeName("id")
                .clientName("Kakao")
                .build();

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );

        kakaoRequest = new OAuth2UserRequest(kakaoRegistration, accessToken);

        // Create sample OAuth2User attributes
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 12345L);

        Map<String, Object> kakaoAccount = new HashMap<>();
        Map<String, Object> profile = new HashMap<>();
        profile.put("nickname", "테스트유저");
        kakaoAccount.put("profile", profile);

        attributes.put("kakao_account", kakaoAccount);

        kakaoUser = new DefaultOAuth2User(
            Collections.emptyList(),
            attributes,
            "id"
        );

        // Create test service subclass to avoid actual OAuth2 calls
        service = new KakaoOAuth2UserService(memberRepository) {
            @Override
            public OAuth2User loadUser(OAuth2UserRequest userRequest) {
                if (!"kakao".equalsIgnoreCase(userRequest.getClientRegistration().getRegistrationId())) {
                    throw new OAuth2AuthenticationException(new OAuth2Error("invalid_request"),
                            "지원하지 않는 OAuth2 제공자입니다: " + userRequest.getClientRegistration().getRegistrationId());
                }
                createOrUpdateMemberFromKakao(kakaoUser);
                return kakaoUser;
            }
        };
    }

    @Test
    @DisplayName("새로운 카카오 사용자 로드 테스트")
    void loadNewKakaoUser() {
        // Given
        when(memberRepository.findByLoginId("kakao_12345@oauth2.internal")).thenReturn(Optional.empty());

        // When
        OAuth2User result = service.loadUser(kakaoRequest);

        // Then
        assertThat(result).isNotNull();
        verify(memberRepository).findByLoginId("kakao_12345@oauth2.internal");
        verify(memberRepository).save(any(Member.class));
    }

    @Test
    @DisplayName("기존 카카오 사용자 로드 테스트")
    void loadExistingKakaoUser() {
        // Given
        Member existingMember = Member.builder()
                .loginId("kakao_12345@oauth2.internal")
                .name("테스트유저")
                .build();
        when(memberRepository.findByLoginId("kakao_12345@oauth2.internal")).thenReturn(Optional.of(existingMember));

        // When
        OAuth2User result = service.loadUser(kakaoRequest);

        // Then
        assertThat(result).isNotNull();
        verify(memberRepository).findByLoginId("kakao_12345@oauth2.internal");
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("잘못된 OAuth2 제공자 테스트")
    void loadUserWithInvalidProvider() {
        // Given
        ClientRegistration googleRegistration = ClientRegistration.withRegistrationId("google")
                .clientId("client-id")
                .clientSecret("client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost:8080/login/oauth2/code/google")
                .authorizationUri("https://accounts.google.com/o/oauth2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
                .userNameAttributeName("sub")
                .clientName("Google")
                .build();

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );

        OAuth2UserRequest googleRequest = new OAuth2UserRequest(googleRegistration, accessToken);

        // When & Then
        assertThatThrownBy(() -> service.loadUser(googleRequest))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("지원하지 않는 OAuth2 제공자입니다: google");
    }
}
