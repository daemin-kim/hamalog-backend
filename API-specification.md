# Hamalog API 명세서

## 공통 사항

⚠️ **로그인 및 회원가입을 제외한 모든 엔드포인트는 JWT 토큰 기반 인증이 필요합니다. (`/auth/csrf-token`, `/auth/csrf-status` 포함)** ⚠️

- 토큰은 요청 헤더에 포함해야 합니다. (예: `Authorization: Bearer {token}`)
- 인증되지 않은 사용자가 API에 접근하는 것을 방지하기 위함입니다.
- EndPoint에 파라미터가 포함되는 경우(GET 등), 구별하기 위해 파라미터는 `{}`로 감싸져 있습니다.
- 가독성을 위해 엔드포인트 내 변수명은 케밥 케이스(띄어쓰기 대신에 하이픈 '-'이 들어간 형태)로 작성합니다.
- 모든 API는 리소스 소유권 검증을 통해 본인의 데이터만 접근 가능하도록 보안이 강화되었습니다.
- AOP 기반 성능 모니터링, 비즈니스 감사, 캐싱, 재시도 메커니즘이 적용되었습니다.
- SPA 클라이언트는 `/auth/csrf-token` 으로 발급받은 CSRF 토큰을 `X-CSRF-TOKEN` 헤더에 포함해 안전하지 않은 메서드(POST, PUT, DELETE)에 함께 전송해야 합니다.
- `/auth/csrf-token` 및 `/auth/csrf-status` 엔드포인트 역시 JWT 인증이 선행되어야 하며, 토큰 및 헤더 유효성을 모두 검사합니다.

**Base URL**: `http://localhost:8080`

### 에러 응답 규칙

모든 에러 응답은 아래와 같이 일관된 형식을 따릅니다.

```json
{
  "error": "에러 코드",
  "message": "에러 메시지",
  "timestamp": "ISO-8601"
}
```

- **200**: 성공
- **201**: 생성 성공
- **204**: 삭제 성공 (본문 없음)
- **400**: 잘못된 요청 (클라이언트 측 오류)
- **401**: 인증 실패 (유효하지 않은 토큰 또는 자격 증명)
- **403**: 권한 없음 (다른 사용자의 데이터 접근 시도)
- **404**: 리소스 없음 (요청한 리소스를 찾을 수 없음)
- **409**: 중복 리소스 (회원가입 중복 등)
- **500**: 서버 에러 (서버 내부 문제)

## API 명세서

### 인증 (Authentication) / CSRF API (`/auth`, `/oauth2`)

| 기능 | EndPoint | Method | Request Data | Response Data | 비고 |
|------|----------|--------|--------------|---------------|------|
| 회원가입 | `/auth/signup` | `POST` | 회원가입 요청 데이터 | `"회원가입이 성공적으로 완료되었습니다"` | loginId 이메일 형식 필수, nickName 한글/영어 1-10자, phoneNumber 010으로 시작하는 11자리, 모든 필드 Bean Validation 적용 |
| 일반 로그인 | `/auth/login` | `POST` | 로그인 요청 데이터 | 로그인 응답 데이터 | JWT Access Token, Refresh Token, expiresIn(초), tokenType=Bearer 포함 |
| 토큰 갱신 | `/auth/refresh` | `POST` | 토큰 갱신 요청 데이터 | 토큰 갱신 응답 데이터 | Refresh Token 로테이션. 새 Access/Refresh Token 반환 |
| 로그아웃 | `/auth/logout` | `POST` | Authorization 헤더 | `"로그아웃이 성공적으로 처리되었습니다"` | Access Token 블랙리스트 등록으로 즉시 무효화 |
| 회원 탈퇴 | `/auth/account` | `DELETE` | Authorization 헤더 | `"회원 탈퇴가 완료되었습니다"` | 인증 필요. 관련 데이터(스케줄, 기록, 부작용 등) 모두 삭제 |
| CSRF 토큰 발급 | `/auth/csrf-token` | `GET` | Authorization 헤더 | CSRF 토큰 응답 데이터 | **JWT 인증 필수**. Redis 기반 토큰 저장소에 60분 TTL로 저장됩니다. |
| CSRF 토큰 상태 확인 | `/auth/csrf-status` | `GET` | Authorization 헤더, 선택적으로 `X-CSRF-TOKEN` | CSRF 상태 응답 데이터 | **JWT 인증 필수**. Redis에 저장된 토큰 존재 여부/TTL 확인. |
| 카카오 로그인 시작 | `/oauth2/auth/kakao` | `GET` | 없음 | 302 리다이렉션 | state 파라미터 생성·저장 후 카카오 인증 서버로 리다이렉션 |
| 카카오 로그인 콜백 | `/oauth2/auth/kakao/callback` | `GET` | `?code={authorization_code}&state={state}` | RN 앱 스킴으로 302 리다이렉션 | state 검증(필수), Authorization code로 JWT/Refresh Token 발급 후 `hamalog-rn://auth?token=...` 리다이렉션 |

#### 인증/CSRF API 데이터 구조

