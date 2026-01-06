package com.Hamalog.service.auth;

import com.Hamalog.domain.member.Member;
import com.Hamalog.dto.auth.request.PasswordChangeRequest;
import com.Hamalog.dto.auth.request.ProfileUpdateRequest;
import com.Hamalog.dto.auth.response.MemberProfileResponse;
import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.exception.member.MemberNotFoundException;
import com.Hamalog.repository.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class MemberProfileService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원 프로필 조회
     */
    public MemberProfileResponse getProfile(Long memberId) {
        log.info("회원 프로필 조회 - memberId: {}", memberId);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        return MemberProfileResponse.from(member);
    }

    /**
     * 로그인 ID로 회원 프로필 조회
     */
    public MemberProfileResponse getProfileByLoginId(String loginId) {
        log.info("회원 프로필 조회 - loginId: {}", loginId);

        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(MemberNotFoundException::new);

        return MemberProfileResponse.from(member);
    }

    /**
     * 회원 프로필 수정
     */
    @Transactional(rollbackFor = {Exception.class})
    public MemberProfileResponse updateProfile(Long memberId, ProfileUpdateRequest request) {
        log.info("회원 프로필 수정 시작 - memberId: {}", memberId);

        if (request.hasNoUpdates()) {
            throw new CustomException(ErrorCode.NO_PROFILE_UPDATE_DATA);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        member.updateProfile(
                request.name(),
                request.nickName(),
                request.phoneNumber(),
                request.birth()
        );

        log.info("회원 프로필 수정 완료 - memberId: {}", memberId);
        return MemberProfileResponse.from(member);
    }

    /**
     * 비밀번호 변경
     */
    @Transactional(rollbackFor = {Exception.class})
    public void changePassword(Long memberId, PasswordChangeRequest request) {
        log.info("비밀번호 변경 시작 - memberId: {}", memberId);

        // 새 비밀번호 확인 일치 검증
        if (!request.isPasswordConfirmed()) {
            throw new CustomException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(request.currentPassword(), member.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CURRENT_PASSWORD);
        }

        // 새 비밀번호가 현재와 같은지 확인
        if (passwordEncoder.matches(request.newPassword(), member.getPassword())) {
            throw new CustomException(ErrorCode.SAME_AS_CURRENT_PASSWORD);
        }

        // 비밀번호 변경
        String encodedNewPassword = passwordEncoder.encode(request.newPassword());
        member.changePassword(encodedNewPassword);

        log.info("비밀번호 변경 완료 - memberId: {}", memberId);
    }
}
