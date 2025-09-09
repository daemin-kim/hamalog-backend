package com.Hamalog.service.auth;

import com.Hamalog.domain.member.Member;
import com.Hamalog.domain.medication.MedicationRecord;
import com.Hamalog.domain.medication.MedicationSchedule;
import com.Hamalog.domain.sideEffect.SideEffectRecord;
import com.Hamalog.dto.auth.request.SignupRequest;
import com.Hamalog.dto.auth.response.LoginResponse;
import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.repository.member.MemberRepository;
import com.Hamalog.repository.medication.MedicationRecordRepository;
import com.Hamalog.repository.medication.MedicationScheduleRepository;
import com.Hamalog.repository.sideEffect.SideEffectRecordRepository;
import com.Hamalog.security.jwt.JwtTokenProvider;
import com.Hamalog.security.jwt.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final MedicationScheduleRepository medicationScheduleRepository;
    private final MedicationRecordRepository medicationRecordRepository;
    private final SideEffectRecordRepository sideEffectRecordRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public void registerMember(SignupRequest request) {
        validateMemberRegistration(request);
        
        Member member = createMemberFromRequest(request);
        memberRepository.save(member);
    }

    public LoginResponse authenticateAndGenerateToken(String loginId, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginId, password)
        );
        
        String token = jwtTokenProvider.createToken(authentication.getName());
        return new LoginResponse(token);
    }

    public void logoutUser(String token) {
        if (isValidTokenFormat(token)) {
            tokenBlacklistService.blacklistToken(token);
        }
    }

    @Transactional
    public void deleteMember(String loginId, String token) {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        deleteMemberRelatedData(member.getMemberId());
        
        memberRepository.delete(member);
        
        if (isValidTokenFormat(token)) {
            tokenBlacklistService.blacklistToken(token);
        }
    }

    private void deleteMemberRelatedData(Long memberId) {
        sideEffectRecordRepository.deleteByMemberId(memberId);

        var medicationScheduleIds = medicationScheduleRepository.findAllByMember_MemberId(memberId)
                .stream()
                .map(schedule -> schedule.getMedicationScheduleId())
                .toList();

        if (!medicationScheduleIds.isEmpty()) {
            medicationRecordRepository.deleteByScheduleIds(medicationScheduleIds);
        }

        medicationScheduleRepository.deleteByMemberId(memberId);
    }

    private void validateMemberRegistration(SignupRequest request) {
        if (memberRepository.findByLoginId(request.loginId()).isPresent()) {
            throw new CustomException(ErrorCode.DUPLICATE_MEMBER);
        }
    }

    private Member createMemberFromRequest(SignupRequest request) {
        return Member.builder()
                .loginId(request.loginId())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .nickName(request.nickName())
                .phoneNumber(request.phoneNumber())
                .birth(request.birth())
                .createdAt(LocalDateTime.now())
                .build();
    }

    private boolean isValidTokenFormat(String token) {
        return token != null && !token.isBlank();
    }
}