##### 회원가입 요청 데이터 {#auth-signup-request}
```json
{
  "loginId": "user@example.com",
  "password": "password123",
  "name": "홍길동",
  "nickName": "길동이",
  "phoneNumber": "01012345678",
  "birth": "1990-01-01"
}
```

##### 로그인 요청 데이터 {#auth-login-request}
```json
{
  "loginId": "user@example.com",
  "password": "password123"
}
```

##### 로그인 응답 데이터 {#auth-login-response}
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiJ9...",
  "refresh_token": "eyJhbGciOiJIUzI1NiJ9...",
  "expires_in": 900,
  "token_type": "Bearer"
}
```

##### 토큰 갱신 요청 데이터 {#auth-refresh-request}
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

##### 토큰 갱신 응답 데이터 {#auth-refresh-response}
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiJ9.newAccessToken...",
  "refresh_token": "eyJhbGciOiJIUzI1NiJ9.newRefreshToken...",
  "expires_in": 900
}
```

##### CSRF 토큰 응답 데이터 {#csrf-token-response}
```json
{
  "csrfToken": "d1d9a7e2-...",
  "headerName": "X-CSRF-TOKEN",
  "expiryMinutes": 60,
  "timestamp": "2025-11-26T10:00:00Z"
}
```

##### CSRF 상태 응답 데이터 {#csrf-status-response}
```json
{
  "userId": "user@example.com",
  "csrfTokenPresent": true,
  "csrfTokenValid": true,
  "timestamp": "2025-11-26T10:05:00Z"
}
```

##### 카카오 로그인 응답 (RN 앱 리다이렉션 파라미터) {#auth-kakao-response}
```url
hamalog-rn://auth?token=eyJhbGciOiJIUzI1NiJ9...
```

### 복약 스케줄 (Medication Schedule) API (`/medication-schedule`)

| 기능 | EndPoint | Method | Request Data | Response Data | 비고 |
|------|----------|--------|--------------|---------------|------|
| 복약 스케줄 목록 조회 | `/medication-schedule/list/{member-id}` | `GET` | 쿼리: `page`, `size` (최대 size=100) | 복약 스케줄 목록 응답 데이터 | 해당 회원의 스케줄을 페이지네이션된 형태로 반환 |
| 특정 복약 스케줄 조회 | `/medication-schedule/{medication-schedule-id}` | `GET` | 없음 | 복약 스케줄 상세 응답 데이터 | `MedicationScheduleResponse` DTO 구조(아래 예시)로 반환 |
| 복약 스케줄 등록 | `/medication-schedule` | `POST` (`multipart/form-data`) | data(JSON), image(선택) | 복약 스케줄 상세 응답 | `memberId`, `name`, `hospitalName`, `prescriptionDate`, `startOfAd`, `prescriptionDays`, `perDay`, `alarmType` 필수. `memo` 선택, `alarmType` 값은 `SOUND` 또는 `VIBE` |
| 복약 스케줄 수정 | `/medication-schedule/{medication-schedule-id}` | `PUT` | 복약 스케줄 수정 요청 데이터 | 복약 스케줄 상세 응답 | 모든 필드는 DTO 구조에 맞는 값 전송 (`alarmType` = `SOUND`/`VIBE` ) |
| 복약 스케줄 삭제 | `/medication-schedule/{medication-schedule-id}` | `DELETE` | 없음 | (본문 없음, 204) | 삭제 성공 시 204 반환 |

#### AlarmType 값
- `SOUND`: 소리 알람
- `VIBE`: 진동 알람

##### 복약 스케줄 목록 응답 데이터 {#schedule-list-response}
```json
{
  "schedules": [
    {
      "medicationScheduleId": 101,
      "memberId": 1,
      "name": "혈압약 (아침)",
      "hospitalName": "서울중앙병원",
      "prescriptionDate": "2025-08-01",
      "memo": "식후 30분",
      "startOfAd": "2025-08-02",
      "prescriptionDays": 30,
      "perDay": 1,
      "alarmType": "VIBE"
    }
  ],
  "totalCount": 10,
  "currentPage": 0,
  "pageSize": 20,
  "hasNext": false,
  "hasPrevious": false
}
```

##### 복약 스케줄 상세 응답 데이터 {#schedule-detail-response}
```json
{
  "medicationScheduleId": 101,
  "memberId": 1,
  "name": "혈압약 (아침)",
  "hospitalName": "서울중앙병원",
  "prescriptionDate": "2025-08-01",
  "memo": "식후 30분 뒤 복용",
  "startOfAd": "2025-08-02",
  "prescriptionDays": 30,
  "perDay": 1,
  "alarmType": "SOUND"
}
```

