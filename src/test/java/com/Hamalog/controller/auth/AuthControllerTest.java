package com.Hamalog.controller.auth;

import com.Hamalog.dto.auth.request.LoginRequest;
import com.Hamalog.dto.auth.request.SignupRequest;
import com.Hamalog.dto.auth.response.LoginResponse;
import com.Hamalog.service.auth.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(AuthController.class)
@DisplayName("Auth Controller Unit Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("회원가입 성공")
    void signup_Success() throws Exception {
        // Given
        SignupRequest signupRequest = new SignupRequest(
                "testuser",
                "password123",
                "테스트 사용자",
                "010-1234-5678",
                LocalDate.of(1990, 1, 1)
        );
        
        willDoNothing().given(authService).registerMember(any(SignupRequest.class));

        // When & Then
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("회원가입 성공"));

        verify(authService).registerMember(any(SignupRequest.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 유효성 검증 실패")
    void signup_ValidationFailed() throws Exception {
        // Given - invalid signup request with empty fields
        SignupRequest invalidRequest = new SignupRequest(
                "",  // empty loginId
                "",  // empty password
                "",  // empty name
                "",  // empty phoneNumber
                null // null birth
        );

        // When & Then
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("회원가입 실패 - 중복된 사용자 ID")
    void signup_DuplicateUser() throws Exception {
        // Given
        SignupRequest signupRequest = new SignupRequest(
                "existinguser",
                "password123",
                "테스트 사용자",
                "010-1234-5678",
                LocalDate.of(1990, 1, 1)
        );
        
        willThrow(new RuntimeException("이미 존재하는 사용자입니다"))
                .given(authService).registerMember(any(SignupRequest.class));

        // When & Then
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("로그인 성공")
    void login_Success() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest("testuser", "password123");
        LoginResponse loginResponse = new LoginResponse(
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
        );
        
        given(authService.authenticateAndGenerateToken("testuser", "password123"))
                .willReturn(loginResponse);

        // When & Then
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."));

        verify(authService).authenticateAndGenerateToken("testuser", "password123");
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 인증 정보")
    void login_InvalidCredentials() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest("testuser", "wrongpassword");
        
        willThrow(new RuntimeException("인증에 실패했습니다"))
                .given(authService).authenticateAndGenerateToken(anyString(), anyString());

        // When & Then
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("로그인 실패 - 유효성 검증 실패")
    void login_ValidationFailed() throws Exception {
        // Given - invalid login request with empty fields
        LoginRequest invalidRequest = new LoginRequest("", "");

        // When & Then
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("로그아웃 성공 - Bearer 토큰과 함께")
    void logout_SuccessWithToken() throws Exception {
        // Given
        String token = "valid-jwt-token";
        willDoNothing().given(authService).logoutUser(token);

        // When & Then
        mockMvc.perform(post("/auth/logout")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("로그아웃 성공 - 토큰이 무효화되었습니다"));

        verify(authService).logoutUser(token);
    }

    @Test
    @DisplayName("로그아웃 성공 - 토큰 없이")
    void logout_SuccessWithoutToken() throws Exception {
        // When & Then
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(content().string("로그아웃 성공"));
    }

    @Test
    @DisplayName("로그아웃 성공 - 잘못된 Authorization 헤더 형식")
    void logout_InvalidAuthorizationHeader() throws Exception {
        // When & Then
        mockMvc.perform(post("/auth/logout")
                .header("Authorization", "InvalidFormat token"))
                .andExpect(status().isOk())
                .andExpect(content().string("로그아웃 성공"));
    }
}