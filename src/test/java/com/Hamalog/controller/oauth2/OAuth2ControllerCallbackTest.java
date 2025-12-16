package com.Hamalog.controller.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.Hamalog.dto.auth.response.LoginResponse;
import com.Hamalog.service.auth.AuthService;
import com.Hamalog.service.oauth2.StatePersistenceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * OAuth2 카카오 로그인 콜백 처리 통합 테스트
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("OAuth2 카카오 로그인 콜백 테스트")
class OAuth2ControllerCallbackTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private StatePersistenceService statePersistenceService;

    private static final String TEST_CODE = "test-authorization-code";
    private static final String TEST_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U";
    private static final String TEST_STATE = "test-state-value";
    private static final String RN_APP_SCHEME = "hamalog-rn";

    @Test
    @DisplayName("카카오 OAuth2 콜백 - 성공: JWT 토큰으로 RN 앱으로 리다이렉트")
    void handleKakaoCallback_Success() throws Exception {
        // Given: AuthService가 정상적으로 JWT 토큰 반환
        LoginResponse loginResponse = new LoginResponse(TEST_TOKEN);
        when(statePersistenceService.validateAndConsumeState(TEST_STATE)).thenReturn(true);
        when(authService.processOAuth2Callback(anyString())).thenReturn(loginResponse);

        // When: 콜백 엔드포인트 호출
        MvcResult result = mockMvc.perform(get("/oauth2/auth/kakao/callback")
                .param("code", TEST_CODE)
                .param("state", TEST_STATE))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // Then: RN 앱 스킴으로 리다이렉트되는지 확인
        String redirectUrl = result.getResponse().getRedirectedUrl();
        assertThat(redirectUrl).isNotNull();
        assertThat(redirectUrl).startsWith(RN_APP_SCHEME + "://auth?token=");

        System.out.println("✅ 성공 - 리다이렉트 URL: " + redirectUrl);
    }

    @Test
    @DisplayName("카카오 OAuth2 콜백 - 실패: 토큰 교환 실패 시 에러와 함께 RN 앱으로 리다이렉트")
    void handleKakaoCallback_TokenExchangeFailed() throws Exception {
        // Given: AuthService가 예외 발생
        when(authService.processOAuth2Callback(anyString()))
                .thenThrow(new RuntimeException("Token exchange failed"));

        // When: 콜백 엔드포인트 호출
        MvcResult result = mockMvc.perform(get("/oauth2/auth/kakao/callback")
                .param("code", "invalid-code")
                .param("state", TEST_STATE))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // Then: RN 앱 스킴으로 에러 정보와 함께 리다이렉트
        String redirectUrl = result.getResponse().getRedirectedUrl();
        assertThat(redirectUrl).isNotNull();
        assertThat(redirectUrl).startsWith(RN_APP_SCHEME + "://auth?error=");

        System.out.println("✅ 실패 처리 - 리다이렉트 URL: " + redirectUrl);
    }

    @Test
    @DisplayName("카카오 OAuth2 콜백 - State 파라미터 없이 호출")
    void handleKakaoCallback_WithoutState() throws Exception {
        // Given: State가 없으므로 validation 실패

        // When: State 파라미터 없이 콜백 엔드포인트 호출
        MvcResult result = mockMvc.perform(get("/oauth2/auth/kakao/callback")
                .param("code", TEST_CODE))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // Then: CSRF 검증 실패로 에러 리다이렉트
        String redirectUrl = result.getResponse().getRedirectedUrl();
        assertThat(redirectUrl).isNotNull();
        assertThat(redirectUrl).startsWith(RN_APP_SCHEME + "://auth?error=");
    }
}
