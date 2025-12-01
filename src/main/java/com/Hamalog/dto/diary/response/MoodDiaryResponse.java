package com.Hamalog.dto.diary.response;

import com.Hamalog.domain.diary.DiaryType;
import com.Hamalog.domain.diary.MoodDiary;
import com.Hamalog.domain.diary.MoodType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MoodDiaryResponse {

    private Long moodDiaryId;
    private Long memberId;
    private LocalDate diaryDate;
    private MoodType moodType;
    private DiaryType diaryType;
    private String templateAnswer1;
    private String templateAnswer2;
    private String templateAnswer3;
    private String templateAnswer4;
    private String freeContent;
    private LocalDateTime createdAt;

    public static MoodDiaryResponse from(MoodDiary moodDiary) {
        return MoodDiaryResponse.builder()
                .moodDiaryId(moodDiary.getMoodDiaryId())
                .memberId(moodDiary.getMember().getMemberId())
                .diaryDate(moodDiary.getDiaryDate())
                .moodType(moodDiary.getMoodType())
                .diaryType(moodDiary.getDiaryType())
                .templateAnswer1(moodDiary.getTemplateAnswer1())
                .templateAnswer2(moodDiary.getTemplateAnswer2())
                .templateAnswer3(moodDiary.getTemplateAnswer3())
                .templateAnswer4(moodDiary.getTemplateAnswer4())
                .freeContent(moodDiary.getFreeContent())
                .createdAt(moodDiary.getCreatedAt())
                .build();
    }
}

