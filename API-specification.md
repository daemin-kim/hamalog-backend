### 공통 사항

⚠️ **로그인 및 회원가입 외의 모든 엔드포인트는 JWT 토큰 기반 인증이 필요합니다.** ⚠️

- 토큰은 요청 헤더에 포함해야 합니다. (예: `Authorization: Bearer {token}`)
- 인증되지 않은 사용자가 API에 접근하는 것을 방지하기 위함입니다.
- EndPoint에 파라미터가 포함되는 경우(GET 등), 구별하기 위해 파라미터는 `{}`로 감싸져 있습니다.
- 가독성을 위해 엔드포인트 내 변수명은 케밥 케이스(띄어쓰기 대신에 하이픈 '-'이 들어간 형태)로 작성합니다.
- 모든 API는 사용자 권한 검증을 통해 본인의 데이터만 접근 가능하도록 보안이 강화되었습니다.

[http://112.72.157.193:8080](http://112.72.248.195:8080)

### 에러 응답 규칙

- 에러 응답은 아래와 같이 일관된 형식으로 처리합니다.

```json
{
  "errorMessage": "에러 메시지",
  "code": "에러 코드"
}
```

- **200**: 성공
- **201**: 생성 성공
- **204**: 삭제 성공 (본문 없음)
- **400**: 잘못된 요청 (클라이언트 측 오류)
- **401**: 인증 실패 (유효하지 않은 토큰 또는 자격 증명)
- **403**: 권한 없음 (다른 사용자의 데이터 접근 시도)
- **404**: 리소스 없음 (요청한 리소스를 찾을 수 없음)
- **500**: 서버 에러 (서버 내부 문제)
- 작성된 요청, 응답 본문의 형태는 개발이 진행됨에 따라 지속적으로 수정될 수 있습니다.

## API 명세서

### 인증 (Authentication) API (`/auth`, `/oauth2`)

| 기능 | EndPoint | Method | Request Data | Response Data | 비고 |
|------|----------|--------|--------------|---------------|------|
| **회원가입** | `/auth/signup` | `POST` | 회원가입 요청 데이터 예시 참고 | `"회원가입 성공"`<br/>(Content-Type: text/plain) | **loginId는 이메일 형식이어야 합니다.** nickName 필드가 추가되었습니다. 각 필드는 유효성 검사 규칙을 따릅니다. `loginId` 중복 시 400 에러를 응답합니다. |
| **일반 로그인** | `/auth/login` | `POST` | 로그인 요청 데이터 예시 참고 | 로그인 응답 데이터 예시 참고 | **응답 구조 확정.** `refreshToken` 없이 JWT 액세스 토큰만 반환됩니다. |
| **로그아웃** | `/auth/logout` | `POST` | (없음) | `"로그아웃 성공 - 토큰이 무효화되었습니다"`<br/>(Content-Type: text/plain) | 요청 시 헤더에 유효한 JWT 토큰이 필요합니다. **서버 측에서 토큰을 블랙리스트에 추가하여 무효화**합니다. |
| **회원 탈퇴** | `/auth/account` | `DELETE` | (없음) | `"회원 탈퇴가 완료되었습니다"`<br/>(Content-Type: text/plain) | **신규 추가된 기능.** 현재 로그인된 회원의 계정을 삭제합니다. 모든 관련 데이터(복용 기록, 부작용 기록 등)도 함께 삭제됩니다. |
| **카카오 로그인 시작** | `/oauth2/auth/kakao` | `GET` | (없음) | 카카오 인증 서버로 리디렉션 (302) | **구현 완료.** 카카오 OAuth2 인증 과정을 시작합니다. |
| **카카오 로그인 콜백** | `/api/auth/kakao/callback` | `POST` | `?code={authorization_code}` | 카카오 로그인 응답 데이터 예시 참고 | **구현 완료.** 카카오에서 전송된 authorization code를 처리하여 JWT 토큰을 반환합니다. |

#### 인증 API 데이터 구조

##### 회원가입 요청 데이터 {#auth-signup-request}
```json
{
  "loginId": "user@example.com", 
  "password": "password123", 
  "name": "김민준",
  "nickName": "민준이",
  "phoneNumber": "01012345678",
  "birth": "1995-05-15"
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
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwiaWF0IjoxNzIzMzg4NzAwLCJleHAiOjE3MjMzOTIzMDB9.abcdef123456"
}
```

##### 카카오 로그인 응답 데이터 {#auth-kakao-response}
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

### 복약 스케쥴 (Medication Schedule) API (`/medication-schedule`)

| 기능 | EndPoint | Method | Request Data | Response Data | 비고 |
|------|----------|--------|--------------|---------------|------|
| **복약 스케쥴 목록 조회** | `/medication-schedule/list/{member-id}` | `GET` | (없음) | 복약 스케쥴 목록 응답 데이터 예시 참고 | **페이지네이션 지원**이 추가되었습니다. 해당 회원의 모든 복약 스케쥴을 페이지네이션된 형태로 반환합니다. 권한 검증 포함. |
| **특정 복약 스케쥴 조회** | `/medication-schedule/{medication-schedule-id}` | `GET` | (없음) | 특정 복약 스케쥴 응답 데이터 예시 참고 | **응답 구조 간소화.** member 객체 대신 memberId 필드를 사용합니다. imagePath 필드는 현재 미사용입니다. 권한 검증 포함. |
| **복약 스케쥴 등록** | `/medication-schedule` | `POST` | 복약 스케쥴 등록 요청 데이터 예시 참고 | 복약 스케쥴 등록 응답 데이터 예시 참고 | **multipart/form-data 형식 확정.** JSON 데이터는 `data`파트에, 이미지 파일은 `image` 파트에 담아 전송합니다. 이미지 파일은 선택 사항입니다. 성공 시 `201 Created` 상태 코드를 반환합니다. 권한 검증 포함. |
| **복약 스케쥴 수정** | `/medication-schedule/{medication-schedule-id}` | `PUT` | 복약 스케쥴 수정 요청 데이터 예시 참고 | 복약 스케쥴 수정 응답 데이터 예시 참고 | 요청 본문이 `MedicationScheduleUpdateRequest` DTO에 맞게 구성되었습니다. 권한 검증 포함. |
| **복약 스케쥴 삭제** | `/medication-schedule/{medication-schedule-id}` | `DELETE` | (없음) | (없음 - 204 No Content) | 성공적으로 삭제되면 본문 없이 `204 No Content` 상태 코드를 반환합니다. 권한 검증 포함. |

#### 복약 스케쥴 API 데이터 구조

##### 복약 스케쥴 목록 응답 데이터 {#schedule-list-response}
```json
{
  "content": [
    {
      "medicationScheduleId": 101,
      "memberId": 1,
      "name": "혈압약 (아침)",
      "hospitalName": "서울중앙병원",
      "prescriptionDate": "2025-08-01"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 10,
  "totalPages": 1,
  "last": true,
  "first": true
}
```

##### 특정 복약 스케쥴 응답 데이터 {#schedule-detail-response}
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

##### 복약 스케쥴 등록 요청 데이터 {#schedule-create-request}
**Content-Type: `multipart/form-data`**

**Part 1: `data` (application/json)**
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
  "alarmType": "VIBRATION"
}
```

**Part 2: `image` (image/*) - 선택사항**

##### 복약 스케쥴 등록 응답 데이터 {#schedule-create-response}
```json
{
  "medicationScheduleId": 103,
  "memberId": 1,
  "name": "종합 비타민",
  "hospitalName": "자가 처방",
  "prescriptionDate": "2025-08-10",
  "memo": "매일 아침 1정",
  "startOfAd": "2025-08-11",
  "prescriptionDays": 90,
  "perDay": 1,
  "alarmType": "VIBRATION"
}
```

##### 복약 스케쥴 수정 요청 데이터 {#schedule-update-request}
```json
{
  "name": "종합 비타민 (골드)", 
  "hospitalName": "자가 처방", 
  "prescriptionDate": "2025-08-10", 
  "memo": "매일 아침 1정, 물과 함께", 
  "startOfAd": "2025-08-11", 
  "prescriptionDays": 120,
  "perDay": 1,
  "alarmType": "SOUND_AND_VIBRATION"
}
```

##### 복약 스케쥴 수정 응답 데이터 {#schedule-update-response}
```json
{
  "medicationScheduleId": 103,
  "memberId": 1,
  "name": "종합 비타민 (골드)",
  "hospitalName": "자가 처방",
  "prescriptionDate": "2025-08-10",
  "memo": "매일 아침 1정, 물과 함께",
  "startOfAd": "2025-08-11",
  "prescriptionDays": 120,
  "perDay": 1,
  "alarmType": "SOUND_AND_VIBRATION"
}
```

### 복약 기록 (Medication Record) API (`/medication-record`)

| 기능 | EndPoint | Method | Request Data | Response Data | 비고 |
|------|----------|--------|--------------|---------------|------|
| **복약 기록 목록 조회** | `/medication-record/list/{medication-schedule-id}` | `GET` | (없음) | 복약 기록 목록 응답 데이터 예시 참고 | 특정 복약 스케쥴에 속한 모든 복약 기록을 배열로 반환합니다. **페이지네이션은 현재 지원되지 않습니다.** 권한 검증 포함. |
| **특정 복약 기록 조회** | `/medication-record/{medication-record-id}` | `GET` | (없음) | 특정 복약 기록 응답 데이터 예시 참고 | **응답 구조 간소화.** medicationSchedule 중첩 객체 대신 medicationScheduleId 필드를 사용합니다. 권한 검증 포함. |
| **복약 기록 생성** | `/medication-record` | `POST` | 복약 기록 생성 요청 데이터 예시 참고 | 복약 기록 생성 응답 데이터 예시 참고 | 요청 본문이 `MedicationRecordCreateRequest` DTO에 맞게 구성되었습니다. 성공 시 `201 Created` 상태 코드를 반환합니다. 권한 검증 포함. |
| **복약 기록 수정** | `/medication-record/{medication-record-id}` | `PUT` | 복약 기록 수정 요청 데이터 예시 참고 | 복약 기록 수정 응답 데이터 예시 참고 | 요청 본문이 `MedicationRecordUpdateRequest` DTO에 맞게 구성되었습니다. 권한 검증 포함. |
| **복약 기록 삭제** | `/medication-record/{medication-record-id}` | `DELETE` | (없음) | (없음 - 204 No Content) | 성공적으로 삭제되면 본문 없이 `204 No Content` 상태 코드를 반환합니다. 권한 검증 포함. |

#### 복약 기록 API 데이터 구조

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

##### 특정 복약 기록 응답 데이터 {#record-detail-response}
```json
{
  "medicationRecordId": 501,
  "medicationScheduleId": 101,
  "medicationTimeId": 1,
  "isTakeMedication": true,
  "realTakeTime": "2025-08-11T09:05:30"
}
```

##### 복약 기록 생성 요청 데이터 {#record-create-request}
```json
{
  "medicationScheduleId": 101, 
  "medicationTimeId": 2, 
  "isTakeMedication": true, 
  "realTakeTime": "2025-08-11T20:00:15"
}
```

##### 복약 기록 생성 응답 데이터 {#record-create-response}
```json
{
  "medicationRecordId": 503,
  "medicationScheduleId": 101,
  "medicationTimeId": 2,
  "isTakeMedication": true,
  "realTakeTime": "2025-08-11T20:00:15"
}
```

##### 복약 기록 수정 요청 데이터 {#record-update-request}
```json
{
  "isTakeMedication": true, 
  "realTakeTime": "2025-08-11T20:01:00"
}
```

##### 복약 기록 수정 응답 데이터 {#record-update-response}
```json
{
  "medicationRecordId": 502,
  "medicationScheduleId": 101,
  "medicationTimeId": 1,
  "isTakeMedication": true,
  "realTakeTime": "2025-08-11T20:01:00"
}
```

### 부작용 (Side Effect) API (`/side-effect`)

| 기능 | EndPoint | Method | Request Data | Response Data | 비고 |
|------|----------|--------|--------------|---------------|------|
| **부작용 기록 생성** | `/side-effect/record` | `POST` | 부작용 기록 생성 요청 데이터 예시 참고 | (없음 - 201 Created) | **구현 완료.** 새로운 부작용 기록을 생성하고 Redis 캐시를 업데이트합니다. 권한 검증 포함. |
| **최근 선택한 부작용 목록 조회** | `/side-effect/recent` | `GET` | (없음 - 쿼리 파라미터 사용: `?userId=1`) | 최근 부작용 목록 응답 데이터 예시 참고 | 쿼리 파라미터 `userId`가 필수입니다. (예: `/side-effect/recent?userId=1`). 사용자의 최근 부작용 기록 5개의 이름을 문자열 배열로 반환합니다. 권한 검증 포함. |

#### 부작용 API 데이터 구조

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

### 보고서 (Report) API (`/report`)

| 기능 | EndPoint | Method | Request Data | Response Data | 비고 |
|------|----------|--------|--------------|---------------|------|
| **보고서 조회** | `/report` | `GET` | `?userId=1&startDate=2025-07-01&endDate=2025-07-31&type=monthly` | (없음) | **미구현 상태.** |

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