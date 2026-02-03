package com.Hamalog.service.queue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.Hamalog.config.MessageQueueProperties;
import com.Hamalog.service.notification.FcmPushService;
import com.Hamalog.service.queue.message.NotificationMessage;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * QueuedNotificationService 단위 테스트
 * 큐 활성화/비활성화 상태에 따른 알림 발송 동작을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("QueuedNotificationService 테스트")
class QueuedNotificationServiceTest {

    @Mock
    private MessageQueueProperties queueProperties;

    @Mock
    private MessageQueueService messageQueueService;

    @Mock
    private FcmPushService fcmPushService;

    @InjectMocks
    private QueuedNotificationService queuedNotificationService;

    private static final Long TEST_MEMBER_ID = 1L;

    @Nested
    @DisplayName("sendPushNotification")
    class SendPushNotification {

        @Test
        @DisplayName("성공: 큐 활성화 시 메시지 큐로 발행")
        void success_queueEnabled() {
            // given
            when(queueProperties.enabled()).thenReturn(true);

            // when
            queuedNotificationService.sendPushNotification(
                    TEST_MEMBER_ID,
                    "테스트 제목",
                    "테스트 내용",
                    Map.of("key", "value"),
                    "TEST_TYPE"
            );

            // then
            verify(messageQueueService).publish(any(NotificationMessage.class));
            verify(fcmPushService, never()).sendPushNotification(anyLong(), anyString(), anyString(), anyMap());
        }

        @Test
        @DisplayName("성공: 큐 비활성화 시 직접 FCM 발송")
        void success_queueDisabled() {
            // given
            when(queueProperties.enabled()).thenReturn(false);

            // when
            queuedNotificationService.sendPushNotification(
                    TEST_MEMBER_ID,
                    "테스트 제목",
                    "테스트 내용",
                    Map.of("key", "value"),
                    "TEST_TYPE"
            );

            // then
            verify(fcmPushService).sendPushNotification(
                    eq(TEST_MEMBER_ID),
                    eq("테스트 제목"),
                    eq("테스트 내용"),
                    anyMap()
            );
            verify(messageQueueService, never()).publish(any(NotificationMessage.class));
        }
    }

    @Nested
    @DisplayName("sendSevereSideEffectAlert")
    class SendSevereSideEffectAlert {

        @Test
        @DisplayName("성공: 심각한 부작용 알림 발송")
        void success() {
            // given
            when(queueProperties.enabled()).thenReturn(true);
            String sideEffectName = "두통";
            Integer degree = 4;

            // when
            queuedNotificationService.sendSevereSideEffectAlert(TEST_MEMBER_ID, sideEffectName, degree);

            // then
            verify(messageQueueService).publish(any(NotificationMessage.class));
        }
    }

    @Nested
    @DisplayName("sendMedicalConsultationReminder")
    class SendMedicalConsultationReminder {

        @Test
        @DisplayName("성공: 의료진 상담 권유 알림 발송")
        void success() {
            // given
            when(queueProperties.enabled()).thenReturn(true);

            // when
            queuedNotificationService.sendMedicalConsultationReminder(TEST_MEMBER_ID);

            // then
            verify(messageQueueService).publish(any(NotificationMessage.class));
        }
    }

    @Nested
    @DisplayName("sendMissedMedicationReminder")
    class SendMissedMedicationReminder {

        @Test
        @DisplayName("성공: 복약 미완료 알림 발송")
        void success() {
            // given
            when(queueProperties.enabled()).thenReturn(true);
            int missedCount = 3;

            // when
            queuedNotificationService.sendMissedMedicationReminder(TEST_MEMBER_ID, missedCount);

            // then
            verify(messageQueueService).publish(any(NotificationMessage.class));
        }
    }

    @Nested
    @DisplayName("sendConsecutiveMedicationAchievement")
    class SendConsecutiveMedicationAchievement {

        @Test
        @DisplayName("성공: 연속 복약 달성 알림 발송")
        void success() {
            // given
            when(queueProperties.enabled()).thenReturn(true);
            int days = 7;

            // when
            queuedNotificationService.sendConsecutiveMedicationAchievement(TEST_MEMBER_ID, days);

            // then
            verify(messageQueueService).publish(any(NotificationMessage.class));
        }
    }

    @Nested
    @DisplayName("sendConsecutiveDiaryAchievement")
    class SendConsecutiveDiaryAchievement {

        @Test
        @DisplayName("성공: 연속 일기 작성 달성 알림 발송")
        void success() {
            // given
            when(queueProperties.enabled()).thenReturn(true);
            int days = 30;

            // when
            queuedNotificationService.sendConsecutiveDiaryAchievement(TEST_MEMBER_ID, days);

            // then
            verify(messageQueueService).publish(any(NotificationMessage.class));
        }
    }

    @Nested
    @DisplayName("sendSideEffectRecordReminder")
    class SendSideEffectRecordReminder {

        @Test
        @DisplayName("성공: 부작용 기록 권유 알림 발송")
        void success() {
            // given
            when(queueProperties.enabled()).thenReturn(true);
            String reason = "최근 복용 시작한 약이 있습니다.";

            // when
            queuedNotificationService.sendSideEffectRecordReminder(TEST_MEMBER_ID, reason);

            // then
            verify(messageQueueService).publish(any(NotificationMessage.class));
        }
    }

    @Nested
    @DisplayName("sendDiaryReminder")
    class SendDiaryReminder {

        @Test
        @DisplayName("성공: 일기 작성 알림 발송")
        void success() {
            // given
            when(queueProperties.enabled()).thenReturn(true);

            // when
            queuedNotificationService.sendDiaryReminder(TEST_MEMBER_ID);

            // then
            verify(messageQueueService).publish(any(NotificationMessage.class));
        }
    }

    @Nested
    @DisplayName("sendNegativeMoodAlert")
    class SendNegativeMoodAlert {

        @Test
        @DisplayName("성공: 부정적 기분 지속 알림 발송")
        void success() {
            // given
            when(queueProperties.enabled()).thenReturn(true);
            int consecutiveDays = 3;

            // when
            queuedNotificationService.sendNegativeMoodAlert(TEST_MEMBER_ID, consecutiveDays);

            // then
            verify(messageQueueService).publish(any(NotificationMessage.class));
        }
    }

    @Nested
    @DisplayName("큐 비활성화 상태에서 각종 알림")
    class QueueDisabled {

        @Test
        @DisplayName("성공: 큐 비활성화 시 심각한 부작용 알림 직접 발송")
        void severeSideEffectAlert_directSend() {
            // given
            when(queueProperties.enabled()).thenReturn(false);

            // when
            queuedNotificationService.sendSevereSideEffectAlert(TEST_MEMBER_ID, "두통", 4);

            // then
            verify(fcmPushService).sendPushNotification(
                    eq(TEST_MEMBER_ID),
                    anyString(),
                    anyString(),
                    anyMap()
            );
            verify(messageQueueService, never()).publish(any(NotificationMessage.class));
        }

        @Test
        @DisplayName("성공: 큐 비활성화 시 일기 알림 직접 발송")
        void diaryReminder_directSend() {
            // given
            when(queueProperties.enabled()).thenReturn(false);

            // when
            queuedNotificationService.sendDiaryReminder(TEST_MEMBER_ID);

            // then
            verify(fcmPushService).sendPushNotification(
                    eq(TEST_MEMBER_ID),
                    anyString(),
                    anyString(),
                    anyMap()
            );
            verify(messageQueueService, never()).publish(any(NotificationMessage.class));
        }
    }
}
