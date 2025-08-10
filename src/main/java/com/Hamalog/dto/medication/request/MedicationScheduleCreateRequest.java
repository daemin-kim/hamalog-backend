package com.Hamalog.dto.medication.request;

import jakarta.validation.constraints.*;

public record MedicationScheduleCreateRequest(
        @NotNull(message = "memberId is required") Long memberId,
        @NotBlank(message = "name is required") @Size(max = 20) String name,
        @NotBlank(message = "hospitalName is required") @Size(max = 20) String hospitalName,
        @NotBlank(message = "prescriptionDate is required")
        @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "prescriptionDate must be yyyy-MM-dd") String prescriptionDate,
        @Size(max = 500) String memo,
        @NotBlank(message = "startOfAd is required")
        @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "startOfAd must be yyyy-MM-dd") String startOfAd,
        @NotNull @Min(1) Integer prescriptionDays,
        @NotNull @Min(1) Integer perDay,
        @NotBlank(message = "alarmType is required") String alarmType
) {}