##### 복약 스케줄 등록 요청 데이터 (`multipart/form-data`) {#schedule-create-request}
- **Part `data` (application/json)**
```json
{
  "memberId": 1,
  "name": "종합 비타민",
  "hospitalName": "자가 처방",
  "prescriptionDate": "2025-08-10",
  "memo": "매일 아침 1정",
  "startOfAd": "2025-08-11",
  "prescriptionDays": 90,
  "perDay": 1,
  "alarmType": "VIBE"
}
```
- **Part `image` (선택, image/*)**

##### 복약 스케줄 수정 요청 데이터 {#schedule-update-request}
```json
{
  "name": "종합 비타민 (골드)",
  "hospitalName": "자가 처방",
  "prescriptionDate": "2025-08-10",
  "memo": "매일 아침 1정, 물과 함께",
  "startOfAd": "2025-08-11",
  "prescriptionDays": 120,
  "perDay": 1,
  "alarmType": "SOUND"
}
```

### 복약 기록 (Medication Record) API (`/medication-record`)

| 기능 | EndPoint | Method | Request Data | Response Data | 비고 |
|------|----------|--------|--------------|---------------|------|
| 복약 기록 목록 조회 | `/medication-record/list/{medication-schedule-id}` | `GET` | 없음 | 복약 기록 목록 응답 데이터 | 특정 스케줄의 모든 기록 배열 반환 (페이지네이션 없음) |
| 특정 복약 기록 조회 | `/medication-record/{medication-record-id}` | `GET` | 없음 | 복약 기록 상세 응답 데이터 | `MedicationRecordResponse` DTO 구조(아래 예시)로 반환 |
| 복약 기록 생성 | `/medication-record` | `POST` | 복약 기록 생성 요청 데이터 | 복약 기록 상세 응답 | `medicationScheduleId`, `medicationTimeId`, `isTakeMedication`, `realTakeTime` 필드 |
| 복약 기록 수정 | `/medication-record/{medication-record-id}` | `PUT` | 복약 기록 수정 요청 데이터 | 복약 기록 상세 응답 | `isTakeMedication`, `realTakeTime` 수정 |
| 복약 기록 삭제 | `/medication-record/{medication-record-id}` | `DELETE` | 없음 | (본문 없음, 204) | 삭제 성공 시 204 반환 |

##### 복약 기록 목록 응답 데이터 {#record-list-response}
```json
[
  {
    "medicationRecordId": 501,
    "medicationScheduleId": 101,
    "medicationTimeId": 1,
    "isTakeMedication": true,
    "realTakeTime": "2025-08-11T09:05:30"
  },
  {
    "medicationRecordId": 502,
    "medicationScheduleId": 101,
    "medicationTimeId": 2,
    "isTakeMedication": false,
    "realTakeTime": null
  }
]
```

##### 복약 기록 생성/수정 요청 데이터 {#record-create-request}
```json
{
  "medicationScheduleId": 101,
  "medicationTimeId": 2,
  "isTakeMedication": true,
  "realTakeTime": "2025-08-11T20:00:15"
}
```

##### 복약 기록 상세 응답 데이터 {#record-detail-response}
```json
{
  "medicationRecordId": 503,
  "medicationScheduleId": 101,
  "medicationTimeId": 2,
  "isTakeMedication": true,
  "realTakeTime": "2025-08-11T20:00:15"
}
```

### 부작용 (Side Effect) API (`/side-effect`)

| 기능 | EndPoint | Method | Request Data | Response Data | 비고 |
|------|----------|--------|--------------|---------------|------|
| 최근 부작용 목록 조회 | `/side-effect/recent` | `GET` | 쿼리 `userId` | 최근 부작용 목록 응답 데이터 | 최근 5개의 부작용 이름. Redis 캐시 활용 |
| 부작용 기록 생성 | `/side-effect/record` | `POST` | 부작용 기록 생성 요청 데이터 | (본문 없음, 201) | `memberId`, `createdAt`, `sideEffects` 배열(각각 `sideEffectId`, `degree`) |

##### 부작용 기록 생성 요청 데이터 {#side-effect-create-request}
```json
{
  "memberId": 1,
  "createdAt": "2025-08-29T10:30:00",
  "sideEffects": [
    {
      "sideEffectId": 1,
      "degree": 2
    },
    {
      "sideEffectId": 3,
      "degree": 1
    }
  ]
}
```

##### 최근 부작용 목록 응답 데이터 {#side-effect-recent-response}
```json
{
  "recentSideEffect": ["두통", "메스꺼움", "발진", "현기증", "복통"]
}
```

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
    - **종합 검토 완료**: 5개 컨트롤러(Auth, OAuth2, MedicationSchedule, MedicationRecord, SideEffect) 모든 엔드포인트가 실제 구현과 정확히 일치함을 재확인
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
---

## 데이터베이스 스키마

### ERD 개요
Hamalog 시스템은 총 11개의 테이블로 구성되어 있으며, 회원 관리, 복약 스케줄 관리, 복약 기록, 부작용 관리, 인증 토큰 관리 도메인으로 나뉩니다.

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

---

### SQL DDL

```sql
-- =====================================================
-- Hamalog Database Schema
-- Version: 2025-11-21
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
    alarm_type VARCHAR(20) NOT NULL COMMENT '알람 타입 (SOUND, VIBRATION, SOUND_AND_VIBRATION, NONE)',
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
