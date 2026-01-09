package com.Hamalog.domain.diary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.Hamalog.domain.member.Member;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("MoodDiary 도메인 테스트")
class MoodDiaryTest {

    private Member mockMember;

    @BeforeEach
    void setUp() {
        mockMember = mock(Member.class);
    }

    @Nested
    @DisplayName("템플릿 형식 일기 생성")
    class CreateTemplateType {

        @Test
        @DisplayName("성공: 템플릿 형식 일기 생성")
        void success() {
            // given
            LocalDate date = LocalDate.now();

            // when
            MoodDiary diary = MoodDiary.createTemplateType(
                    mockMember, date, MoodType.HAPPY,
                    "답변1", "답변2", "답변3", "답변4"
            );

            // then
            assertThat(diary.getMoodType()).isEqualTo(MoodType.HAPPY);
            assertThat(diary.getDiaryType()).isEqualTo(DiaryType.TEMPLATE);
            assertThat(diary.getTemplateAnswer1()).isEqualTo("답변1");
            assertThat(diary.getFreeContent()).isNull();
        }
    }

    @Nested
    @DisplayName("자유 형식 일기 생성")
    class CreateFreeFormType {

        @Test
        @DisplayName("성공: 자유 형식 일기 생성")
        void success() {
            // given
            LocalDate date = LocalDate.now();

            // when
            MoodDiary diary = MoodDiary.createFreeFormType(
                    mockMember, date, MoodType.PEACEFUL, "오늘의 일기 내용"
            );

            // then
            assertThat(diary.getMoodType()).isEqualTo(MoodType.PEACEFUL);
            assertThat(diary.getDiaryType()).isEqualTo(DiaryType.FREE_FORM);
            assertThat(diary.getFreeContent()).isEqualTo("오늘의 일기 내용");
            assertThat(diary.getTemplateAnswer1()).isNull();
        }
    }

    @Nested
    @DisplayName("일기 형식 확인")
    class DiaryTypeCheck {

        @Test
        @DisplayName("성공: 템플릿 형식 확인")
        void success_isTemplateType() {
            // given
            MoodDiary diary = MoodDiary.createTemplateType(
                    mockMember, LocalDate.now(), MoodType.HAPPY,
                    "답변1", null, null, null
            );

            // when & then
            assertThat(diary.isTemplateType()).isTrue();
            assertThat(diary.isFreeFormType()).isFalse();
        }

        @Test
        @DisplayName("성공: 자유 형식 확인")
        void success_isFreeFormType() {
            // given
            MoodDiary diary = MoodDiary.createFreeFormType(
                    mockMember, LocalDate.now(), MoodType.HAPPY, "내용"
            );

            // when & then
            assertThat(diary.isFreeFormType()).isTrue();
            assertThat(diary.isTemplateType()).isFalse();
        }
    }

    @Nested
    @DisplayName("기분 상태 확인")
    class MoodCheck {

        @Test
        @DisplayName("성공: 긍정적 기분 확인 - HAPPY")
        void success_isPositive_happy() {
            // given
            MoodDiary diary = MoodDiary.createFreeFormType(
                    mockMember, LocalDate.now(), MoodType.HAPPY, "내용"
            );

            // when & then
            assertThat(diary.isPositiveMood()).isTrue();
            assertThat(diary.isNegativeMood()).isFalse();
        }

        @Test
        @DisplayName("성공: 긍정적 기분 확인 - EXCITED")
        void success_isPositive_excited() {
            // given
            MoodDiary diary = MoodDiary.createFreeFormType(
                    mockMember, LocalDate.now(), MoodType.EXCITED, "내용"
            );

            // when & then
            assertThat(diary.isPositiveMood()).isTrue();
        }

        @Test
        @DisplayName("성공: 긍정적 기분 확인 - PEACEFUL")
        void success_isPositive_peaceful() {
            // given
            MoodDiary diary = MoodDiary.createFreeFormType(
                    mockMember, LocalDate.now(), MoodType.PEACEFUL, "내용"
            );

            // when & then
            assertThat(diary.isPositiveMood()).isTrue();
        }

        @Test
        @DisplayName("성공: 부정적 기분 확인 - ANXIOUS")
        void success_isNegative_anxious() {
            // given
            MoodDiary diary = MoodDiary.createFreeFormType(
                    mockMember, LocalDate.now(), MoodType.ANXIOUS, "내용"
            );

            // when & then
            assertThat(diary.isNegativeMood()).isTrue();
            assertThat(diary.isPositiveMood()).isFalse();
        }

        @Test
        @DisplayName("성공: 부정적 기분 확인 - SAD")
        void success_isNegative_sad() {
            // given
            MoodDiary diary = MoodDiary.createFreeFormType(
                    mockMember, LocalDate.now(), MoodType.SAD, "내용"
            );

            // when & then
            assertThat(diary.isNegativeMood()).isTrue();
        }

