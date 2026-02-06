package com.Hamalog.service.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("회원 탈퇴 스케줄러 서비스 테스트")
class MemberDeletionSchedulerServiceTest {

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
    private MemberDeletionSchedulerService memberDeletionSchedulerService;

    @Nested
    @DisplayName("purgeScheduledMembers")
    class PurgeScheduledMembers {

        @Test
        @DisplayName("성공: 삭제 대상 회원이 없는 경우")
        void success_noTargetMembers() {
            // given
            when(memberRepository.findAllByDeletionScheduledTrueAndDeletionDueAtBefore(any(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());

            // when
            memberDeletionSchedulerService.purgeScheduledMembers();

            // then
            verify(memberRepository).findAllByDeletionScheduledTrueAndDeletionDueAtBefore(any(LocalDateTime.class));
            verify(memberRepository, never()).delete(any());
        }

        @Test
        @DisplayName("성공: 삭제 대상 회원이 있는 경우 - 관련 데이터 삭제 후 회원 삭제")
        void success_withTargetMembers() {
            // given
            Member member = createTestMember(1L, "test@test.com");

            when(memberRepository.findAllByDeletionScheduledTrueAndDeletionDueAtBefore(any(LocalDateTime.class)))
                    .thenReturn(List.of(member));
            when(medicationScheduleRepository.findAllByMember_MemberId(1L))
                    .thenReturn(Collections.emptyList());

            // when
            memberDeletionSchedulerService.purgeScheduledMembers();

            // then
            verify(sideEffectRecordRepository).deleteByMemberId(1L);
            verify(moodDiaryRepository).deleteByMember_MemberId(1L);
            verify(medicationScheduleRepository).findAllByMember_MemberId(1L);
            verify(medicationScheduleRepository).deleteByMemberId(1L);
            verify(memberRepository).delete(member);
        }

        @Test
        @DisplayName("성공: 복약 스케줄이 있는 경우 - 복약 기록도 삭제")
        void success_withMedicationSchedules() {
            // given
            Member member = createTestMember(1L, "test@test.com");
            MedicationSchedule schedule = mock(MedicationSchedule.class);
            when(schedule.getMedicationScheduleId()).thenReturn(100L);

            when(memberRepository.findAllByDeletionScheduledTrueAndDeletionDueAtBefore(any(LocalDateTime.class)))
                    .thenReturn(List.of(member));
            when(medicationScheduleRepository.findAllByMember_MemberId(1L))
                    .thenReturn(List.of(schedule));

            // when
            memberDeletionSchedulerService.purgeScheduledMembers();

            // then
            verify(medicationRecordRepository).deleteByScheduleIds(List.of(100L));
            verify(memberRepository).delete(member);
        }

        @Test
        @DisplayName("실패: 삭제 중 예외 발생 시 다음 회원 처리 계속")
        void failure_exceptionDuringDeletion() {
            // given
            Member member1 = createTestMember(1L, "test1@test.com");
            Member member2 = createTestMember(2L, "test2@test.com");

            when(memberRepository.findAllByDeletionScheduledTrueAndDeletionDueAtBefore(any(LocalDateTime.class)))
                    .thenReturn(List.of(member1, member2));
            // 첫 번째 회원 삭제 시 예외 발생
            doThrow(new RuntimeException("DB Error"))
                    .when(sideEffectRecordRepository).deleteByMemberId(1L);
            // 두 번째 회원은 정상 처리
            when(medicationScheduleRepository.findAllByMember_MemberId(2L))
                    .thenReturn(Collections.emptyList());

            // when
            memberDeletionSchedulerService.purgeScheduledMembers();

            // then - 두 번째 회원은 정상 삭제됨
            verify(sideEffectRecordRepository).deleteByMemberId(2L);
            verify(memberRepository).delete(member2);
        }
    }

    private Member createTestMember(Long memberId, String loginId) {
        Member member = Member.builder()
                .loginId(loginId)
                .password("password123")
                .name("테스트")
                .nickName("테스트")
                .phoneNumber("01012345678")
                .build();
        ReflectionTestUtils.setField(member, "memberId", memberId);
        return member;
    }
}
