package com.Hamalog.dto.medication.request;

import com.Hamalog.domain.medication.AlarmType;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record MedicationScheduleCreateRequest(
        @NotNull(message = "{medicationSchedule.memberId.notNull}") Long memberId,
        @NotBlank(message = "{medicationSchedule.name.notBlank}") @Size(max = 20, message = "{medicationSchedule.name.size}") String name,
        @NotBlank(message = "{medicationSchedule.hospitalName.notBlank}") @Size(max = 20, message = "{medicationSchedule.hospitalName.size}") String hospitalName,
        @NotNull(message = "{medicationSchedule.prescriptionDate.notNull}") LocalDate prescriptionDate,
        @Size(max = 500, message = "{medicationSchedule.memo.size}") String memo,
        @NotNull(message = "{medicationSchedule.startOfAd.notNull}") LocalDate startOfAd,
        @NotNull(message = "{medicationSchedule.prescriptionDays.notNull}") @Min(value = 1, message = "{medicationSchedule.prescriptionDays.min}") Integer prescriptionDays,
        @NotNull(message = "{medicationSchedule.perDay.notNull}") @Min(value = 1, message = "{medicationSchedule.perDay.min}") Integer perDay,
        @NotNull(message = "{medicationSchedule.alarmType.notNull}") AlarmType alarmType
) {}
