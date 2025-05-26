package com.Hamalog.dto.medication.request;

import com.Hamalog.domain.medication.AlarmType;

import java.time.LocalDate;

public record MedicationScheduleUpdateRequest(
        String name,
        String hospitalName,
        LocalDate prescriptionDate,
        String memo,
        LocalDate startOfAd,
        Integer prescriptionDays,
        Integer perDay,
        AlarmType alarmType
) {}
