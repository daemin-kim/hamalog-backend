package com.Hamalog.domain.notification;

import com.Hamalog.domain.member.Member;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.*;

/**
 * 알림 설정 엔티티
 * 사용자별 푸시 알림 설정을 관리합니다.
 */
@Entity
@Table(name = "notification_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_settings_id")
    private Long notificationSettingsId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    // 전체 알림 활성화 여부
    @Column(name = "push_enabled", nullable = false)
    private boolean pushEnabled;

    // 복약 알림 활성화
    @Column(name = "medication_reminder_enabled", nullable = false)
    private boolean medicationReminderEnabled;

    // 복약 알림 사전 알림 시간 (분 단위)
    @Column(name = "medication_reminder_minutes_before")
    private Integer medicationReminderMinutesBefore;

    // 일기 작성 리마인더 활성화
    @Column(name = "diary_reminder_enabled", nullable = false)
    private boolean diaryReminderEnabled;

    // 일기 작성 리마인더 시간
    @Column(name = "diary_reminder_time")
    private LocalTime diaryReminderTime;

    // 방해 금지 모드 활성화
    @Column(name = "quiet_hours_enabled", nullable = false)
    private boolean quietHoursEnabled;

    // 방해 금지 시작 시간
    @Column(name = "quiet_hours_start")
    private LocalTime quietHoursStart;

    // 방해 금지 종료 시간
    @Column(name = "quiet_hours_end")
    private LocalTime quietHoursEnd;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    /**
     * 새 알림 설정 생성 (기본값으로 초기화)
     */
    public NotificationSettings(Member member) {
        this.member = member;
        this.pushEnabled = true;
        this.medicationReminderEnabled = true;
        this.medicationReminderMinutesBefore = 10;
        this.diaryReminderEnabled = false;
        this.diaryReminderTime = LocalTime.of(21, 0); // 기본 저녁 9시
        this.quietHoursEnabled = false;
        this.quietHoursStart = LocalTime.of(23, 0);
        this.quietHoursEnd = LocalTime.of(7, 0);
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 알림 설정 업데이트
     */
    public void updateSettings(
            Boolean pushEnabled,
            Boolean medicationReminderEnabled,
            Integer medicationReminderMinutesBefore,
            Boolean diaryReminderEnabled,
            LocalTime diaryReminderTime,
            Boolean quietHoursEnabled,
            LocalTime quietHoursStart,
            LocalTime quietHoursEnd
    ) {
        if (pushEnabled != null) {
            this.pushEnabled = pushEnabled;
        }
        if (medicationReminderEnabled != null) {
            this.medicationReminderEnabled = medicationReminderEnabled;
        }
        if (medicationReminderMinutesBefore != null) {
            this.medicationReminderMinutesBefore = medicationReminderMinutesBefore;
        }
        if (diaryReminderEnabled != null) {
            this.diaryReminderEnabled = diaryReminderEnabled;
        }
        if (diaryReminderTime != null) {
            this.diaryReminderTime = diaryReminderTime;
        }
        if (quietHoursEnabled != null) {
            this.quietHoursEnabled = quietHoursEnabled;
        }
        if (quietHoursStart != null) {
            this.quietHoursStart = quietHoursStart;
        }
        if (quietHoursEnd != null) {
            this.quietHoursEnd = quietHoursEnd;
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 현재 시간이 방해 금지 시간대인지 확인
     */
    public boolean isQuietTime(LocalTime currentTime) {
        if (!quietHoursEnabled || quietHoursStart == null || quietHoursEnd == null) {
            return false;
        }

        // 자정을 넘어가는 경우 (예: 23:00 ~ 07:00)
        if (quietHoursStart.isAfter(quietHoursEnd)) {
            return currentTime.isAfter(quietHoursStart) || currentTime.isBefore(quietHoursEnd);
        }

        // 같은 날 내 (예: 13:00 ~ 15:00)
        return currentTime.isAfter(quietHoursStart) && currentTime.isBefore(quietHoursEnd);
    }
}
