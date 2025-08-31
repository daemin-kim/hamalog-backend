-- 현재 프로젝트에 맞게 업데이트된 데이터베이스 스키마
-- Updated: 2025-08-31
-- 기존 스키마를 현재 Hamalog 프로젝트의 엔티티 구조에 맞게 수정

-- 테이블 삭제 (의존성 역순으로)
DROP TABLE IF EXISTS `medication_record`;
DROP TABLE IF EXISTS `medication_time`;
DROP TABLE IF EXISTS `medication_schedule_medication_schedule_group`;
DROP TABLE IF EXISTS `side_effect_side_effect_record`;
DROP TABLE IF EXISTS `medication_schedule`;
DROP TABLE IF EXISTS `side_effect_record`;
DROP TABLE IF EXISTS `medication_schedule_group`;
DROP TABLE IF EXISTS `side_effect`;
DROP TABLE IF EXISTS `member`;

-- 테이블 생성
CREATE TABLE `member` (
	`member_id`	bigint	NOT NULL AUTO_INCREMENT,
	`login_id`	varchar(100)	NOT NULL,  -- 이메일 형식이므로 길이를 100으로 증가
	`password`	varchar(60)	NOT NULL,   -- BCrypt 해시 길이에 맞춘 60자
	`name`	varchar(15)	NOT NULL,       -- 한글 이름을 고려하여 15자로 증가
	`phone_number`	varchar(11)	NOT NULL,   -- 01012345678 형식에 맞춘 11자
	`nickname`	varchar(10)	NOT NULL,       -- 새로 추가된 닉네임 필드
	`birthday`	date	NOT NULL,
	`created_at`	datetime	NOT NULL
);

CREATE TABLE `side_effect` (
	`side_effect_id`	bigint	NOT NULL AUTO_INCREMENT,
	`type`	varchar(20)	NOT NULL,
	`name`	varchar(20)	NOT NULL
);

CREATE TABLE `medication_schedule_group` (
	`medication_schedule_group_id`	bigint	NOT NULL AUTO_INCREMENT,
	`member_id`	bigint	NOT NULL,
	`name`	varchar(20)	NOT NULL
);

CREATE TABLE `side_effect_record` (
	`side_effect_record_id`	bigint	NOT NULL AUTO_INCREMENT,
	`member_id`	bigint	NOT NULL,
	`created_at`	datetime	NOT NULL
);

CREATE TABLE `medication_schedule` (
	`medication_schedule_id`	bigint	NOT NULL AUTO_INCREMENT,
	`member_id`	bigint	NOT NULL,
	`name`	varchar(20)	NOT NULL,
	`hospital_name`	varchar(20)	NOT NULL,
	`prescription_date`	date	NOT NULL,
	`memo`	text	NULL,
	`start_of_ad`	date	NOT NULL,
	`prescription_days`	int	NOT NULL,
	`per_day`	int	NOT NULL,
	`alarm_type`	varchar(10)	NOT NULL DEFAULT 'SOUND'  -- ENUM 대신 VARCHAR 사용, JPA enum 값에 맞춤
);

CREATE TABLE `medication_time` (
	`medication_time_id`	bigint	NOT NULL AUTO_INCREMENT,
	`medication_schedule_id`	bigint	NOT NULL,
	`take_time`	time	NOT NULL
);

CREATE TABLE `side_effect_side_effect_record` (
	`side_effect_record_id`	bigint	NOT NULL,
	`side_effect_id`	bigint	NOT NULL,
	`degree`	int	NOT NULL COMMENT '부작용 정도 (예: 1~5)'
);

CREATE TABLE `medication_record` (
	`medication_record_id`	bigint	NOT NULL AUTO_INCREMENT,
	`medication_schedule_id`	bigint	NOT NULL,
	`medication_time_id`	bigint	NOT NULL,
	`is_take_medication`	boolean	NOT NULL DEFAULT false,
	`real_take_time`	datetime	NULL
);

CREATE TABLE `medication_schedule_medication_schedule_group` (
	`medication_schedule_group_id`	bigint	NOT NULL,
	`medication_schedule_id`	bigint	NOT NULL
);

-- 기본 키(PK) 제약 조건 설정
ALTER TABLE `member` ADD CONSTRAINT `PK_MEMBER` PRIMARY KEY (`member_id`);
ALTER TABLE `side_effect` ADD CONSTRAINT `PK_SIDE_EFFECT` PRIMARY KEY (`side_effect_id`);
ALTER TABLE `medication_schedule_group` ADD CONSTRAINT `PK_MEDICATION_SCHEDULE_GROUP` PRIMARY KEY (`medication_schedule_group_id`);
ALTER TABLE `side_effect_record` ADD CONSTRAINT `PK_SIDE_EFFECT_RECORD` PRIMARY KEY (`side_effect_record_id`);
ALTER TABLE `medication_schedule` ADD CONSTRAINT `PK_MEDICATION_SCHEDULE` PRIMARY KEY (`medication_schedule_id`);
ALTER TABLE `medication_time` ADD CONSTRAINT `PK_MEDICATION_TIME` PRIMARY KEY (`medication_time_id`);
ALTER TABLE `side_effect_side_effect_record` ADD CONSTRAINT `PK_SIDE_EFFECT_SIDE_EFFECT_RECORD` PRIMARY KEY (`side_effect_record_id`, `side_effect_id`);
ALTER TABLE `medication_record` ADD CONSTRAINT `PK_MEDICATION_RECORD` PRIMARY KEY (`medication_record_id`);
ALTER TABLE `medication_schedule_medication_schedule_group` ADD CONSTRAINT `PK_MEDICATION_SCHEDULE_MEDICATION_SCHEDULE_GROUP` PRIMARY KEY (`medication_schedule_group_id`, `medication_schedule_id`);

