package com.Hamalog.repository.diary;

import static org.assertj.core.api.Assertions.assertThat;

import com.Hamalog.config.TestEncryptionConfig;
import com.Hamalog.domain.diary.DiaryType;
import com.Hamalog.domain.diary.MoodDiary;
import com.Hamalog.domain.diary.MoodType;
import com.Hamalog.domain.member.Member;
import com.Hamalog.dto.diary.projection.MoodDiaryProjection;
import com.Hamalog.repository.member.MemberRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(TestEncryptionConfig.class)
@DisplayName("MoodDiaryRepository 테스트")
class MoodDiaryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MoodDiaryRepository moodDiaryRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member testMember;
    private Member anotherMember;

    @BeforeEach
    void setUp() {
        testMember = createTestMember("test@example.com");
        anotherMember = createTestMember("another@example.com");

        entityManager.persistAndFlush(testMember);
        entityManager.persistAndFlush(anotherMember);
    }

    @Nested
    @DisplayName("회원과 날짜로 일기 조회")
    class FindByMemberAndDiaryDate {

        @Test
        @DisplayName("성공: 일기 조회")
        void success() {
            // given
            LocalDate today = LocalDate.now();
            MoodDiary diary = createMoodDiary(testMember, today, MoodType.HAPPY);
            entityManager.persistAndFlush(diary);

            // when
            Optional<MoodDiary> result = moodDiaryRepository.findByMemberAndDiaryDate(testMember, today);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getMoodType()).isEqualTo(MoodType.HAPPY);
        }

        @Test
        @DisplayName("성공: 일기 없음")
        void success_notFound() {
            // when
            Optional<MoodDiary> result = moodDiaryRepository.findByMemberAndDiaryDate(testMember, LocalDate.now());

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("날짜 중복 확인")
    class ExistsByMemberAndDiaryDate {

        @Test
        @DisplayName("성공: 존재하는 경우")
        void success_exists() {
            // given
            LocalDate today = LocalDate.now();
            MoodDiary diary = createMoodDiary(testMember, today, MoodType.PEACEFUL);
            entityManager.persistAndFlush(diary);

            // when
            boolean exists = moodDiaryRepository.existsByMemberAndDiaryDate(testMember, today);

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("성공: 존재하지 않는 경우")
        void success_notExists() {
            // when
            boolean exists = moodDiaryRepository.existsByMemberAndDiaryDate(testMember, LocalDate.now());

            // then
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("기간별 일기 조회")
    class FindByMemberIdAndDateRange {

        @Test
        @DisplayName("성공: 기간 내 일기 목록 조회")
        void success() {
            // given
            LocalDate startDate = LocalDate.of(2025, 1, 1);
            LocalDate endDate = LocalDate.of(2025, 1, 7);

            for (int i = 1; i <= 5; i++) {
                MoodDiary diary = createMoodDiary(testMember, LocalDate.of(2025, 1, i), MoodType.HAPPY);
                entityManager.persistAndFlush(diary);
            }

            // when
            List<MoodDiary> result = moodDiaryRepository.findByMemberIdAndDateRange(
                    testMember.getMemberId(), startDate, endDate
            );

            // then
            assertThat(result).hasSize(5);
        }
    }

    @Nested
    @DisplayName("월별 일기 조회")
    class FindByMemberIdAndYearMonth {

        @Test
        @DisplayName("성공: 특정 년월의 일기 조회")
        void success() {
            // given
            MoodDiary diary1 = createMoodDiary(testMember, LocalDate.of(2025, 1, 1), MoodType.HAPPY);
            MoodDiary diary2 = createMoodDiary(testMember, LocalDate.of(2025, 1, 15), MoodType.PEACEFUL);
            MoodDiary diary3 = createMoodDiary(testMember, LocalDate.of(2025, 2, 1), MoodType.ANXIOUS);

            entityManager.persistAndFlush(diary1);
            entityManager.persistAndFlush(diary2);
            entityManager.persistAndFlush(diary3);

            // when
            List<MoodDiary> result = moodDiaryRepository.findByMemberIdAndYearMonth(
                    testMember.getMemberId(), 2025, 1
            );

            // then
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("페이지네이션 목록 조회")
    class FindByMemberOrderByDiaryDateDesc {

        @Test
        @DisplayName("성공: 페이지네이션으로 일기 조회")
        void success() {
            // given
            for (int i = 1; i <= 10; i++) {
                MoodDiary diary = createMoodDiary(testMember, LocalDate.now().minusDays(i), MoodType.HAPPY);
                entityManager.persistAndFlush(diary);
            }

            // when
            Page<MoodDiary> result = moodDiaryRepository.findByMemberOrderByDiaryDateDesc(
                    testMember, PageRequest.of(0, 5)
            );

            // then
            assertThat(result.getContent()).hasSize(5);
            assertThat(result.getTotalElements()).isEqualTo(10);
            assertThat(result.getTotalPages()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("DTO Projection 조회")
    class FindProjectionsByMemberId {

        @Test
        @DisplayName("성공: Projection으로 일기 목록 조회")
        void success() {
            // given
            for (int i = 1; i <= 5; i++) {
                MoodDiary diary = createMoodDiary(testMember, LocalDate.now().minusDays(i), MoodType.HAPPY);
                entityManager.persistAndFlush(diary);
            }

            // when
            Page<MoodDiaryProjection> result = moodDiaryRepository.findProjectionsByMemberId(
                    testMember.getMemberId(), PageRequest.of(0, 10)
            );

            // then
            assertThat(result.getContent()).hasSize(5);
            assertThat(result.getContent().get(0).moodType()).isEqualTo(MoodType.HAPPY);
        }
    }

    @Nested
    @DisplayName("회원 탈퇴 시 일기 삭제")
    class DeleteByMemberId {

        @Test
        @DisplayName("성공: 회원의 모든 일기 삭제")
        void success() {
            // given
            for (int i = 1; i <= 3; i++) {
                MoodDiary diary = createMoodDiary(testMember, LocalDate.now().minusDays(i), MoodType.HAPPY);
                entityManager.persistAndFlush(diary);
            }

            // when
            moodDiaryRepository.deleteByMember_MemberId(testMember.getMemberId());
            entityManager.flush();

            // then
            List<MoodDiary> remaining = moodDiaryRepository.findByMemberIdAndDateRange(
                    testMember.getMemberId(), LocalDate.MIN, LocalDate.MAX
            );
            assertThat(remaining).isEmpty();
        }
    }

    // 헬퍼 메서드
    private Member createTestMember(String loginId) {
        return Member.builder()
                .loginId(loginId)
                .password("encodedPassword")
                .name("테스트 사용자")
                .nickName("테스트닉네임")
                .phoneNumber("01012345678")
                .birth(LocalDate.of(1990, 1, 1))
                .createdAt(java.time.LocalDateTime.now())
                .build();
    }

    private MoodDiary createMoodDiary(Member member, LocalDate date, MoodType moodType) {
        return MoodDiary.builder()
                .member(member)
                .diaryDate(date)
                .moodType(moodType)
                .diaryType(DiaryType.FREE_FORM)
                .freeContent("오늘의 일기 내용입니다.")
                .build();
    }
}
