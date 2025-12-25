package com.Hamalog.domain.notification;

/**
 * 알림 타입 열거형
 */
public enum NotificationType {
    // 복약 알림
    MEDICATION_REMINDER("복약 알림", "약 복용 시간입니다"),

    // 일기 작성 리마인더
    DIARY_REMINDER("일기 리마인더", "오늘 하루는 어떠셨나요?"),

    // 복약 미복용 알림
    MEDICATION_MISSED("미복용 알림", "복약 시간이 지났습니다"),

    // 시스템 알림
    SYSTEM("시스템 알림", "");

    private final String title;
    private final String defaultMessage;

    NotificationType(String title, String defaultMessage) {
        this.title = title;
        this.defaultMessage = defaultMessage;
    }

    public String getTitle() {
        return title;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
