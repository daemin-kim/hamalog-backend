package com.Hamalog.dto.diary.request;

import com.Hamalog.domain.diary.DiaryType;
import com.Hamalog.domain.diary.MoodType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Schema(description = "마음 일기 생성 요청")
public class MoodDiaryCreateRequest {

    @NotNull(message = "{moodDiary.memberId.notNull}")
    @Schema(description = "회원 ID", example = "1", required = true)
    private Long memberId;

    @NotNull(message = "{moodDiary.diaryDate.notNull}")
    @Schema(description = "일기 날짜", example = "2025-12-01", required = true)
    private LocalDate diaryDate;

    @NotNull(message = "{moodDiary.moodType.notNull}")
    @Schema(description = "오늘의 기분 (HAPPY, EXCITED, PEACEFUL, ANXIOUS, LETHARGIC, ANGRY, SAD)",
            example = "HAPPY", required = true)
    private MoodType moodType;

    @NotNull(message = "{moodDiary.diaryType.notNull}")
    @Schema(description = "일기 형식 (TEMPLATE: 템플릿 형식, FREE_FORM: 자유 형식)",
            example = "TEMPLATE", required = true)
    private DiaryType diaryType;

    @Size(max = 500, message = "{moodDiary.templateAnswer1.size}")
    @Schema(description = "템플릿 질문 1 답변: 오늘 나에게 가장 인상 깊었던 사건은 무엇이었나요? (최대 500자)",
            example = "오늘은 친구와 오랜만에 만나서 즐거운 시간을 보냈습니다.")
    private String templateAnswer1;

    @Size(max = 500, message = "{moodDiary.templateAnswer2.size}")
    @Schema(description = "템플릿 질문 2 답변: 그 순간, 나는 어떤 감정을 느꼈나요? (최대 500자)",
            example = "정말 행복하고 편안한 감정을 느꼈습니다.")
    private String templateAnswer2;

    @Size(max = 500, message = "{moodDiary.templateAnswer3.size}")
    @Schema(description = "템플릿 질문 3 답변: 그 감정을 느낀 이유는 무엇이라고 생각하나요? (최대 500자)",
            example = "오랜만에 친구를 만나서 그동안 쌓였던 스트레스가 풀린 것 같습니다.")
    private String templateAnswer3;

    @Size(max = 500, message = "{moodDiary.templateAnswer4.size}")
    @Schema(description = "템플릿 질문 4 답변: 지금 이 감정에 대해 내가 해주고 싶은 말은 무엇인가요? (최대 500자)",
            example = "앞으로도 이런 소중한 시간을 자주 가지면 좋겠습니다.")
    private String templateAnswer4;

    @Size(max = 1500, message = "{moodDiary.freeContent.size}")
    @Schema(description = "자유 형식 내용 (최대 1500자)",
            example = "오늘 하루를 돌아보면...")
    private String freeContent;

    /**
     * 템플릿 형식 유효성 검증
     */
    public boolean isValidTemplateType() {
        if (diaryType != DiaryType.TEMPLATE) {
            return true;
        }
        return templateAnswer1 != null && !templateAnswer1.isBlank()
                && templateAnswer2 != null && !templateAnswer2.isBlank()
                && templateAnswer3 != null && !templateAnswer3.isBlank()
                && templateAnswer4 != null && !templateAnswer4.isBlank();
    }

    /**
     * 자유 형식 유효성 검증
     */
    public boolean isValidFreeFormType() {
        if (diaryType != DiaryType.FREE_FORM) {
            return true;
        }
        return freeContent != null && !freeContent.isBlank();
    }
}

