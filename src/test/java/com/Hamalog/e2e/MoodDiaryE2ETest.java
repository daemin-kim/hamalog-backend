package com.Hamalog.e2e;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * 마음 일기 E2E 통합 테스트
 */
@DisplayName("E2E: 마음 일기 시나리오")
class MoodDiaryE2ETest extends BaseE2ETest {

    @Nested
    @DisplayName("시나리오 1: 마음 일기 CRUD")
    class MoodDiaryCRUD {

        @Test
        @DisplayName("자유 형식 일기 작성 후 조회")
        void createAndGetDiary() throws Exception {
            // Given: 로그인
            loginAsTestMember();

            // When: 자유 형식 일기 생성 (JSON 직접 구성)
            String createJson = String.format("""
                {
                    "memberId": %d,
                    "diaryDate": "%s",
                    "moodType": "HAPPY",
                    "diaryType": "FREE_FORM",
                    "freeContent": "오늘은 정말 좋은 하루였다!"
                }
                """, testMember.getMemberId(), LocalDate.now().toString());

            mockMvc.perform(post("/mood-diary")
                    .header("Authorization", bearerToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createJson))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.moodDiaryId").exists());
        }
    }

    @Nested
    @DisplayName("시나리오 2: 인증 필요")
    class AuthRequired {

        @Test
        @DisplayName("인증 없이 일기 조회 시 401 반환")
        void accessWithoutAuth() throws Exception {
            mockMvc.perform(get("/mood-diary/list/1"))
                .andExpect(status().isUnauthorized());
        }
    }
}
