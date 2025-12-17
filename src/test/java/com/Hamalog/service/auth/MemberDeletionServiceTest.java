package com.Hamalog.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.Hamalog.domain.events.member.MemberDeletedEvent;
import com.Hamalog.domain.medication.MedicationSchedule;
import com.Hamalog.domain.member.Member;
import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.repository.diary.MoodDiaryRepository;
import com.Hamalog.repository.medication.MedicationRecordRepository;
import com.Hamalog.repository.medication.MedicationScheduleRepository;
import com.Hamalog.repository.member.MemberRepository;
import com.Hamalog.repository.sideEffect.SideEffectRecordRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberDeletionService 테스트")
class MemberDeletionServiceTest {

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

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private MemberCacheService memberCacheService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private MemberDeletionService memberDeletionService;

    private Member testMember;

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
            .memberId(1L)
            .loginId("test@example.com")
            .password("encodedPassword")
            .name("테스트유저")
            .nickName("테스터")
            .phoneNumber("01012345678")
            .birth(LocalDate.of(1990, 1, 1))
            .createdAt(LocalDateTime.now())
            .build();
    }

    @Nested
    @DisplayName("회원 탈퇴 테스트")
    class DeleteMemberTest {

        @Test
        @DisplayName("회원 탈퇴 성공 - 모든 관련 데이터 삭제 및 이벤트 발행")
        void deleteMember_Success() {
            // given
            String loginId = "test@example.com";
            String token = "valid-jwt-token";

            MedicationSchedule schedule1 = mock(MedicationSchedule.class);
            MedicationSchedule schedule2 = mock(MedicationSchedule.class);
            given(schedule1.getMedicationScheduleId()).willReturn(1L);
            given(schedule2.getMedicationScheduleId()).willReturn(2L);
            List<MedicationSchedule> schedules = Arrays.asList(schedule1, schedule2);

            given(authenticationService.isValidTokenFormat(token)).willReturn(true);
            given(memberRepository.findByLoginId(loginId)).willReturn(Optional.of(testMember));
            given(medicationScheduleRepository.findAllByMember_MemberId(testMember.getMemberId()))
                .willReturn(schedules);

            // when
            memberDeletionService.deleteMember(loginId, token);

            // then
            verify(authenticationService).blacklistToken(token);
            verify(sideEffectRecordRepository).deleteByMemberId(testMember.getMemberId());
            verify(moodDiaryRepository).deleteByMember_MemberId(testMember.getMemberId());
            verify(medicationRecordRepository).deleteByScheduleIds(Arrays.asList(1L, 2L));
            verify(medicationScheduleRepository).deleteByMemberId(testMember.getMemberId());
            verify(memberRepository).delete(testMember);
            verify(memberCacheService).evictByLoginId(loginId, testMember.getMemberId());
            verify(memberCacheService).evictByMemberId(testMember.getMemberId());

            ArgumentCaptor<MemberDeletedEvent> eventCaptor = ArgumentCaptor.forClass(MemberDeletedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            MemberDeletedEvent capturedEvent = eventCaptor.getValue();
            assertThat(capturedEvent.getLoginId()).isEqualTo(loginId);
            assertThat(capturedEvent.getToken()).isEqualTo(token);
            assertThat(capturedEvent.getMemberId()).isEqualTo(testMember.getMemberId());
        }

        @Test
        @DisplayName("회원 탈퇴 실패 - 존재하지 않는 회원")
        void deleteMember_MemberNotFound_ThrowsException() {
            // given
            String loginId = "nonexistent@example.com";
            String token = "valid-jwt-token";

            given(authenticationService.isValidTokenFormat(token)).willReturn(true);
            given(memberRepository.findByLoginId(loginId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberDeletionService.deleteMember(loginId, token))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MEMBER_NOT_FOUND.getMessage());

            verify(memberRepository, never()).delete(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("회원 탈퇴 성공 - null 토큰인 경우 블랙리스트 호출 안함")
        void deleteMember_NullToken_NotBlacklisted() {
            // given
            String loginId = "test@example.com";
            String token = null;

            given(authenticationService.isValidTokenFormat(token)).willReturn(false);
            given(memberRepository.findByLoginId(loginId)).willReturn(Optional.of(testMember));
            given(medicationScheduleRepository.findAllByMember_MemberId(anyLong()))
                .willReturn(Collections.emptyList());

            // when
            memberDeletionService.deleteMember(loginId, token);

            // then
            verify(authenticationService, never()).blacklistToken(anyString());
            verify(memberRepository).delete(testMember);
            verify(eventPublisher).publishEvent(any(MemberDeletedEvent.class));
        }

        @Test
        @DisplayName("회원 탈퇴 성공 - 복약 스케줄이 없는 경우")
        void deleteMember_NoMedicationSchedules_Success() {
            // given
            String loginId = "test@example.com";
            String token = "valid-jwt-token";

            given(authenticationService.isValidTokenFormat(token)).willReturn(true);
            given(memberRepository.findByLoginId(loginId)).willReturn(Optional.of(testMember));
            given(medicationScheduleRepository.findAllByMember_MemberId(testMember.getMemberId()))
                .willReturn(Collections.emptyList());

            // when
            memberDeletionService.deleteMember(loginId, token);

            // then
            verify(medicationRecordRepository, never()).deleteByScheduleIds(anyList());
            verify(medicationScheduleRepository).deleteByMemberId(testMember.getMemberId());
            verify(memberRepository).delete(testMember);
        }

        @Test
        @DisplayName("회원 탈퇴 성공 - 모든 관련 데이터가 순서대로 삭제된다")
        void deleteMember_DataDeletedInCorrectOrder() {
            // given
            String loginId = "test@example.com";
            String token = "valid-jwt-token";

            given(authenticationService.isValidTokenFormat(token)).willReturn(true);
            given(memberRepository.findByLoginId(loginId)).willReturn(Optional.of(testMember));
            given(medicationScheduleRepository.findAllByMember_MemberId(testMember.getMemberId()))
                .willReturn(Collections.emptyList());

            // when
            memberDeletionService.deleteMember(loginId, token);

            // then - 삭제 순서 검증
            var inOrder = inOrder(
                authenticationService,
                sideEffectRecordRepository,
                moodDiaryRepository,
                medicationScheduleRepository,
                memberRepository,
                eventPublisher
            );

            inOrder.verify(authenticationService).blacklistToken(token);
            inOrder.verify(sideEffectRecordRepository).deleteByMemberId(testMember.getMemberId());
            inOrder.verify(moodDiaryRepository).deleteByMember_MemberId(testMember.getMemberId());
            inOrder.verify(medicationScheduleRepository).deleteByMemberId(testMember.getMemberId());
            inOrder.verify(memberRepository).delete(testMember);
            inOrder.verify(eventPublisher).publishEvent(any(MemberDeletedEvent.class));
        }
    }

    @Nested
    @DisplayName("토큰 처리 테스트")
    class TokenHandlingTest {

        @Test
        @DisplayName("빈 토큰인 경우 블랙리스트 호출 안함")
        void deleteMember_BlankToken_NotBlacklisted() {
            // given
            String loginId = "test@example.com";
            String token = "   ";

            given(authenticationService.isValidTokenFormat(token)).willReturn(false);
            given(memberRepository.findByLoginId(loginId)).willReturn(Optional.of(testMember));
            given(medicationScheduleRepository.findAllByMember_MemberId(anyLong()))
                .willReturn(Collections.emptyList());

            // when
            memberDeletionService.deleteMember(loginId, token);

            // then
            verify(authenticationService, never()).blacklistToken(anyString());
        }

        @Test
        @DisplayName("빈 문자열 토큰인 경우 블랙리스트 호출 안함")
        void deleteMember_EmptyToken_NotBlacklisted() {
            // given
            String loginId = "test@example.com";
            String token = "";

            given(authenticationService.isValidTokenFormat(token)).willReturn(false);
            given(memberRepository.findByLoginId(loginId)).willReturn(Optional.of(testMember));
            given(medicationScheduleRepository.findAllByMember_MemberId(anyLong()))
                .willReturn(Collections.emptyList());

            // when
            memberDeletionService.deleteMember(loginId, token);

            // then
            verify(authenticationService, never()).blacklistToken(anyString());
        }
    }

    @Nested
    @DisplayName("이벤트 발행 테스트")
    class EventPublishingTest {

        @Test
        @DisplayName("회원 탈퇴 시 MemberDeletedEvent가 올바른 정보로 발행된다")
        void deleteMember_EventPublishedWithCorrectInfo() {
            // given
            String loginId = "test@example.com";
            String token = "valid-jwt-token";

            given(authenticationService.isValidTokenFormat(token)).willReturn(true);
            given(memberRepository.findByLoginId(loginId)).willReturn(Optional.of(testMember));
            given(medicationScheduleRepository.findAllByMember_MemberId(anyLong()))
                .willReturn(Collections.emptyList());

            // when
            memberDeletionService.deleteMember(loginId, token);

            // then
            ArgumentCaptor<MemberDeletedEvent> eventCaptor = ArgumentCaptor.forClass(MemberDeletedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            MemberDeletedEvent event = eventCaptor.getValue();
            assertThat(event.getLoginId()).isEqualTo(loginId);
            assertThat(event.getToken()).isEqualTo(token);
            assertThat(event.getMemberId()).isEqualTo(testMember.getMemberId());
        }

        @Test
        @DisplayName("회원 탈퇴 시 null 토큰도 이벤트에 포함된다")
        void deleteMember_NullTokenIncludedInEvent() {
            // given
            String loginId = "test@example.com";
            String token = null;

            given(authenticationService.isValidTokenFormat(token)).willReturn(false);
            given(memberRepository.findByLoginId(loginId)).willReturn(Optional.of(testMember));
            given(medicationScheduleRepository.findAllByMember_MemberId(anyLong()))
                .willReturn(Collections.emptyList());

            // when
            memberDeletionService.deleteMember(loginId, token);

            // then
            ArgumentCaptor<MemberDeletedEvent> eventCaptor = ArgumentCaptor.forClass(MemberDeletedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            MemberDeletedEvent event = eventCaptor.getValue();
            assertThat(event.getToken()).isNull();
        }
    }
}
