package com.Hamalog.domain.events.notification;

import com.Hamalog.domain.events.DomainEvent;
import java.time.LocalTime;
import lombok.Getter;

/**
 * 알림 설정 변경 이벤트
 * 알림 설정이 변경될 때 발행되어 스케줄러 재등록 등 후속 작업 처리에 사용
 */
@Getter
public class NotificationSettingsUpdated extends DomainEvent {

    private final Long notificationSettingsId;
    private final Long memberId;
    private final String memberLoginId;
    private final Boolean pushEnabled;
    private final Boolean medicationReminderEnabled;
    private final Integer medicationReminderMinutesBefore;
    private final Boolean diaryReminderEnabled;
    private final LocalTime diaryReminderTime;
    private final Boolean quietHoursEnabled;
    private final LocalTime quietHoursStart;
    private final LocalTime quietHoursEnd;

    public NotificationSettingsUpdated(
            Long notificationSettingsId,
            Long memberId,
            String memberLoginId,
            Boolean pushEnabled,
            Boolean medicationReminderEnabled,
            Integer medicationReminderMinutesBefore,
            Boolean diaryReminderEnabled,
            LocalTime diaryReminderTime,
            Boolean quietHoursEnabled,
            LocalTime quietHoursStart,
            LocalTime quietHoursEnd
    ) {
        super();
        this.notificationSettingsId = notificationSettingsId;
        this.memberId = memberId;
        this.memberLoginId = memberLoginId;
        this.pushEnabled = pushEnabled;
        this.medicationReminderEnabled = medicationReminderEnabled;
        this.medicationReminderMinutesBefore = medicationReminderMinutesBefore;
        this.diaryReminderEnabled = diaryReminderEnabled;
        this.diaryReminderTime = diaryReminderTime;
        this.quietHoursEnabled = quietHoursEnabled;
        this.quietHoursStart = quietHoursStart;
        this.quietHoursEnd = quietHoursEnd;
    }

    @Override
    public String getAggregateId() {
        return memberId.toString();
    }

    /**
     * 푸시 알림이 비활성화되었는지 확인
     */
    public boolean isPushDisabled() {
        return !Boolean.TRUE.equals(pushEnabled);
    }

    /**
     * 일기 알림이 활성화되었는지 확인
     */
    public boolean isDiaryReminderActive() {
        return Boolean.TRUE.equals(pushEnabled) && Boolean.TRUE.equals(diaryReminderEnabled);
    }

    @Override
    public String toString() {
        return String.format(
                "NotificationSettingsUpdated{memberId=%d, push=%s, medication=%s, diary=%s, occurredOn=%s}",
                memberId, pushEnabled, medicationReminderEnabled, diaryReminderEnabled, getOccurredOn()
        );
    }
}
