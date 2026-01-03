package com.Hamalog.service.queue;

import com.Hamalog.config.MessageQueueProperties;
import com.Hamalog.service.notification.FcmPushService;
import com.Hamalog.service.queue.message.NotificationMessage;
import com.Hamalog.service.queue.message.NotificationType;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 큐 기반 푸시 알림 서비스
 * 메시지 큐가 활성화되면 큐를 통해, 비활성화되면 직접 FCM 호출합니다.
 * Facade 패턴으로 기존 FcmPushService와 MessageQueueService를 통합합니다.
 */
@Service
@Slf4j
public class QueuedNotificationService {

    private final MessageQueueProperties queueProperties;
    private final MessageQueueService messageQueueService;
    private final FcmPushService fcmPushService;

    public QueuedNotificationService(
            @Autowired(required = false) MessageQueueProperties queueProperties,
            @Autowired(required = false) MessageQueueService messageQueueService,
            FcmPushService fcmPushService
    ) {
        this.queueProperties = queueProperties;
        this.messageQueueService = messageQueueService;
        this.fcmPushService = fcmPushService;
    }

    /**
     * 푸시 알림 발송 (큐 활성화 시 비동기, 비활성화 시 직접 발송)
     */
    public void sendPushNotification(
            Long memberId,
            String title,
            String body,
            Map<String, String> data,
            String notificationType
    ) {
        if (isQueueEnabled()) {
            NotificationMessage message = NotificationMessage.of(
                    memberId, title, body, data, notificationType
            );
            messageQueueService.publish(message);
            log.debug("Notification queued for memberId: {}, type: {}", memberId, notificationType);
        } else {
            // 큐 비활성화 시 기존 방식으로 직접 발송
            fcmPushService.sendPushNotification(memberId, title, body, data);
            log.debug("Notification sent directly for memberId: {}", memberId);
        }
    }

    /**
     * 심각한 부작용 알림 발송
     */
    public void sendSevereSideEffectAlert(Long memberId, String sideEffectName, Integer degree) {
        String title = "⚠️ 심각한 부작용 발생";
        String body = String.format("%s (심각도: %d) 발생이 기록되었습니다. 의료진 상담을 권장합니다.",
                sideEffectName, degree);

        Map<String, String> data = Map.of(
                "type", "SEVERE_SIDE_EFFECT",
                "sideEffectName", sideEffectName,
                "degree", degree.toString()
        );

        sendPushNotification(memberId, title, body, data, NotificationType.SEVERE_SIDE_EFFECT);
    }

    /**
     * 의료진 상담 권유 메시지 발송
     */
    public void sendMedicalConsultationReminder(Long memberId) {
        String title = "🏥 의료진 상담 권유";
        String body = "최근 심각한 부작용이 기록되었습니다. 담당 의료진과 상담하시기 바랍니다.";

        Map<String, String> data = Map.of("type", "MEDICAL_CONSULTATION");

        sendPushNotification(memberId, title, body, data, NotificationType.MEDICAL_CONSULTATION);
    }

    /**
     * 복약 미완료 알림 발송
     */
    public void sendMissedMedicationReminder(Long memberId, int missedCount) {
        String title = "💊 복약 알림";
        String body = String.format("오늘 %d건의 복약이 완료되지 않았습니다.", missedCount);

        Map<String, String> data = Map.of(
                "type", "MISSED_MEDICATION",
                "missedCount", String.valueOf(missedCount)
        );

        sendPushNotification(memberId, title, body, data, NotificationType.MISSED_MEDICATION);
    }

    /**
     * 연속 복약 달성 알림 발송
     */
    public void sendConsecutiveMedicationAchievement(Long memberId, int days) {
        String title = "🎉 복약 달성!";
        String body = String.format("축하합니다! %d일 연속 복약을 달성하셨습니다.", days);

        Map<String, String> data = Map.of(
                "type", "ACHIEVEMENT",
                "achievementType", "CONSECUTIVE_MEDICATION",
                "days", String.valueOf(days)
        );

        sendPushNotification(memberId, title, body, data,
                NotificationType.ACHIEVEMENT_CONSECUTIVE_MEDICATION);
    }

    /**
     * 연속 일기 작성 달성 알림 발송
     */
    public void sendConsecutiveDiaryAchievement(Long memberId, int days) {
        String title = "🎉 일기 작성 달성!";
        String body = String.format("축하합니다! %d일 연속 일기 작성을 달성하셨습니다.", days);

        Map<String, String> data = Map.of(
                "type", "ACHIEVEMENT",
                "achievementType", "CONSECUTIVE_DIARY",
                "days", String.valueOf(days)
        );

        sendPushNotification(memberId, title, body, data,
                NotificationType.ACHIEVEMENT_CONSECUTIVE_DIARY);
    }

    /**
     * 부작용 기록 권유 알림 발송
     */
    public void sendSideEffectRecordReminder(Long memberId, String reason) {
        String title = "📝 부작용 기록 권유";
        String body = reason;

        Map<String, String> data = Map.of("type", "SIDE_EFFECT_REMINDER");

        sendPushNotification(memberId, title, body, data, NotificationType.SIDE_EFFECT_REMINDER);
    }

    /**
     * 일기 작성 알림 발송
     */
    public void sendDiaryReminder(Long memberId) {
        String title = "📔 오늘의 마음 일기";
        String body = "오늘 하루는 어땠나요? 마음 일기를 작성해보세요.";

        Map<String, String> data = Map.of("type", "DIARY_REMINDER");

        sendPushNotification(memberId, title, body, data, NotificationType.DIARY_REMINDER);
    }

    /**
     * 부정적 기분 지속 알림 발송
     */
    public void sendNegativeMoodAlert(Long memberId, int consecutiveDays) {
        String title = "💙 마음 건강 체크";
        String body = String.format(
                "%d일 연속 힘든 하루가 이어지고 있네요. 부작용으로 인한 것은 아닌지 확인해보세요.",
                consecutiveDays);

        Map<String, String> data = Map.of(
                "type", "NEGATIVE_MOOD_ALERT",
                "consecutiveDays", String.valueOf(consecutiveDays)
        );

        sendPushNotification(memberId, title, body, data, NotificationType.NEGATIVE_MOOD_ALERT);
    }

    /**
     * 큐 활성화 여부 확인
     */
    private boolean isQueueEnabled() {
        return queueProperties != null
                && queueProperties.enabled()
                && messageQueueService != null;
    }
}
