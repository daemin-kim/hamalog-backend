package com.Hamalog.dto.medication.request;

import jakarta.validation.constraints.*;

public record MedicationScheduleCreateRequest(
        @NotNull(message = "{medicationSchedule.memberId.notNull}") Long memberId,
        @NotBlank(message = "{medicationSchedule.name.notBlank}") @Size(max = 20, message = "{medicationSchedule.name.size}") String name,
        @NotBlank(message = "{medicationSchedule.hospitalName.notBlank}") @Size(max = 20, message = "{medicationSchedule.hospitalName.size}") String hospitalName,
        @NotBlank(message = "{medicationSchedule.prescriptionDate.notBlank}")
        @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "{medicationSchedule.prescriptionDate.pattern}") String prescriptionDate,
        @Size(max = 500, message = "{medicationSchedule.memo.size}") String memo,
        @NotBlank(message = "{medicationSchedule.startOfAd.notBlank}")
        @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "{medicationSchedule.startOfAd.pattern}") String startOfAd,
        @NotNull(message = "{medicationSchedule.prescriptionDays.notNull}") @Min(value = 1, message = "{medicationSchedule.prescriptionDays.min}") Integer prescriptionDays,
        @NotNull(message = "{medicationSchedule.perDay.notNull}") @Min(value = 1, message = "{medicationSchedule.perDay.min}") Integer perDay,
        @NotBlank(message = "{medicationSchedule.alarmType.notBlank}") String alarmType
) {}
