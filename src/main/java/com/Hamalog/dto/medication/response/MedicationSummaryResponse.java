package com.Hamalog.dto.medication.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "복약 현황 요약 응답")
public record MedicationSummaryResponse(
        @Schema(description = "총 활성 복약 스케줄 수", example = "5")
        int totalActiveSchedules,

        @Schema(description = "오늘 예정된 복약 횟수", example = "12")
        int todayScheduled,

        @Schema(description = "오늘 복용 완료 횟수", example = "8")
        int todayTaken,

        @Schema(description = "오늘 복약 이행률 (%)", example = "66.7")
        double todayAdherenceRate,

        @Schema(description = "이번 주 복약 이행률 (%)", example = "85.5")
        double weeklyAdherenceRate,

        @Schema(description = "이번 달 복약 이행률 (%)", example = "91.2")
        double monthlyAdherenceRate,

        @Schema(description = "복약 스케줄별 요약")
        List<ScheduleSummary> scheduleSummaries
) {
    @Schema(description = "복약 스케줄별 요약")
    public record ScheduleSummary(
            @Schema(description = "스케줄 ID", example = "1")
            Long scheduleId,

            @Schema(description = "약 이름", example = "타이레놀")
            String medicationName,

            @Schema(description = "약 별명", example = "두통약")
            String medicationNickname,

            @Schema(description = "총 복약 기록 수", example = "30")
            long totalRecords,

            @Schema(description = "복용한 횟수", example = "27")
            long takenCount,

            @Schema(description = "이행률 (%)", example = "90.0")
            double adherenceRate
    ) {}

    /**
     * 오늘 이행률 계산 팩토리 메서드
     */
    public static MedicationSummaryResponse of(
            int totalActiveSchedules,
            int todayScheduled,
            int todayTaken,
            double weeklyRate,
            double monthlyRate,
            List<ScheduleSummary> scheduleSummaries
    ) {
        double todayRate = todayScheduled > 0
                ? Math.round((double) todayTaken / todayScheduled * 1000) / 10.0
                : 0.0;
        return new MedicationSummaryResponse(
                totalActiveSchedules, todayScheduled, todayTaken,
                todayRate, weeklyRate, monthlyRate, scheduleSummaries
        );
    }
}

