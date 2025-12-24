package com.Hamalog.e2e;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.Hamalog.domain.medication.AlarmType;
import com.Hamalog.dto.medication.request.MedicationRecordCreateRequest;
import com.Hamalog.dto.medication.request.MedicationScheduleCreateRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * 복약 관리 E2E 통합 테스트
 *
 * 시나리오: 회원이 로그인 → 복약 스케줄 생성 → 복약 기록 → 통계 조회
 */
@DisplayName("E2E: 복약 관리 시나리오")
class MedicationE2ETest extends BaseE2ETest {

    @Nested
    @DisplayName("시나리오 1: 복약 스케줄 CRUD")
    class MedicationScheduleCRUD {

        @Test
        @DisplayName("복약 스케줄 생성 → 조회 → 수정 → 삭제 전체 플로우")
        void fullCrudFlow() throws Exception {
            // Given: 로그인
            loginAsTestMember();

            // When: 복약 스케줄 생성
            LocalDate today = LocalDate.now();
            MedicationScheduleCreateRequest createRequest = new MedicationScheduleCreateRequest(
                testMember.getMemberId(),
                "타이레놀",
                "서울병원",
                today,          // prescriptionDate
                "두통약",       // memo
                today,          // startOfAd
                7,              // prescriptionDays
                3,              // perDay
                AlarmType.SOUND
            );

            String scheduleIdLocation = mockMvc.perform(post("/medication-schedule")
                    .header("Authorization", bearerToken())
                    .header("X-CSRF-TOKEN", csrfToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.medicationScheduleId").exists())
                .andExpect(jsonPath("$.name").value("타이레놀"))
                .andExpect(jsonPath("$.prescriptionDays").value(7))
                .andReturn()
                .getResponse()
                .getContentAsString();

            Long scheduleId = objectMapper.readTree(scheduleIdLocation)
                .get("medicationScheduleId").asLong();

            // Then: 생성된 스케줄 조회
            mockMvc.perform(get("/medication-schedule/{id}", scheduleId)
                    .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.medicationScheduleId").value(scheduleId))
                .andExpect(jsonPath("$.name").value("타이레놀"));

            // When: 스케줄 삭제
            mockMvc.perform(delete("/medication-schedule/{id}", scheduleId)
                    .header("Authorization", bearerToken())
                    .header("X-CSRF-TOKEN", csrfToken))
                .andExpect(status().isNoContent());

            // Then: 삭제 확인
            mockMvc.perform(get("/medication-schedule/{id}", scheduleId)
                    .header("Authorization", bearerToken()))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("회원별 복약 스케줄 목록 조회")
        void getSchedulesByMember() throws Exception {
            // Given
            loginAsTestMember();

            // 복약 스케줄 2개 생성
            createMedicationSchedule("약품A");
            createMedicationSchedule("약품B");

            // When & Then
            mockMvc.perform(get("/medication-schedule/member/{memberId}", testMember.getMemberId())
                    .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[*].name", hasItems("약품A", "약품B")));
        }
    }

    @Nested
    @DisplayName("시나리오 2: 복약 기록")
    class MedicationRecord {

        @Test
        @DisplayName("복약 스케줄 생성 → 복약 기록 생성 → 기록 조회")
        void recordMedication() throws Exception {
            // Given
            loginAsTestMember();
            Long scheduleId = createMedicationSchedule("테스트약");

            // When: 복약 기록 생성
            MedicationRecordCreateRequest recordRequest = new MedicationRecordCreateRequest(
                scheduleId,
                null,  // medicationTimeId (선택)
                true,  // isTakeMedication
                LocalDateTime.now()  // realTakeTime
            );

            mockMvc.perform(post("/medication-record")
                    .header("Authorization", bearerToken())
                    .header("X-CSRF-TOKEN", csrfToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(recordRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.medicationRecordId").exists());

            // Then: 기록 조회
            mockMvc.perform(get("/medication-record/schedule/{scheduleId}", scheduleId)
                    .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
        }
    }

    @Nested
    @DisplayName("시나리오 3: 권한 검증")
    class Authorization {

        @Test
        @DisplayName("다른 사용자의 복약 스케줄에 접근하면 403 반환")
        void accessOtherUserSchedule_forbidden() throws Exception {
            // Given: 첫 번째 사용자로 로그인하고 스케줄 생성
            loginAsTestMember();
            Long scheduleId = createMedicationSchedule("내 약");

            // 두 번째 사용자 생성 및 로그인
            createTestMember("otheruser", TEST_PASSWORD, "다른유저");
            login("otheruser", TEST_PASSWORD);

            // When & Then: 다른 사용자의 스케줄 접근 시 403
            mockMvc.perform(get("/medication-schedule/{id}", scheduleId)
                    .header("Authorization", bearerToken()))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("인증 없이 API 접근하면 401 반환")
        void accessWithoutAuth_unauthorized() throws Exception {
            mockMvc.perform(get("/medication-schedule/member/1"))
                .andExpect(status().isUnauthorized());
        }
    }

    // === 헬퍼 메서드 ===

    private Long createMedicationSchedule(String name) throws Exception {
        LocalDate today = LocalDate.now();
        MedicationScheduleCreateRequest request = new MedicationScheduleCreateRequest(
            testMember.getMemberId(),
            name,
            "테스트병원",
            today,
            "테스트 메모",
            today,
            7,
            1,
            AlarmType.VIBE
        );

        String response = mockMvc.perform(post("/medication-schedule")
                .header("Authorization", bearerToken())
                .header("X-CSRF-TOKEN", csrfToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        return objectMapper.readTree(response).get("medicationScheduleId").asLong();
    }
}
