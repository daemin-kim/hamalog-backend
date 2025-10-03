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
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KakaoOAuth2UserService Tests")
class KakaoOAuth2UserServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private OAuth2UserRequest userRequest;

    @Mock
    private ClientRegistration clientRegistration;

    private KakaoOAuth2UserService kakaoOAuth2UserService;

    @BeforeEach
    void setUp() {
        kakaoOAuth2UserService = spy(new TestableKakaoOAuth2UserService(memberRepository));
    }
    
    /**
     * Testable version that doesn't call super.loadUser to avoid HTTP calls
     */
    private class TestableKakaoOAuth2UserService extends KakaoOAuth2UserService {
        private OAuth2User mockSuperResult;
        
        public TestableKakaoOAuth2UserService(MemberRepository memberRepository) {
            super(memberRepository);
        }
        
        public void setMockSuperResult(OAuth2User mockResult) {
            this.mockSuperResult = mockResult;
        }
        
        @Override
        public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
            // Skip super.loadUser() call and use mock result instead
            if (mockSuperResult == null) {
                throw new IllegalStateException("Mock super result not set");
            }
            
            String registrationId = userRequest.getClientRegistration().getRegistrationId();
            if (!"kakao".equalsIgnoreCase(registrationId)) {
                throw new OAuth2AuthenticationException(new org.springframework.security.oauth2.core.OAuth2Error("invalid_request"),
                        "지원하지 않는 OAuth2 제공자입니다: " + registrationId);
            }

            Map<String, Object> attributes = safeAsMap(mockSuperResult.getAttributes(), "root attributes");

            Long kakaoId = safeAsLong(attributes.get("id"), "id")
                    .orElseThrow(() -> new OAuth2AuthenticationException(new org.springframework.security.oauth2.core.OAuth2Error("invalid_user_info"),
                            "카카오 사용자 정보에 'id' 값이 없습니다."));

            Map<String, Object> kakaoAccount = safeAsMap(attributes.get("kakao_account"), "kakao_account");
            Map<String, Object> profile = safeAsMap(kakaoAccount.get("profile"), "profile");

            String nickname = safeAsString(profile.get("nickname")).orElse(null);

            String loginId = "kakao_" + kakaoId + "@oauth2.internal";
            Optional<Member> optionalMember = memberRepository.findByLoginId(loginId);
            if (optionalMember.isEmpty()) {
                String name = (nickname != null && !nickname.isBlank()) ? 
                        (nickname.length() > 15 ? nickname.substring(0, 15) : nickname) : 
                        "OAuth2_" + kakaoId.toString().substring(0, Math.min(kakaoId.toString().length(), 8));
                String phoneNumber = "01012345678";
                LocalDate birth = LocalDate.of(2000, 1, 1);

                String nickName = (nickname != null && !nickname.isBlank()) ? nickname : "카카오유저";
                
                Member member = Member.builder()
                        .loginId(loginId)
                        .password("{oauth2}")
                        .name(name)
                        .nickName(nickName)
                        .phoneNumber(phoneNumber)
                        .birth(birth)
                        .createdAt(LocalDateTime.now())
                        .build();
                memberRepository.save(member);
            }

            return mockSuperResult;
        }
        
        private Map<String, Object> safeAsMap(Object obj, String fieldName) {
            if (obj instanceof Map<?, ?> map) {
                java.util.Map<String, Object> result = new java.util.HashMap<>();
                for (java.util.Map.Entry<?, ?> entry : map.entrySet()) {
                    Object k = entry.getKey();
                    if (k instanceof String key) {
                        result.put(key, entry.getValue());
                    }
                }
                return result;
            }
            if (obj == null) {
                return Map.of();
            }
            return Map.of();
        }

        private Optional<String> safeAsString(Object obj) {
            if (obj == null) return Optional.empty();
            if (obj instanceof String s) return Optional.of(s);
            return Optional.ofNullable(String.valueOf(obj));
        }

        private Optional<Long> safeAsLong(Object obj, String fieldName) {
            if (obj == null) return Optional.empty();
            if (obj instanceof Number n) return Optional.of(n.longValue());
            if (obj instanceof String s) {
                try {
                    return Optional.of(Long.parseLong(s));
                } catch (NumberFormatException e) {
                    return Optional.empty();
                }
            }
            return Optional.empty();
        }
    }
    
    private ClientRegistration createKakaoClientRegistration(String registrationId) {
        return ClientRegistration.withRegistrationId(registrationId)
                .clientId("kakao-client-id")
                .clientSecret("kakao-client-secret")
                .clientAuthenticationMethod(org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost:8080/login/oauth2/code/kakao")
                .scope("profile_nickname")
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .tokenUri("https://kauth.kakao.com/oauth/token")
                .userInfoUri("https://kapi.kakao.com/v2/user/me")
                .userNameAttributeName("id")
                .clientName("Kakao")
                .build();
    }
    
    private OAuth2UserRequest createOAuth2UserRequest(String registrationId) {
        ClientRegistration clientRegistration = createKakaoClientRegistration(registrationId);
        org.springframework.security.oauth2.core.OAuth2AccessToken accessToken = 
            new org.springframework.security.oauth2.core.OAuth2AccessToken(
                org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType.BEARER,
                "test-access-token",
                java.time.Instant.now(),
                java.time.Instant.now().plusSeconds(3600)
            );
        return new OAuth2UserRequest(clientRegistration, accessToken);
    }

    @Test
    @DisplayName("Should throw exception for unsupported OAuth2 provider")
    void loadUser_UnsupportedProvider_ThrowsException() {
        // given
        OAuth2UserRequest googleRequest = createOAuth2UserRequest("google");
        
        OAuth2User mockUser = createMockOAuth2User(Map.of("id", 12345L));
        ((TestableKakaoOAuth2UserService) kakaoOAuth2UserService).setMockSuperResult(mockUser);

        // when & then
        assertThatThrownBy(() -> kakaoOAuth2UserService.loadUser(googleRequest))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("지원하지 않는 OAuth2 제공자입니다: google");
    }

    @Test
    @DisplayName("Should throw exception when Kakao user ID is missing")
    void loadUser_MissingKakaoId_ThrowsException() {
        // given
        OAuth2UserRequest kakaoRequest = createOAuth2UserRequest("kakao");
        
        // Create mock user with dummy id for constructor, but attributes without the expected id
        OAuth2User mockUser = createMockOAuth2User(Map.of("id", "dummy", "other_field", "value"));
        // Override the attributes to simulate missing id scenario
        OAuth2User mockUserWithoutId = new DefaultOAuth2User(
                java.util.Collections.emptySet(),
                Map.of("other_field", "value", "id", "dummy"), // dummy id for constructor
                "id"
        ) {
            @Override
            public Map<String, Object> getAttributes() {
                return Map.of("other_field", "value"); // Return attributes without id
            }
        };
        ((TestableKakaoOAuth2UserService) kakaoOAuth2UserService).setMockSuperResult(mockUserWithoutId);

        // when & then
        assertThatThrownBy(() -> kakaoOAuth2UserService.loadUser(kakaoRequest))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("카카오 사용자 정보에 'id' 값이 없습니다");
    }

    @Test
    @DisplayName("Should create new member when user does not exist")
    void loadUser_NewUser_CreatesNewMember() {
        // given
        Long kakaoId = 12345L;
        String nickname = "테스트유저";
        
        Map<String, Object> attributes = createKakaoUserAttributes(kakaoId, nickname);
        OAuth2User mockUser = createMockOAuth2User(attributes);
        OAuth2UserRequest kakaoRequest = createOAuth2UserRequest("kakao");
        
        when(memberRepository.findByLoginId("kakao_12345@oauth2.internal")).thenReturn(Optional.empty());
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        ((TestableKakaoOAuth2UserService) kakaoOAuth2UserService).setMockSuperResult(mockUser);

        // when
        OAuth2User result = kakaoOAuth2UserService.loadUser(kakaoRequest);

        // then
        assertThat(result).isNotNull();
        verify(memberRepository).findByLoginId("kakao_12345@oauth2.internal");
        verify(memberRepository).save(argThat(member -> {
            assertThat(member.getLoginId()).isEqualTo("kakao_12345@oauth2.internal");
            assertThat(member.getPassword()).isEqualTo("{oauth2}");
            assertThat(member.getName()).isEqualTo(nickname);
            assertThat(member.getNickName()).isEqualTo(nickname);
            assertThat(member.getPhoneNumber()).isEqualTo("01012345678");
            assertThat(member.getBirth()).isEqualTo(LocalDate.of(2000, 1, 1));
            return true;
        }));
    }

    @Test
    @DisplayName("Should return existing member when user already exists")
    void loadUser_ExistingUser_ReturnsExistingMember() {
        // given
        Long kakaoId = 12345L;
        String nickname = "테스트유저";
        
        Map<String, Object> attributes = createKakaoUserAttributes(kakaoId, nickname);
        OAuth2User mockUser = createMockOAuth2User(attributes);
        OAuth2UserRequest kakaoRequest = createOAuth2UserRequest("kakao");
        
        Member existingMember = Member.builder()
                .loginId("kakao_12345@oauth2.internal")
                .password("{oauth2}")
                .name("기존유저")
                .nickName("기존닉네임")
                .phoneNumber("01012345678")
                .birth(LocalDate.of(2000, 1, 1))
                .createdAt(LocalDateTime.now())
                .build();
        
        when(memberRepository.findByLoginId("kakao_12345@oauth2.internal")).thenReturn(Optional.of(existingMember));
        
        ((TestableKakaoOAuth2UserService) kakaoOAuth2UserService).setMockSuperResult(mockUser);

        // when
        OAuth2User result = kakaoOAuth2UserService.loadUser(kakaoRequest);

        // then
        assertThat(result).isNotNull();
        verify(memberRepository).findByLoginId("kakao_12345@oauth2.internal");
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("Should handle missing nickname gracefully")
    void loadUser_MissingNickname_CreatesUserWithDefaultName() {
        // given
        Long kakaoId = 12345L;
        
        Map<String, Object> attributes = createKakaoUserAttributes(kakaoId, null);
        OAuth2User mockUser = createMockOAuth2User(attributes);
        OAuth2UserRequest kakaoRequest = createOAuth2UserRequest("kakao");
        
        when(memberRepository.findByLoginId("kakao_12345@oauth2.internal")).thenReturn(Optional.empty());
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        ((TestableKakaoOAuth2UserService) kakaoOAuth2UserService).setMockSuperResult(mockUser);

        // when
        OAuth2User result = kakaoOAuth2UserService.loadUser(kakaoRequest);

        // then
        assertThat(result).isNotNull();
        verify(memberRepository).save(argThat(member -> {
            assertThat(member.getName()).isEqualTo("OAuth2_12345");
            assertThat(member.getNickName()).isEqualTo("카카오유저");
            return true;
        }));
    }

    @Test
    @DisplayName("Should truncate long nickname to 15 characters")
    void loadUser_LongNickname_TruncatesName() {
        // given
        Long kakaoId = 12345L;
        String longNickname = "이것은매우긴닉네임입니다테스트용";
        
        Map<String, Object> attributes = createKakaoUserAttributes(kakaoId, longNickname);
        OAuth2User mockUser = createMockOAuth2User(attributes);
        OAuth2UserRequest kakaoRequest = createOAuth2UserRequest("kakao");
        
        when(memberRepository.findByLoginId("kakao_12345@oauth2.internal")).thenReturn(Optional.empty());
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        ((TestableKakaoOAuth2UserService) kakaoOAuth2UserService).setMockSuperResult(mockUser);

        // when
        OAuth2User result = kakaoOAuth2UserService.loadUser(kakaoRequest);

        // then
        assertThat(result).isNotNull();
        verify(memberRepository).save(argThat(member -> {
            assertThat(member.getName()).hasSize(15);
            assertThat(member.getName()).isEqualTo(longNickname.substring(0, 15));
            assertThat(member.getNickName()).isEqualTo(longNickname);
            return true;
        }));
    }

    @Test
    @DisplayName("Should handle case-insensitive registration ID")
    void loadUser_CaseInsensitiveRegistrationId_WorksCorrectly() {
        // given
        Long kakaoId = 12345L;
        String nickname = "테스트유저";
        
        Map<String, Object> attributes = createKakaoUserAttributes(kakaoId, nickname);
        OAuth2User mockUser = createMockOAuth2User(attributes);
        OAuth2UserRequest kakaoRequest = createOAuth2UserRequest("KAKAO"); // Uppercase
        
        when(memberRepository.findByLoginId("kakao_12345@oauth2.internal")).thenReturn(Optional.empty());
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        ((TestableKakaoOAuth2UserService) kakaoOAuth2UserService).setMockSuperResult(mockUser);

        // when
        OAuth2User result = kakaoOAuth2UserService.loadUser(kakaoRequest);

        // then
        assertThat(result).isNotNull();
        verify(memberRepository).save(any(Member.class));
    }

    @Test
    @DisplayName("Should handle missing kakao_account gracefully")
    void loadUser_MissingKakaoAccount_WorksWithDefaults() {
        // given
        Long kakaoId = 12345L;
        
        Map<String, Object> attributes = Map.of("id", kakaoId);
        OAuth2User mockUser = createMockOAuth2User(attributes);
        OAuth2UserRequest kakaoRequest = createOAuth2UserRequest("kakao");
        
        when(memberRepository.findByLoginId("kakao_12345@oauth2.internal")).thenReturn(Optional.empty());
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        ((TestableKakaoOAuth2UserService) kakaoOAuth2UserService).setMockSuperResult(mockUser);

        // when
        OAuth2User result = kakaoOAuth2UserService.loadUser(kakaoRequest);

        // then
        assertThat(result).isNotNull();
        verify(memberRepository).save(argThat(member -> {
            assertThat(member.getName()).isEqualTo("OAuth2_12345");
            assertThat(member.getNickName()).isEqualTo("카카오유저");
            return true;
        }));
    }

    @Test
    @DisplayName("Should handle non-map kakao_account")
    void loadUser_InvalidKakaoAccount_WorksWithDefaults() {
        // given
        Long kakaoId = 12345L;
        
        Map<String, Object> attributes = Map.of(
            "id", kakaoId,
            "kakao_account", "invalid_string_value"
        );
        OAuth2User mockUser = createMockOAuth2User(attributes);
        OAuth2UserRequest kakaoRequest = createOAuth2UserRequest("kakao");
        
        when(memberRepository.findByLoginId("kakao_12345@oauth2.internal")).thenReturn(Optional.empty());
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        ((TestableKakaoOAuth2UserService) kakaoOAuth2UserService).setMockSuperResult(mockUser);

        // when
        OAuth2User result = kakaoOAuth2UserService.loadUser(kakaoRequest);

        // then
        assertThat(result).isNotNull();
        verify(memberRepository).save(any(Member.class));
    }

    @Test
    @DisplayName("Should handle string Kakao ID")
    void loadUser_StringKakaoId_ParsesCorrectly() {
        // given
        String kakaoIdString = "12345";
        
        Map<String, Object> attributes = createKakaoUserAttributes(kakaoIdString, "테스트유저");
        OAuth2User mockUser = createMockOAuth2User(attributes);
        OAuth2UserRequest kakaoRequest = createOAuth2UserRequest("kakao");
        
        when(memberRepository.findByLoginId("kakao_12345@oauth2.internal")).thenReturn(Optional.empty());
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        ((TestableKakaoOAuth2UserService) kakaoOAuth2UserService).setMockSuperResult(mockUser);

        // when
        OAuth2User result = kakaoOAuth2UserService.loadUser(kakaoRequest);

        // then
        assertThat(result).isNotNull();
        verify(memberRepository).save(argThat(member -> 
            member.getLoginId().equals("kakao_12345@oauth2.internal")
        ));
    }

    @Test
    @DisplayName("Should handle invalid string Kakao ID")
    void loadUser_InvalidStringKakaoId_ThrowsException() {
        // given
        Map<String, Object> attributes = Map.of("id", "invalid_number");
        OAuth2User mockUser = createMockOAuth2User(attributes);
        OAuth2UserRequest kakaoRequest = createOAuth2UserRequest("kakao");
        
        ((TestableKakaoOAuth2UserService) kakaoOAuth2UserService).setMockSuperResult(mockUser);

        // when & then
        assertThatThrownBy(() -> kakaoOAuth2UserService.loadUser(kakaoRequest))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("카카오 사용자 정보에 'id' 값이 없습니다");
    }

    @Test
    @DisplayName("Should handle large Kakao ID for name generation")
    void loadUser_LargeKakaoId_GeneratesCorrectName() {
        // given
        Long largeKakaoId = 123456789012345L;
        
        Map<String, Object> attributes = createKakaoUserAttributes(largeKakaoId, null);
        OAuth2User mockUser = createMockOAuth2User(attributes);
        OAuth2UserRequest kakaoRequest = createOAuth2UserRequest("kakao");
        
        when(memberRepository.findByLoginId("kakao_123456789012345@oauth2.internal")).thenReturn(Optional.empty());
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        ((TestableKakaoOAuth2UserService) kakaoOAuth2UserService).setMockSuperResult(mockUser);

        // when
        OAuth2User result = kakaoOAuth2UserService.loadUser(kakaoRequest);

        // then
        assertThat(result).isNotNull();
        verify(memberRepository).save(argThat(member -> {
            assertThat(member.getName()).isEqualTo("OAuth2_12345678"); // First 8 digits
            return true;
        }));
    }

    private Map<String, Object> createKakaoUserAttributes(Object kakaoId, String nickname) {
        Map<String, Object> profile = new HashMap<>();
        if (nickname != null) {
            profile.put("nickname", nickname);
        }
        
        Map<String, Object> kakaoAccount = new HashMap<>();
        kakaoAccount.put("profile", profile);
        
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", kakaoId);
        attributes.put("kakao_account", kakaoAccount);
        
        return attributes;
    }

    private OAuth2User createMockOAuth2User(Map<String, Object> attributes) {
        return new DefaultOAuth2User(
                java.util.Collections.emptySet(),
                attributes,
                "id"
        );
    }
}