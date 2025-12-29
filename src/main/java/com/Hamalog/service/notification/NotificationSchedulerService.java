package com.Hamalog.service.notification;

import com.Hamalog.domain.notification.NotificationSettings;
import com.Hamalog.repository.notification.NotificationSettingsRepository;
import java.time.LocalTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

/**
 * 알림 스케줄러 서비스
 * 예약된 알림을 관리하고 스케줄링합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationSchedulerService {

    private final TaskScheduler taskScheduler;
    private final NotificationSettingsRepository notificationSettingsRepository;
    private final FcmPushService fcmPushService;

    // 회원별 예약된 알림 작업 관리
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    /**
     * 일기 알림 스케줄 등록/재등록
     */
    public void scheduleDiaryReminder(Long memberId, LocalTime reminderTime) {
        String taskKey = "diary_reminder:" + memberId;

        // 기존 스케줄 취소
        cancelScheduledTask(taskKey);

        // 새 스케줄 등록
        String cronExpression = buildCronExpression(reminderTime);

        ScheduledFuture<?> future = taskScheduler.schedule(
                () -> sendDiaryReminder(memberId),
                new CronTrigger(cronExpression)
        );

        scheduledTasks.put(taskKey, future);
        log.info("Scheduled diary reminder for memberId: {} at {}", memberId, reminderTime);
    }

    /**
     * 모든 예약된 알림 취소 (푸시 비활성화 시)
     */
    public void cancelAllScheduledNotifications(Long memberId) {
        // 일기 알림 취소
        cancelScheduledTask("diary_reminder:" + memberId);

        // 복약 알림 취소
        cancelScheduledTask("medication_reminder:" + memberId);

        log.info("Cancelled all scheduled notifications for memberId: {}", memberId);
    }

    /**
     * 일기 알림 스케줄 취소
     */
    public void cancelDiaryReminder(Long memberId) {
        cancelScheduledTask("diary_reminder:" + memberId);
        log.info("Cancelled diary reminder for memberId: {}", memberId);
    }

    /**
     * 일기 알림 발송
     */
    private void sendDiaryReminder(Long memberId) {
        try {
            // 알림 설정 확인
            NotificationSettings settings = notificationSettingsRepository.findByMember_MemberId(memberId)
                    .orElse(null);

            if (settings == null || !settings.isDiaryReminderEnabled()) {
                log.debug("Diary reminder disabled for memberId: {}", memberId);
                return;
            }

            fcmPushService.sendDiaryReminder(memberId);
            log.info("Sent diary reminder to memberId: {}", memberId);

        } catch (Exception e) {
            log.error("Failed to send diary reminder to memberId: {}", memberId, e);
        }
    }

    /**
     * 예약된 작업 취소
     */
    private void cancelScheduledTask(String taskKey) {
        ScheduledFuture<?> future = scheduledTasks.remove(taskKey);
        if (future != null && !future.isCancelled()) {
            future.cancel(false);
            log.debug("Cancelled scheduled task: {}", taskKey);
        }
    }

    /**
     * Cron 표현식 생성 (매일 지정 시간)
     */
    private String buildCronExpression(LocalTime time) {
        return String.format("0 %d %d * * *", time.getMinute(), time.getHour());
    }

    /**
     * 알림 스케줄 초기화 (애플리케이션 시작 시)
     * 활성화된 일기 알림 설정을 가진 모든 사용자의 알림을 스케줄링
     */
    public void initializeSchedules() {
        // 주의: 대량의 사용자가 있을 경우 배치 처리 필요
        log.info("Initializing notification schedules...");

        // 실제 구현에서는 Redis나 DB에서 활성화된 알림 설정을 조회하여 스케줄링
        // 현재는 로그인 시 개별 스케줄링으로 처리
    }

    /**
     * 스케줄링된 작업 수 조회 (모니터링용)
     */
    public int getScheduledTaskCount() {
        return scheduledTasks.size();
    }
}
