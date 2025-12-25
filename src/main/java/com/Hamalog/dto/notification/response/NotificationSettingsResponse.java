package com.Hamalog.dto.notification.response;

import com.Hamalog.domain.notification.NotificationSettings;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 알림 설정 응답 DTO
 */
public record NotificationSettingsResponse(
        Long notificationSettingsId,
        Long memberId,
        boolean pushEnabled,
        boolean medicationReminderEnabled,
        Integer medicationReminderMinutesBefore,
        boolean diaryReminderEnabled,
        LocalTime diaryReminderTime,
        boolean quietHoursEnabled,
        LocalTime quietHoursStart,
        LocalTime quietHoursEnd,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * Entity → DTO 변환 팩토리 메서드
     */
    public static NotificationSettingsResponse from(NotificationSettings entity) {
        return new NotificationSettingsResponse(
                entity.getNotificationSettingsId(),
                entity.getMember().getMemberId(),
                entity.isPushEnabled(),
                entity.isMedicationReminderEnabled(),
                entity.getMedicationReminderMinutesBefore(),
                entity.isDiaryReminderEnabled(),
                entity.getDiaryReminderTime(),
                entity.isQuietHoursEnabled(),
                entity.getQuietHoursStart(),
                entity.getQuietHoursEnd(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
