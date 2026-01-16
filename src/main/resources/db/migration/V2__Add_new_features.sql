-- Hamalog Database Schema V2
-- 신규 기능 추가: 로그인 이력, 그룹 관리 개선
-- 작성일: 2025-12-23
-- 수정: 2026-01-16 - V1에 이미 있는 컬럼 제거

-- 1. 로그인 이력 테이블 생성 (보안 개선)
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

-- 2. MedicationScheduleGroup 테이블 개선 (member_id 추가)
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

-- 3. mood_diary 테이블에 template_answer4 컬럼은 이미 V1에서 추가됨
-- 빈 섹션으로 유지

