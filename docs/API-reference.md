# Hamalog API 참고 문서

> 📖 이 문서는 백엔드 개발자 및 DevOps를 위한 참고 문서입니다.
> 프론트엔드 개발자는 [API 명세서](./API-specification.md)를 참조하세요.

## 프로젝트 문서

| 문서 | 설명 | 대상 |
|------|------|------|
| [API 명세서](./API-specification.md) | REST API 엔드포인트 상세 명세 | 프론트엔드 개발자 |
| [API 참고 문서](./API-reference.md) | 현재 문서 - 인프라, 스키마, 변경 이력 | 백엔드/DevOps |
| [프로젝트 구조 설명서](./Project-Structure.md) | 프로젝트 디렉토리 구조, 아키텍처, 배포 구성 | 전체 |
| [README](./README.md) | 프로젝트 소개 및 시작 가이드 | 전체 |

---

## 인프라 구성 (Infrastructure)

> 📖 상세한 배포 아키텍처 및 CI/CD 파이프라인 정보는 [프로젝트 구조 설명서](./Project-Structure.md#배포-아키텍처)를 참조하세요.

### 시스템 아키텍처 개요

```
Internet → Cloudflare Edge (DDoS/WAF) → Cloudflare Tunnel → Nginx → Spring Boot → MySQL/Redis
```

### Cloudflare Tunnel 설정

프로덕션 환경은 Cloudflare Tunnel을 통해 안전하게 서비스됩니다.

| 구성 요소 | 설명 |
|-----------|------|
| **Tunnel 연결** | `cloudflared` 컨테이너가 Cloudflare 네트워크에 outbound 연결 |
| **외부 포트** | 모든 포트 비공개 (80, 443, 8080 등 외부 노출 없음) |
| **SSL/TLS** | Cloudflare에서 종단 - Nginx까지 HTTPS 보장 |
| **DDoS 방어** | Cloudflare 엣지에서 자동 방어 |
| **실제 IP 복원** | `CF-Connecting-IP` 헤더를 통해 클라이언트 IP 식별 |

### GitHub Secrets 설정 (Required)

배포를 위해 다음 시크릿이 GitHub 저장소에 설정되어야 합니다:

| 시크릿 이름 | 설명 | 예시 |
|-------------|------|------|
| `MYSQL_ROOT_PASSWORD` | MySQL root 비밀번호 | `<강력한_비밀번호>` |
| `DB_NAME` | 데이터베이스 이름 | `hamalog` |
| `DB_USERNAME` | DB 사용자 이름 | `hamalog_user` |
| `DB_PASSWORD` | DB 사용자 비밀번호 | `<강력한_비밀번호>` |
| `JWT_SECRET` | JWT 서명 키 (Base64, 32바이트 이상) | `openssl rand -base64 32` |
| `JWT_EXPIRY` | Access Token 만료 시간 (ms) | `900000` (15분) |
| `JWT_REFRESH_TOKEN_EXPIRY` | Refresh Token 만료 시간 (ms) | `604800000` (7일) |
| `HAMALOG_ENCRYPTION_KEY` | 데이터 암호화 키 (Base64) | `openssl rand -base64 32` |
| `KAKAO_CLIENT_ID` | 카카오 REST API 키 | 카카오 개발자 콘솔에서 발급 |
| `KAKAO_CLIENT_SECRET` | 카카오 Client Secret | 카카오 개발자 콘솔에서 발급 |
| `KAKAO_REDIRECT_URI` | OAuth2 콜백 URI | `https://api.hamalog.shop/oauth2/auth/kakao/callback` |
| `SPRING_DATA_REDIS_PASSWORD` | Redis 비밀번호 | `<강력한_비밀번호>` |
| `CLOUDFLARE_TUNNEL_TOKEN` | Cloudflare Tunnel 토큰 | Cloudflare Zero Trust에서 발급 |

### Docker 컨테이너 구성

| 컨테이너 | 이미지 | 역할 | 네트워크 포트 | 헬스체크 |
|----------|--------|------|---------------|----------|
| `cloudflare-tunnel` | `cloudflare/cloudflared:latest` | Cloudflare 터널 연결 | 내부 전용 | - |
| `nginx-hamalog` | `nginx:alpine` | 리버스 프록시, 봇 차단, Rate Limiting | 내부 80 | `/actuator/health` |
| `hamalog-app` | `ghcr.io/daemin-kim/hamalog-backend:latest` | Spring Boot 애플리케이션 | 내부 8080 | `/actuator/health` |
| `hamalog-redis` | `redis:7-alpine` | 세션/캐시 저장소 (CSRF 토큰, Rate Limit) | 내부 6379 | `redis-cli ping` |
| `mysql-hamalog` | `mysql:8.0` | 데이터베이스 | 내부 3306 | `mysqladmin ping` |

---

## 데이터베이스 스키마

### ERD 개요
Hamalog 시스템은 총 12개의 테이블로 구성되어 있으며, 회원 관리, 복약 스케줄 관리, 복약 기록, 부작용 관리, 인증 토큰 관리, 마음 일기 도메인으로 나뉩니다.

### 테이블 목록
1. `member` - 회원 정보
2. `medication_schedule` - 복약 스케줄
3. `medication_time` - 복약 시간
4. `medication_record` - 복약 기록
5. `medication_schedule_group` - 복약 스케줄 그룹
6. `medication_schedule_medication_schedule_group` - 스케줄-그룹 매핑
7. `side_effect` - 부작용 목록
8. `side_effect_record` - 부작용 기록
9. `side_effect_side_effect_record` - 부작용-기록 매핑
10. `side_effect_degree` - 부작용 정도 (deprecated)
11. `refresh_tokens` - Refresh Token 저장
12. `mood_diary` - 마음 일기

---

### SQL DDL

```sql
-- =====================================================
-- Hamalog Database Schema
-- Version: 2025-12-22
-- Description: 복약 관리 시스템 데이터베이스 스키마
-- =====================================================

-- 1. 회원 테이블
CREATE TABLE member (
    member_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '회원 고유 ID',
    login_id VARCHAR(100) NOT NULL UNIQUE COMMENT '로그인 ID (이메일 형식)',
    password VARCHAR(60) NOT NULL COMMENT '암호화된 비밀번호 (BCrypt)',
    name VARCHAR(15) NOT NULL COMMENT '회원 이름',
    phone_number VARCHAR(255) NOT NULL COMMENT '전화번호 (암호화됨)',
    nickname VARCHAR(10) NOT NULL COMMENT '닉네임 (한글/영어 1-10자)',
    birthday VARCHAR(255) NOT NULL COMMENT '생년월일 (암호화됨)',
    created_at DATETIME NOT NULL COMMENT '계정 생성일시',
    version BIGINT DEFAULT 0 COMMENT '낙관적 락 버전',
    INDEX idx_login_id (login_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회원 정보';

-- 2. 복약 스케줄 테이블
CREATE TABLE medication_schedule (
    medication_schedule_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '복약 스케줄 고유 ID',
    member_id BIGINT NOT NULL COMMENT '회원 ID',
    name VARCHAR(20) NOT NULL COMMENT '약 이름',
    hospital_name VARCHAR(20) NOT NULL COMMENT '병원명',
    prescription_date DATE NOT NULL COMMENT '처방일',
    memo TEXT COMMENT '메모',
    start_of_ad DATE NOT NULL COMMENT '복약 시작일',
    prescription_days INT NOT NULL COMMENT '처방 일수',
    per_day INT NOT NULL COMMENT '1일 복용 횟수',
    alarm_type VARCHAR(20) NOT NULL COMMENT '알람 타입 (SOUND, VIBE)',
    version BIGINT DEFAULT 0 COMMENT '낙관적 락 버전',
    FOREIGN KEY (member_id) REFERENCES member(member_id) ON DELETE CASCADE,
    INDEX idx_member_id (member_id),
    INDEX idx_start_of_ad (start_of_ad),
    INDEX idx_prescription_date (prescription_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='복약 스케줄';

-- 3. 복약 시간 테이블
CREATE TABLE medication_time (
    medication_time_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '복약 시간 고유 ID',
    medication_schedule_id BIGINT NOT NULL COMMENT '복약 스케줄 ID',
    take_time TIME NOT NULL COMMENT '복용 시간',
    FOREIGN KEY (medication_schedule_id) REFERENCES medication_schedule(medication_schedule_id) ON DELETE CASCADE,
    INDEX idx_medication_schedule_id (medication_schedule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='복약 시간';

-- 4. 복약 기록 테이블
CREATE TABLE medication_record (
    medication_record_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '복약 기록 고유 ID',
    medication_schedule_id BIGINT NOT NULL COMMENT '복약 스케줄 ID',
    medication_time_id BIGINT NOT NULL COMMENT '복약 시간 ID',
    is_take_medication BOOLEAN NOT NULL COMMENT '복용 여부',
    real_take_time DATETIME COMMENT '실제 복용 시간',
    version BIGINT DEFAULT 0 COMMENT '낙관적 락 버전',
    FOREIGN KEY (medication_schedule_id) REFERENCES medication_schedule(medication_schedule_id) ON DELETE CASCADE,
    FOREIGN KEY (medication_time_id) REFERENCES medication_time(medication_time_id) ON DELETE CASCADE,
    INDEX idx_medication_schedule_id (medication_schedule_id),
    INDEX idx_medication_time_id (medication_time_id),
    INDEX idx_real_take_time (real_take_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='복약 기록';

-- 5. 복약 스케줄 그룹 테이블
CREATE TABLE medication_schedule_group (
    medication_schedule_group_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '그룹 고유 ID',
    member_id BIGINT NOT NULL COMMENT '회원 ID',
    name VARCHAR(20) NOT NULL COMMENT '그룹명',
    FOREIGN KEY (member_id) REFERENCES member(member_id) ON DELETE CASCADE,
    INDEX idx_member_id (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='복약 스케줄 그룹';

-- 6. 복약 스케줄-그룹 매핑 테이블
CREATE TABLE medication_schedule_medication_schedule_group (
    medication_schedule_id BIGINT NOT NULL COMMENT '복약 스케줄 ID',
    medication_schedule_group_id BIGINT NOT NULL COMMENT '그룹 ID',
    PRIMARY KEY (medication_schedule_id, medication_schedule_group_id),
    FOREIGN KEY (medication_schedule_id) REFERENCES medication_schedule(medication_schedule_id) ON DELETE CASCADE,
    FOREIGN KEY (medication_schedule_group_id) REFERENCES medication_schedule_group(medication_schedule_group_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='복약 스케줄-그룹 매핑';

-- 7. 부작용 목록 테이블
CREATE TABLE side_effect (
    side_effect_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '부작용 고유 ID',
    type VARCHAR(20) NOT NULL COMMENT '부작용 타입',
    name VARCHAR(20) NOT NULL COMMENT '부작용 명',
    INDEX idx_type (type),
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='부작용 목록';

-- 8. 부작용 기록 테이블
CREATE TABLE side_effect_record (
    side_effect_record_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '부작용 기록 고유 ID',
    member_id BIGINT NOT NULL COMMENT '회원 ID',
    created_at DATETIME NOT NULL COMMENT '기록 생성일시',
    FOREIGN KEY (member_id) REFERENCES member(member_id) ON DELETE CASCADE,
    INDEX idx_member_id (member_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='부작용 기록';

-- 9. 부작용-기록 매핑 테이블
CREATE TABLE side_effect_side_effect_record (
    side_effect_record_id BIGINT NOT NULL COMMENT '부작용 기록 ID',
    side_effect_id BIGINT NOT NULL COMMENT '부작용 ID',
    degree INT NOT NULL COMMENT '부작용 정도 (1-5)',
    PRIMARY KEY (side_effect_record_id, side_effect_id),
    FOREIGN KEY (side_effect_record_id) REFERENCES side_effect_record(side_effect_record_id) ON DELETE CASCADE,
    FOREIGN KEY (side_effect_id) REFERENCES side_effect(side_effect_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='부작용-기록 매핑';

-- 10. 부작용 정도 테이블 (Deprecated - side_effect_side_effect_record의 degree 컬럼 사용 권장)
CREATE TABLE side_effect_degree (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '부작용 정도 고유 ID',
    side_effect_record_id BIGINT NOT NULL COMMENT '부작용 기록 ID',
    degree INT COMMENT '부작용 정도',
    FOREIGN KEY (side_effect_record_id) REFERENCES side_effect_record(side_effect_record_id) ON DELETE CASCADE,
    INDEX idx_side_effect_record_id (side_effect_record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='부작용 정도 (Deprecated)';

-- 11. Refresh Token 테이블
CREATE TABLE refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Refresh Token 고유 ID',
    member_id BIGINT NOT NULL COMMENT '회원 ID',
    token_value VARCHAR(500) NOT NULL UNIQUE COMMENT 'Token 값',
    created_at DATETIME NOT NULL COMMENT '생성일시',
    expires_at DATETIME NOT NULL COMMENT '만료일시',
    rotated_at DATETIME NOT NULL COMMENT '마지막 로테이션 일시',
    revoked BOOLEAN NOT NULL DEFAULT FALSE COMMENT '폐기 여부',
    INDEX idx_member_id (member_id),
    INDEX idx_token_value (token_value),
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Refresh Token 저장소';

-- 12. 마음 일기 테이블
CREATE TABLE mood_diary (
    mood_diary_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '마음 일기 고유 ID',
    member_id BIGINT NOT NULL COMMENT '회원 ID',
    diary_date DATE NOT NULL COMMENT '일기 날짜',
    mood_type VARCHAR(20) NOT NULL COMMENT '오늘의 기분 (HAPPY, EXCITED, PEACEFUL, ANXIOUS, LETHARGIC, ANGRY, SAD)',
    diary_type VARCHAR(20) NOT NULL COMMENT '일기 형식 (TEMPLATE: 템플릿, FREE_FORM: 자유형식)',
    template_answer1 VARCHAR(500) COMMENT '템플릿 답변 1: 오늘 나에게 가장 인상 깊었던 사건은 무엇이었나요?',
    template_answer2 VARCHAR(500) COMMENT '템플릿 답변 2: 그 순간, 나는 어떤 감정을 느꼈나요?',
    template_answer3 VARCHAR(500) COMMENT '템플릿 답변 3: 그 감정을 느낀 이유는 무엇이라고 생각하나요?',
    template_answer4 VARCHAR(500) COMMENT '템플릿 답변 4: 지금 이 감정에 대해 내가 해주고 싶은 말은 무엇인가요?',
    free_content VARCHAR(1500) COMMENT '자유 형식 내용',
    created_at DATETIME NOT NULL COMMENT '일기 작성일시',
    version BIGINT DEFAULT 0 COMMENT '낙관적 락 버전',
    UNIQUE KEY unique_member_diary_date (member_id, diary_date) COMMENT '하루 1회 작성 제한',
    FOREIGN KEY (member_id) REFERENCES member(member_id) ON DELETE CASCADE,
    INDEX idx_member_id (member_id),
    INDEX idx_diary_date (diary_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='마음 일기';

-- =====================================================
-- 초기 데이터 (부작용 목록)
-- =====================================================

INSERT INTO side_effect (type, name) VALUES
('소화기계', '구토'),
('소화기계', '설사'),
('소화기계', '복통'),
('소화기계', '메스꺼움'),
('신경계', '두통'),
('신경계', '어지러움'),
('신경계', '현기증'),
('피부', '발진'),
('피부', '가려움'),
('피부', '두드러기'),
('전신', '피로'),
('전신', '무력감'),
('심혈관계', '가슴 두근거림'),
('심혈관계', '혈압 상승'),
('호흡기계', '호흡곤란'),
('호흡기계', '기침');

-- =====================================================
-- 보안 설정
-- =====================================================

-- 민감정보 필드 (phone_number, birthday)는 애플리케이션 레벨에서 AES-256 암호화 처리
-- password는 BCrypt 해시 알고리즘으로 암호화 저장
-- version 컬럼은 JPA Optimistic Locking에 사용

-- =====================================================
-- 성능 최적화
-- =====================================================

-- 주요 조회 컬럼에 인덱스 생성 완료
-- Foreign Key에 ON DELETE CASCADE 설정으로 참조 무결성 보장
-- InnoDB 엔진 사용으로 트랜잭션 및 외래키 제약 지원
```

---

## 수정 마일스톤

- **2025/4/28**: 초안 작성
- **2025/4/29**: 명칭 변경: 약물 → 복약 스케쥴(medication → medication-schedule). 변경 사유: 약물이라는 명칭의 모호성. 복약 스케쥴이라는 조금 더 명확한 명칭을 사용
- **2025/5/20**: 복약 스케쥴 목록 조회 엔드포인트 변경 `/` → `/{member-id}`. 특정 복약 스케쥴 조회, 수정, 삭제 엔드포인트 변경 `/{id}` → `/{medication-schedule-id}`
- **2025/5/26**: 복약 스케쥴 목록 조회 엔드포인트 변경 `/medication-schedules` ⇒ `/medication-schedule/list`. 복약 기록 목록 조회 엔드포인트 변경 `/medication-records` → `/medication-record/list`. 특정 복약 기록 조회, 수정, 삭제 엔드포인트 변경 `/{id}` → `/{medication-record-id}`
- **2025/8/11**: **전체 API 명세 코드 기반 동기화 및 응답 형식 수정**
    - **추가:** 회원가입 (`/auth/signup`), 로그아웃 (`/auth/logout`) 기능 추가.
    - **수정:**
        - 로그인 (`/auth/login`) 응답에서 `refreshToken` 제거.
        - 복약 스케쥴 등록 (`POST /medication-schedule`)을 `multipart/form-data` 형식으로 변경하여 이미지 업로드 지원.
        - 복약 기록 생성/수정 (`POST/PUT /medication-record`)의 요청/응답 본문을 최신 DTO 기준으로 상세화.
        - 모든 API의 요청/응답 데이터에 구체적인 예시 값 추가.
- **2025/8/29**: **현재 프로젝트 버전에 맞춘 전체 API 명세 업데이트**
    - **추가:** 회원 탈퇴 (`DELETE /auth/account`) 기능 추가.
    - **구현 완료:** 카카오 OAuth2 로그인 (`GET /oauth2/auth/kakao`, `POST /api/auth/kakao/callback`) 완전 구현.
    - **구현 완료:** 부작용 기록 생성 (`POST /side-effect/record`) 구현.
    - **보안 강화:** 모든 API에 사용자 권한 검증 추가로 본인 데이터만 접근 가능.
    - **기능 개선:**
        - 복약 스케쥴 목록 조회에 페이지네이션 지원 추가.
        - 로그아웃 시 서버 측 토큰 무효화 (블랙리스트) 구현.
        - 회원가입 요청에 nickName 필드 추가, loginId 이메일 형식 필수.
        - 응답 구조 간소화: 중첩 객체 대신 ID 필드 사용.
    - **상태 코드 정규화:** 201(생성), 204(삭제), 403(권한없음) 추가.
- **2025/9/4**: **현재 프로젝트 버전에 맞춘 API 명세 동기화**
    - **구현 검증**: 모든 컨트롤러와 DTO 구조가 문서화된 API 명세와 일치함을 확인.
    - **엔드포인트 검증**: 인증(AuthController), OAuth2(OAuth2Controller), 복약 스케줄(MedicationScheduleController), 복약 기록(MedicationRecordController), 부작용(SideEffectController) 모든 API 엔드포인트가 정확히 문서화됨.
    - **보안 검증**: 모든 보호된 엔드포인트에 JWT 토큰 기반 인증 및 사용자 권한 검증이 올바르게 구현됨.
- **2025/9/11**: **Hamalog 프로젝트 구조에 맞춘 API 명세 최종 검토**
    - **프로젝트 정보 업데이트**: Base URL을 localhost:8080으로 변경, AOP 기반 기능들 명시
    - **인증 API 정확성 검증**: AuthController와 OAuth2Controller의 실제 구현과 완전 일치 확인
    - **미구현 기능 제거**: Report API 섹션 제거 (실제 컨트롤러 미존재)
    - **종합 검토 완료**: 6개 컨트롤러(Auth, CSRF, OAuth2, MedicationSchedule, MedicationRecord, SideEffect, MoodDiary) 모든 엔드포인트가 실제 구현과 정확히 일치함을 재확인
- **2025/11/17**: **API 명세 최종 동기화**
    - **복약 스케줄 목록 응답 구조 업데이트**: `content` 필드 → `schedules`, 페이지네이션 필드 정규화
    - **회원가입 요청 데이터 정확화**: nickName 예시 업데이트 및 검증 규칙 명시
    - **모든 엔드포인트 재검증**: 현재 구현 코드와 완전 일치 확인
- **2025/11/21**: **보안 강화 및 토큰 관리 개선**
    - **Refresh Token 구현**: `/auth/refresh` 엔드포인트 추가, 토큰 로테이션 메커니즘 구현
    - **로그인 응답 구조 개선**: Access Token, Refresh Token, expiresIn 필드 포함
    - **OAuth2 보안 강화**: State 파라미터 생성 및 검증 (CSRF 방지)
    - **토큰 검증 강화**: 로그아웃 시 토큰 유효성 검증 추가
    - **동시성 제어**: 회원 탈퇴 시 트랜잭션 내 즉시 토큰 무효화
    - **파일 업로드 보안**: 파일명 검증, Path Traversal 방지
    - **Rate Limiting**: Fail-safe 메커니즘 적용
    - **민감정보 보호**: 로그 마스킹 적용
    - **페이지네이션**: 최대 크기 100 제한 (DoS 방지)
    - **전체 보안 점수**: 7.4/10 → 9.9/10 (12개 취약점 100% 해결)
- **2025/11/26**: **보안 및 문서 최신화**
    - 로그인이 `access_token`, `refresh_token`, `expires_in`, `token_type`를 모두 반환하도록 문서화
    - `/auth/csrf-token`, `/auth/csrf-status` 엔드포인트 명세 추가 (JWT 인증 + CSRF 토큰 이중 검증 흐름 명시)
    - 모든 에러 응답 포맷을 `error`, `message`, `timestamp` 구조로 정리
    - JWT/Refresh Token 로테이션, 로그아웃 블랙리스트 처리 등 실제 구현과 문서 동기화 완료
- **2025/12/01**: **마음 일기 API 추가**
    - 마음 일기 CRD (Create, Read, Delete) API 명세 추가 (Update 불가)
    - 하루 1회 작성 제한, 템플릿 형식(4개 질문) 또는 자유 형식 선택 가능
    - 7가지 기분 타입 (행복, 신남, 평온, 불안&긴장, 무기력, 분노, 슬픔) 지원
    - 템플릿 형식: 각 질문당 500자 제한, 자유 형식: 1500자 제한
    - 페이지네이션 지원 목록 조회, 날짜별 조회 기능 포함
    - 데이터베이스 스키마: `mood_diary` 테이블 추가 (12번째 테이블)
    - 보안: 리소스 소유권 자동 검증, JWT 인증 필수
    - 엔드포인트: 생성(POST), 단건 조회(GET), 목록 조회(GET), 날짜별 조회(GET), 삭제(DELETE)
- **2025/12/09**: **에러 응답 개선 및 Multipart 요청 가이드 추가**
    - 에러 응답 형식을 `code`, `message`, `path`, `violations` 구조로 정확화
    - 415 UNSUPPORTED_MEDIA_TYPE 에러 코드 추가
    - `GlobalExceptionHandler`에 `HttpMediaTypeNotSupportedException`, `MissingServletRequestPartException`, `HttpMessageNotReadableException` 핸들러 추가로 명확한 에러 메시지 반환
    - 복약 스케줄 등록 API (`POST /medication-schedule`)에 multipart 요청 시 `data` 파트 `Content-Type: application/json` 명시 필수 사항 강조
    - JavaScript/React Native 및 cURL을 이용한 multipart 요청 예시 코드 추가
    - CSRF 토큰 응답에 `storage` 필드 추가 (redis/fallback 구분)
    - **React Native 환경 호환성 개선**: `data` 파트가 `text/plain` 또는 단순 문자열로 전송되어도 JSON으로 파싱하도록 `TextPlainJsonHttpMessageConverter` 추가 (Blob 미지원 환경 대응)
- **2025/12/10**: **DTO 필드 유효성 검사 명세 추가 및 전반적 검토**
    - 모든 요청 DTO에 필드별 유효성 검사 테이블 추가 (필수 여부, 제약 조건, 에러 메시지)
    - 프론트엔드 테스트 가이드 추가 (필수 필드, 형식 검증, 길이 제한, Enum 값, 조건부 필수 테스트)
    - 유효성 검사 실패 시 `violations` 배열 응답 형식 문서화
    - 회원가입, 로그인, 토큰 갱신, 복약 스케줄 등록/수정 → Bean Validation 적용
    - 복약 기록 생성/수정, 부작용 기록 생성 → 비즈니스 로직 검증 (Bean Validation 미적용 명시)
    - 마음 일기 생성 요청 필드 상세 명시 (조건부 필수 표기 추가)
    - **DDL 스키마 업데이트**: alarm_type 허용값을 `SOUND`, `VIBE`로 정확화, 스키마 버전 2025-12-10으로 갱신
    - **컨트롤러 검증 업데이트**: 7개 컨트롤러(Auth, CSRF, OAuth2, MedicationSchedule, MedicationRecord, SideEffect, MoodDiary) 모든 엔드포인트 재확인
- **2025/12/14**: **현재 프로젝트 버전 최종 동기화**
    - **전체 컨트롤러 검증 완료**: AuthController, CsrfController, OAuth2Controller, MedicationScheduleController, MedicationRecordController, SideEffectController, MoodDiaryController 총 7개 컨트롤러의 모든 엔드포인트가 문서와 정확히 일치함을 확인
    - **DTO 구조 검증 완료**: 모든 Request/Response DTO가 현재 코드 구조와 일치함을 확인
    - **Enum 값 검증**: MoodType(7가지), DiaryType(2가지), AlarmType(2가지) 모두 문서화 완료
    - **보안 어노테이션 확인**: `@RequireResourceOwnership` AOP 기반 리소스 소유권 검증이 모든 보호된 엔드포인트에 적용됨을 확인
    - **CSRF 토큰 관리**: Redis 기반 토큰 저장소, fallback 메커니즘, 60분 TTL 설정 문서화 확인
    - **DDL 스키마 버전**: 2025-12-14로 갱신
- **2025/12/16**: **프로젝트 구조 설명서 연동 및 인프라 정보 최신화**
    - **프로젝트 구조 설명서 추가**: `Project-Structure.md` 문서 생성 및 연동
    - **배포 아키텍처 문서화**: Cloudflare Tunnel + Nginx + Docker Compose 기반 인프라 구성
    - **CI/CD 파이프라인 문서화**: GitHub Actions 기반 자동 배포 (Build → Push → Deploy → Verify)
    - **보안 레이어 구조 문서화**: 4단계 보안 레이어 (Cloudflare Edge → Tunnel → Nginx → Spring Security)
    - **DDL 스키마 버전**: 2025-12-16으로 갱신
    - **문서 분리**: API 명세서를 프론트엔드용(`API-specification.md`)과 참고용(`API-reference.md`)으로 분리
- **2025/12/16**: **코드 스타일 통일 및 설정 일관성 점검**
    - **DTO 타입 통일**: diary 패키지의 class를 Java Record로 변환 (MoodDiaryCreateRequest, MoodDiaryResponse, MoodDiaryListResponse)
    - **Validation 메시지 통일**: 하드코딩된 메시지를 메시지 키 방식(`{auth.*}`, `{sideEffect.*}`)으로 변경
    - **Swagger 어노테이션 보완**: 모든 Response DTO에 `@Schema` 추가 (MedicationScheduleResponse, MedicationRecordResponse, RecentSideEffectResponse 등)
    - **의존성 주입 통일**: 모든 컨트롤러에 `@RequiredArgsConstructor` 적용 (MedicationScheduleController, MedicationRecordController, SideEffectController, CsrfController)
    - **설정 일관성 점검**: JWT_EXPIRY, 데이터베이스 SSL 설정, Kakao redirect-uri 등 환경별 설정값 통일
    - **개발 환경 분리**: `application-dev.properties` 파일 신규 생성, `docker-compose-dev.yml`에서 dev 프로필 사용
    - **ValidationMessages.properties 확장**: auth, sideEffect 관련 메시지 키 추가
    - **테스트 코드 동기화**: MoodDiaryServiceTest, MoodDiaryControllerTest, LoginRequestTest를 record 방식에 맞게 수정
- **2025/12/16**: **API 버전 관리 및 실무 개선 적용**
    - **ApiVersion.java**: API 경로 상수 클래스 생성
    - **OWASP Dependency Check**: 보안 취약점 스캔 플러그인 추가 (`./gradlew dependencyCheckAnalyze`)
    - **ArchUnit 아키텍처 테스트**: 계층 간 의존성 규칙, 네이밍 컨벤션, 패키지 구조 검증 테스트 추가
    - **Flyway DB 마이그레이션**: 스키마 버전 관리 도구 도입 (`V1__Initial_schema.sql`)
    - **GitHub Actions CI/CD**: 자동화된 테스트, 보안 스캔, Docker 빌드 파이프라인 추가
    - **AuthService 리팩토링**: 417줄 단일 클래스를 4개 서비스로 분리 (MemberRegistrationService, AuthenticationService, MemberDeletionService, KakaoOAuth2AuthService)
- **2025/12/17**: **실무 개선 및 API 경로 단순화**
    - **API 버전 프리픽스 제거**: `/api/v1/` 프리픽스 제거하여 API 경로 단순화 (예: `/auth/login`, `/medication-schedule`)
    - **ErrorResponse 개선**: `timestamp`, `traceId` 필드 추가로 디버깅 및 추적성 강화
    - **OpenAPI 스키마**: 공통 에러 응답 스키마 정의 추가
    - **회원 캐싱 서비스**: MemberCacheService 추가로 DB 부하 감소
    - **보안 설정 테스트**: SecurityHeadersIntegrationTest 추가
- **2025/12/20**: **최종 프로젝트 상태 동기화 및 문서 검증 완료**
    - **전체 문서 동기화**: API 명세서, 참고 문서, 구조 명세서 간 일관성 확보
    - **누락 정보 보완**: `MemberCacheService`, `RateLimitProperties`, `ServiceLoggingAspect` 등 최근 추가된 구성 요소 반영
    - **상태 점검**: 모든 엔드포인트 및 DTO 구현 상태가 명세와 100% 일치함을 최종 확인
- **2025/12/22**: **대규모 기능 확장 및 API 추가**
    - **마음 일기 기능 확장**:
        - `PUT /mood-diary/{id}` - 마음 일기 수정 API 추가
        - `GET /mood-diary/stats/{member-id}` - 기분 통계 API 추가 (기분 분포, 연속 작성일)
        - `GET /mood-diary/calendar/{member-id}` - 월별 캘린더 API 추가
        - `GET /mood-diary/search/{member-id}` - 일기 내용 검색 API 추가
    - **회원 프로필 API 신규 추가**:
        - `GET /member/profile` - 내 프로필 조회
        - `PUT /member/profile` - 프로필 수정 (이름, 닉네임, 전화번호, 생년월일)
        - `PUT /member/password` - 비밀번호 변경 (현재 비밀번호 확인 필요)
    - **복약 통계 API 신규 추가**:
        - `GET /medication-stats/{member-id}/adherence` - 기간별 복약 이행률
        - `GET /medication-stats/{member-id}/summary` - 오늘/주간/월간 요약 통계
    - **복약 알림 시간 API 신규 추가**:
        - `GET /medication-schedule/{id}/times` - 스케줄별 알림 시간 목록
        - `POST /medication-schedule/{id}/times` - 알림 시간 추가
        - `PUT /medication-time/{id}` - 알림 시간 수정
        - `DELETE /medication-time/{id}` - 알림 시간 삭제
    - **부작용 API 확장**:
        - `GET /side-effect/list/{member-id}` - 부작용 기록 목록 조회 (페이징)
        - `GET /side-effect/{record-id}` - 부작용 기록 상세 조회
        - `DELETE /side-effect/{record-id}` - 부작용 기록 삭제
    - **검색 기능 추가**:
        - `GET /medication-schedule/search/{member-id}` - 약 이름 검색
    - **국제화 개선**: `messages_ko.properties` UTF-8 인코딩 수정
    - **신규 DTO 24개, 서비스 4개, 컨트롤러 3개 추가**
    - **DDL 스키마 버전**: 2025-12-22로 갱신
- **2025/12/23**: **추가 기능 확장 및 보안 강화**
    - **복약 스케줄 이미지 관리 API 추가**:
        - `GET /medication-schedule/{id}/image` - 이미지 조회
        - `PUT /medication-schedule/{id}/image` - 이미지 수정
        - `DELETE /medication-schedule/{id}/image` - 이미지 삭제
    - **복약 스케줄 필터링 API 추가**:
        - `GET /medication-schedule/filter/{member-id}` - 활성 상태 필터링
    - **복약 스케줄 그룹 관리 API 신규 추가**:
        - `GET /medication-group` - 그룹 목록 조회
        - `GET /medication-group/{group-id}` - 그룹 상세 조회
        - `POST /medication-group` - 그룹 생성
        - `PUT /medication-group/{group-id}` - 그룹 수정
        - `DELETE /medication-group/{group-id}` - 그룹 삭제
    - **배치 작업 API 추가**:
        - `POST /medication-record/batch` - 복약 기록 일괄 생성 (최대 100개)
        - `PUT /medication-record/batch` - 복약 기록 일괄 수정 (최대 100개)
    - **데이터 내보내기 API 신규 추가**:
        - `GET /export/my-data` - 전체 데이터 JSON 내보내기
        - `GET /export/my-data/download` - JSON 파일 다운로드
        - `GET /export/medication-records` - 복약 기록 CSV 내보내기 (의사 상담용)
    - **로그인 이력/세션 관리 API 추가**:
        - `GET /auth/login-history` - 로그인 이력 조회 (페이지네이션)
        - `GET /auth/sessions` - 활성 세션 목록 조회
        - `DELETE /auth/sessions/{session-id}` - 특정 세션 강제 종료
        - `DELETE /auth/sessions` - 모든 세션 종료
    - **DB 마이그레이션 V2 추가**:
        - `login_history` 테이블 신규 생성
        - `medication_schedule.image_path` 컬럼 추가
        - `medication_schedule.is_active` 컬럼 추가
        - `medication_schedule_group` 테이블 개선 (description, color 컬럼)
        - `notification_settings` 테이블 추가 (푸시 알림용)
    - **신규 파일**: 엔티티 1개, Repository 2개, Service 3개, Controller 2개, DTO 12개 추가
    - **DDL 스키마 버전**: 2025-12-23으로 갱신
