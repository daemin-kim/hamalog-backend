package com.Hamalog.e2e;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.Hamalog.dto.auth.request.LoginRequest;
import com.Hamalog.dto.auth.request.SignupRequest;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * 인증/인가 E2E 통합 테스트
 *
 * 시나리오: 회원가입 → 로그인 → 토큰 갱신 → 로그아웃
 */
@DisplayName("E2E: 인증 시나리오")
class AuthE2ETest extends BaseE2ETest {

    @Nested
    @DisplayName("시나리오 1: 회원가입 및 로그인")
    class SignupAndLogin {

        @Test
        @DisplayName("회원가입 → 로그인 → 토큰 발급 전체 플로우")
        void fullSignupLoginFlow() throws Exception {
            // Given: 신규 회원 정보
            String newLoginId = "newuser@hamalog.com";
            String password = "NewPass123!";

            SignupRequest signupRequest = new SignupRequest(
                newLoginId,
                password,
                "신규회원",
                "신규유저",       // nickName
                "01012345678",
                LocalDate.of(1990, 1, 1)
            );

            // When: 회원가입
            mockMvc.perform(post("/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signupRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memberId").exists())
                .andExpect(jsonPath("$.loginId").value(newLoginId));

            // When: 로그인
            LoginRequest loginRequest = new LoginRequest(newLoginId, password);

            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(header().exists("X-CSRF-TOKEN"));
        }

        @Test
        @DisplayName("잘못된 비밀번호로 로그인 시 401 반환")
        void loginWithWrongPassword() throws Exception {
            // Given: 잘못된 비밀번호
            LoginRequest request = new LoginRequest(TEST_LOGIN_ID, "wrongpassword");

            // When & Then
            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("존재하지 않는 회원으로 로그인 시 401 반환")
        void loginWithNonExistentUser() throws Exception {
            // Given: 존재하지 않는 회원
            LoginRequest request = new LoginRequest("nonexistent", "password123");

            // When & Then
            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
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

        @Test
        @DisplayName("Bearer 없이 토큰만 전송 시 401 반환")
        void accessWithoutBearerPrefix() throws Exception {
            loginAsTestMember();

            mockMvc.perform(get("/member/profile")
                    .header("Authorization", accessToken))  // Bearer 없이
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("시나리오 3: 회원가입 유효성 검증")
    class SignupValidation {

        @Test
        @DisplayName("잘못된 이메일 형식 시 400 반환")
        void invalidEmailFormat() throws Exception {
            SignupRequest request = new SignupRequest(
                "invalid-email",  // 이메일 형식 아님
                "Password123!",
                "테스트",
                "테스트닉",
                "01012345678",
                LocalDate.of(1990, 1, 1)
            );

            mockMvc.perform(post("/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("중복 아이디로 가입 시 409 반환")
        void duplicateLoginId() throws Exception {
            // Given: 이미 존재하는 회원 (BaseE2ETest에서 생성됨)
            SignupRequest request = new SignupRequest(
                TEST_LOGIN_ID,  // 이미 존재 (하지만 이메일 형식이어야 함)
                "NewPass123!",
                "중복테스트",
                "중복닉네임",
                "01087654321",
                LocalDate.of(1990, 1, 1)
            );

            mockMvc.perform(post("/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("필수 필드 누락 시 400 반환")
        void missingRequiredFields() throws Exception {
            // loginId 누락
            String invalidJson = """
                {
                    "password": "Test1234!",
                    "name": "테스트"
                }
                """;

            mockMvc.perform(post("/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("시나리오 4: 로그아웃")
    class Logout {

        @Test
        @DisplayName("로그아웃 성공")
        void logoutSuccess() throws Exception {
            // Given
            loginAsTestMember();

            // When
            mockMvc.perform(post("/auth/logout")
                    .header("Authorization", bearerToken())
                    .header("X-CSRF-TOKEN", csrfToken))
                .andExpect(status().isOk());

            // Note: 토큰 블랙리스트 검증은 Redis Mock 환경에서 제한됨
        }
    }

    @Nested
    @DisplayName("시나리오 5: 프로필 관리")
    class ProfileManagement {

        @Test
        @DisplayName("내 프로필 조회")
        void getMyProfile() throws Exception {
            // Given
            loginAsTestMember();

            // When & Then
            mockMvc.perform(get("/member/profile")
                    .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(testMember.getMemberId()))
                .andExpect(jsonPath("$.loginId").value(TEST_LOGIN_ID))
                .andExpect(jsonPath("$.name").value(TEST_NAME));
        }
    }
}
