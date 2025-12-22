package com.Hamalog.dto.medication.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

@Schema(description = "복약 알림 시간 생성 요청")
public record MedicationTimeCreateRequest(
        @NotNull(message = "{medicationTime.takeTime.notNull}")
        @Schema(description = "복약 시간 (HH:mm)", example = "09:00", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalTime takeTime
) {
}
