package com.Hamalog.controller.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.Hamalog.dto.auth.request.LoginRequest;
import com.Hamalog.dto.auth.request.SignupRequest;
import com.Hamalog.dto.auth.response.LoginResponse;
import com.Hamalog.handler.ExceptionHandlerUtils;
import com.Hamalog.handler.GlobalExceptionHandler;
import com.Hamalog.logging.StructuredLogger;
import com.Hamalog.security.filter.TrustedProxyService;
import com.Hamalog.security.jwt.JwtTokenProvider;
import com.Hamalog.service.auth.AuthenticationService;
import com.Hamalog.service.auth.MemberDeletionService;
import com.Hamalog.service.auth.MemberRegistrationService;
import com.Hamalog.service.i18n.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController 테스트")
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MemberRegistrationService memberRegistrationService;

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private MemberDeletionService memberDeletionService;

    @Mock
    private MessageService messageService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TrustedProxyService trustedProxyService;

    @Mock
    private StructuredLogger structuredLogger;

    @Mock
    private ExceptionHandlerUtils handlerUtils;

    @InjectMocks
    private AuthController authController;

    private ObjectMapper objectMapper;

    private SignupRequest signupRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        jsonConverter.setObjectMapper(objectMapper);
        
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler(structuredLogger, handlerUtils))
                .setMessageConverters(stringConverter, jsonConverter)
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
        
        // MessageService 모킹 설정 (lenient를 사용하여 불필요한 stubbing 경고 방지)
        lenient().when(messageService.getMessage("auth.signup.success")).thenReturn("회원가입 성공");
        lenient().when(messageService.getMessage("auth.logout.success")).thenReturn("로그아웃 성공 - 토큰이 무효화되었습니다");
        lenient().when(messageService.getMessage("auth.logout.success.simple")).thenReturn("로그아웃 성공");
        lenient().when(messageService.getMessage("auth.authentication.required")).thenReturn("인증이 필요합니다");
        lenient().when(messageService.getMessage("auth.account.deletion.success")).thenReturn("회원 탈퇴가 완료되었습니다");
        
        signupRequest = new SignupRequest(
                "test@example.com",
                "password123",
                "테스트유저",
                "테스터",
                "01012345678",
                LocalDate.of(1990, 1, 1)
        );

        loginRequest = new LoginRequest("test@example.com", "password123");
    }

    @Test
    @DisplayName("회원가입 성공")
    void signup_Success() throws Exception {
        // Given
        doNothing().when(memberRegistrationService).registerMember(any(SignupRequest.class));

        // When & Then
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("회원가입 성공"));

        verify(memberRegistrationService).registerMember(any(SignupRequest.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 유효하지 않은 요청")
    void signup_InvalidRequest() throws Exception {
        // Given - 빈 이메일로 유효하지 않은 요청 생성
        SignupRequest invalidRequest = new SignupRequest(
                "", // 빈 이메일
                "password123",
                "테스트유저",
                "테스터",
                "01012345678",
                LocalDate.of(1990, 1, 1)
        );

        // When & Then
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(memberRegistrationService, never()).registerMember(any(SignupRequest.class));
    }

    @Test
    @DisplayName("로그인 성공")
    void login_Success() throws Exception {
        // Given
        LoginResponse loginResponse = new LoginResponse("jwt.token.here", "refresh-token", 900L);
        given(authenticationService.authenticateAndGenerateToken(anyString(), anyString()))
                .willReturn(loginResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("jwt.token.here"));

        verify(authenticationService).authenticateAndGenerateToken("test@example.com", "password123");
    }

    @Test
    @DisplayName("로그인 실패 - 유효하지 않은 요청")
    void login_InvalidRequest() throws Exception {
        // Given - 빈 패스워드로 유효하지 않은 요청 생성
        LoginRequest invalidRequest = new LoginRequest("test@example.com", "");

        // When & Then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).authenticateAndGenerateToken(anyString(), anyString());
    }

    @Test
    @DisplayName("로그아웃 성공 - JWT 토큰 포함")
    void logout_WithJwtToken_Success() throws Exception {
        // Given
        String token = "jwt.token.here";
        given(jwtTokenProvider.validateToken(token)).willReturn(true);
        doNothing().when(authenticationService).logoutUser(anyString());

        // When & Then
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("로그아웃 성공 - 토큰이 무효화되었습니다"));

        verify(jwtTokenProvider).validateToken(token);
        verify(authenticationService).logoutUser(token);
    }

    @Test
    @DisplayName("로그아웃 실패 - JWT 토큰 없음")
    void logout_WithoutJwtToken_Failure() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().is4xxClientError());

        verify(authenticationService, never()).logoutUser(anyString());
    }

    @Test
    @DisplayName("회원탈퇴 성공")
    void deleteAccount_Success() throws Exception {
        // Given
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.isAuthenticated()).willReturn(true);
        given(authentication.getName()).willReturn("test@example.com");
        
        SecurityContextHolder.setContext(securityContext);
        
        doNothing().when(memberDeletionService).deleteMember(anyString(), anyString());

        // When & Then
        mockMvc.perform(delete("/api/v1/auth/account")
                        .header("Authorization", "Bearer jwt.token.here"))
                .andExpect(status().isOk())
                .andExpect(content().string("회원 탈퇴가 완료되었습니다"));

        verify(memberDeletionService).deleteMember("test@example.com", "jwt.token.here");
    }

    @Test
    @DisplayName("회원탈퇴 실패 - 인증되지 않은 사용자")
    void deleteAccount_Unauthenticated() throws Exception {
        // Given
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        
        given(securityContext.getAuthentication()).willReturn(null);
        SecurityContextHolder.setContext(securityContext);

        // When & Then
        mockMvc.perform(delete("/api/v1/auth/account"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("인증이 필요합니다"));

        verify(memberDeletionService, never()).deleteMember(anyString(), anyString());
    }

    @Test
    @DisplayName("회원탈퇴 실패 - 익명 사용자")
    void deleteAccount_AnonymousUser() throws Exception {
        // Given
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.isAuthenticated()).willReturn(true);
        given(authentication.getName()).willReturn("anonymousUser");
        
        SecurityContextHolder.setContext(securityContext);

        // When & Then
        mockMvc.perform(delete("/api/v1/auth/account"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("인증이 필요합니다"));

        verify(memberDeletionService, never()).deleteMember(anyString(), anyString());
    }
}