package com.Hamalog.dto.medication.projection;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 복약 기록 목록 조회용 DTO Projection
 * JPQL Constructor Expression으로 직접 생성
 * 엔티티 전체가 아닌 필요한 필드만 조회하여 성능 최적화
 */
@Schema(description = "복약 기록 목록 조회용 Projection")
public record MedicationRecordProjection(
        @Schema(description = "복약 기록 ID", example = "503")
        Long medicationRecordId,

        @Schema(description = "복약 스케줄 ID", example = "101")
        Long medicationScheduleId,

        @Schema(description = "복약 스케줄 이름", example = "혈압약")
        String scheduleName,

        @Schema(description = "복약 시간 ID", example = "2")
        Long medicationTimeId,

        @Schema(description = "복용 여부", example = "true")
        Boolean isTakeMedication,

        @Schema(description = "실제 복용 시간", example = "2025-08-11T20:00:15")
        LocalDateTime realTakeTime
) {
}
