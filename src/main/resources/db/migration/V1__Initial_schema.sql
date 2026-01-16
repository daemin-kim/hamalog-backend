-- Hamalog Database Schema V1
-- 초기 스키마 생성

-- 1. Member (회원) 테이블
CREATE TABLE IF NOT EXISTS member (
    member_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    login_id VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(60) NOT NULL,
    name VARCHAR(15) NOT NULL,
    phone_number VARCHAR(255) NOT NULL,
    nickname VARCHAR(10) NOT NULL,
    birthday VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deletion_requested_at TIMESTAMP NULL,
    deletion_due_at TIMESTAMP NULL,
    deletion_scheduled BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT DEFAULT 0,
    INDEX idx_member_login_id (login_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Refresh Token (리프레시 토큰) 테이블
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    token_value VARCHAR(500) NOT NULL UNIQUE,
    created_at DATETIME NOT NULL,
    expires_at DATETIME NOT NULL,
    rotated_at DATETIME NOT NULL,
    last_used_at DATETIME,
    reuse_detected_at DATETIME,
    reuse_client_fingerprint VARCHAR(255),
    reuse_detected BOOLEAN NOT NULL DEFAULT FALSE,
    reuse_count BIGINT NOT NULL DEFAULT 0,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_refresh_tokens_member_id (member_id),
    INDEX idx_refresh_tokens_token_value (token_value),
    INDEX idx_refresh_tokens_expires_at (expires_at),
    FOREIGN KEY (member_id) REFERENCES member(member_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Mood Diary (마음 일기) 테이블
CREATE TABLE IF NOT EXISTS mood_diary (
    mood_diary_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    mood_type VARCHAR(20) NOT NULL,
    diary_type VARCHAR(20) NOT NULL,
    template_answer1 VARCHAR(500),
    template_answer2 VARCHAR(500),
    template_answer3 VARCHAR(500),
    template_answer4 VARCHAR(500),
    free_content VARCHAR(1500),
    diary_date DATE NOT NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_mood_diary_member_id (member_id),
    INDEX idx_mood_diary_diary_date (diary_date),
    UNIQUE KEY uk_mood_diary_member_date (member_id, diary_date),
    FOREIGN KEY (member_id) REFERENCES member(member_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Medication Schedule Group (복약 스케줄 그룹) 테이블
CREATE TABLE IF NOT EXISTS medication_schedule_group (
    medication_schedule_group_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_name VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Medication Schedule (복약 스케줄) 테이블
CREATE TABLE IF NOT EXISTS medication_schedule (
    medication_schedule_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    name VARCHAR(20) NOT NULL,
    hospital_name VARCHAR(20) NOT NULL,
    prescription_date DATE NOT NULL,
    memo TEXT,
    start_of_ad DATE NOT NULL,
    prescription_days INT NOT NULL,
    per_day INT NOT NULL,
    alarm_type VARCHAR(50) NOT NULL,
    image_path VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    version BIGINT DEFAULT 0,
    INDEX idx_medication_schedule_member (member_id),
    FOREIGN KEY (member_id) REFERENCES member(member_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Medication Schedule - Group 연결 테이블
CREATE TABLE IF NOT EXISTS medication_schedule_medication_schedule_group (
    medication_schedule_id BIGINT NOT NULL,
    medication_schedule_group_id BIGINT NOT NULL,
    PRIMARY KEY (medication_schedule_id, medication_schedule_group_id),
    FOREIGN KEY (medication_schedule_id) REFERENCES medication_schedule(medication_schedule_id) ON DELETE CASCADE,
    FOREIGN KEY (medication_schedule_group_id) REFERENCES medication_schedule_group(medication_schedule_group_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Medication Time (복약 시간) 테이블
CREATE TABLE IF NOT EXISTS medication_time (
    medication_time_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    medication_schedule_id BIGINT NOT NULL,
    take_time TIME NOT NULL,
    FOREIGN KEY (medication_schedule_id) REFERENCES medication_schedule(medication_schedule_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Medication Record (복약 기록) 테이블
CREATE TABLE IF NOT EXISTS medication_record (
    medication_record_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    medication_schedule_id BIGINT NOT NULL,
    medication_time_id BIGINT NOT NULL,
    is_take_medication BOOLEAN NOT NULL DEFAULT FALSE,
    real_take_time DATETIME,
    version BIGINT DEFAULT 0,
    INDEX idx_medication_record_schedule (medication_schedule_id),
    FOREIGN KEY (medication_schedule_id) REFERENCES medication_schedule(medication_schedule_id) ON DELETE CASCADE,
    FOREIGN KEY (medication_time_id) REFERENCES medication_time(medication_time_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. Side Effect (부작용) 테이블
CREATE TABLE IF NOT EXISTS side_effect (
    side_effect_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. Side Effect Degree (부작용 정도) 테이블
CREATE TABLE IF NOT EXISTS side_effect_degree (
    side_effect_degree_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    degree_name VARCHAR(50) NOT NULL,
    degree_value INT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. Side Effect Record (부작용 기록) 테이블
CREATE TABLE IF NOT EXISTS side_effect_record (
    side_effect_record_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    linked_medication_schedule_id BIGINT NULL COMMENT '연계된 복약 스케줄 ID (선택)',
    created_at DATETIME NOT NULL,
    description TEXT,
    INDEX idx_side_effect_record_member (member_id),
    INDEX idx_side_effect_linked_schedule (linked_medication_schedule_id),
    FOREIGN KEY (member_id) REFERENCES member(member_id) ON DELETE CASCADE,
    FOREIGN KEY (linked_medication_schedule_id) REFERENCES medication_schedule(medication_schedule_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. Side Effect - Side Effect Record 연결 테이블
CREATE TABLE IF NOT EXISTS side_effect_side_effect_record (
    side_effect_id BIGINT NOT NULL,
    side_effect_record_id BIGINT NOT NULL,
    PRIMARY KEY (side_effect_id, side_effect_record_id),
    FOREIGN KEY (side_effect_id) REFERENCES side_effect(side_effect_id) ON DELETE CASCADE,
    FOREIGN KEY (side_effect_record_id) REFERENCES side_effect_record(side_effect_record_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

