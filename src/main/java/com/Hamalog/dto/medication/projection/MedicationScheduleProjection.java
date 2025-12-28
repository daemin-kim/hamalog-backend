package com.Hamalog.dto.medication.projection;

import com.Hamalog.domain.medication.AlarmType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * 복약 스케줄 목록 조회용 DTO Projection
 * JPQL Constructor Expression으로 직접 생성
 * 엔티티 전체가 아닌 필요한 필드만 조회하여 성능 최적화
 */
@Schema(description = "복약 스케줄 목록 조회용 Projection")
public record MedicationScheduleProjection(
        @Schema(description = "복약 스케줄 ID", example = "101")
        Long medicationScheduleId,

        @Schema(description = "회원 ID", example = "1")
        Long memberId,

        @Schema(description = "약 이름", example = "혈압약")
        String name,

        @Schema(description = "병원 이름", example = "서울중앙병원")
        String hospitalName,

        @Schema(description = "처방 날짜", example = "2025-08-01")
        LocalDate prescriptionDate,

        @Schema(description = "메모", example = "식후 30분")
        String memo,

        @Schema(description = "복용 시작일", example = "2025-08-02")
        LocalDate startOfAd,

        @Schema(description = "처방 일수", example = "30")
        Integer prescriptionDays,

        @Schema(description = "1일 복용 횟수", example = "1")
        Integer perDay,

        @Schema(description = "알람 타입", example = "VIBE")
        AlarmType alarmType,

        @Schema(description = "활성 상태", example = "true")
        Boolean isActive
) {
}
