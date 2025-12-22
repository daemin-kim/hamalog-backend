package com.Hamalog.dto.diary.response;

import com.Hamalog.domain.diary.MoodType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Schema(description = "마음 일기 통계 응답")
public record MoodDiaryStatsResponse(
        @Schema(description = "통계 기간 시작일", example = "2025-12-01")
        LocalDate startDate,

        @Schema(description = "통계 기간 종료일", example = "2025-12-31")
        LocalDate endDate,

        @Schema(description = "총 일기 작성 일수", example = "25")
        int totalWrittenDays,

        @Schema(description = "기간 내 총 일수", example = "31")
        int totalDays,

        @Schema(description = "작성률 (%)", example = "80.6")
        double writingRate,

        @Schema(description = "연속 작성일 수", example = "12")
        int consecutiveDays,

        @Schema(description = "기분 유형별 분포")
        Map<MoodType, Integer> moodDistribution,

        @Schema(description = "가장 많이 기록된 기분", example = "HAPPY")
        MoodType mostFrequentMood,

        @Schema(description = "일자별 기분 기록")
        List<DailyMoodRecord> dailyRecords
) {
    @Schema(description = "일자별 기분 기록")
    public record DailyMoodRecord(
            @Schema(description = "날짜", example = "2025-12-15")
            LocalDate date,

            @Schema(description = "기분 유형", example = "HAPPY")
            MoodType moodType,

            @Schema(description = "일기 형식", example = "TEMPLATE")
            String diaryType
    ) {}

    public static MoodDiaryStatsResponse of(
            LocalDate startDate,
            LocalDate endDate,
            int totalWrittenDays,
            int totalDays,
            int consecutiveDays,
            Map<MoodType, Integer> moodDistribution,
            List<DailyMoodRecord> dailyRecords
    ) {
        double rate = totalDays > 0
                ? Math.round((double) totalWrittenDays / totalDays * 1000) / 10.0
                : 0.0;

        MoodType mostFrequent = moodDistribution.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        return new MoodDiaryStatsResponse(
                startDate, endDate, totalWrittenDays, totalDays, rate,
                consecutiveDays, moodDistribution, mostFrequent, dailyRecords
        );
    }
}

