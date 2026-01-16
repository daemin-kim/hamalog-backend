-- Hamalog Database Schema V2
-- 신규 기능 추가: 이미지 관리, 로그인 이력, 그룹 관리 개선
-- 작성일: 2025-12-23

-- 1. MedicationSchedule에 이미지 경로 컬럼 추가 (이미지 관리 API용)
-- MySQL 8.0에서는 ADD COLUMN IF NOT EXISTS가 지원되지 않으므로 프로시저 사용
DROP PROCEDURE IF EXISTS add_image_path_column;
DELIMITER //
CREATE PROCEDURE add_image_path_column()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'medication_schedule'
        AND COLUMN_NAME = 'image_path'
    ) THEN
        ALTER TABLE medication_schedule ADD COLUMN image_path VARCHAR(500) NULL COMMENT '저장된 이미지 파일 경로';
    END IF;
END //
DELIMITER ;
CALL add_image_path_column();
DROP PROCEDURE IF EXISTS add_image_path_column;

-- 2. 로그인 이력 테이블 생성 (보안 개선)
CREATE TABLE IF NOT EXISTS login_history (
    login_history_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    login_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45) NOT NULL COMMENT 'IPv4 또는 IPv6 주소',
    user_agent VARCHAR(500) NULL COMMENT '브라우저/앱 정보',
    device_type VARCHAR(50) NULL COMMENT 'MOBILE, DESKTOP, TABLET 등',
    login_status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS' COMMENT 'SUCCESS, FAILED',
    failure_reason VARCHAR(255) NULL COMMENT '로그인 실패 시 사유',
    session_id VARCHAR(100) NULL COMMENT '세션 식별자',
    is_active BOOLEAN DEFAULT TRUE COMMENT '세션 활성 상태',
    logout_time TIMESTAMP NULL COMMENT '로그아웃 시간',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_login_history_member_id (member_id),
    INDEX idx_login_history_login_time (login_time),
    INDEX idx_login_history_session_id (session_id),
    INDEX idx_login_history_active (member_id, is_active),
    FOREIGN KEY (member_id) REFERENCES member(member_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='로그인 이력 관리';

-- 3. MedicationScheduleGroup 테이블 개선 (member_id 추가)
-- MySQL 8.0 호환 - 프로시저로 조건부 컬럼 추가
DROP PROCEDURE IF EXISTS add_schedule_group_columns;
DELIMITER //
CREATE PROCEDURE add_schedule_group_columns()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'medication_schedule_group'
        AND COLUMN_NAME = 'member_id'
    ) THEN
        ALTER TABLE medication_schedule_group ADD COLUMN member_id BIGINT NULL COMMENT '회원 ID';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'medication_schedule_group'
        AND COLUMN_NAME = 'description'
    ) THEN
        ALTER TABLE medication_schedule_group ADD COLUMN description VARCHAR(255) NULL COMMENT '그룹 설명';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'medication_schedule_group'
        AND COLUMN_NAME = 'color'
    ) THEN
        ALTER TABLE medication_schedule_group ADD COLUMN color VARCHAR(7) NULL COMMENT '그룹 색상 (#RRGGBB)';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'medication_schedule_group'
        AND COLUMN_NAME = 'created_at'
    ) THEN
        ALTER TABLE medication_schedule_group ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
    END IF;
END //
DELIMITER ;
CALL add_schedule_group_columns();
DROP PROCEDURE IF EXISTS add_schedule_group_columns;

-- member_id가 NULL인 경우를 위한 인덱스는 나중에 데이터 마이그레이션 후 추가
-- ALTER TABLE medication_schedule_group ADD INDEX idx_schedule_group_member (member_id);

-- 4. 알림 설정 테이블 (푸시 알림 관리용)
CREATE TABLE IF NOT EXISTS notification_settings (
    notification_settings_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL UNIQUE,
    fcm_token VARCHAR(500) NULL COMMENT 'Firebase Cloud Messaging 토큰',
    push_enabled BOOLEAN DEFAULT TRUE COMMENT '푸시 알림 활성화',
    medication_reminder BOOLEAN DEFAULT TRUE COMMENT '복약 알림',
    diary_reminder BOOLEAN DEFAULT TRUE COMMENT '일기 작성 알림',
    quiet_hours_start TIME NULL COMMENT '방해금지 시작 시간',
    quiet_hours_end TIME NULL COMMENT '방해금지 종료 시간',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_notification_member (member_id),
    FOREIGN KEY (member_id) REFERENCES member(member_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='알림 설정';

-- 5. mood_diary 테이블에 template_answer4 컬럼 추가 (기존에 없다면)
DROP PROCEDURE IF EXISTS add_mood_diary_column;
DELIMITER //
CREATE PROCEDURE add_mood_diary_column()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'mood_diary'
        AND COLUMN_NAME = 'template_answer4'
    ) THEN
        ALTER TABLE mood_diary ADD COLUMN template_answer4 TEXT NULL COMMENT '템플릿 답변 4';
    END IF;
END //
DELIMITER ;
CALL add_mood_diary_column();
DROP PROCEDURE IF EXISTS add_mood_diary_column;

-- 6. medication_schedule에 활성 상태 컬럼 추가 (필터링용)
DROP PROCEDURE IF EXISTS add_is_active_column;
DELIMITER //
CREATE PROCEDURE add_is_active_column()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'medication_schedule'
        AND COLUMN_NAME = 'is_active'
    ) THEN
        ALTER TABLE medication_schedule ADD COLUMN is_active BOOLEAN DEFAULT TRUE COMMENT '활성 상태';
    END IF;
END //
DELIMITER ;
CALL add_is_active_column();
DROP PROCEDURE IF EXISTS add_is_active_column;

