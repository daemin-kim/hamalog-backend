-- Hamalog Database Schema V1
-- 초기 스키마 생성

-- 1. Member (회원) 테이블
CREATE TABLE IF NOT EXISTS member (
    member_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    login_id VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    nick_name VARCHAR(100),
    phone_number VARCHAR(20),
    birth DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_member_login_id (login_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Refresh Token (리프레시 토큰) 테이블
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    token_value VARCHAR(512) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    rotated_at TIMESTAMP,
    INDEX idx_refresh_tokens_member_id (member_id),
    INDEX idx_refresh_tokens_token_value (token_value),
    FOREIGN KEY (member_id) REFERENCES member(member_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Mood Diary (마음 일기) 테이블
CREATE TABLE IF NOT EXISTS mood_diary (
    mood_diary_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    mood_type VARCHAR(50) NOT NULL,
    diary_type VARCHAR(50) NOT NULL,
    content TEXT,
    template_answer1 TEXT,
    template_answer2 TEXT,
    template_answer3 TEXT,
    diary_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_mood_diary_member_date (member_id, diary_date),
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
    medication_name VARCHAR(255) NOT NULL,
    medication_nickname VARCHAR(255),
    dosage VARCHAR(100),
    alarm_type VARCHAR(50),
    alarm_time TIME,
    daily_doses INT,
    total_amount INT,
    remaining_amount INT,
    start_date DATE,
    end_date DATE,
    image_url VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
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
    time TIME NOT NULL,
    FOREIGN KEY (medication_schedule_id) REFERENCES medication_schedule(medication_schedule_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Medication Record (복약 기록) 테이블
CREATE TABLE IF NOT EXISTS medication_record (
    medication_record_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    medication_schedule_id BIGINT NOT NULL,
    is_taken BOOLEAN DEFAULT FALSE,
    taken_at TIMESTAMP,
    scheduled_time TIME,
    record_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_medication_record_schedule_date (medication_schedule_id, record_date),
    FOREIGN KEY (medication_schedule_id) REFERENCES medication_schedule(medication_schedule_id) ON DELETE CASCADE
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
    degree VARCHAR(50),
    note TEXT,
    record_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_side_effect_record_member_date (member_id, record_date),
    FOREIGN KEY (member_id) REFERENCES member(member_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. Side Effect - Side Effect Record 연결 테이블
CREATE TABLE IF NOT EXISTS side_effect_side_effect_record (
    side_effect_id BIGINT NOT NULL,
    side_effect_record_id BIGINT NOT NULL,
    PRIMARY KEY (side_effect_id, side_effect_record_id),
    FOREIGN KEY (side_effect_id) REFERENCES side_effect(side_effect_id) ON DELETE CASCADE,
    FOREIGN KEY (side_effect_record_id) REFERENCES side_effect_record(side_effect_record_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

