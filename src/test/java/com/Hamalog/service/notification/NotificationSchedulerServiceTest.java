package com.Hamalog.service.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.Hamalog.repository.notification.NotificationSettingsRepository;
import java.time.LocalTime;
import java.util.concurrent.ScheduledFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.CronTrigger;

@ExtendWith(MockitoExtension.class)
@DisplayName("알림 스케줄러 서비스 테스트")
class NotificationSchedulerServiceTest {

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private NotificationSettingsRepository notificationSettingsRepository;

    @Mock
    private FcmPushService fcmPushService;

    private NotificationSchedulerService notificationSchedulerService;

    @BeforeEach
    void setUp() {
        notificationSchedulerService = new NotificationSchedulerService(
                taskScheduler,
                notificationSettingsRepository,
                fcmPushService
        );
    }

    @Nested
    @DisplayName("scheduleDiaryReminder")
    class ScheduleDiaryReminder {

        @Test
        @DisplayName("성공: 일기 알림 스케줄 등록")
        void success() {
            // given
            Long memberId = 1L;
            LocalTime reminderTime = LocalTime.of(21, 0);

            ScheduledFuture<?> mockFuture = mock(ScheduledFuture.class);
            doReturn(mockFuture).when(taskScheduler).schedule(any(Runnable.class), any(Trigger.class));

            // when
            notificationSchedulerService.scheduleDiaryReminder(memberId, reminderTime);

            // then
            ArgumentCaptor<CronTrigger> cronCaptor = ArgumentCaptor.forClass(CronTrigger.class);
            verify(taskScheduler).schedule(any(Runnable.class), cronCaptor.capture());

            CronTrigger capturedTrigger = cronCaptor.getValue();
            assertThat(capturedTrigger.getExpression()).isEqualTo("0 0 21 * * *");
        }

        @Test
        @DisplayName("성공: 기존 스케줄이 있으면 취소 후 재등록")
        void success_cancelExistingSchedule() {
            // given
            Long memberId = 1L;
            LocalTime firstTime = LocalTime.of(20, 0);
            LocalTime secondTime = LocalTime.of(21, 0);

            ScheduledFuture<?> mockFuture1 = mock(ScheduledFuture.class);
            ScheduledFuture<?> mockFuture2 = mock(ScheduledFuture.class);
            when(mockFuture1.isCancelled()).thenReturn(false);

            doReturn(mockFuture1, mockFuture2).when(taskScheduler).schedule(any(Runnable.class), any(Trigger.class));

            // when
            notificationSchedulerService.scheduleDiaryReminder(memberId, firstTime);
            notificationSchedulerService.scheduleDiaryReminder(memberId, secondTime);

            // then
            verify(mockFuture1).cancel(false);
            assertThat(notificationSchedulerService.getScheduledTaskCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("cancelDiaryReminder")
    class CancelDiaryReminder {

        @Test
        @DisplayName("성공: 일기 알림 취소")
        void success() {
            // given
            Long memberId = 1L;
            ScheduledFuture<?> mockFuture = mock(ScheduledFuture.class);
            when(mockFuture.isCancelled()).thenReturn(false);
            doReturn(mockFuture).when(taskScheduler).schedule(any(Runnable.class), any(Trigger.class));

            notificationSchedulerService.scheduleDiaryReminder(memberId, LocalTime.of(21, 0));

            // when
            notificationSchedulerService.cancelDiaryReminder(memberId);

            // then
            verify(mockFuture).cancel(false);
            assertThat(notificationSchedulerService.getScheduledTaskCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("성공: 스케줄이 없어도 예외 없이 처리")
        void success_noScheduleExists() {
            // given
            Long memberId = 999L;

            // when & then - 예외 없이 정상 처리
            notificationSchedulerService.cancelDiaryReminder(memberId);
            assertThat(notificationSchedulerService.getScheduledTaskCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("cancelAllScheduledNotifications")
    class CancelAllScheduledNotifications {

        @Test
        @DisplayName("성공: 모든 알림 취소")
        void success() {
            // given
            Long memberId = 1L;
            ScheduledFuture<?> mockFuture = mock(ScheduledFuture.class);
            when(mockFuture.isCancelled()).thenReturn(false);
            doReturn(mockFuture).when(taskScheduler).schedule(any(Runnable.class), any(Trigger.class));

            notificationSchedulerService.scheduleDiaryReminder(memberId, LocalTime.of(21, 0));

            // when
            notificationSchedulerService.cancelAllScheduledNotifications(memberId);

            // then
            verify(mockFuture).cancel(false);
        }
    }

    @Nested
    @DisplayName("getScheduledTaskCount")
    class GetScheduledTaskCount {

        @Test
        @DisplayName("성공: 스케줄된 작업 수 조회")
        void success() {
            // given
            ScheduledFuture<?> mockFuture = mock(ScheduledFuture.class);
            doReturn(mockFuture).when(taskScheduler).schedule(any(Runnable.class), any(Trigger.class));

            // when
            notificationSchedulerService.scheduleDiaryReminder(1L, LocalTime.of(21, 0));
            notificationSchedulerService.scheduleDiaryReminder(2L, LocalTime.of(22, 0));

            // then
            assertThat(notificationSchedulerService.getScheduledTaskCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("initializeSchedules")
    class InitializeSchedules {

        @Test
        @DisplayName("성공: 초기화 메서드 호출")
        void success() {
            // when & then - 예외 없이 정상 호출
            notificationSchedulerService.initializeSchedules();
        }
    }
}
