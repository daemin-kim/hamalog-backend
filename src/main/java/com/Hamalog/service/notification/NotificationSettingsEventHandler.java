package com.Hamalog.service.notification;

import com.Hamalog.domain.events.notification.NotificationSettingsUpdated;
import com.Hamalog.logging.StructuredLogger;
import com.Hamalog.logging.events.AuditEvent;
import com.Hamalog.logging.events.BusinessEvent;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 알림 설정 이벤트 핸들러
 * 캐시 무효화(동기)와 스케줄러 재등록(비동기)를 분리하여 처리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationSettingsEventHandler {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StructuredLogger structuredLogger;

    /**
     * 알림 설정 변경 시 캐시 무효화 (동기 처리)
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCacheInvalidation(NotificationSettingsUpdated event) {
        log.debug("Invalidating caches for NotificationSettingsUpdated: memberId={}", event.getMemberId());

        try {
            // 알림 설정 캐시 무효화
            String settingsKey = "notification_settings:" + event.getMemberId();
            redisTemplate.delete(settingsKey);

            log.debug("Invalidated notification settings cache for memberId: {}", event.getMemberId());

        } catch (Exception e) {
            log.error("Failed to invalidate caches for NotificationSettingsUpdated: memberId={}",
                    event.getMemberId(), e);
        }
    }

    /**
     * 알림 설정 변경 시 스케줄러 재등록 및 후속 작업 (비동기)
     */
    @Async("eventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAsyncProcessing(NotificationSettingsUpdated event) {
        log.debug("Async processing for NotificationSettingsUpdated: memberId={}", event.getMemberId());

        try {
            // 감사 로그 기록
            AuditEvent auditEvent = AuditEvent.builder()
                    .userId(event.getMemberLoginId())
                    .operation("NOTIFICATION_SETTINGS_UPDATED")
                    .entityType("NotificationSettings")
                    .entityId(event.getNotificationSettingsId().toString())
                    .details("Push: " + event.getPushEnabled() +
                            ", MedicationReminder: " + event.getMedicationReminderEnabled() +
                            ", DiaryReminder: " + event.getDiaryReminderEnabled())
                    .build();
            structuredLogger.audit(auditEvent);

            // 비즈니스 이벤트 로깅
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("pushEnabled", event.getPushEnabled().toString());
            metadata.put("medicationReminderEnabled", event.getMedicationReminderEnabled().toString());
            metadata.put("diaryReminderEnabled", event.getDiaryReminderEnabled().toString());
            if (event.getDiaryReminderTime() != null) {
                metadata.put("diaryReminderTime", event.getDiaryReminderTime().toString());
            }

            BusinessEvent businessEvent = BusinessEvent.builder()
                    .eventType("NOTIFICATION_SETTINGS_UPDATED")
                    .userId(event.getMemberLoginId())
                    .entity("NotificationSettings")
                    .action("UPDATED")
                    .result("SUCCESS")
                    .metadata(metadata)
                    .build();

            structuredLogger.business(businessEvent);

            // 푸시 비활성화 시 예약된 알림 취소
            if (event.isPushDisabled()) {
                log.info("Push disabled for memberId: {} - Cancelling scheduled notifications",
                        event.getMemberId());
                // TODO: 예약된 알림 취소 로직
            }

            // 일기 알림 시간 변경 시 스케줄러 재등록
            if (event.isDiaryReminderActive() && event.getDiaryReminderTime() != null) {
                log.info("Diary reminder updated for memberId: {} at {}",
                        event.getMemberId(), event.getDiaryReminderTime());
                // TODO: 일기 알림 스케줄러 재등록
            }

            log.info("Completed async processing for NotificationSettingsUpdated: memberId={}",
                    event.getMemberId());

        } catch (Exception e) {
            log.error("Failed async processing for NotificationSettingsUpdated: memberId={}",
                    event.getMemberId(), e);
        }
    }
}
