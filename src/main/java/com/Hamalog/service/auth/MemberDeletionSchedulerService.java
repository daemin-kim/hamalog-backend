package com.Hamalog.service.auth;

import com.Hamalog.domain.member.Member;
import com.Hamalog.logging.SensitiveDataMasker;
import com.Hamalog.repository.diary.MoodDiaryRepository;
import com.Hamalog.repository.medication.MedicationRecordRepository;
import com.Hamalog.repository.medication.MedicationScheduleRepository;
import com.Hamalog.repository.member.MemberRepository;
import com.Hamalog.repository.sideEffect.SideEffectRecordRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 탈퇴 예약된 회원의 실제 삭제를 수행하는 스케줄러 서비스.
 * 30일 경과 시 데이터 삭제 및 계정 제거.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MemberDeletionSchedulerService {

    private final MemberRepository memberRepository;
    private final MedicationScheduleRepository medicationScheduleRepository;
    private final MedicationRecordRepository medicationRecordRepository;
    private final SideEffectRecordRepository sideEffectRecordRepository;
    private final MoodDiaryRepository moodDiaryRepository;

    @Scheduled(cron = "0 0 3 * * *") // 매일 03:00 실행
    @Transactional
    public void purgeScheduledMembers() {
        LocalDateTime now = LocalDateTime.now();
        List<Member> targets = memberRepository.findAllByDeletionScheduledTrueAndDeletionDueAtBefore(now);

        if (targets.isEmpty()) {
            return;
        }

        for (Member member : targets) {
            Long memberId = member.getMemberId();
            try {
                deleteMemberRelatedData(memberId);
                memberRepository.delete(member);
                log.info("[DELETION] Member purged - loginId: {}, memberId: {}",
                    SensitiveDataMasker.maskEmail(member.getLoginId()),
                    SensitiveDataMasker.maskUserId(memberId));
            } catch (Exception e) {
                log.error("[DELETION_ERROR] Failed to purge memberId={} : {}", memberId, e.getMessage(), e);
            }
        }
    }

    private void deleteMemberRelatedData(Long memberId) {
        sideEffectRecordRepository.deleteByMemberId(memberId);
        moodDiaryRepository.deleteByMember_MemberId(memberId);

        var medicationScheduleIds = medicationScheduleRepository.findAllByMember_MemberId(memberId)
            .stream()
            .map(com.Hamalog.domain.medication.MedicationSchedule::getMedicationScheduleId)
            .toList();

        if (!medicationScheduleIds.isEmpty()) {
            medicationRecordRepository.deleteByScheduleIds(medicationScheduleIds);
        }

        medicationScheduleRepository.deleteByMemberId(memberId);
    }
}