-- 유니크 제약 조건 추가 (현재 엔티티 구조에 맞춤)
ALTER TABLE `member` ADD CONSTRAINT `UK_MEMBER_LOGIN_ID` UNIQUE (`login_id`);

-- 외래 키(FK) 제약 조건 설정
ALTER TABLE `medication_schedule_group` ADD CONSTRAINT `FK_ms_group_to_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`);
ALTER TABLE `side_effect_record` ADD CONSTRAINT `FK_se_record_to_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`);
ALTER TABLE `medication_schedule` ADD CONSTRAINT `FK_ms_to_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`);
ALTER TABLE `medication_time` ADD CONSTRAINT `FK_m_time_to_ms` FOREIGN KEY (`medication_schedule_id`) REFERENCES `medication_schedule` (`medication_schedule_id`);
ALTER TABLE `side_effect_side_effect_record` ADD CONSTRAINT `FK_seser_to_se_record` FOREIGN KEY (`side_effect_record_id`) REFERENCES `side_effect_record` (`side_effect_record_id`);
ALTER TABLE `side_effect_side_effect_record` ADD CONSTRAINT `FK_seser_to_se` FOREIGN KEY (`side_effect_id`) REFERENCES `side_effect` (`side_effect_id`);
ALTER TABLE `medication_record` ADD CONSTRAINT `FK_m_record_to_ms` FOREIGN KEY (`medication_schedule_id`) REFERENCES `medication_schedule` (`medication_schedule_id`);
ALTER TABLE `medication_record` ADD CONSTRAINT `FK_m_record_to_m_time` FOREIGN KEY (`medication_time_id`) REFERENCES `medication_time` (`medication_time_id`);
ALTER TABLE `medication_schedule_medication_schedule_group` ADD CONSTRAINT `FK_msmsg_to_ms_group` FOREIGN KEY (`medication_schedule_group_id`) REFERENCES `medication_schedule_group` (`medication_schedule_group_id`);
ALTER TABLE `medication_schedule_medication_schedule_group` ADD CONSTRAINT `FK_msmsg_to_ms` FOREIGN KEY (`medication_schedule_id`) REFERENCES `medication_schedule` (`medication_schedule_id`);

-- 인덱스 추가 (성능 최적화를 위한 권장사항)
CREATE INDEX `idx_member_login_id` ON `member` (`login_id`);
CREATE INDEX `idx_medication_schedule_member_id` ON `medication_schedule` (`member_id`);
CREATE INDEX `idx_medication_time_schedule_id` ON `medication_time` (`medication_schedule_id`);
CREATE INDEX `idx_medication_record_schedule_id` ON `medication_record` (`medication_schedule_id`);
CREATE INDEX `idx_medication_record_time_id` ON `medication_record` (`medication_time_id`);
CREATE INDEX `idx_side_effect_record_member_id` ON `side_effect_record` (`member_id`);

-- 테이블 및 컬럼 코멘트 추가
ALTER TABLE `member` COMMENT = '사용자 정보 테이블';
ALTER TABLE `side_effect` COMMENT = '부작용 종류 마스터 테이블';
ALTER TABLE `medication_schedule_group` COMMENT = '복용 일정 그룹 테이블';
ALTER TABLE `side_effect_record` COMMENT = '부작용 기록 테이블';
ALTER TABLE `medication_schedule` COMMENT = '복용 일정 테이블';
ALTER TABLE `medication_time` COMMENT = '복용 시간 테이블';
ALTER TABLE `side_effect_side_effect_record` COMMENT = '부작용 기록과 부작용 종류 연결 테이블';
ALTER TABLE `medication_record` COMMENT = '실제 복용 기록 테이블';
ALTER TABLE `medication_schedule_medication_schedule_group` COMMENT = '복용 일정과 그룹 연결 테이블';

-- 주요 변경 사항 요약:
-- 1. Member 테이블에 nickname 필드 추가
-- 2. Member 테이블 필드 길이 조정 (login_id: 100, password: 60, name: 15, phone_number: 11)
-- 3. alarm_type을 ENUM에서 VARCHAR(10)으로 변경 (JPA enum 매핑에 맞춤)
-- 4. 모든 ID 필드에 AUTO_INCREMENT 추가
-- 5. login_id에 UNIQUE 제약 조건 추가
-- 6. 성능 최적화를 위한 인덱스 추가
-- 7. 테이블 코멘트 추가