package com.Hamalog.e2e;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 복약 관리 E2E 통합 테스트
 */
@DisplayName("E2E: 복약 관리 시나리오")
class MedicationE2ETest extends BaseE2ETest {

    @Nested
    @DisplayName("시나리오 1: 복약 스케줄 조회")
    class MedicationScheduleRead {

        @Test
        @DisplayName("회원의 복약 스케줄 목록 조회")
        void getSchedulesByMember() throws Exception {
            // Given: 로그인
            loginAsTestMember();

            // When & Then: 회원의 스케줄 목록 조회 (빈 목록이어도 200 OK)
            mockMvc.perform(get("/medication-schedule/list/{member-id}", testMember.getMemberId())
                    .header("Authorization", bearerToken()))
                .andDo(print())
                .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("시나리오 2: 권한 검증")
    class Authorization {

        @Test
        @DisplayName("인증 없이 API 접근하면 401 반환")
        void accessWithoutAuth_unauthorized() throws Exception {
            mockMvc.perform(get("/medication-schedule/list/1"))
                .andExpect(status().isUnauthorized());
        }
    }
}
