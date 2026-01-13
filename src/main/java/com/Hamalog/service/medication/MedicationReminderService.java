package com.Hamalog.service.medication;

import com.Hamalog.domain.medication.MedicationSchedule;
import com.Hamalog.domain.medication.MedicationTime;
import com.Hamalog.repository.medication.MedicationRecordRepository;
import com.Hamalog.repository.medication.MedicationScheduleRepository;
import com.Hamalog.repository.medication.MedicationTimeRepository;
import com.Hamalog.service.notification.FcmPushService;
import com.Hamalog.service.queue.QueuedNotificationService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 복약 리마인더 서비스
 * 복약 미완료 확인 및 알림 발송
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MedicationReminderService {

    private final MedicationScheduleRepository scheduleRepository;
    private final MedicationTimeRepository medicationTimeRepository;
    private final MedicationRecordRepository recordRepository;
    private final FcmPushService fcmPushService;
    private final QueuedNotificationService queuedNotificationService;

    /**
     * 복약 미완료 건 확인 및 알림 발송
     * 로그인 시 호출되어 오늘 복약 미완료 건을 확인합니다.
     */
    @Async("eventExecutor")
    public void checkAndNotifyMissedMedications(Long memberId) {
        try {
            int missedCount = countTodayMissedMedications(memberId);

            if (missedCount > 0) {
                log.info("Found {} missed medications for memberId: {}", missedCount, memberId);
                fcmPushService.sendMissedMedicationReminder(memberId, missedCount);
            }
        } catch (Exception e) {
            log.error("Failed to check missed medications for memberId: {}", memberId, e);
        }
    }

    /**
     * 오늘 복약 미완료 건수 계산
     */
    public int countTodayMissedMedications(Long memberId) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        // 활성화된 복약 스케줄 조회
        List<MedicationSchedule> schedules = scheduleRepository.findAllByMember_MemberId(memberId);

        int missedCount = 0;

        for (MedicationSchedule schedule : schedules) {
            // 비활성화된 스케줄 제외
            if (!schedule.getIsActive()) {
                continue;
            }

            // 처방 기간 외의 스케줄 제외
            if (!isWithinPrescriptionPeriod(schedule, today)) {
                continue;
            }

            // 해당 스케줄의 복약 시간 조회
            List<MedicationTime> medicationTimes = medicationTimeRepository
                    .findByMedicationSchedule_MedicationScheduleIdOrderByTakeTimeAsc(
                            schedule.getMedicationScheduleId());

            for (MedicationTime time : medicationTimes) {
                // 현재 시간 이전의 복약 시간만 체크 (지난 복약)
                if (time.getTakeTime().isBefore(now)) {
                    // 오늘 해당 시간에 복약 기록이 있는지 확인
                    boolean taken = isMedicationTaken(schedule.getMedicationScheduleId(),
                            time.getMedicationTimeId(), today);

                    if (!taken) {
                        missedCount++;
                    }
                }
            }
        }

        return missedCount;
    }

    /**
     * 처방 기간 내인지 확인
     */
    private boolean isWithinPrescriptionPeriod(MedicationSchedule schedule, LocalDate date) {
        LocalDate startDate = schedule.getStartOfAd();
        if (startDate == null) {
            return true; // 시작일 없으면 항상 유효
        }

        LocalDate endDate = startDate.plusDays(schedule.getPrescriptionDays());
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    /**
     * 복약 기록 존재 여부 확인
     */
    private boolean isMedicationTaken(Long scheduleId, Long timeId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        // 해당 날짜에 해당 스케줄/시간에 대한 복용 완료 기록이 있는지 확인
        long takenCount = recordRepository.countByMemberIdAndDateRange(
                scheduleId, startOfDay, endOfDay);

        return takenCount > 0;
    }

    /**
     * 복용 1시간 후 부작용 기록 권유 알림 예약
     * Redis Stream 메시지 큐를 통해 비동기로 처리됩니다.
     */
    @Async("eventExecutor")
    public void scheduleSideEffectRecordReminder(Long memberId, Long scheduleId, LocalDateTime takeTime) {
        try {
            LocalDateTime reminderTime = takeTime.plusHours(1);

            if (reminderTime.isAfter(LocalDateTime.now())) {
                log.info("Scheduling side effect reminder for memberId: {} at {}", memberId, reminderTime);

                // 메시지 큐를 통해 부작용 기록 권유 알림 발송
                queuedNotificationService.sendSideEffectRecordReminder(
                        memberId,
                        "약 복용 1시간이 지났습니다. 혹시 부작용이 있다면 기록해주세요."
                );
            }
        } catch (Exception e) {
            log.error("Failed to schedule side effect reminder for memberId: {}", memberId, e);
        }
    }

    /**
     * 연속 복약 일수 계산
     */
    public int calculateConsecutiveMedicationDays(Long memberId, Long scheduleId) {
        LocalDate today = LocalDate.now();
        int consecutiveDays = 0;

        for (int i = 0; i < 365; i++) {
            LocalDate date = today.minusDays(i);
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(23, 59, 59);

            // 해당 날짜에 복약 기록이 있는지 확인
            long recordCount = recordRepository.countTakenByMemberIdAndDateRange(
                    memberId, startOfDay, endOfDay);

            if (recordCount > 0) {
                consecutiveDays++;
            } else {
                break; // 연속이 끊기면 종료
            }
        }

        return consecutiveDays;
    }
}
