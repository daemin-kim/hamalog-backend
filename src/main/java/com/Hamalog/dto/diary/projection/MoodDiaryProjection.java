package com.Hamalog.dto.diary.projection;

import com.Hamalog.domain.diary.DiaryType;
import com.Hamalog.domain.diary.MoodType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 마음 일기 목록 조회용 DTO Projection
 * JPQL Constructor Expression으로 직접 생성
 * 엔티티 전체가 아닌 필요한 필드만 조회하여 성능 최적화
 */
@Schema(description = "마음 일기 목록 조회용 Projection")
public record MoodDiaryProjection(
        @Schema(description = "마음 일기 ID", example = "1")
        Long moodDiaryId,

        @Schema(description = "회원 ID", example = "1")
        Long memberId,

        @Schema(description = "일기 날짜", example = "2025-12-01")
        LocalDate diaryDate,

        @Schema(description = "기분 타입", example = "HAPPY")
        MoodType moodType,

        @Schema(description = "일기 형식", example = "TEMPLATE")
        DiaryType diaryType,

        @Schema(description = "생성 일시", example = "2025-12-01T20:30:00")
        LocalDateTime createdAt
) {
}
