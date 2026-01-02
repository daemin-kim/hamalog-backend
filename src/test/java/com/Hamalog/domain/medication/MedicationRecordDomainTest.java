package com.Hamalog.domain.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * MedicationRecord 도메인 로직 테스트
 * Entity에 추가된 비즈니스 로직 검증
 */
@DisplayName("MedicationRecord 도메인 로직 테스트")
class MedicationRecordDomainTest {

    private MedicationSchedule mockSchedule;
    private MedicationTime mockTime;

    @BeforeEach
    void setUp() {
        mockSchedule = mock(MedicationSchedule.class);
        mockTime = mock(MedicationTime.class);
    }

    private MedicationRecord createRecord(Boolean isTaken, LocalDateTime realTakeTime) {
        return new MedicationRecord(mockSchedule, mockTime, isTaken, realTakeTime);
    }

    @Nested
    @DisplayName("복용 상태 확인")
    class TakeStatus {

        @Test
        @DisplayName("성공: 복용 완료 상태 확인")
        void shouldReturnTrueWhenTaken() {
            // given
            MedicationRecord record = createRecord(true, LocalDateTime.now());

            // when & then
            assertThat(record.isTaken()).isTrue();
            assertThat(record.isSkipped()).isFalse();
        }

        @Test
        @DisplayName("성공: 복용 미완료 상태 확인")
        void shouldReturnTrueWhenSkipped() {
            // given
            MedicationRecord record = createRecord(false, null);

            // when & then
            assertThat(record.isTaken()).isFalse();
            assertThat(record.isSkipped()).isTrue();
        }
    }

    @Nested
    @DisplayName("지연 복용 판단")
    class DelayedStatus {

        @Test
        @DisplayName("성공: 30분 이상 늦으면 지연 복용")
        void shouldReturnTrueWhenDelayed() {
            // given - 예정 시간 09:00, 실제 복용 10:00 (60분 지연)
            when(mockTime.getTakeTime()).thenReturn(LocalTime.of(9, 0));
            LocalDateTime realTakeTime = LocalDateTime.now().withHour(10).withMinute(0);
            MedicationRecord record = createRecord(true, realTakeTime);

            // when
            boolean delayed = record.isDelayed();

            // then
            assertThat(delayed).isTrue();
        }

        @Test
        @DisplayName("성공: 30분 이내면 정시 복용")
        void shouldReturnFalseWhenOnTime() {
            // given - 예정 시간 09:00, 실제 복용 09:15 (15분 지연)
            when(mockTime.getTakeTime()).thenReturn(LocalTime.of(9, 0));
            LocalDateTime realTakeTime = LocalDateTime.now().withHour(9).withMinute(15);
            MedicationRecord record = createRecord(true, realTakeTime);

            // when
            boolean delayed = record.isDelayed();

            // then
            assertThat(delayed).isFalse();
        }

        @Test
        @DisplayName("성공: 복용하지 않았으면 지연 아님")
        void shouldReturnFalseWhenNotTaken() {
            // given
            MedicationRecord record = createRecord(false, null);

            // when
            boolean delayed = record.isDelayed();

            // then
            assertThat(delayed).isFalse();
        }
    }

    @Nested
    @DisplayName("조기 복용 판단")
    class EarlyStatus {

        @Test
        @DisplayName("성공: 30분 이상 일찍이면 조기 복용")
        void shouldReturnTrueWhenEarly() {
            // given - 예정 시간 10:00, 실제 복용 09:00 (60분 조기)
            when(mockTime.getTakeTime()).thenReturn(LocalTime.of(10, 0));
            LocalDateTime realTakeTime = LocalDateTime.now().withHour(9).withMinute(0);
            MedicationRecord record = createRecord(true, realTakeTime);

            // when
            boolean early = record.isEarly();

            // then
            assertThat(early).isTrue();
        }
    }

    @Nested
    @DisplayName("정시 복용 판단")
    class OnTimeStatus {

        @Test
        @DisplayName("성공: 예정 시간 전후 30분 이내면 정시")
        void shouldReturnTrueWhenOnTime() {
            // given - 예정 시간 09:00, 실제 복용 09:10 (10분 지연)
            when(mockTime.getTakeTime()).thenReturn(LocalTime.of(9, 0));
            LocalDateTime realTakeTime = LocalDateTime.now().withHour(9).withMinute(10);
            MedicationRecord record = createRecord(true, realTakeTime);

            // when
            boolean onTime = record.isOnTime();

            // then
            assertThat(onTime).isTrue();
        }
    }

    @Nested
    @DisplayName("시간 차이 계산")
    class TimeDifference {

        @Test
        @DisplayName("성공: 지연 시 양수 반환")
        void shouldReturnPositiveWhenDelayed() {
            // given - 예정 시간 09:00, 실제 복용 09:30 (30분 지연)
            when(mockTime.getTakeTime()).thenReturn(LocalTime.of(9, 0));
            LocalDateTime realTakeTime = LocalDateTime.now().withHour(9).withMinute(30);
            MedicationRecord record = createRecord(true, realTakeTime);

            // when
            long diff = record.getTimeDifferenceMinutes();

            // then
            assertThat(diff).isEqualTo(30);
        }

        @Test
        @DisplayName("성공: 조기 시 음수 반환")
        void shouldReturnNegativeWhenEarly() {
            // given - 예정 시간 09:30, 실제 복용 09:00 (30분 조기)
            when(mockTime.getTakeTime()).thenReturn(LocalTime.of(9, 30));
            LocalDateTime realTakeTime = LocalDateTime.now().withHour(9).withMinute(0);
            MedicationRecord record = createRecord(true, realTakeTime);

            // when
            long diff = record.getTimeDifferenceMinutes();

            // then - Duration.between(scheduled, actual) 이므로 음수가 반환됨
            assertThat(diff).isNegative();
        }
    }

    @Nested
    @DisplayName("상태 변경 메서드")
    class StatusChange {

        @Test
        @DisplayName("성공: markAsTaken으로 복용 완료 처리")
        void shouldMarkAsTaken() {
            // given
            MedicationRecord record = createRecord(false, null);
            LocalDateTime takeTime = LocalDateTime.now();

            // when
            record.markAsTaken(takeTime);

            // then
            assertThat(record.isTaken()).isTrue();
            assertThat(record.getRealTakeTime()).isEqualTo(takeTime);
        }

        @Test
        @DisplayName("성공: markAsSkipped로 복용 미완료 처리")
        void shouldMarkAsSkipped() {
            // given
            MedicationRecord record = createRecord(true, LocalDateTime.now());

            // when
            record.markAsSkipped();

            // then
            assertThat(record.isSkipped()).isTrue();
            assertThat(record.getRealTakeTime()).isNull();
        }
    }
}
