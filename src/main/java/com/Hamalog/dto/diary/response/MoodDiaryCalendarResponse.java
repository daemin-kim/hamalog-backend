package com.Hamalog.dto.diary.response;

import com.Hamalog.domain.diary.MoodType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Schema(description = "마음 일기 캘린더 응답")
public record MoodDiaryCalendarResponse(
        @Schema(description = "조회 년도", example = "2025")
        int year,

        @Schema(description = "조회 월", example = "12")
        int month,

        @Schema(description = "해당 월 총 일수", example = "31")
        int totalDays,

        @Schema(description = "일기 작성 일수", example = "25")
        int writtenDays,

        @Schema(description = "작성률 (%)", example = "80.6")
        double writingRate,

        @Schema(description = "일자별 일기 기록")
        List<CalendarDayRecord> records
) {
    @Schema(description = "일자별 일기 기록")
    public record CalendarDayRecord(
            @Schema(description = "일자 (1-31)", example = "15")
            int day,

            @Schema(description = "해당 날짜", example = "2025-12-15")
            LocalDate date,

            @Schema(description = "일기 작성 여부", example = "true")
            boolean hasEntry,

            @Schema(description = "기분 유형 (작성된 경우)", example = "HAPPY")
            MoodType moodType,

            @Schema(description = "일기 ID (작성된 경우)", example = "123")
            Long moodDiaryId
    ) {}

    public static MoodDiaryCalendarResponse of(
            YearMonth yearMonth,
            int writtenDays,
            List<CalendarDayRecord> records
    ) {
        int totalDays = yearMonth.lengthOfMonth();
        double rate = totalDays > 0
                ? Math.round((double) writtenDays / totalDays * 1000) / 10.0
                : 0.0;

        return new MoodDiaryCalendarResponse(
                yearMonth.getYear(),
                yearMonth.getMonthValue(),
                totalDays,
                writtenDays,
                rate,
                records
        );
    }
}

