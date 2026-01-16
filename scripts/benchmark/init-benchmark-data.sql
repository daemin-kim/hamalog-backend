-- ============================================
-- Hamalog 벤치마크 테스트 데이터
-- ============================================
--
-- 용도: N+1 vs Optimized 성능 비교를 위한 충분한 데이터
--
-- 데이터 규모:
--   - Member: 1명 (벤치마크 사용자)
--   - MedicationSchedule: 100개
--   - MedicationTime: 각 Schedule당 3개 = 300개
--
-- N+1 문제 시:
--   - 1 (Schedule 목록) + 100 (각 Schedule의 Times) = 101 쿼리
-- Optimized (fetch join) 시:
--   - 1 쿼리

-- ============================================
-- 기존 데이터 정리 (Flyway 이후 실행)
-- ============================================
-- 외래키 제약 조건 임시 비활성화
SET FOREIGN_KEY_CHECKS = 0;

-- 기존 벤치마크 데이터 삭제 (존재하는 경우만)
DELETE IGNORE FROM medication_time WHERE medication_schedule_id IN (
    SELECT medication_schedule_id FROM medication_schedule WHERE member_id = 1
);
DELETE IGNORE FROM medication_record WHERE medication_schedule_id IN (
    SELECT medication_schedule_id FROM medication_schedule WHERE member_id = 1
);
DELETE IGNORE FROM medication_schedule WHERE member_id = 1;
DELETE IGNORE FROM refresh_tokens WHERE member_id = 1;
DELETE IGNORE FROM member WHERE member_id = 1;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================
-- 벤치마크 사용자 생성
-- ============================================
INSERT IGNORE INTO member (member_id, login_id, password, name, nick_name, created_at)
VALUES (
    1,
    'benchmark@test.com',
    -- BCrypt encoded 'Benchmark1234!'
    '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqgKLjHKvDqFfSqJHYT7YEqmPqK7W',
    'BenchmarkUser',
    'benchmark',
    NOW()
);

-- ============================================
-- 복약 스케줄 100개 생성
-- ============================================
-- 다양한 약물 이름으로 생성
INSERT IGNORE INTO medication_schedule (
    medication_schedule_id, member_id, medication_name, dosage, unit, frequency,
    start_date, end_date, is_active, created_at, updated_at, version
)
SELECT
    n.num,
    1,
    CONCAT('TestMedication_', n.num),
    CASE WHEN n.num % 3 = 0 THEN '1정' WHEN n.num % 3 = 1 THEN '2정' ELSE '0.5정' END,
    'tablet',
    CASE WHEN n.num % 4 = 0 THEN 'DAILY' WHEN n.num % 4 = 1 THEN 'TWICE_DAILY'
         WHEN n.num % 4 = 2 THEN 'THREE_TIMES_DAILY' ELSE 'AS_NEEDED' END,
    DATE_SUB(CURDATE(), INTERVAL (n.num % 30) DAY),
    DATE_ADD(CURDATE(), INTERVAL (90 + n.num % 60) DAY),
    TRUE,
    NOW(),
    NOW(),
    0
FROM (
    SELECT a.N + b.N * 10 + 1 AS num
    FROM
        (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
         UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a,
        (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
         UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) b
    ORDER BY num
    LIMIT 100
) n;

-- ============================================
-- 복약 시간 300개 생성 (각 스케줄당 3개)
-- ============================================
-- 아침, 점심, 저녁 시간대로 생성
INSERT IGNORE INTO medication_time (
    medication_time_id, medication_schedule_id, time, created_at, updated_at
)
SELECT
    (schedule_id - 1) * 3 + time_offset,
    schedule_id,
    CASE time_offset
        WHEN 1 THEN '08:00:00'
        WHEN 2 THEN '13:00:00'
        WHEN 3 THEN '19:00:00'
    END,
    NOW(),
    NOW()
FROM (
    SELECT medication_schedule_id AS schedule_id
    FROM medication_schedule
    WHERE member_id = 1
) schedules
CROSS JOIN (
    SELECT 1 AS time_offset UNION SELECT 2 UNION SELECT 3
) times;

-- ============================================
-- 데이터 검증
-- ============================================
SELECT 'Benchmark Data Summary' AS info;
SELECT COUNT(*) AS member_count FROM member WHERE member_id = 1;
SELECT COUNT(*) AS schedule_count FROM medication_schedule WHERE member_id = 1;
SELECT COUNT(*) AS time_count FROM medication_time WHERE medication_schedule_id IN (
    SELECT medication_schedule_id FROM medication_schedule WHERE member_id = 1
);

