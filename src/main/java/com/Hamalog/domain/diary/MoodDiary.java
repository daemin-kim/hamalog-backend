package com.Hamalog.domain.diary;

import com.Hamalog.domain.member.Member;
import com.Hamalog.security.encryption.EncryptedStringConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode(of = "moodDiaryId")
@Table(name = "mood_diary",
       uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "diary_date"}),
       indexes = {
           @Index(name = "idx_member_id", columnList = "member_id"),
           @Index(name = "idx_diary_date", columnList = "diary_date")
       })
public class MoodDiary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mood_diary_id")
    private Long moodDiaryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "diary_date", nullable = false)
    private LocalDate diaryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "mood_type", nullable = false, length = 20)
    private MoodType moodType;

    @Enumerated(EnumType.STRING)
    @Column(name = "diary_type", nullable = false, length = 20)
    private DiaryType diaryType;

    @Column(name = "template_answer1", length = 500)
    @Size(max = 500, message = "{moodDiary.templateAnswer1.size}")
    @Convert(converter = EncryptedStringConverter.class)
    private String templateAnswer1;

    @Column(name = "template_answer2", length = 500)
    @Size(max = 500, message = "{moodDiary.templateAnswer2.size}")
    @Convert(converter = EncryptedStringConverter.class)
    private String templateAnswer2;

    @Column(name = "template_answer3", length = 500)
    @Size(max = 500, message = "{moodDiary.templateAnswer3.size}")
    @Convert(converter = EncryptedStringConverter.class)
    private String templateAnswer3;

    @Column(name = "template_answer4", length = 500)
    @Size(max = 500, message = "{moodDiary.templateAnswer4.size}")
    @Convert(converter = EncryptedStringConverter.class)
    private String templateAnswer4;

    @Column(name = "free_content", length = 1500)
    @Size(max = 1500, message = "{moodDiary.freeContent.size}")
    @Convert(converter = EncryptedStringConverter.class)
    private String freeContent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.diaryDate == null) {
            this.diaryDate = LocalDate.now();
        }
    }

    public static MoodDiary createTemplateType(Member member, LocalDate diaryDate, MoodType moodType,
                                                String answer1, String answer2, String answer3, String answer4) {
        return MoodDiary.builder()
                .member(member)
                .diaryDate(diaryDate)
                .moodType(moodType)
                .diaryType(DiaryType.TEMPLATE)
                .templateAnswer1(answer1)
                .templateAnswer2(answer2)
                .templateAnswer3(answer3)
                .templateAnswer4(answer4)
                .build();
    }

    public static MoodDiary createFreeFormType(Member member, LocalDate diaryDate, MoodType moodType, String content) {
        return MoodDiary.builder()
                .member(member)
                .diaryDate(diaryDate)
                .moodType(moodType)
                .diaryType(DiaryType.FREE_FORM)
                .freeContent(content)
                .build();
    }

    /**
     * 템플릿 형식으로 일기 내용 수정
     */
    public void updateAsTemplateType(MoodType moodType, String answer1, String answer2,
                                      String answer3, String answer4) {
        this.moodType = moodType;
        this.diaryType = DiaryType.TEMPLATE;
        this.templateAnswer1 = answer1;
        this.templateAnswer2 = answer2;
        this.templateAnswer3 = answer3;
        this.templateAnswer4 = answer4;
        this.freeContent = null;
    }

    /**
     * 자유 형식으로 일기 내용 수정
     */
    public void updateAsFreeFormType(MoodType moodType, String content) {
        this.moodType = moodType;
        this.diaryType = DiaryType.FREE_FORM;
        this.freeContent = content;
        this.templateAnswer1 = null;
        this.templateAnswer2 = null;
        this.templateAnswer3 = null;
        this.templateAnswer4 = null;
    }
}
