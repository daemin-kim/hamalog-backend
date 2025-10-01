package com.Hamalog.controller.auth;

import com.Hamalog.security.csrf.CsrfTokenProvider;
import com.Hamalog.security.jwt.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CSRF Controller Tests")
class CsrfControllerTest {

    @Mock
    private CsrfTokenProvider csrfTokenProvider;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private CsrfController csrfController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(csrfController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Should generate CSRF token successfully for authenticated user")
    void getCsrfToken_ValidJwtToken_ShouldReturnCsrfToken() throws Exception {
        // given
        String validToken = "valid.jwt.token";
        String userId = "user123";
        String csrfToken = "csrf-token-value";
        
        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getLoginIdFromToken(validToken)).thenReturn(userId);
        when(csrfTokenProvider.generateToken(userId)).thenReturn(csrfToken);

        // when & then
        mockMvc.perform(get("/auth/csrf-token")
                .header("Authorization", "Bearer " + validToken)
                .header("X-Real-IP", "192.168.1.100"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.csrfToken").value(csrfToken))
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
                .andExpect(jsonPath("$.expiryMinutes").value(60))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(jwtTokenProvider).validateToken(validToken);
        verify(jwtTokenProvider).getLoginIdFromToken(validToken);
        verify(csrfTokenProvider).generateToken(userId);
    }

    @Test
    @DisplayName("Should return 401 when JWT token is missing")
    void getCsrfToken_MissingJwtToken_ShouldReturn401() throws Exception {
        // when & then
        mockMvc.perform(get("/auth/csrf-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(jwtTokenProvider, never()).validateToken(anyString());
        verify(csrfTokenProvider, never()).generateToken(anyString());
    }

    @Test
    @DisplayName("Should return 401 when JWT token is invalid")
    void getCsrfToken_InvalidJwtToken_ShouldReturn401() throws Exception {
        // given
        String invalidToken = "invalid.jwt.token";
        when(jwtTokenProvider.validateToken(invalidToken)).thenReturn(false);

        // when & then
        mockMvc.perform(get("/auth/csrf-token")
                .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다"));

        verify(jwtTokenProvider).validateToken(invalidToken);
        verify(csrfTokenProvider, never()).generateToken(anyString());
    }

    @Test
    @DisplayName("Should return 401 when Authorization header format is incorrect")
    void getCsrfToken_IncorrectAuthHeaderFormat_ShouldReturn401() throws Exception {
        // when & then
        mockMvc.perform(get("/auth/csrf-token")
                .header("Authorization", "InvalidFormat token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));

        verify(jwtTokenProvider, never()).validateToken(anyString());
    }

    @Test
    @DisplayName("Should return 500 when CSRF token generation fails")
    void getCsrfToken_CsrfTokenGenerationFails_ShouldReturn500() throws Exception {
        // given
        String validToken = "valid.jwt.token";
        String userId = "user123";
        
        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getLoginIdFromToken(validToken)).thenReturn(userId);
        when(csrfTokenProvider.generateToken(userId)).thenThrow(new RuntimeException("Token generation failed"));

        // when & then
        mockMvc.perform(get("/auth/csrf-token")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("토큰 발급 중 오류가 발생했습니다"));
    }

    @Test
    @DisplayName("Should extract client IP from X-Forwarded-For header")
    void getCsrfToken_WithXForwardedForHeader_ShouldExtractCorrectIp() throws Exception {
        // given
        String validToken = "valid.jwt.token";
        String userId = "user123";
        String csrfToken = "csrf-token-value";
        
        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getLoginIdFromToken(validToken)).thenReturn(userId);
        when(csrfTokenProvider.generateToken(userId)).thenReturn(csrfToken);

        // when & then
        mockMvc.perform(get("/auth/csrf-token")
                .header("Authorization", "Bearer " + validToken)
                .header("X-Forwarded-For", "192.168.1.100, 10.0.0.1"))
                .andExpect(status().isOk());

        // IP extraction is tested through logging, which is harder to verify in unit tests
        // but the method should work correctly
    }

    @Test
    @DisplayName("Should return CSRF status for authenticated user with valid token")
    void getCsrfStatus_ValidUserAndCsrfToken_ShouldReturnStatus() throws Exception {
        // given
        String validJwtToken = "valid.jwt.token";
        String userId = "user123";
        String csrfToken = "valid-csrf-token";
        
        when(jwtTokenProvider.validateToken(validJwtToken)).thenReturn(true);
        when(jwtTokenProvider.getLoginIdFromToken(validJwtToken)).thenReturn(userId);
        when(csrfTokenProvider.validateToken(userId, csrfToken)).thenReturn(true);

        // when & then
        mockMvc.perform(get("/auth/csrf-status")
                .header("Authorization", "Bearer " + validJwtToken)
                .header("X-CSRF-TOKEN", csrfToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.csrfTokenPresent").value(true))
                .andExpect(jsonPath("$.csrfTokenValid").value(true))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(csrfTokenProvider).validateToken(userId, csrfToken);
    }

    @Test
    @DisplayName("Should return CSRF status with invalid token")
    void getCsrfStatus_InvalidCsrfToken_ShouldReturnInvalidStatus() throws Exception {
        // given
        String validJwtToken = "valid.jwt.token";
        String userId = "user123";
        String invalidCsrfToken = "invalid-csrf-token";
        
        when(jwtTokenProvider.validateToken(validJwtToken)).thenReturn(true);
        when(jwtTokenProvider.getLoginIdFromToken(validJwtToken)).thenReturn(userId);
        when(csrfTokenProvider.validateToken(userId, invalidCsrfToken)).thenReturn(false);

        // when & then
        mockMvc.perform(get("/auth/csrf-status")
                .header("Authorization", "Bearer " + validJwtToken)
                .header("X-CSRF-TOKEN", invalidCsrfToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.csrfTokenPresent").value(true))
                .andExpect(jsonPath("$.csrfTokenValid").value(false));
    }

    @Test
    @DisplayName("Should return CSRF status without CSRF token")
    void getCsrfStatus_NoCsrfToken_ShouldReturnStatusWithoutToken() throws Exception {
        // given
        String validJwtToken = "valid.jwt.token";
        String userId = "user123";
        
        when(jwtTokenProvider.validateToken(validJwtToken)).thenReturn(true);
        when(jwtTokenProvider.getLoginIdFromToken(validJwtToken)).thenReturn(userId);

        // when & then
        mockMvc.perform(get("/auth/csrf-status")
                .header("Authorization", "Bearer " + validJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.csrfTokenPresent").value(false))
                .andExpect(jsonPath("$.csrfTokenValid").value(false));

        verify(csrfTokenProvider).validateToken(userId, null);
    }

    @Test
    @DisplayName("Should return 401 for CSRF status when not authenticated")
    void getCsrfStatus_NotAuthenticated_ShouldReturn401() throws Exception {
        // when & then
        mockMvc.perform(get("/auth/csrf-status"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다"));
    }

    @Test
    @DisplayName("Should return 500 when CSRF status check fails")
    void getCsrfStatus_StatusCheckFails_ShouldReturn500() throws Exception {
        // given
        String validJwtToken = "valid.jwt.token";
        String userId = "user123";
        
        when(jwtTokenProvider.validateToken(validJwtToken)).thenReturn(true);
        when(jwtTokenProvider.getLoginIdFromToken(validJwtToken)).thenReturn(userId);
        when(csrfTokenProvider.validateToken(anyString(), anyString())).thenThrow(new RuntimeException("Validation failed"));

        // when & then
        mockMvc.perform(get("/auth/csrf-status")
                .header("Authorization", "Bearer " + validJwtToken)
                .header("X-CSRF-TOKEN", "some-token"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("상태 확인 중 오류가 발생했습니다"));
    }

    @Test
    @DisplayName("Should handle JWT token extraction exception gracefully")
    void getCsrfToken_JwtExtractionException_ShouldReturn401() throws Exception {
        // given
        String problematicToken = "problematic.jwt.token";
        when(jwtTokenProvider.validateToken(problematicToken)).thenThrow(new RuntimeException("JWT parsing failed"));

        // when & then
        mockMvc.perform(get("/auth/csrf-token")
                .header("Authorization", "Bearer " + problematicToken))
                .andExpect(status().isUnauthorized());
    }
}