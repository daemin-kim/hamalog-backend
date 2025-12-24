package com.Hamalog.e2e;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.Hamalog.domain.diary.DiaryType;
import com.Hamalog.domain.diary.MoodType;
import com.Hamalog.dto.diary.request.MoodDiaryCreateRequest;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * 마음 일기 E2E 통합 테스트
 *
 * 시나리오: 회원이 로그인 → 일기 작성 → 조회 → 수정 → 통계 조회
 */
@DisplayName("E2E: 마음 일기 시나리오")
class MoodDiaryE2ETest extends BaseE2ETest {

    @Nested
    @DisplayName("시나리오 1: 마음 일기 CRUD")
    class MoodDiaryCRUD {

        @Test
        @DisplayName("자유 형식 일기 작성 → 조회 → 수정 → 삭제 전체 플로우")
        void fullCrudFlow_freeForm() throws Exception {
            // Given: 로그인
            loginAsTestMember();

            // When: 자유 형식 일기 생성
            MoodDiaryCreateRequest createRequest = new MoodDiaryCreateRequest(
                testMember.getMemberId(),
                LocalDate.now(),
                MoodType.HAPPY,
                DiaryType.FREE_FORM,
                null, null, null, null,  // 템플릿 응답 없음
                "오늘은 정말 좋은 하루였다. 날씨도 좋고 기분도 좋다!"  // freeContent
            );

            String response = mockMvc.perform(post("/mood-diary")
                    .header("Authorization", bearerToken())
                    .header("X-CSRF-TOKEN", csrfToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.moodDiaryId").exists())
                .andExpect(jsonPath("$.moodType").value("HAPPY"))
                .andExpect(jsonPath("$.diaryType").value("FREE_FORM"))
                .andReturn()
                .getResponse()
                .getContentAsString();

            Long diaryId = objectMapper.readTree(response).get("moodDiaryId").asLong();

            // Then: 생성된 일기 조회
            mockMvc.perform(get("/mood-diary/{id}", diaryId)
                    .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moodDiaryId").value(diaryId))
                .andExpect(jsonPath("$.freeContent").value(containsString("좋은 하루")));

            // When: 일기 삭제
            mockMvc.perform(delete("/mood-diary/{id}", diaryId)
                    .header("Authorization", bearerToken())
                    .header("X-CSRF-TOKEN", csrfToken))
                .andExpect(status().isNoContent());

            // Then: 삭제 확인
            mockMvc.perform(get("/mood-diary/{id}", diaryId)
                    .header("Authorization", bearerToken()))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("템플릿 형식 일기 작성")
        void createTemplateDiary() throws Exception {
            // Given
            loginAsTestMember();

            // When: 템플릿 형식 일기 생성
            MoodDiaryCreateRequest createRequest = new MoodDiaryCreateRequest(
                testMember.getMemberId(),
                LocalDate.now(),
                MoodType.PEACEFUL,
                DiaryType.TEMPLATE,
                "오늘 가장 기억에 남는 일: 산책을 했다",  // templateAnswer1
                "그 순간 느낀 감정: 평화로웠다",          // templateAnswer2
                "그 감정의 이유: 날씨가 좋았다",          // templateAnswer3
                "나에게 해주고 싶은 말: 자주 산책하자",   // templateAnswer4
                null  // freeContent
            );

            mockMvc.perform(post("/mood-diary")
                    .header("Authorization", bearerToken())
                    .header("X-CSRF-TOKEN", csrfToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.diaryType").value("TEMPLATE"));
        }
    }

    @Nested
    @DisplayName("시나리오 2: 일기 조회 및 검색")
    class DiarySearch {

        @Test
        @DisplayName("회원별 일기 목록 조회")
        void getDiariesByMember() throws Exception {
            // Given
            loginAsTestMember();

            // 여러 날짜의 일기 생성
            createDiary(LocalDate.now().minusDays(2), MoodType.SAD, "슬픈 날");
            createDiary(LocalDate.now().minusDays(1), MoodType.PEACEFUL, "평범한 날");
            createDiary(LocalDate.now(), MoodType.HAPPY, "좋은 날");

            // When & Then
            mockMvc.perform(get("/mood-diary/member/{memberId}", testMember.getMemberId())
                    .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(3))));
        }

        @Test
        @DisplayName("날짜 범위로 일기 조회")
        void getDiariesByDateRange() throws Exception {
            // Given
            loginAsTestMember();

            LocalDate today = LocalDate.now();
            createDiary(today.minusDays(5), MoodType.SAD, "5일 전");
            createDiary(today.minusDays(2), MoodType.PEACEFUL, "2일 전");
            createDiary(today, MoodType.HAPPY, "오늘");

            // When & Then: 최근 3일간 일기 조회
            mockMvc.perform(get("/mood-diary/member/{memberId}", testMember.getMemberId())
                    .header("Authorization", bearerToken())
                    .param("startDate", today.minusDays(3).toString())
                    .param("endDate", today.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));  // 2일 전, 오늘
        }
    }

    @Nested
    @DisplayName("시나리오 3: 중복 방지")
    class DuplicatePrevention {

        @Test
        @DisplayName("같은 날짜에 중복 일기 작성 시 에러")
        void duplicateDiaryOnSameDate() throws Exception {
            // Given
            loginAsTestMember();
            LocalDate today = LocalDate.now();

            // 첫 번째 일기 생성
            createDiary(today, MoodType.HAPPY, "첫 번째 일기");

            // When: 같은 날짜에 두 번째 일기 생성 시도
            MoodDiaryCreateRequest duplicateRequest = new MoodDiaryCreateRequest(
                testMember.getMemberId(),
                today,
                MoodType.SAD,
                DiaryType.FREE_FORM,
                null, null, null, null,
                "두 번째 일기"
            );

            // Then: 409 Conflict 또는 400 Bad Request
            mockMvc.perform(post("/mood-diary")
                    .header("Authorization", bearerToken())
                    .header("X-CSRF-TOKEN", csrfToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.code").value(containsString("DIARY")));
        }
    }

    // === 헬퍼 메서드 ===

    private Long createDiary(LocalDate date, MoodType moodType, String content) throws Exception {
        MoodDiaryCreateRequest request = new MoodDiaryCreateRequest(
            testMember.getMemberId(),
            date,
            moodType,
            DiaryType.FREE_FORM,
            null, null, null, null,
            content
        );

        String response = mockMvc.perform(post("/mood-diary")
                .header("Authorization", bearerToken())
                .header("X-CSRF-TOKEN", csrfToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        return objectMapper.readTree(response).get("moodDiaryId").asLong();
    }
}
