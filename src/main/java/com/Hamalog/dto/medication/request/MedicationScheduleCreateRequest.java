package com.Hamalog.dto.medication.request;

public record MedicationScheduleCreateRequest(
        Long memberId,
        String name,
        String hospitalName,
        String prescriptionDate,
        String memo,
        String startOfAd,
        Integer prescriptionDays,
        Integer perDay,
        String alarmType
) {
}
