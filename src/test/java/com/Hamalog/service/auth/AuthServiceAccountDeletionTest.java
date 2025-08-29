package com.Hamalog.service.auth;

import com.Hamalog.domain.member.Member;
import com.Hamalog.domain.medication.MedicationSchedule;
import com.Hamalog.domain.medication.MedicationRecord;
import com.Hamalog.domain.medication.MedicationTime;
import com.Hamalog.domain.medication.AlarmType;
import com.Hamalog.domain.sideEffect.SideEffectRecord;
import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.repository.member.MemberRepository;
import com.Hamalog.repository.medication.MedicationRecordRepository;
import com.Hamalog.repository.medication.MedicationScheduleRepository;
import com.Hamalog.repository.sideEffect.SideEffectRecordRepository;
import com.Hamalog.security.jwt.JwtTokenProvider;
import com.Hamalog.security.jwt.TokenBlacklistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Account Deletion Tests")
class AuthServiceAccountDeletionTest {

    @Mock
    private MemberRepository memberRepository;
    
    @Mock
    private MedicationScheduleRepository medicationScheduleRepository;
    
    @Mock
    private MedicationRecordRepository medicationRecordRepository;
    
    @Mock
    private SideEffectRecordRepository sideEffectRecordRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    
    @Mock
    private TokenBlacklistService tokenBlacklistService;
    
    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("회원 탈퇴 성공 - 모든 관련 데이터 삭제 및 토큰 무효화")
    void deleteMember_Success() {
        // Given
        String loginId = "test@example.com";
        String token = "valid-jwt-token";
        Long memberId = 1L;
        
        Member member = Member.builder()
                .memberId(memberId)
                .loginId(loginId)
                .password("password")
                .name("Test User")
                .phoneNumber("01012345678")
                .nickName("TestNick")
                .birth(LocalDate.of(1990, 1, 1))
                .createdAt(LocalDateTime.now())
                .build();

        MedicationSchedule schedule = new MedicationSchedule(
                member, "Test Medicine", "Test Hospital", LocalDate.now(),
                "Test memo", LocalDate.now(), 7, 2, AlarmType.SOUND
        );

        SideEffectRecord sideEffectRecord = SideEffectRecord.builder()
                .member(member)
                .createdAt(LocalDateTime.now())
                .build();

        List<SideEffectRecord> sideEffectRecords = Arrays.asList(sideEffectRecord);
        List<MedicationSchedule> schedules = Arrays.asList(schedule);
        List<MedicationRecord> records = Arrays.asList();

        given(memberRepository.findByLoginId(loginId)).willReturn(Optional.of(member));
        given(sideEffectRecordRepository.findAll()).willReturn(sideEffectRecords);
        given(medicationScheduleRepository.findAllByMember_MemberId(memberId)).willReturn(schedules);
        given(medicationRecordRepository.findAllByMedicationSchedule_MedicationScheduleId(any()))
                .willReturn(records);

        // When
        authService.deleteMember(loginId, token);

        // Then
        verify(memberRepository).findByLoginId(loginId);
        verify(sideEffectRecordRepository).delete(sideEffectRecord);
        verify(medicationScheduleRepository).delete(schedule);
        verify(memberRepository).delete(member);
        verify(tokenBlacklistService).blacklistToken(token);
    }

    @Test
    @DisplayName("회원 탈퇴 실패 - 존재하지 않는 회원")
    void deleteMember_MemberNotFound() {
        // Given
        String loginId = "nonexistent@example.com";
        String token = "valid-jwt-token";
        
        given(memberRepository.findByLoginId(loginId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.deleteMember(loginId, token))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MEMBER_NOT_FOUND.getMessage());

        verify(memberRepository).findByLoginId(loginId);
        verify(memberRepository, never()).delete(any());
        verify(tokenBlacklistService, never()).blacklistToken(any());
    }

    @Test
    @DisplayName("회원 탈퇴 성공 - 토큰이 null인 경우")
    void deleteMember_Success_NullToken() {
        // Given
        String loginId = "test@example.com";
        String token = null;
        Long memberId = 1L;
        
        Member member = Member.builder()
                .memberId(memberId)
                .loginId(loginId)
                .password("password")
                .name("Test User")
                .phoneNumber("01012345678")
                .nickName("TestNick")
                .birth(LocalDate.of(1990, 1, 1))
                .createdAt(LocalDateTime.now())
                .build();

        given(memberRepository.findByLoginId(loginId)).willReturn(Optional.of(member));
        given(sideEffectRecordRepository.findAll()).willReturn(Arrays.asList());
        given(medicationScheduleRepository.findAllByMember_MemberId(memberId)).willReturn(Arrays.asList());

        // When
        authService.deleteMember(loginId, token);

        // Then
        verify(memberRepository).findByLoginId(loginId);
        verify(memberRepository).delete(member);
        verify(tokenBlacklistService, never()).blacklistToken(any());
    }
}