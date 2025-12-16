package com.Hamalog.service.auth;

import com.Hamalog.domain.events.member.MemberDeletedEvent;
import com.Hamalog.domain.member.Member;
import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.logging.SensitiveDataMasker;
import com.Hamalog.repository.diary.MoodDiaryRepository;
import com.Hamalog.repository.medication.MedicationRecordRepository;
import com.Hamalog.repository.medication.MedicationScheduleRepository;
import com.Hamalog.repository.member.MemberRepository;
import com.Hamalog.repository.sideEffect.SideEffectRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 탈퇴 관련 서비스
 * 회원 삭제 및 관련 데이터 정리의 책임을 담당합니다.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class MemberDeletionService {

    private final MemberRepository memberRepository;
    private final MedicationScheduleRepository medicationScheduleRepository;
    private final MedicationRecordRepository medicationRecordRepository;
    private final SideEffectRecordRepository sideEffectRecordRepository;
    private final MoodDiaryRepository moodDiaryRepository;
    private final AuthenticationService authenticationService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 회원을 삭제합니다.
     * 관련된 모든 데이터를 함께 삭제하고, 토큰을 무효화합니다.
     *
     * @param loginId 삭제할 회원의 로그인 ID
     * @param token 현재 사용 중인 토큰 (블랙리스트 등록용)
     * @throws CustomException 회원을 찾을 수 없는 경우
     */
    @Transactional(rollbackFor = {Exception.class})
    public void deleteMember(String loginId, String token) {
        // 1. 즉시 토큰 무효화 (트랜잭션 내부에서 처리하여 동시성 문제 방지)
        if (authenticationService.isValidTokenFormat(token)) {
            authenticationService.blacklistToken(token);
            log.info("[AUTH] Token blacklisted immediately during member deletion - loginId: {}",
                SensitiveDataMasker.maskEmail(loginId));
        }

        // 2. 회원 조회 및 검증
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        Long memberId = member.getMemberId();

        // 3. 관련 데이터 삭제 (cascade 순서 보장)
        deleteMemberRelatedData(memberId);

        // 4. 회원 완전 삭제
        memberRepository.delete(member);

        log.info("[AUTH] Member deleted successfully - loginId: {}, memberId: {}",
            SensitiveDataMasker.maskEmail(loginId),
            SensitiveDataMasker.maskUserId(memberId));

        // 5. 이벤트 발행 (추가 후처리용)
        eventPublisher.publishEvent(new MemberDeletedEvent(loginId, token, memberId));
    }

    /**
     * 회원 관련 모든 데이터를 삭제합니다.
     *
     * @param memberId 삭제할 회원의 ID
     */
    private void deleteMemberRelatedData(Long memberId) {
        // 부작용 기록 삭제
        sideEffectRecordRepository.deleteByMemberId(memberId);

        // 마음 일기 삭제
        moodDiaryRepository.deleteByMember_MemberId(memberId);

        // 복약 기록 삭제 (스케줄 ID 조회 후 삭제)
        var medicationScheduleIds = medicationScheduleRepository.findAllByMember_MemberId(memberId)
                .stream()
                .map(com.Hamalog.domain.medication.MedicationSchedule::getMedicationScheduleId)
                .toList();

        if (!medicationScheduleIds.isEmpty()) {
            medicationRecordRepository.deleteByScheduleIds(medicationScheduleIds);
        }

        // 복약 스케줄 삭제
        medicationScheduleRepository.deleteByMemberId(memberId);

        log.debug("[DELETION] Member related data deleted - memberId: {}", memberId);
    }
}

