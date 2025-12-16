package com.Hamalog.dto.diary.response;

import com.Hamalog.domain.diary.DiaryType;
import com.Hamalog.domain.diary.MoodDiary;
import com.Hamalog.domain.diary.MoodType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "마음 일기 응답 데이터")
public record MoodDiaryResponse(
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

        @Schema(description = "템플릿 질문 1 답변")
        String templateAnswer1,

        @Schema(description = "템플릿 질문 2 답변")
        String templateAnswer2,

        @Schema(description = "템플릿 질문 3 답변")
        String templateAnswer3,

        @Schema(description = "템플릿 질문 4 답변")
        String templateAnswer4,

        @Schema(description = "자유 형식 내용")
        String freeContent,

        @Schema(description = "생성 일시", example = "2025-12-01T20:30:00")
        LocalDateTime createdAt
) {
    public static MoodDiaryResponse from(MoodDiary moodDiary) {
        return new MoodDiaryResponse(
                moodDiary.getMoodDiaryId(),
                moodDiary.getMember().getMemberId(),
                moodDiary.getDiaryDate(),
                moodDiary.getMoodType(),
                moodDiary.getDiaryType(),
                moodDiary.getTemplateAnswer1(),
                moodDiary.getTemplateAnswer2(),
                moodDiary.getTemplateAnswer3(),
                moodDiary.getTemplateAnswer4(),
                moodDiary.getFreeContent(),
                moodDiary.getCreatedAt()
        );
    }
}