        @Test
        @DisplayName("성공: 부정적 기분 확인 - ANGRY")
        void success_isNegative_angry() {
            // given
            MoodDiary diary = MoodDiary.createFreeFormType(
                    mockMember, LocalDate.now(), MoodType.ANGRY, "내용"
            );

            // when & then
            assertThat(diary.isNegativeMood()).isTrue();
        }

        @Test
        @DisplayName("성공: 부정적 기분 확인 - LETHARGIC")
        void success_isNegative_lethargic() {
            // given
            MoodDiary diary = MoodDiary.createFreeFormType(
                    mockMember, LocalDate.now(), MoodType.LETHARGIC, "내용"
            );

            // when & then
            assertThat(diary.isNegativeMood()).isTrue();
        }
    }

    @Nested
    @DisplayName("날짜 확인")
    class DateCheck {

        @Test
        @DisplayName("성공: 오늘 일기 확인")
        void success_isToday() {
            // given
            MoodDiary diary = MoodDiary.createFreeFormType(
                    mockMember, LocalDate.now(), MoodType.HAPPY, "내용"
            );

            // when & then
            assertThat(diary.isToday()).isTrue();
        }

        @Test
        @DisplayName("성공: 과거 일기는 오늘이 아님")
        void success_isNotToday() {
            // given
            MoodDiary diary = MoodDiary.createFreeFormType(
                    mockMember, LocalDate.now().minusDays(1), MoodType.HAPPY, "내용"
            );

            // when & then
            assertThat(diary.isToday()).isFalse();
        }

        @Test
        @DisplayName("성공: 날짜 범위 확인")
        void success_isWithinDateRange() {
            // given
            LocalDate date = LocalDate.of(2025, 1, 15);
            MoodDiary diary = MoodDiary.createFreeFormType(
                    mockMember, date, MoodType.HAPPY, "내용"
            );

            // when & then
            assertThat(diary.isWithinDateRange(
                    LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31)
            )).isTrue();

            assertThat(diary.isWithinDateRange(
                    LocalDate.of(2025, 2, 1), LocalDate.of(2025, 2, 28)
            )).isFalse();
        }
    }

    @Nested
    @DisplayName("내용 확인")
    class ContentCheck {

        @Test
        @DisplayName("성공: 자유 형식 내용 있음")
        void success_hasFreeContent() {
            // given
            MoodDiary diary = MoodDiary.createFreeFormType(
                    mockMember, LocalDate.now(), MoodType.HAPPY, "내용 있음"
            );

            // when & then
            assertThat(diary.hasContent()).isTrue();
        }

        @Test
        @DisplayName("성공: 자유 형식 내용 없음")
        void success_noFreeContent() {
            // given
            MoodDiary diary = MoodDiary.createFreeFormType(
                    mockMember, LocalDate.now(), MoodType.HAPPY, ""
            );

            // when & then
            assertThat(diary.hasContent()).isFalse();
        }

        @Test
        @DisplayName("성공: 템플릿 형식 답변 있음")
        void success_hasTemplateContent() {
            // given
            MoodDiary diary = MoodDiary.createTemplateType(
                    mockMember, LocalDate.now(), MoodType.HAPPY,
                    "답변1", null, null, null
            );

            // when & then
            assertThat(diary.hasContent()).isTrue();
        }

        @Test
        @DisplayName("성공: 템플릿 형식 답변 없음")
        void success_noTemplateContent() {
            // given
            MoodDiary diary = MoodDiary.createTemplateType(
                    mockMember, LocalDate.now(), MoodType.HAPPY,
                    null, null, null, null
            );

            // when & then
            assertThat(diary.hasContent()).isFalse();
        }
    }

    @Nested
    @DisplayName("일기 수정")
    class UpdateDiary {

        @Test
        @DisplayName("성공: 템플릿 형식으로 수정")
        void success_updateAsTemplateType() {
            // given
            MoodDiary diary = MoodDiary.createFreeFormType(
                    mockMember, LocalDate.now(), MoodType.HAPPY, "자유 내용"
            );

            // when
            diary.updateAsTemplateType(MoodType.PEACEFUL, "새답변1", "새답변2", null, null);

            // then
            assertThat(diary.isTemplateType()).isTrue();
            assertThat(diary.getMoodType()).isEqualTo(MoodType.PEACEFUL);
            assertThat(diary.getTemplateAnswer1()).isEqualTo("새답변1");
            assertThat(diary.getFreeContent()).isNull();
        }

        @Test
        @DisplayName("성공: 자유 형식으로 수정")
        void success_updateAsFreeFormType() {
            // given
            MoodDiary diary = MoodDiary.createTemplateType(
                    mockMember, LocalDate.now(), MoodType.HAPPY,
                    "답변1", "답변2", null, null
            );

            // when
            diary.updateAsFreeFormType(MoodType.ANXIOUS, "새 자유 내용");

            // then
            assertThat(diary.isFreeFormType()).isTrue();
            assertThat(diary.getMoodType()).isEqualTo(MoodType.ANXIOUS);
            assertThat(diary.getFreeContent()).isEqualTo("새 자유 내용");
            assertThat(diary.getTemplateAnswer1()).isNull();
        }
    }
}
