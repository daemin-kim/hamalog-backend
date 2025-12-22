package com.Hamalog.dto.medication.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

@Schema(description = "복약 알림 시간 수정 요청")
public record MedicationTimeUpdateRequest(
        @NotNull(message = "{medicationTime.takeTime.notNull}")
        @Schema(description = "수정할 복약 시간 (HH:mm)", example = "10:00", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalTime takeTime
) {
}
