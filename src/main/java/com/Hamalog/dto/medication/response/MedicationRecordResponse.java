package com.Hamalog.dto.medication.response;

import com.Hamalog.domain.medication.MedicationRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "복약 기록 응답 데이터")
public record MedicationRecordResponse(
        @Schema(description = "복약 기록 ID", example = "503")
        Long medicationRecordId,

        @Schema(description = "복약 스케줄 ID", example = "101")
        Long medicationScheduleId,

        @Schema(description = "복약 시간 ID", example = "2")
        Long medicationTimeId,

        @Schema(description = "복용 여부", example = "true")
        Boolean isTakeMedication,

        @Schema(description = "실제 복용 시간", example = "2025-08-11T20:00:15")
        LocalDateTime realTakeTime
) {
    public static MedicationRecordResponse from(MedicationRecord medicationRecord) {
        return new MedicationRecordResponse(
                medicationRecord.getMedicationRecordId(),
                medicationRecord.getMedicationSchedule().getMedicationScheduleId(),
                medicationRecord.getMedicationTime().getMedicationTimeId(),
                medicationRecord.getIsTakeMedication(),
                medicationRecord.getRealTakeTime()
        );
    }
}