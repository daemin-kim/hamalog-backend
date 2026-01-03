package com.Hamalog.service.queue.message;

/**
 * 알림 유형 상수
 */
public final class NotificationType {

    private NotificationType() {
        // 인스턴스화 방지
    }

    // 부작용 관련
    public static final String SEVERE_SIDE_EFFECT = "SEVERE_SIDE_EFFECT";
    public static final String MEDICAL_CONSULTATION = "MEDICAL_CONSULTATION";
    public static final String SIDE_EFFECT_REMINDER = "SIDE_EFFECT_REMINDER";

    // 복약 관련
    public static final String MISSED_MEDICATION = "MISSED_MEDICATION";
    public static final String MEDICATION_REMINDER = "MEDICATION_REMINDER";

    // 성취 관련
    public static final String ACHIEVEMENT_CONSECUTIVE_MEDICATION = "ACHIEVEMENT_CONSECUTIVE_MEDICATION";
    public static final String ACHIEVEMENT_CONSECUTIVE_DIARY = "ACHIEVEMENT_CONSECUTIVE_DIARY";

    // 일기 관련
    public static final String DIARY_REMINDER = "DIARY_REMINDER";
    public static final String NEGATIVE_MOOD_ALERT = "NEGATIVE_MOOD_ALERT";

    // 일반
    public static final String GENERAL = "GENERAL";
}
