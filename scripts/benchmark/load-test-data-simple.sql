-- ============================================
-- Hamalog 성능 벤치마크용 테스트 데이터 (간소화 버전)
-- ============================================
-- Docker exec에서 실행 가능한 간소화된 버전
-- DELIMITER 없이 동작

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
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM member WHERE login_id = 'benchmark@test.com');

-- 벤치마크 멤버 ID 조회
SET @benchmark_member_id = (SELECT member_id FROM member WHERE login_id = 'benchmark@test.com');

-- ============================================
-- 2. 기존 벤치마크 데이터 삭제
-- ============================================
DELETE FROM medication_record WHERE medication_schedule_id IN
    (SELECT medication_schedule_id FROM medication_schedule WHERE member_id = @benchmark_member_id);
DELETE FROM medication_time WHERE medication_schedule_id IN
    (SELECT medication_schedule_id FROM medication_schedule WHERE member_id = @benchmark_member_id);
DELETE FROM medication_schedule WHERE member_id = @benchmark_member_id;

-- ============================================
-- 3. 복약 스케줄 생성 (100개 - 간소화)
-- ============================================
-- 반복문 대신 INSERT ... SELECT로 대량 생성
INSERT INTO medication_schedule (member_id, name, hospital_name, prescription_date, memo, start_of_ad, prescription_days, per_day, alarm_type, is_active, created_at)
SELECT @benchmark_member_id, CONCAT('벤치마크약물_', LPAD(n.num, 3, '0')), CONCAT('벤치마크병원_', LPAD(n.num, 3, '0')),
       DATE_SUB(CURDATE(), INTERVAL n.num DAY), CONCAT('벤치마크 테스트 메모 #', n.num), CURDATE(), 30,
       (n.num % 4) + 1, CASE n.num % 3 WHEN 0 THEN 'SOUND' WHEN 1 THEN 'VIBRATION' ELSE 'NONE' END,
       IF(n.num % 10 = 0, 0, 1), NOW()
FROM (
    SELECT 1 AS num UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
    UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
    UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15
    UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20
    UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24 UNION SELECT 25
    UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29 UNION SELECT 30
    UNION SELECT 31 UNION SELECT 32 UNION SELECT 33 UNION SELECT 34 UNION SELECT 35
    UNION SELECT 36 UNION SELECT 37 UNION SELECT 38 UNION SELECT 39 UNION SELECT 40
    UNION SELECT 41 UNION SELECT 42 UNION SELECT 43 UNION SELECT 44 UNION SELECT 45
    UNION SELECT 46 UNION SELECT 47 UNION SELECT 48 UNION SELECT 49 UNION SELECT 50
    UNION SELECT 51 UNION SELECT 52 UNION SELECT 53 UNION SELECT 54 UNION SELECT 55
    UNION SELECT 56 UNION SELECT 57 UNION SELECT 58 UNION SELECT 59 UNION SELECT 60
    UNION SELECT 61 UNION SELECT 62 UNION SELECT 63 UNION SELECT 64 UNION SELECT 65
    UNION SELECT 66 UNION SELECT 67 UNION SELECT 68 UNION SELECT 69 UNION SELECT 70
    UNION SELECT 71 UNION SELECT 72 UNION SELECT 73 UNION SELECT 74 UNION SELECT 75
    UNION SELECT 76 UNION SELECT 77 UNION SELECT 78 UNION SELECT 79 UNION SELECT 80
    UNION SELECT 81 UNION SELECT 82 UNION SELECT 83 UNION SELECT 84 UNION SELECT 85
    UNION SELECT 86 UNION SELECT 87 UNION SELECT 88 UNION SELECT 89 UNION SELECT 90
    UNION SELECT 91 UNION SELECT 92 UNION SELECT 93 UNION SELECT 94 UNION SELECT 95
    UNION SELECT 96 UNION SELECT 97 UNION SELECT 98 UNION SELECT 99 UNION SELECT 100
) n;

-- ============================================
-- 4. 복약 시간 생성 (각 스케줄당 2개씩)
-- ============================================
INSERT INTO medication_time (medication_schedule_id, take_time)
SELECT ms.medication_schedule_id, '08:00:00'
FROM medication_schedule ms
WHERE ms.member_id = @benchmark_member_id;

INSERT INTO medication_time (medication_schedule_id, take_time)
SELECT ms.medication_schedule_id, '20:00:00'
FROM medication_schedule ms
WHERE ms.member_id = @benchmark_member_id
  AND ms.per_day >= 2;

-- ============================================
-- 5. 데이터 확인
-- ============================================
SELECT 'Benchmark Data Loaded' AS status;
SELECT @benchmark_member_id AS benchmark_member_id;
SELECT COUNT(*) AS total_schedules FROM medication_schedule WHERE member_id = @benchmark_member_id;
SELECT COUNT(*) AS total_times FROM medication_time mt
    JOIN medication_schedule ms ON mt.medication_schedule_id = ms.medication_schedule_id
    WHERE ms.member_id = @benchmark_member_id;

