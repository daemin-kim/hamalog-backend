-- RefreshToken 테이블 마이그레이션 스크립트
-- 작성 날짜: 2025-11-20
-- 목적: JWT RefreshToken 메커니즘 구현
-- 데이터베이스: MySQL

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    token_value VARCHAR(500) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    rotated_at TIMESTAMP NOT NULL,
    is_revoked BOOLEAN NOT NULL DEFAULT false,

    -- 인덱스 생성
    KEY idx_member_id (member_id),
    KEY idx_token_value (token_value),
    KEY idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 주석
COMMENT ON TABLE refresh_tokens IS 'JWT RefreshToken 저장소';

