package com.Hamalog.service.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.Hamalog.domain.medication.MedicationSchedule;
import com.Hamalog.domain.medication.MedicationTime;
import com.Hamalog.domain.member.Member;
import com.Hamalog.dto.medication.request.MedicationTimeCreateRequest;
import com.Hamalog.dto.medication.request.MedicationTimeUpdateRequest;
import com.Hamalog.dto.medication.response.MedicationTimeResponse;
import com.Hamalog.exception.medication.MedicationScheduleNotFoundException;
import com.Hamalog.exception.medication.MedicationTimeNotFoundException;
import com.Hamalog.repository.medication.MedicationScheduleRepository;
import com.Hamalog.repository.medication.MedicationTimeRepository;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("MedicationTimeService 테스트")
class MedicationTimeServiceTest {

    @Mock
    private MedicationTimeRepository medicationTimeRepository;

    @Mock
    private MedicationScheduleRepository medicationScheduleRepository;

    @InjectMocks
    private MedicationTimeService medicationTimeService;

    private Member mockMember;
    private MedicationSchedule mockSchedule;
    private MedicationTime mockTime;

    @BeforeEach
    void setUp() {
        // Mock Member 설정
        mockMember = mock(Member.class);
        lenient().when(mockMember.getMemberId()).thenReturn(1L);

        // Mock Schedule 설정
        mockSchedule = mock(MedicationSchedule.class);
        lenient().when(mockSchedule.getMedicationScheduleId()).thenReturn(1L);
        lenient().when(mockSchedule.getMember()).thenReturn(mockMember);

        // Mock Time 설정
        mockTime = mock(MedicationTime.class);
        lenient().when(mockTime.getMedicationTimeId()).thenReturn(1L);
        lenient().when(mockTime.getMedicationSchedule()).thenReturn(mockSchedule);
        lenient().when(mockTime.getTakeTime()).thenReturn(LocalTime.of(9, 0));
    }

    @Nested
    @DisplayName("스케줄별 알림 시간 목록 조회")
    class GetMedicationTimes {

