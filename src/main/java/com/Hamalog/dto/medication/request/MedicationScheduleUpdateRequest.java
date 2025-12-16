package com.Hamalog.dto.medication.request;

import com.Hamalog.domain.medication.AlarmType;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record MedicationScheduleUpdateRequest(
        @NotBlank @Size(max = 20) String name,
        @NotBlank @Size(max = 20) String hospitalName,
        @NotNull LocalDate prescriptionDate,
        @Size(max = 500) String memo,
        @NotNull LocalDate startOfAd,
        @NotNull @Min(1) Integer prescriptionDays,
        @NotNull @Min(1) Integer perDay,
        @NotNull AlarmType alarmType
) {}
