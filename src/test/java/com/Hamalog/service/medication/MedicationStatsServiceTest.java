package com.Hamalog.service.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.Hamalog.domain.medication.MedicationRecord;
import com.Hamalog.domain.medication.MedicationSchedule;
import com.Hamalog.dto.medication.response.MedicationAdherenceResponse;
import com.Hamalog.dto.medication.response.MedicationSummaryResponse;
import com.Hamalog.exception.member.MemberNotFoundException;
import com.Hamalog.repository.medication.MedicationRecordRepository;
import com.Hamalog.repository.medication.MedicationScheduleRepository;
import com.Hamalog.repository.member.MemberRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("MedicationStatsService 테스트")
class MedicationStatsServiceTest {

    @Mock
    private MedicationRecordRepository medicationRecordRepository;

    @Mock
    private MedicationScheduleRepository medicationScheduleRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MedicationStatsService medicationStatsService;

    private MedicationRecord mockRecord;
    private MedicationSchedule mockSchedule;

    @BeforeEach
    void setUp() {
        // Mock Schedule 설정
        mockSchedule = mock(MedicationSchedule.class);
        lenient().when(mockSchedule.getMedicationScheduleId()).thenReturn(1L);
        lenient().when(mockSchedule.getName()).thenReturn("테스트 약");

        // Mock Record 설정
        mockRecord = mock(MedicationRecord.class);
        lenient().when(mockRecord.getMedicationRecordId()).thenReturn(1L);
        lenient().when(mockRecord.getIsTakeMedication()).thenReturn(true);
        lenient().when(mockRecord.getRealTakeTime()).thenReturn(LocalDateTime.now());
    }

    @Nested
    @DisplayName("복약 이행률 조회")
    class GetAdherence {

        @Test
        @DisplayName("성공: 기간 내 복약 이행률 조회")
        void success() {
            // given
            Long memberId = 1L;
            LocalDate startDate = LocalDate.of(2025, 1, 1);
            LocalDate endDate = LocalDate.of(2025, 1, 7);

            when(memberRepository.existsById(memberId)).thenReturn(true);
            when(medicationRecordRepository.findByMemberIdAndDateRange(eq(memberId), any(), any()))
                    .thenReturn(Arrays.asList(mockRecord));

            // when
            MedicationAdherenceResponse result = medicationStatsService.getAdherence(memberId, startDate, endDate);

            // then
            assertThat(result).isNotNull();
            assertThat(result.startDate()).isEqualTo(startDate);
            assertThat(result.endDate()).isEqualTo(endDate);
            assertThat(result.dailyStats()).isNotNull();
            verify(medicationRecordRepository).findByMemberIdAndDateRange(eq(memberId), any(), any());
        }

        @Test
        @DisplayName("성공: 복약 기록이 없는 경우")
        void success_noRecords() {
            // given
            Long memberId = 1L;
            LocalDate startDate = LocalDate.of(2025, 1, 1);
            LocalDate endDate = LocalDate.of(2025, 1, 7);

            when(memberRepository.existsById(memberId)).thenReturn(true);
            when(medicationRecordRepository.findByMemberIdAndDateRange(eq(memberId), any(), any()))
                    .thenReturn(List.of());

            // when
            MedicationAdherenceResponse result = medicationStatsService.getAdherence(memberId, startDate, endDate);

            // then
            assertThat(result).isNotNull();
            assertThat(result.totalScheduled()).isEqualTo(0);
            assertThat(result.totalTaken()).isEqualTo(0);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 회원")
        void fail_memberNotFound() {
            // given
            Long memberId = 999L;
            LocalDate startDate = LocalDate.of(2025, 1, 1);
            LocalDate endDate = LocalDate.of(2025, 1, 7);

            when(memberRepository.existsById(memberId)).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> medicationStatsService.getAdherence(memberId, startDate, endDate))
                    .isInstanceOf(MemberNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("복약 현황 요약 조회")
    class GetSummary {

        @Test
        @DisplayName("성공: 복약 현황 요약 조회")
        void success() {
            // given
            Long memberId = 1L;

            when(memberRepository.existsById(memberId)).thenReturn(true);
            when(medicationScheduleRepository.findAllByMember_MemberId(memberId))
                    .thenReturn(Arrays.asList(mockSchedule));
            when(medicationRecordRepository.countByMemberIdAndDateRange(eq(memberId), any(), any()))
                    .thenReturn(3L);
            when(medicationRecordRepository.countTakenByMemberIdAndDateRange(eq(memberId), any(), any()))
                    .thenReturn(2L);
            when(medicationRecordRepository.getScheduleStatsByMemberId(memberId))
                    .thenReturn(java.util.Collections.singletonList(new Object[]{1L, 10L, 8L}));

            // when
            MedicationSummaryResponse result = medicationStatsService.getSummary(memberId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.totalActiveSchedules()).isEqualTo(1);
            verify(medicationScheduleRepository).findAllByMember_MemberId(memberId);
        }

        @Test
        @DisplayName("성공: 활성 스케줄이 없는 경우")
        void success_noSchedules() {
            // given
            Long memberId = 1L;

            when(memberRepository.existsById(memberId)).thenReturn(true);
            when(medicationScheduleRepository.findAllByMember_MemberId(memberId))
                    .thenReturn(List.of());
            when(medicationRecordRepository.countByMemberIdAndDateRange(eq(memberId), any(), any()))
                    .thenReturn(0L);
            when(medicationRecordRepository.countTakenByMemberIdAndDateRange(eq(memberId), any(), any()))
                    .thenReturn(0L);
            when(medicationRecordRepository.getScheduleStatsByMemberId(memberId))
                    .thenReturn(List.of());

            // when
            MedicationSummaryResponse result = medicationStatsService.getSummary(memberId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.totalActiveSchedules()).isEqualTo(0);
            assertThat(result.scheduleSummaries()).isEmpty();
        }

        @Test
        @DisplayName("실패: 존재하지 않는 회원")
        void fail_memberNotFound() {
            // given
            Long memberId = 999L;

            when(memberRepository.existsById(memberId)).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> medicationStatsService.getSummary(memberId))
                    .isInstanceOf(MemberNotFoundException.class);
        }
    }
}
