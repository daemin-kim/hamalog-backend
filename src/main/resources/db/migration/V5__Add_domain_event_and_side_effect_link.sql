-- V5: 도메인 이벤트 저장소 추가
-- 이벤트 소싱 및 감사 로그를 위한 테이블

-- 도메인 이벤트 저장 테이블
CREATE TABLE IF NOT EXISTS domain_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(100) NOT NULL COMMENT '이벤트 고유 ID (UUID)',
    event_type VARCHAR(100) NOT NULL COMMENT '이벤트 타입 (클래스명)',
    aggregate_type VARCHAR(100) NOT NULL COMMENT '집계 루트 타입',
    aggregate_id VARCHAR(100) NOT NULL COMMENT '집계 루트 ID',
    payload TEXT NOT NULL COMMENT '이벤트 페이로드 (JSON)',
    occurred_on DATETIME(6) NOT NULL COMMENT '이벤트 발생 시간',
    stored_at DATETIME(6) NOT NULL COMMENT '저장 시간',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '처리 상태 (PENDING, PROCESSED, FAILED)',
    processed_at DATETIME(6) NULL COMMENT '처리 완료 시간',
    error_message VARCHAR(500) NULL COMMENT '오류 메시지',
    retry_count INT DEFAULT 0 COMMENT '재시도 횟수',

    INDEX idx_domain_event_aggregate (aggregate_type, aggregate_id),
    INDEX idx_domain_event_occurred (occurred_on),
    INDEX idx_domain_event_type (event_type),
    INDEX idx_domain_event_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='도메인 이벤트 저장소 - 감사 로그 및 이벤트 소싱용';

-- 부작용 기록에 linked_medication_schedule_id는 이미 V1에서 추가됨

