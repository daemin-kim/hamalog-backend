package com.Hamalog.dto.medication.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "복약 이행률 통계 응답")
public record MedicationAdherenceResponse(
        @Schema(description = "통계 기간 시작일", example = "2025-12-01")
        LocalDate startDate,

        @Schema(description = "통계 기간 종료일", example = "2025-12-31")
        LocalDate endDate,

        @Schema(description = "총 예정된 복약 횟수", example = "90")
        long totalScheduled,

        @Schema(description = "실제 복용한 횟수", example = "82")
        long totalTaken,

        @Schema(description = "복약 이행률 (%)", example = "91.1")
        double adherenceRate,

        @Schema(description = "미복용 날짜 목록")
        List<LocalDate> missedDates,

        @Schema(description = "일별 복약 통계")
        List<DailyAdherenceStat> dailyStats
) {
    @Schema(description = "일별 복약 통계")
    public record DailyAdherenceStat(
            @Schema(description = "날짜", example = "2025-12-15")
            LocalDate date,

            @Schema(description = "예정된 복약 횟수", example = "3")
            int scheduled,

            @Schema(description = "복용한 횟수", example = "2")
            int taken,

            @Schema(description = "이행률 (%)", example = "66.7")
            double rate
    ) {}

    /**
     * 통계 계산 팩토리 메서드
     */
    public static MedicationAdherenceResponse of(
            LocalDate startDate,
            LocalDate endDate,
            long totalScheduled,
            long totalTaken,
            List<LocalDate> missedDates,
            List<DailyAdherenceStat> dailyStats
    ) {
        double rate = totalScheduled > 0
                ? Math.round((double) totalTaken / totalScheduled * 1000) / 10.0
                : 0.0;
        return new MedicationAdherenceResponse(
                startDate, endDate, totalScheduled, totalTaken, rate, missedDates, dailyStats
        );
    }
}
