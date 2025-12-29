package com.Hamalog.service.auth;

import com.Hamalog.domain.events.auth.LoginSucceeded;
import com.Hamalog.logging.StructuredLogger;
import com.Hamalog.logging.events.BusinessEvent;
import com.Hamalog.service.medication.MedicationReminderService;
import com.Hamalog.service.notification.NotificationSettingsService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 로그인 이벤트 핸들러
 * 로그인 이력 저장 및 후속 작업 처리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LoginEventHandler {

    private final StructuredLogger structuredLogger;
    private final NotificationSettingsService notificationSettingsService;
    private final MedicationReminderService medicationReminderService;

    /**
     * 로그인 성공 시 비동기 후속 작업 처리
     */
    @Async("eventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLoginSucceeded(LoginSucceeded event) {
        log.debug("Processing LoginSucceeded event for memberId: {}", event.getMemberId());

        try {
            // 비즈니스 이벤트 로깅
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("ipAddress", event.getIpAddress());
            metadata.put("deviceType", event.getDeviceType());

            BusinessEvent businessEvent = BusinessEvent.builder()
                    .eventType("LOGIN_SUCCEEDED")
                    .userId(event.getLoginId())
                    .entity("Member")
                    .action("LOGIN")
                    .result("SUCCESS")
                    .metadata(metadata)
                    .build();

            structuredLogger.business(businessEvent);

            // 알림 설정 기본값 존재 확인 및 생성
            try {
                notificationSettingsService.getSettings(event.getMemberId());
                log.debug("Notification settings ensured for memberId: {}", event.getMemberId());
            } catch (Exception e) {
                log.warn("Failed to ensure notification settings for memberId: {}", event.getMemberId(), e);
            }

            // 오늘 복약 미완료 건 있으면 알림 전송
            try {
                medicationReminderService.checkAndNotifyMissedMedications(event.getMemberId());
            } catch (Exception e) {
                log.warn("Failed to check missed medications for memberId: {}", event.getMemberId(), e);
            }

            log.info("Completed async processing for LoginSucceeded: memberId={}, loginId={}",
                    event.getMemberId(), event.getLoginId());

        } catch (Exception e) {
            log.error("Failed to process LoginSucceeded event for memberId: {}",
                    event.getMemberId(), e);
        }
    }
}
