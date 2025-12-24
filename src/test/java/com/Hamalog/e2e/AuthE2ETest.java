package com.Hamalog.e2e;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * 인증/인가 E2E 통합 테스트
 */
@DisplayName("E2E: 인증 시나리오")
class AuthE2ETest extends BaseE2ETest {

    @Nested
    @DisplayName("시나리오 1: 로그인")
    class Login {

        @Test
        @DisplayName("올바른 자격 증명으로 로그인 성공")
        void loginSuccess() throws Exception {
            String loginJson = String.format("""
                {
                    "loginId": "%s",
                    "password": "%s"
                }
                """, testLoginId, TEST_PASSWORD);

            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists());
        }

        @Test
        @DisplayName("잘못된 비밀번호로 로그인 실패")
        void loginWithWrongPassword() throws Exception {
            String loginJson = String.format("""
                {
                    "loginId": "%s",
                    "password": "wrongpassword"
                }
                """, testLoginId);

            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("존재하지 않는 회원으로 로그인 실패")
        void loginWithNonExistentUser() throws Exception {
            String loginJson = """
                {
                    "loginId": "nonexistent@test.com",
                    "password": "password123"
                }
                """;

            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("시나리오 2: 토큰 관리")
    class TokenManagement {

        @Test
        @DisplayName("유효한 토큰으로 보호된 API 접근 성공")
        void accessProtectedApiWithValidToken() throws Exception {
            // Given
            loginAsTestMember();

            // When & Then
            mockMvc.perform(get("/member/profile")
                    .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(testMember.getMemberId()));
        }

        @Test
        @DisplayName("토큰 없이 보호된 API 접근 시 401 반환")
        void accessProtectedApiWithoutToken() throws Exception {
            mockMvc.perform(get("/member/profile"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("잘못된 형식의 토큰으로 접근 시 401 반환")
        void accessWithMalformedToken() throws Exception {
            mockMvc.perform(get("/member/profile")
                    .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
        }
    }
}
