package com.Hamalog.service.auth;

import com.Hamalog.domain.member.Member;
import com.Hamalog.dto.auth.request.SignupRequest;
import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.repository.member.MemberRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 등록 관련 서비스
 * 회원가입, 회원 정보 검증 등의 책임을 담당합니다.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class MemberRegistrationService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 새 회원을 등록합니다.
     *
     * @param request 회원가입 요청 정보
     * @throws CustomException 이미 존재하는 회원인 경우 DUPLICATE_MEMBER 오류
     */
    @Transactional(rollbackFor = {Exception.class})
    public void registerMember(SignupRequest request) {
        validateMemberRegistration(request);

        Member member = createMemberFromRequest(request);
        memberRepository.save(member);

        log.info("[REGISTRATION] New member registered - loginId: {}",
            maskEmail(request.loginId()));
    }

    /**
     * 회원가입 요청을 검증합니다.
     *
     * @param request 검증할 회원가입 요청
     * @throws CustomException 이미 존재하는 회원인 경우
     */
    private void validateMemberRegistration(SignupRequest request) {
        if (memberRepository.findByLoginId(request.loginId()).isPresent()) {
            throw new CustomException(ErrorCode.DUPLICATE_MEMBER);
        }
    }

    /**
     * SignupRequest로부터 Member 엔티티를 생성합니다.
     *
     * @param request 회원가입 요청
     * @return 생성된 Member 엔티티
     */
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

    /**
     * 이메일 마스킹 처리
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@");
        String local = parts[0];
        if (local.length() <= 2) {
            return "**@" + parts[1];
        }
        return local.substring(0, 2) + "***@" + parts[1];
    }
}

