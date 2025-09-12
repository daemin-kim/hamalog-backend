package com.Hamalog.security.oauth2;

import com.Hamalog.security.jwt.JwtTokenProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2 Authentication Success Handler Tests")
class OAuth2AuthenticationSuccessHandlerTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @Mock
    private OAuth2User oAuth2User;

    private OAuth2AuthenticationSuccessHandler handler;
    private final String validRedirectUri = "http://localhost:3000/oauth/kakao";
    private final String testToken = "test-jwt-token";

    @BeforeEach
    void setUp() {
        // Constructor will be created in individual tests to avoid unnecessary stubbing
    }

    @Test
    @DisplayName("유효한 redirect URI로 생성자 호출 시 성공")
    void constructor_ValidRedirectUri_Success() {
        // given & when & then
        assertDoesNotThrow(() -> 
            new OAuth2AuthenticationSuccessHandler(jwtTokenProvider, "http://localhost:3000/oauth/kakao")
        );
    }

    @Test
    @DisplayName("로컬호스트 IP로 생성자 호출 시 성공")
    void constructor_LocalhostIp_Success() {
        // given & when & then
        assertDoesNotThrow(() -> 
            new OAuth2AuthenticationSuccessHandler(jwtTokenProvider, "http://127.0.0.1:3000/oauth/kakao")
        );
    }

    @Test
    @DisplayName("운영 서버 IP로 생성자 호출 시 성공")
    void constructor_ProductionServerIp_Success() {
        // given & when & then
        assertDoesNotThrow(() -> 
            new OAuth2AuthenticationSuccessHandler(jwtTokenProvider, "http://112.72.248.195:3000/oauth/kakao")
        );
    }

    @Test
    @DisplayName("허용되지 않는 호스트로 생성자 호출 시 예외 발생")
    void constructor_InvalidHost_ThrowsException() {
        // given
        String invalidUri = "http://malicious-site.com/oauth/callback";

        // when & then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
            new OAuth2AuthenticationSuccessHandler(jwtTokenProvider, invalidUri)
        );
        
        assertTrue(exception.getMessage().contains("Invalid OAuth2 redirect URI configured"));
    }

    @Test
    @DisplayName("잘못된 형식의 URI로 생성자 호출 시 예외 발생")
    void constructor_MalformedUri_ThrowsException() {
        // given
        String malformedUri = "not-a-valid-url";

        // when & then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
            new OAuth2AuthenticationSuccessHandler(jwtTokenProvider, malformedUri)
        );
        
        assertTrue(exception.getMessage().contains("Invalid OAuth2 redirect URI configured"));
    }

    @Test
    @DisplayName("OAuth2User가 ID를 가지고 있을 때 인증 성공 처리")
    void onAuthenticationSuccess_OAuth2UserWithId_Success() throws IOException, ServletException {
        // given
        when(jwtTokenProvider.createToken(anyString())).thenReturn(testToken);
        handler = new OAuth2AuthenticationSuccessHandler(jwtTokenProvider, validRedirectUri);
        
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 12345L);
        
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttributes()).thenReturn(attributes);
        
        // when
        handler.onAuthenticationSuccess(request, response, authentication);

        // then
        verify(jwtTokenProvider).createToken("kakao_12345");
        verify(response).addCookie(argThat(cookie -> 
            cookie.getName().equals("auth_token") &&
            cookie.getValue().equals(testToken) &&
            cookie.isHttpOnly() &&
            cookie.getSecure() &&
            cookie.getPath().equals("/") &&
            cookie.getMaxAge() == 3600
        ));
        verify(response).setStatus(HttpServletResponse.SC_FOUND);
        verify(response).setHeader("Location", validRedirectUri);
    }

    @Test
    @DisplayName("OAuth2User가 ID를 가지지 않을 때 인증 성공 처리")
    void onAuthenticationSuccess_OAuth2UserWithoutId_Success() throws IOException, ServletException {
        // given
        when(jwtTokenProvider.createToken(anyString())).thenReturn(testToken);
        handler = new OAuth2AuthenticationSuccessHandler(jwtTokenProvider, validRedirectUri);
        
        Map<String, Object> attributes = new HashMap<>();
        String authName = "testuser";
        
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttributes()).thenReturn(attributes);
        when(authentication.getName()).thenReturn(authName);
        
        // when
        handler.onAuthenticationSuccess(request, response, authentication);

        // then
        verify(jwtTokenProvider).createToken(authName);
        verify(response).addCookie(any(Cookie.class));
        verify(response).setStatus(HttpServletResponse.SC_FOUND);
        verify(response).setHeader("Location", validRedirectUri);
    }

    @Test
    @DisplayName("OAuth2User가 null ID를 가질 때 인증 성공 처리")
    void onAuthenticationSuccess_OAuth2UserWithNullId_Success() throws IOException, ServletException {
        // given
        when(jwtTokenProvider.createToken(anyString())).thenReturn(testToken);
        handler = new OAuth2AuthenticationSuccessHandler(jwtTokenProvider, validRedirectUri);
        
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", null);
        String authName = "testuser";
        
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttributes()).thenReturn(attributes);
        when(authentication.getName()).thenReturn(authName);
        
        // when
        handler.onAuthenticationSuccess(request, response, authentication);

        // then
        verify(jwtTokenProvider).createToken(authName);
        verify(response).addCookie(any(Cookie.class));
        verify(response).setStatus(HttpServletResponse.SC_FOUND);
        verify(response).setHeader("Location", validRedirectUri);
    }

    @Test
    @DisplayName("OAuth2User가 아닌 Principal일 때 인증 성공 처리")
    void onAuthenticationSuccess_NonOAuth2UserPrincipal_Success() throws IOException, ServletException {
        // given
        when(jwtTokenProvider.createToken(anyString())).thenReturn(testToken);
        handler = new OAuth2AuthenticationSuccessHandler(jwtTokenProvider, validRedirectUri);
        
        String authName = "regularuser";
        Object nonOAuth2Principal = new Object();
        
        when(authentication.getPrincipal()).thenReturn(nonOAuth2Principal);
        when(authentication.getName()).thenReturn(authName);
        
        // when
        handler.onAuthenticationSuccess(request, response, authentication);

        // then
        verify(jwtTokenProvider).createToken(authName);
        verify(response).addCookie(any(Cookie.class));
        verify(response).setStatus(HttpServletResponse.SC_FOUND);
        verify(response).setHeader("Location", validRedirectUri);
    }

    @Test
    @DisplayName("Integer 타입의 Kakao ID 처리")
    void onAuthenticationSuccess_IntegerKakaoId_Success() throws IOException, ServletException {
        // given
        when(jwtTokenProvider.createToken(anyString())).thenReturn(testToken);
        handler = new OAuth2AuthenticationSuccessHandler(jwtTokenProvider, validRedirectUri);
        
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 98765);
        
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttributes()).thenReturn(attributes);
        
        // when
        handler.onAuthenticationSuccess(request, response, authentication);

        // then
        verify(jwtTokenProvider).createToken("kakao_98765");
        verify(response).addCookie(any(Cookie.class));
        verify(response).setStatus(HttpServletResponse.SC_FOUND);
        verify(response).setHeader("Location", validRedirectUri);
    }

    @Test
    @DisplayName("쿠키 속성이 올바르게 설정되는지 확인")
    void onAuthenticationSuccess_CookiePropertiesSetCorrectly() throws IOException, ServletException {
        // given
        when(jwtTokenProvider.createToken(anyString())).thenReturn(testToken);
        handler = new OAuth2AuthenticationSuccessHandler(jwtTokenProvider, validRedirectUri);
        
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttributes()).thenReturn(Map.of("id", 12345L));
        
        // when
        handler.onAuthenticationSuccess(request, response, authentication);

        // then
        verify(response).addCookie(argThat(cookie -> {
            assertEquals("auth_token", cookie.getName());
            assertEquals(testToken, cookie.getValue());
            assertTrue(cookie.isHttpOnly());
            assertTrue(cookie.getSecure());
            assertEquals("/", cookie.getPath());
            assertEquals(3600, cookie.getMaxAge());
            return true;
        }));
    }

    @Test
    @DisplayName("응답 상태와 헤더가 올바르게 설정되는지 확인")
    void onAuthenticationSuccess_ResponseStatusAndHeaderSetCorrectly() throws IOException, ServletException {
        // given
        when(jwtTokenProvider.createToken(anyString())).thenReturn(testToken);
        handler = new OAuth2AuthenticationSuccessHandler(jwtTokenProvider, validRedirectUri);
        
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttributes()).thenReturn(Map.of("id", 12345L));
        
        // when
        handler.onAuthenticationSuccess(request, response, authentication);

        // then
        verify(response).setStatus(HttpServletResponse.SC_FOUND);
        verify(response).setHeader("Location", validRedirectUri);
    }
}