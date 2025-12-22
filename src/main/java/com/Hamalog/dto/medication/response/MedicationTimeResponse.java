package com.Hamalog.dto.medication.response;

import com.Hamalog.domain.medication.MedicationTime;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalTime;

@Schema(description = "복약 알림 시간 응답")
public record MedicationTimeResponse(
        @Schema(description = "알림 시간 ID", example = "1")
        Long medicationTimeId,

        @Schema(description = "복약 스케줄 ID", example = "1")
        Long medicationScheduleId,

        @Schema(description = "복약 시간", example = "09:00:00")
        LocalTime takeTime
) {
    public static MedicationTimeResponse from(MedicationTime medicationTime) {
        return new MedicationTimeResponse(
                medicationTime.getMedicationTimeId(),
                medicationTime.getMedicationSchedule().getMedicationScheduleId(),
                medicationTime.getTakeTime()
        );
    }
}
