-- V3: 푸시 알림 기능 테이블 추가
-- 알림 설정 테이블
CREATE TABLE IF NOT EXISTS notification_settings (
    notification_settings_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL UNIQUE,
    push_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    medication_reminder_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    medication_reminder_minutes_before INT DEFAULT 10,
    diary_reminder_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    diary_reminder_time TIME DEFAULT '21:00:00',
    quiet_hours_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    quiet_hours_start TIME DEFAULT '23:00:00',
    quiet_hours_end TIME DEFAULT '07:00:00',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_notification_settings_member
        FOREIGN KEY (member_id) REFERENCES member(member_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- FCM 디바이스 토큰 테이블
CREATE TABLE IF NOT EXISTS fcm_device_token (
    fcm_device_token_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    token VARCHAR(500) NOT NULL,
    device_type VARCHAR(20) NOT NULL,
    device_name VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_used_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,
    CONSTRAINT fk_fcm_device_token_member
        FOREIGN KEY (member_id) REFERENCES member(member_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- FCM 토큰 인덱스 생성
CREATE INDEX idx_fcm_device_token_member ON fcm_device_token(member_id);
CREATE INDEX idx_fcm_device_token_token ON fcm_device_token(token);
CREATE INDEX idx_fcm_device_token_active ON fcm_device_token(member_id, is_active);

