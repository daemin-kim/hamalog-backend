package com.Hamalog.service.notification;

import com.Hamalog.domain.notification.FcmDeviceToken;
import com.Hamalog.domain.notification.NotificationSettings;
import com.Hamalog.repository.notification.FcmDeviceTokenRepository;
import com.Hamalog.repository.notification.NotificationSettingsRepository;
import com.google.firebase.messaging.*;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * FCM Push 알림 발송 서비스
 * Firebase Cloud Messaging을 통해 사용자에게 푸시 알림을 발송합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FcmPushService {

    private final FcmDeviceTokenRepository fcmDeviceTokenRepository;
    private final NotificationSettingsRepository notificationSettingsRepository;

    /**
     * 단일 사용자에게 푸시 알림 발송
     *
     * @param memberId 회원 ID
     * @param title 알림 제목
     * @param body 알림 내용
     * @param data 추가 데이터
     */
    @Async("eventExecutor")
    public CompletableFuture<Void> sendPushNotification(
            Long memberId, String title, String body, Map<String, String> data) {

        // 알림 설정 확인
        if (!isPushEnabled(memberId)) {
            log.debug("Push disabled for memberId: {}", memberId);
            return CompletableFuture.completedFuture(null);
        }

        // 조용한 시간 확인
        if (isQuietHours(memberId)) {
            log.debug("Quiet hours active for memberId: {}", memberId);
            return CompletableFuture.completedFuture(null);
        }

        // 활성화된 디바이스 토큰 조회
        List<FcmDeviceToken> tokens = fcmDeviceTokenRepository.findByMember_MemberIdAndIsActiveTrue(memberId);
        if (tokens.isEmpty()) {
            log.debug("No active device tokens for memberId: {}", memberId);
            return CompletableFuture.completedFuture(null);
        }

        // 각 디바이스에 알림 발송
        for (FcmDeviceToken deviceToken : tokens) {
            try {
                sendToDevice(deviceToken.getToken(), title, body, data);
                deviceToken.markAsUsed();
                log.info("Push notification sent to memberId: {}, device: {}",
                        memberId, deviceToken.getDeviceName());
            } catch (FirebaseMessagingException e) {
                handleFirebaseError(deviceToken, e);
            }
        }

        return CompletableFuture.completedFuture(null);
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

        sendPushNotification(memberId, title, body, data);
    }

    /**
     * 의료진 상담 권유 메시지 발송
     */
    public void sendMedicalConsultationReminder(Long memberId) {
        String title = "🏥 의료진 상담 권유";
        String body = "최근 심각한 부작용이 기록되었습니다. 담당 의료진과 상담하시기 바랍니다.";

        Map<String, String> data = Map.of("type", "MEDICAL_CONSULTATION");

        sendPushNotification(memberId, title, body, data);
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

        sendPushNotification(memberId, title, body, data);
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

        sendPushNotification(memberId, title, body, data);
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

        sendPushNotification(memberId, title, body, data);
    }

    /**
     * 부작용 기록 권유 알림 발송
     */
    public void sendSideEffectRecordReminder(Long memberId, String reason) {
        String title = "📝 부작용 기록 권유";
        String body = reason;

        Map<String, String> data = Map.of("type", "SIDE_EFFECT_REMINDER");

        sendPushNotification(memberId, title, body, data);
    }

    /**
     * 일기 작성 알림 발송
     */
    public void sendDiaryReminder(Long memberId) {
        String title = "📔 오늘의 마음 일기";
        String body = "오늘 하루는 어땠나요? 마음 일기를 작성해보세요.";

        Map<String, String> data = Map.of("type", "DIARY_REMINDER");

        sendPushNotification(memberId, title, body, data);
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

        sendPushNotification(memberId, title, body, data);
    }

    /**
     * 디바이스에 FCM 메시지 전송
     */
    private void sendToDevice(String token, String title, String body, Map<String, String> data)
            throws FirebaseMessagingException {

        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        Message.Builder messageBuilder = Message.builder()
                .setToken(token)
                .setNotification(notification);

        if (data != null && !data.isEmpty()) {
            messageBuilder.putAllData(data);
        }

        // Android 설정
        messageBuilder.setAndroidConfig(AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .setNotification(AndroidNotification.builder()
                        .setClickAction("OPEN_APP")
                        .build())
                .build());

        // iOS(APNs) 설정
        messageBuilder.setApnsConfig(ApnsConfig.builder()
                .setAps(Aps.builder()
                        .setSound("default")
                        .setBadge(1)
                        .build())
                .build());

        FirebaseMessaging.getInstance().send(messageBuilder.build());
    }

    /**
     * Firebase 오류 처리
     */
    private void handleFirebaseError(FcmDeviceToken deviceToken, FirebaseMessagingException e) {
        MessagingErrorCode errorCode = e.getMessagingErrorCode();

        if (errorCode == MessagingErrorCode.UNREGISTERED ||
                errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
            // 유효하지 않은 토큰은 비활성화
            deviceToken.deactivate();
            fcmDeviceTokenRepository.save(deviceToken);
            log.warn("Deactivated invalid FCM token: {}", deviceToken.getToken());
        } else {
            log.error("Failed to send FCM message: {}", e.getMessage());
        }
    }

    /**
     * 푸시 알림 활성화 여부 확인
     */
    private boolean isPushEnabled(Long memberId) {
        return notificationSettingsRepository.findByMember_MemberId(memberId)
                .map(NotificationSettings::isPushEnabled)
                .orElse(true); // 기본값: 활성화
    }

    /**
     * 조용한 시간 여부 확인
     */
    private boolean isQuietHours(Long memberId) {
        return notificationSettingsRepository.findByMember_MemberId(memberId)
                .filter(NotificationSettings::isQuietHoursEnabled)
                .map(settings -> {
                    LocalTime now = LocalTime.now();
                    LocalTime start = settings.getQuietHoursStart();
                    LocalTime end = settings.getQuietHoursEnd();

                    if (start == null || end == null) {
                        return false;
                    }

                    // 자정을 넘어가는 경우 처리
                    if (start.isAfter(end)) {
                        return now.isAfter(start) || now.isBefore(end);
                    }
                    return now.isAfter(start) && now.isBefore(end);
                })
                .orElse(false);
    }
}
