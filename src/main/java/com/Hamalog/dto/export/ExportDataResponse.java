package com.Hamalog.dto.export;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ExportDataResponse(
    ExportMeta meta,
    MemberExportData member,
    List<MedicationScheduleExportData> medicationSchedules,
    List<MedicationRecordExportData> medicationRecords,
    List<MoodDiaryExportData> moodDiaries,
    List<SideEffectRecordExportData> sideEffectRecords
) {
    public record ExportMeta(
        LocalDateTime exportedAt,
        String format,
        String version
    ) {}

    public record MemberExportData(
        Long memberId,
        String name,
        String nickName,
        LocalDate birth,
        LocalDateTime createdAt
    ) {}

    public record MedicationScheduleExportData(
        Long scheduleId,
        String name,
        String hospitalName,
        LocalDate prescriptionDate,
        String memo,
        LocalDate startOfAd,
        Integer prescriptionDays,
        Integer perDay,
        String alarmType
    ) {}

    public record MedicationRecordExportData(
        Long recordId,
        Long scheduleId,
        String medicationName,
        Boolean isTakeMedication,
        LocalDateTime realTakeTime
    ) {}

    public record MoodDiaryExportData(
        Long diaryId,
        LocalDate diaryDate,
        String moodType,
        String diaryType,
        String content
    ) {}

    public record SideEffectRecordExportData(
        Long recordId,
        LocalDateTime createdAt,
        List<String> sideEffects
    ) {}
}
