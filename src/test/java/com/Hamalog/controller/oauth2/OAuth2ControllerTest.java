package com.Hamalog.controller.oauth2;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(OAuth2Controller.class)
@DisplayName("OAuth2 Controller Unit Tests")
class OAuth2ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @Test
    @DisplayName("카카오 OAuth2 인증 시작 성공")
    void kakaoAuth_Success() throws Exception {
        // Given
        ClientRegistration kakaoRegistration = ClientRegistration.withRegistrationId("kakao")
                .clientId("test-client-id")
                .clientSecret("test-client-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost:8080/login/oauth2/code/kakao")
                .scope(Set.of("profile_nickname"))
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .tokenUri("https://kauth.kakao.com/oauth/token")
                .userInfoUri("https://kapi.kakao.com/v2/user/me")
                .userNameAttributeName("id")
                .clientName("kakao")
                .build();
        
        given(clientRegistrationRepository.findByRegistrationId("kakao"))
                .willReturn(kakaoRegistration);

        // When & Then
        mockMvc.perform(get("/oauth2/auth/kakao"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", 
                    containsString("https://kauth.kakao.com/oauth/authorize")))
                .andExpect(header().string("Location", 
                    containsString("client_id=test-client-id")))
                .andExpect(header().string("Location", 
                    containsString("response_type=code")))
                .andExpect(header().string("Location", 
                    containsString("redirect_uri=http://localhost:8080/login/oauth2/code/kakao")));

        verify(clientRegistrationRepository).findByRegistrationId("kakao");
    }

    @Test
    @DisplayName("카카오 OAuth2 인증 실패 - 클라이언트 등록 없음")
    void kakaoAuth_ClientRegistrationNotFound() throws Exception {
        // Given
        given(clientRegistrationRepository.findByRegistrationId("kakao"))
                .willReturn(null);

        // When & Then
        mockMvc.perform(get("/oauth2/auth/kakao"))
                .andExpect(status().isInternalServerError());

        verify(clientRegistrationRepository).findByRegistrationId("kakao");
    }

    @Test
    @DisplayName("카카오 OAuth2 인증 실패 - 예외 발생")
    void kakaoAuth_ExceptionThrown() throws Exception {
        // Given
        given(clientRegistrationRepository.findByRegistrationId("kakao"))
                .willThrow(new RuntimeException("Database connection failed"));

        // When & Then
        mockMvc.perform(get("/oauth2/auth/kakao"))
                .andExpect(status().isInternalServerError());

        verify(clientRegistrationRepository).findByRegistrationId("kakao");
    }

    @Test
    @DisplayName("카카오 OAuth2 리다이렉트 URL 파라미터 검증")
    void kakaoAuth_RedirectUrlParameters() throws Exception {
        // Given
        ClientRegistration kakaoRegistration = ClientRegistration.withRegistrationId("kakao")
                .clientId("test-client-id")
                .clientSecret("test-client-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost:8080/login/oauth2/code/kakao")
                .scope(Set.of("profile_nickname", "account_email"))
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .tokenUri("https://kauth.kakao.com/oauth/token")
                .userInfoUri("https://kapi.kakao.com/v2/user/me")
                .userNameAttributeName("id")
                .clientName("kakao")
                .build();
        
        given(clientRegistrationRepository.findByRegistrationId("kakao"))
                .willReturn(kakaoRegistration);

        // When & Then
        mockMvc.perform(get("/oauth2/auth/kakao"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", 
                    containsString("scope=profile_nickname account_email")))
                .andExpect(header().string("Location", 
                    containsString("state=")));  // State parameter should be present

        verify(clientRegistrationRepository).findByRegistrationId("kakao");
    }
}