        @Test
        @DisplayName("성공: 유효한 스케줄 ID로 조회")
        void success() {
            // given
            Long scheduleId = 1L;
            MedicationTime time2 = mock(MedicationTime.class);
            when(time2.getMedicationTimeId()).thenReturn(2L);
            when(time2.getMedicationSchedule()).thenReturn(mockSchedule);
            when(time2.getTakeTime()).thenReturn(LocalTime.of(18, 0));

            when(medicationScheduleRepository.existsById(scheduleId)).thenReturn(true);
            when(medicationTimeRepository.findByMedicationSchedule_MedicationScheduleIdOrderByTakeTimeAsc(scheduleId))
                    .thenReturn(Arrays.asList(mockTime, time2));

            // when
            List<MedicationTimeResponse> result = medicationTimeService.getMedicationTimes(scheduleId);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).takeTime()).isEqualTo(LocalTime.of(9, 0));
            assertThat(result.get(1).takeTime()).isEqualTo(LocalTime.of(18, 0));
            verify(medicationScheduleRepository).existsById(scheduleId);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 스케줄 ID")
        void fail_scheduleNotFound() {
            // given
            Long scheduleId = 999L;
            when(medicationScheduleRepository.existsById(scheduleId)).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> medicationTimeService.getMedicationTimes(scheduleId))
                    .isInstanceOf(MedicationScheduleNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("알림 시간 단건 조회")
    class GetMedicationTime {

        @Test
        @DisplayName("성공: 유효한 시간 ID로 조회")
        void success() {
            // given
            Long timeId = 1L;
            when(medicationTimeRepository.findByIdWithScheduleAndMember(timeId))
                    .thenReturn(Optional.of(mockTime));

            // when
            MedicationTimeResponse result = medicationTimeService.getMedicationTime(timeId);

            // then
            assertThat(result.medicationTimeId()).isEqualTo(1L);
            assertThat(result.takeTime()).isEqualTo(LocalTime.of(9, 0));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 시간 ID")
        void fail_timeNotFound() {
            // given
            Long timeId = 999L;
            when(medicationTimeRepository.findByIdWithScheduleAndMember(timeId))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> medicationTimeService.getMedicationTime(timeId))
                    .isInstanceOf(MedicationTimeNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("알림 시간 생성")
    class CreateMedicationTime {

        @Test
        @DisplayName("성공: 유효한 요청으로 생성")
        void success() {
            // given
            Long scheduleId = 1L;
            MedicationTimeCreateRequest request = new MedicationTimeCreateRequest(LocalTime.of(12, 30));

            MedicationTime savedTime = mock(MedicationTime.class);
            when(savedTime.getMedicationTimeId()).thenReturn(2L);
            when(savedTime.getMedicationSchedule()).thenReturn(mockSchedule);
            when(savedTime.getTakeTime()).thenReturn(LocalTime.of(12, 30));

            when(medicationScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(mockSchedule));
            when(medicationTimeRepository.save(any(MedicationTime.class))).thenReturn(savedTime);

            // when
            MedicationTimeResponse result = medicationTimeService.createMedicationTime(scheduleId, request);

            // then
            assertThat(result.medicationTimeId()).isEqualTo(2L);
            assertThat(result.takeTime()).isEqualTo(LocalTime.of(12, 30));
            verify(medicationTimeRepository).save(any(MedicationTime.class));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 스케줄")
        void fail_scheduleNotFound() {
            // given
            Long scheduleId = 999L;
            MedicationTimeCreateRequest request = new MedicationTimeCreateRequest(LocalTime.of(12, 30));

            when(medicationScheduleRepository.findById(scheduleId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> medicationTimeService.createMedicationTime(scheduleId, request))
                    .isInstanceOf(MedicationScheduleNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("알림 시간 수정")
    class UpdateMedicationTime {

        @Test
        @DisplayName("성공: 유효한 요청으로 수정")
        void success() {
            // given
            Long timeId = 1L;
            MedicationTimeUpdateRequest request = new MedicationTimeUpdateRequest(LocalTime.of(14, 0));

            when(medicationTimeRepository.findByIdWithScheduleAndMember(timeId))
                    .thenReturn(Optional.of(mockTime));
            when(mockTime.getTakeTime()).thenReturn(LocalTime.of(14, 0));

            // when
            MedicationTimeResponse result = medicationTimeService.updateMedicationTime(timeId, request);

            // then
            assertThat(result).isNotNull();
            verify(mockTime).updateTime(LocalTime.of(14, 0));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 시간 ID")
        void fail_timeNotFound() {
            // given
            Long timeId = 999L;
            MedicationTimeUpdateRequest request = new MedicationTimeUpdateRequest(LocalTime.of(14, 0));

            when(medicationTimeRepository.findByIdWithScheduleAndMember(timeId))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> medicationTimeService.updateMedicationTime(timeId, request))
                    .isInstanceOf(MedicationTimeNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("알림 시간 삭제")
    class DeleteMedicationTime {

        @Test
        @DisplayName("성공: 유효한 시간 ID로 삭제")
        void success() {
            // given
            Long timeId = 1L;
            when(medicationTimeRepository.existsById(timeId)).thenReturn(true);

            // when
            medicationTimeService.deleteMedicationTime(timeId);

            // then
            verify(medicationTimeRepository).deleteById(timeId);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 시간 ID")
        void fail_timeNotFound() {
            // given
            Long timeId = 999L;
            when(medicationTimeRepository.existsById(timeId)).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> medicationTimeService.deleteMedicationTime(timeId))
                    .isInstanceOf(MedicationTimeNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("알림 시간 소유권 확인")
    class IsOwnedByMember {

        @Test
        @DisplayName("성공: 본인 소유")
        void success_owned() {
            // given
            Long timeId = 1L;
            Long memberId = 1L;
            when(medicationTimeRepository.existsByIdAndMemberId(timeId, memberId)).thenReturn(true);

            // when
            boolean result = medicationTimeService.isOwnedByMember(timeId, memberId);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("실패: 타인 소유")
        void fail_notOwned() {
            // given
            Long timeId = 1L;
            Long memberId = 2L;
            when(medicationTimeRepository.existsByIdAndMemberId(timeId, memberId)).thenReturn(false);

            // when
            boolean result = medicationTimeService.isOwnedByMember(timeId, memberId);

            // then
            assertThat(result).isFalse();
        }
    }
}
