package com.Hamalog.domain.medication;

import static org.assertj.core.api.Assertions.assertThat;

import com.Hamalog.domain.member.Member;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * MedicationSchedule 도메인 로직 테스트
 * Entity에 추가된 비즈니스 로직 검증
 */
@DisplayName("MedicationSchedule 도메인 로직 테스트")
class MedicationScheduleDomainTest {

    private Member testMember;

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
                .memberId(1L)
                .loginId("testUser")
                .build();
    }

    private MedicationSchedule createSchedule(LocalDate startDate, int prescriptionDays) {
        return new MedicationSchedule(
                testMember,
                "테스트 약",
                "테스트 병원",
                startDate,
                "메모",
                startDate,
                prescriptionDays,
                3,
                AlarmType.SOUND
        );
    }

    @Nested
    @DisplayName("종료일 계산")
    class GetEndDate {

        @Test
        @DisplayName("성공: 7일 처방 시 시작일로부터 6일 후가 종료일")
        void shouldReturnCorrectEndDate() {
            // given
            LocalDate startDate = LocalDate.of(2026, 1, 1);
            MedicationSchedule schedule = createSchedule(startDate, 7);

            // when
            LocalDate endDate = schedule.getEndDate();

            // then
            assertThat(endDate).isEqualTo(LocalDate.of(2026, 1, 7));
        }

        @Test
        @DisplayName("성공: 1일 처방 시 시작일과 종료일이 동일")
        void shouldReturnSameDateForSingleDay() {
            // given
            LocalDate startDate = LocalDate.of(2026, 1, 1);
            MedicationSchedule schedule = createSchedule(startDate, 1);

            // when
            LocalDate endDate = schedule.getEndDate();

            // then
            assertThat(endDate).isEqualTo(startDate);
        }
    }

    @Nested
    @DisplayName("만료 여부 확인")
    class IsExpired {

        @Test
        @DisplayName("성공: 종료일 이후면 만료됨")
        void shouldReturnTrueWhenExpired() {
            // given - 30일 전에 시작해서 7일간 복용
            LocalDate startDate = LocalDate.now().minusDays(30);
            MedicationSchedule schedule = createSchedule(startDate, 7);

            // when
            boolean expired = schedule.isExpired();

            // then
            assertThat(expired).isTrue();
        }

        @Test
        @DisplayName("성공: 아직 복용 중이면 만료되지 않음")
        void shouldReturnFalseWhenOngoing() {
            // given - 오늘 시작해서 7일간 복용
            LocalDate startDate = LocalDate.now();
            MedicationSchedule schedule = createSchedule(startDate, 7);

            // when
            boolean expired = schedule.isExpired();

            // then
            assertThat(expired).isFalse();
        }
    }

    @Nested
    @DisplayName("남은 일수 계산")
    class GetRemainingDays {

        @Test
        @DisplayName("성공: 만료된 경우 0 반환")
        void shouldReturnZeroWhenExpired() {
            // given
            LocalDate startDate = LocalDate.now().minusDays(30);
            MedicationSchedule schedule = createSchedule(startDate, 7);

            // when
            int remainingDays = schedule.getRemainingDays();

            // then
            assertThat(remainingDays).isZero();
        }

        @Test
        @DisplayName("성공: 오늘 시작한 7일 처방은 7일 남음")
        void shouldReturnCorrectRemainingDays() {
            // given
            LocalDate startDate = LocalDate.now();
            MedicationSchedule schedule = createSchedule(startDate, 7);

            // when
            int remainingDays = schedule.getRemainingDays();

            // then
            assertThat(remainingDays).isEqualTo(7);
        }
    }

    @Nested
    @DisplayName("진행률 계산")
    class GetProgressPercentage {

        @Test
        @DisplayName("성공: 시작 전이면 0%")
        void shouldReturnZeroBeforeStart() {
            // given - 내일 시작
            LocalDate startDate = LocalDate.now().plusDays(1);
            MedicationSchedule schedule = createSchedule(startDate, 10);

            // when
            int progress = schedule.getProgressPercentage();

            // then
            assertThat(progress).isZero();
        }

        @Test
        @DisplayName("성공: 완료되면 100%")
        void shouldReturn100WhenCompleted() {
            // given - 30일 전에 시작해서 7일간 복용 (이미 완료)
            LocalDate startDate = LocalDate.now().minusDays(30);
            MedicationSchedule schedule = createSchedule(startDate, 7);

            // when
            int progress = schedule.getProgressPercentage();

            // then
            assertThat(progress).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("복약 상태 확인")
    class MedicationStatus {

        @Test
        @DisplayName("성공: 시작일 이후면 hasStarted() true")
        void shouldReturnTrueWhenStarted() {
            // given
            LocalDate startDate = LocalDate.now().minusDays(1);
            MedicationSchedule schedule = createSchedule(startDate, 7);

            // when
            boolean started = schedule.hasStarted();

            // then
            assertThat(started).isTrue();
        }

        @Test
        @DisplayName("성공: 시작 전이면 hasStarted() false")
        void shouldReturnFalseBeforeStart() {
            // given
            LocalDate startDate = LocalDate.now().plusDays(1);
            MedicationSchedule schedule = createSchedule(startDate, 7);

            // when
            boolean started = schedule.hasStarted();

            // then
            assertThat(started).isFalse();
        }

        @Test
        @DisplayName("성공: 활성화된 진행 중인 스케줄은 isOngoing() true")
        void shouldReturnTrueWhenOngoing() {
            // given
            LocalDate startDate = LocalDate.now();
            MedicationSchedule schedule = createSchedule(startDate, 7);

            // when
            boolean ongoing = schedule.isOngoing();

            // then
            assertThat(ongoing).isTrue();
        }
    }

    @Nested
    @DisplayName("총 복용 횟수 계산")
    class GetTotalDosageCount {

        @Test
        @DisplayName("성공: 7일 3회/일 = 21회")
        void shouldCalculateCorrectTotalDosage() {
            // given
            LocalDate startDate = LocalDate.now();
            MedicationSchedule schedule = createSchedule(startDate, 7);

            // when
            int totalDosage = schedule.getTotalDosageCount();

            // then
            assertThat(totalDosage).isEqualTo(21);
        }
    }
}
