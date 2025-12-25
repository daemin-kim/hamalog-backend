package com.Hamalog.service.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.Hamalog.domain.medication.MedicationSchedule;
import com.Hamalog.domain.member.Member;
import com.Hamalog.repository.diary.MoodDiaryRepository;
import com.Hamalog.repository.medication.MedicationRecordRepository;
import com.Hamalog.repository.medication.MedicationScheduleRepository;
import com.Hamalog.repository.member.MemberRepository;
import com.Hamalog.repository.sideEffect.SideEffectRecordRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("회원 삭제 예약 스케줄러 테스트")
class MemberDeletionSchedulerTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private MedicationScheduleRepository medicationScheduleRepository;
    @Mock
    private MedicationRecordRepository medicationRecordRepository;
    @Mock
    private SideEffectRecordRepository sideEffectRecordRepository;
    @Mock
    private MoodDiaryRepository moodDiaryRepository;

    @InjectMocks
    private MemberDeletionSchedulerService memberDeletionScheduler;

    @Test
    @DisplayName("만기 회원을 정리한다")
    void purgeScheduledMembers_success() {
        Member member = Member.builder()
                .memberId(1L)
                .loginId("user@example.com")
                .deletionScheduled(true)
                .deletionDueAt(LocalDateTime.now().minusDays(1))
                .build();

        MedicationSchedule schedule = mock(MedicationSchedule.class);
        when(schedule.getMedicationScheduleId()).thenReturn(10L);

        when(memberRepository.findAllByDeletionScheduledTrueAndDeletionDueAtBefore(any()))
                .thenReturn(List.of(member));
        when(medicationScheduleRepository.findAllByMember_MemberId(eq(1L)))
                .thenReturn(List.of(schedule));

        memberDeletionScheduler.purgeScheduledMembers();

        verify(sideEffectRecordRepository).deleteByMemberId(1L);
        verify(moodDiaryRepository).deleteByMember_MemberId(1L);
        verify(medicationRecordRepository).deleteByScheduleIds(List.of(10L));
        verify(medicationScheduleRepository).deleteByMemberId(1L);
        verify(memberRepository).delete(member);
    }

    @Test
    @DisplayName("만기 대상이 없으면 아무 것도 하지 않는다")
    void purgeScheduledMembers_noTargets() {
        when(memberRepository.findAllByDeletionScheduledTrueAndDeletionDueAtBefore(any()))
                .thenReturn(Collections.emptyList());

        memberDeletionScheduler.purgeScheduledMembers();

        verify(medicationScheduleRepository, never()).findAllByMember_MemberId(any());
        verify(memberRepository, never()).delete(any(Member.class));
        verify(medicationScheduleRepository, never()).deleteByMemberId(any());
        verify(sideEffectRecordRepository, never()).deleteByMemberId(any());
        verify(moodDiaryRepository, never()).deleteByMember_MemberId(any());
        verify(medicationRecordRepository, never()).deleteByScheduleIds(any());
    }
}
