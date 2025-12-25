package com.Hamalog.dto.notification.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalTime;

/**
 * 알림 설정 수정 요청 DTO
 */
public record NotificationSettingsUpdateRequest(
        // 전체 푸시 알림 활성화
        Boolean pushEnabled,

        // 복약 알림 활성화
        Boolean medicationReminderEnabled,

        // 복약 알림 사전 알림 시간 (분 단위, 0~60)
        @Min(value = 0, message = "사전 알림 시간은 0분 이상이어야 합니다")
        @Max(value = 60, message = "사전 알림 시간은 60분 이하여야 합니다")
        Integer medicationReminderMinutesBefore,

        // 일기 작성 리마인더 활성화
        Boolean diaryReminderEnabled,

        // 일기 작성 리마인더 시간 (HH:mm 형식)
        LocalTime diaryReminderTime,

        // 방해 금지 모드 활성화
        Boolean quietHoursEnabled,

        // 방해 금지 시작 시간
        LocalTime quietHoursStart,

        // 방해 금지 종료 시간
        LocalTime quietHoursEnd
) {}
