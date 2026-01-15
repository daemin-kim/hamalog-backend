-- ============================================
-- Hamalog 성능 벤치마크용 테스트 데이터
-- ============================================
--
-- 용도: N+1 문제 Before/After 비교를 위한 대량 테스트 데이터 생성
-- 실행 방법: mysql -u root -p hamalog < scripts/benchmark/load-test-data.sql
--
-- 주의: 테스트/개발 환경에서만 사용할 것

-- ============================================
-- 1. 벤치마크 테스트 사용자 생성
-- ============================================
-- 비밀번호: Benchmark1234! (BCrypt 인코딩됨)
INSERT INTO member (login_id, password, name, phone_number, nick_name, birth, created_at)
SELECT 'benchmark@test.com',
       '$2a$10$N9qo8uLOickgx2ZMRZoMye.AQ3.NhZDdy/M7H3rN0Y5VQ3R3qD.Ei',
       '벤치마크테스트유저',
       '01000000000',
       'benchmark_user',
       '1990-01-01',
       NOW()
WHERE NOT EXISTS (SELECT 1 FROM member WHERE login_id = 'benchmark@test.com');

SET @benchmark_member_id = (SELECT member_id FROM member WHERE login_id = 'benchmark@test.com');

-- ============================================
-- 2. 대량 복약 스케줄 생성 (100개)
-- ============================================
-- N+1 문제 효과를 극대화하기 위해 많은 레코드 생성

DELIMITER //

DROP PROCEDURE IF EXISTS generate_benchmark_schedules//

CREATE PROCEDURE generate_benchmark_schedules()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE schedule_count INT DEFAULT 100;

    -- 기존 벤치마크 데이터 삭제
    DELETE FROM medication_record WHERE medication_schedule_id IN
        (SELECT medication_schedule_id FROM medication_schedule WHERE member_id = @benchmark_member_id);
    DELETE FROM medication_time WHERE medication_schedule_id IN
        (SELECT medication_schedule_id FROM medication_schedule WHERE member_id = @benchmark_member_id);
    DELETE FROM medication_schedule WHERE member_id = @benchmark_member_id;

    -- 복약 스케줄 생성
    WHILE i <= schedule_count DO
        INSERT INTO medication_schedule (
            member_id,
            name,
            hospital_name,
            prescription_date,
            memo,
            start_of_ad,
            prescription_days,
            per_day,
            alarm_type,
            is_active,
            created_at
        ) VALUES (
            @benchmark_member_id,
            CONCAT('벤치마크약물_', LPAD(i, 3, '0')),
            CONCAT('벤치마크병원_', LPAD(i, 3, '0')),
            DATE_SUB(CURDATE(), INTERVAL i DAY),
            CONCAT('벤치마크 테스트 메모 #', i, ' - N+1 문제 재현용 데이터'),
            CURDATE(),
            30,
            MOD(i, 4) + 1,  -- 1~4회/일
            CASE MOD(i, 3)
                WHEN 0 THEN 'SOUND'
                WHEN 1 THEN 'VIBRATION'
                ELSE 'NONE'
            END,
            IF(MOD(i, 10) = 0, 0, 1),  -- 10개 중 1개는 비활성
            NOW()
        );

        SET i = i + 1;
    END WHILE;

    SELECT CONCAT('Created ', schedule_count, ' medication schedules for benchmark user') AS result;
END//

DELIMITER ;

-- 프로시저 실행
CALL generate_benchmark_schedules();

-- 프로시저 삭제
DROP PROCEDURE IF EXISTS generate_benchmark_schedules;

-- ============================================
-- 3. 복약 시간 생성 (각 스케줄당 1~4개)
-- ============================================
INSERT INTO medication_time (medication_schedule_id, take_time)
SELECT
    ms.medication_schedule_id,
    ADDTIME('08:00:00', SEC_TO_TIME((ROW_NUMBER() OVER (PARTITION BY ms.medication_schedule_id ORDER BY ms.medication_schedule_id) - 1) * 6 * 3600))
FROM medication_schedule ms
CROSS JOIN (SELECT 1 AS n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) numbers
WHERE ms.member_id = @benchmark_member_id
  AND numbers.n <= ms.per_day;

-- ============================================
-- 4. 복약 기록 생성 (최근 30일)
-- ============================================
INSERT INTO medication_record (medication_schedule_id, medication_time_id, record_date, is_taken, taken_at, created_at)
SELECT
    mt.medication_schedule_id,
    mt.medication_time_id,
    DATE_SUB(CURDATE(), INTERVAL d.day_offset DAY),
    IF(RAND() > 0.2, 1, 0),  -- 80% 복용
    IF(RAND() > 0.2,
       TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL d.day_offset DAY), mt.take_time),
       NULL),
    NOW()
FROM medication_time mt
JOIN medication_schedule ms ON mt.medication_schedule_id = ms.medication_schedule_id
CROSS JOIN (
    SELECT 0 AS day_offset UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
    UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9
    UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14
    UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19
    UNION SELECT 20 UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24
    UNION SELECT 25 UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29
) d
WHERE ms.member_id = @benchmark_member_id
  AND ms.is_active = 1
ON DUPLICATE KEY UPDATE is_taken = VALUES(is_taken);

-- ============================================
-- 5. 데이터 확인
-- ============================================
SELECT 'Benchmark Data Summary' AS title;
SELECT '=====================' AS separator;
SELECT COUNT(*) AS total_schedules FROM medication_schedule WHERE member_id = @benchmark_member_id;
SELECT COUNT(*) AS total_times FROM medication_time mt
    JOIN medication_schedule ms ON mt.medication_schedule_id = ms.medication_schedule_id
    WHERE ms.member_id = @benchmark_member_id;
SELECT COUNT(*) AS total_records FROM medication_record mr
    JOIN medication_schedule ms ON mr.medication_schedule_id = ms.medication_schedule_id
    WHERE ms.member_id = @benchmark_member_id;

