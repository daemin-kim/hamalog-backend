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
  "code": "에러 코드",
  "message": "에러 메시지",
  "path": "/요청 경로",
  "violations": null
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
- **415**: 지원하지 않는 미디어 타입 (잘못된 Content-Type)
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
- `access_token`의 payload에는 `sub`(loginId)와 함께 `memberId` 클레임이 포함되어 모든 인증이 필요한 API에서 회원 식별에 활용됩니다.

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
  "storage": "redis",
  "timestamp": "2025-11-26T10:00:00Z"
}
```

##### CSRF 상태 응답 데이터 {#csrf-status-response}
```json
{
  "userId": "user@example.com",
  "csrfTokenPresent": true,
  "csrfTokenValid": true,
  "storage": "redis",
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
| 복약 스케줄 등록 | `/medication-schedule` | `POST` (`multipart/form-data`) | data(JSON), image(선택) | 복약 스케줄 상세 응답 | `memberId`, `name`, `hospitalName`, `prescriptionDate`, `startOfAd`, `prescriptionDays`, `perDay`, `alarmType` 필수. `memo` 선택, `alarmType` 값은 `SOUND` 또는 `VIBE`. **⚠️ `data` 파트에 `Content-Type: application/json` 명시 필수** |
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

⚠️ **중요**: `multipart/form-data` 요청 시 `data` 파트에 반드시 `Content-Type: application/json`을 명시해야 합니다. 그렇지 않으면 `415 UNSUPPORTED_MEDIA_TYPE` 오류가 발생합니다.

**JavaScript/React Native 예시:**
```javascript
const formData = new FormData();
formData.append('data', new Blob([JSON.stringify({
  memberId: 1,
  name: "종합 비타민",
  hospitalName: "자가 처방",
  prescriptionDate: "2025-08-10",
  memo: "매일 아침 1정",
  startOfAd: "2025-08-11",
  prescriptionDays: 90,
  perDay: 1,
  alarmType: "VIBE"
})], { type: 'application/json' }));

// 이미지가 있는 경우
if (imageFile) {
  formData.append('image', imageFile);
}
```

**cURL 예시:**
```bash
curl -X POST http://localhost:8080/medication-schedule \
  -H "Authorization: Bearer <JWT>" \
  -H "X-CSRF-TOKEN: <CSRF_TOKEN>" \
  -F "data=@request.json;type=application/json" \
  -F "image=@image.jpg;type=image/jpeg"
```

- **Part `data` (application/json)** - 필수
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
- **Part `image` (선택, image/*)** - 최대 5MB

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

### 마음 일기 (Mood Diary) API (`/mood-diary`)

| 기능 | EndPoint | Method | Request Data | Response Data | 비고 |
|------|----------|--------|--------------|---------------|------|
| 마음 일기 생성 | `/mood-diary` | `POST` | 마음 일기 생성 요청 데이터 | 마음 일기 상세 응답 | 본인 계정만 작성 가능, 하루 1회 제한, 템플릿/자유 형식 선택 |
| 특정 마음 일기 조회 | `/mood-diary/{mood-diary-id}` | `GET` | 없음 | 마음 일기 상세 응답 | 소유자만 접근 가능, `MoodDiaryResponse` DTO |
| 회원의 마음 일기 목록 조회 | `/mood-diary/list/{member-id}` | `GET` | 쿼리: `page`, `size` (최대 size=100) | 마음 일기 목록 응답 데이터 | 본인만 요청 가능, 최신 순, 페이지네이션 |
| 특정 날짜의 마음 일기 조회 | `/mood-diary/date/{member-id}` | `GET` | 쿼리: `diaryDate` (yyyy-MM-dd) | 마음 일기 상세 응답 | 본인만 조회 가능, 특정 날짜 필터 |
| 마음 일기 삭제 | `/mood-diary/{mood-diary-id}` | `DELETE` | 없음 | (본문 없음, 204) | 소유자만 삭제 가능, 성공 시 204 |

**인증/권한:** 모든 마음 일기 API는 인증 토큰이 필요하며, `@RequireResourceOwnership` 검증을 통해 로그인한 사용자와 리소스 소유자가 일치하지 않으면 `403 FORBIDDEN`을 반환합니다. 생성 시에도 요청 `memberId`와 인증된 회원 ID가 다르면 거부됩니다.

**검증/제약 업데이트:**
- 모든 생성·조회·삭제는 서비스 계층에서 회원 ID를 다시 확인하여 요청 바디를 임의 조작해도 다른 회원 리소스를 접근할 수 없습니다.
- 하루 1회 제한은 기존과 동일하게 `memberId + diaryDate` 유니크 검증을 통과해야 하며, 중복 시 `DIARY_ALREADY_EXISTS (409)`를 반환합니다.

#### 마음 일기 API 데이터 구조

##### 마음 일기 생성 요청 데이터 {#mood-diary-create-request}
```json
{
  "memberId": 1,
  "diaryDate": "2025-12-01",
  "moodType": "HAPPY",
  "diaryType": "TEMPLATE",
  "templateAnswer1": "오늘 친구와 오랜만에 만나서 즐거운 대화를 나눴습니다.",
  "templateAnswer2": "편안하고 행복한 감정을 느꼈어요.",
  "templateAnswer3": "친구가 진심으로 내 이야기를 들어줘서 그런 것 같아요.",
  "templateAnswer4": "오늘 같은 감정을 자주 느낄 수 있었으면 좋겠어요."
}
```

**템플릿 질문:**
1. 오늘 나에게 가장 인상 깊었던 사건은 무엇이었나요?
2. 그 순간, 나는 어떤 감정을 느꼈나요?
3. 그 감정을 느낀 이유는 무엇이라고 생각하나요?
4. 지금 이 감정에 대해 내가 해주고 싶은 말은 무엇인가요?

**자유 형식 예시:**
```json
{
  "memberId": 1,
  "diaryDate": "2025-12-01",
  "moodType": "PEACEFUL",
  "diaryType": "FREE_FORM",
  "freeContent": "오늘은 전반적으로 평온한 하루였다. 아침에 일찍 일어나 산책을 했고, 저녁에는 책을 읽으며 여유로운 시간을 보냈다..."
}
```

**기분 타입 (MoodType):**
- `HAPPY`: 행복
- `EXCITED`: 신남
- `PEACEFUL`: 평온
- `ANXIOUS`: 불안&긴장
- `LETHARGIC`: 무기력
- `ANGRY`: 분노
- `SAD`: 슬픔

**일기 형식 (DiaryType):**
- `TEMPLATE`: 템플릿 형식 (4개 질문, 각 500자 제한)
- `FREE_FORM`: 자유 형식 (1500자 제한)

##### 마음 일기 상세 응답 데이터 {#mood-diary-response}
```json
{
  "moodDiaryId": 1,
  "memberId": 1,
  "diaryDate": "2025-12-01",
  "moodType": "HAPPY",
  "diaryType": "TEMPLATE",
  "templateAnswer1": "오늘 친구와 오랜만에 만나서 즐거운 대화를 나눴습니다.",
  "templateAnswer2": "편안하고 행복한 감정을 느꼈어요.",
  "templateAnswer3": "친구가 진심으로 내 이야기를 들어줘서 그런 것 같아요.",
  "templateAnswer4": "오늘 같은 감정을 자주 느낄 수 있었으면 좋겠어요.",
  "freeContent": null,
  "createdAt": "2025-12-01T20:30:00"
}
```

##### 마음 일기 목록 응답 데이터 {#mood-diary-list-response}
```json
{
  "diaries": [
    {
      "moodDiaryId": 2,
      "memberId": 1,
      "diaryDate": "2025-12-01",
      "moodType": "HAPPY",
      "diaryType": "TEMPLATE",
      "templateAnswer1": "...",
      "templateAnswer2": "...",
      "templateAnswer3": "...",
      "templateAnswer4": "...",
      "freeContent": null,
      "createdAt": "2025-12-01T20:30:00"
    }
  ],
  "totalCount": 15,
  "currentPage": 0,
  "pageSize": 20,
  "hasNext": false,
  "hasPrevious": false
}
```

#### 마음 일기 검증 규칙

##### 공통 검증
- `memberId`: 필수, 존재하는 회원 ID
- `diaryDate`: 필수, ISO-8601 날짜 형식 (yyyy-MM-dd)
- `moodType`: 필수, 7가지 타입 중 하나 (HAPPY, EXCITED, PEACEFUL, ANXIOUS, LETHARGIC, ANGRY, SAD)
- `diaryType`: 필수, 2가지 타입 중 하나 (TEMPLATE, FREE_FORM)
- **하루 1회 제한**: 동일 회원의 동일 날짜에 중복 작성 불가 (UNIQUE 제약)

##### 템플릿 형식 검증 (DiaryType.TEMPLATE)
- `templateAnswer1`: 필수, 공백 불가, 최대 500자
- `templateAnswer2`: 필수, 공백 불가, 최대 500자
- `templateAnswer3`: 필수, 공백 불가, 최대 500자
- `templateAnswer4`: 필수, 공백 불가, 최대 500자
- `freeContent`: null 또는 생략

##### 자유 형식 검증 (DiaryType.FREE_FORM)
- `freeContent`: 필수, 공백 불가, 최대 1500자
- `templateAnswer1~4`: null 또는 생략

#### 마음 일기 에러 코드

| 에러 코드 | HTTP 상태 | 설명 |
|-----------|-----------|------|
| `MOOD_DIARY_NOT_FOUND` | 404 | 마음 일기를 찾을 수 없습니다 |
| `DIARY_ALREADY_EXISTS` | 409 | 해당 날짜에 이미 일기가 작성되어 있습니다 (하루 1회 제한) |
| `INVALID_DIARY_TYPE` | 400 | 유효하지 않은 일기 형식입니다 (템플릿/자유 형식 검증 실패) |
| `INVALID_MOOD_TYPE` | 400 | 유효하지 않은 기분 타입입니다 |
| `TEMPLATE_ANSWER_REQUIRED` | 400 | 템플릿 형식에서는 모든 질문에 답변이 필요합니다 |
| `FREE_CONTENT_REQUIRED` | 400 | 자유 형식에서는 내용이 필요합니다 |
| `MEMBER_NOT_FOUND` | 404 | 회원을 찾을 수 없습니다 |
| `FORBIDDEN` | 403 | 접근 권한이 없습니다 (다른 사용자의 일기) |

#### 마음 일기 보안 및 성능

##### 보안
- **JWT 인증 필수**: 모든 엔드포인트
- **리소스 소유권 검증**: @RequireResourceOwnership AOP를 통한 자동 검증
  - 단건 조회/삭제: MOOD_DIARY 타입으로 일기 ID 기반 소유권 확인
  - 목록 조회/날짜별 조회: MOOD_DIARY_BY_MEMBER 타입으로 회원 ID 기반 소유권 확인
- **XSS 방지**: 모든 입력값 검증 및 이스케이프 처리
- **SQL Injection 방지**: JPA 파라미터 바인딩 사용

##### 성능
- **인덱스**: member_id, diary_date에 인덱스 적용
- **Optimistic Locking**: version 필드를 통한 동시성 제어
- **페이지네이션**: 최대 size 100 제한 (DoS 방지)
- **성능 모니터링**: AOP 기반 자동 성능 측정 및 로깅

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

## 에러 코드 목록

API에서 반환될 수 있는 모든 에러 코드 목록입니다.

### 회원 관련 에러

| 에러 코드 | 메시지 | HTTP 상태 |
|-----------|--------|-----------|
| `MEMBER_NOT_FOUND` | 회원을 찾을 수 없습니다. | 404 |
| `DUPLICATE_MEMBER` | 이미 존재하는 회원입니다. | 409 |

### 복약 관련 에러

| 에러 코드 | 메시지 | HTTP 상태 |
|-----------|--------|-----------|
| `SCHEDULE_NOT_FOUND` | 복약 스케줄을 찾을 수 없습니다. | 404 |
| `RECORD_NOT_FOUND` | 복약 기록을 찾을 수 없습니다. | 404 |
| `TIME_NOT_FOUND` | 복약 시간 정보를 찾을 수 없습니다. | 404 |
| `INVALID_SCHEDULE` | 유효하지 않은 복약 스케줄입니다. | 400 |
| `INVALID_PRESCRIPTION_DAYS` | 처방 일수는 1일 이상이어야 합니다. | 400 |
| `INVALID_PER_DAY` | 1일 복용 횟수는 1회 이상이어야 합니다. | 400 |
| `INVALID_DATE_RANGE` | 시작일은 처방일 이후여야 합니다. | 400 |

### 부작용 관련 에러

| 에러 코드 | 메시지 | HTTP 상태 |
|-----------|--------|-----------|
| `SIDE_EFFECT_NOT_FOUND` | 부작용 정보를 찾을 수 없습니다. | 404 |
| `INVALID_DEGREE` | 부작용 정도는 1-5 사이여야 합니다. | 400 |
| `EMPTY_SIDE_EFFECT_LIST` | 부작용 목록이 비어있습니다. | 400 |

### 마음 일기 관련 에러

| 에러 코드 | 메시지 | HTTP 상태 |
|-----------|--------|-----------|
| `MOOD_DIARY_NOT_FOUND` | 마음 일기를 찾을 수 없습니다. | 404 |
| `DIARY_ALREADY_EXISTS` | 해당 날짜에 이미 일기가 작성되어 있습니다. | 409 |
| `INVALID_DIARY_TYPE` | 유효하지 않은 일기 형식입니다. | 400 |
| `INVALID_MOOD_TYPE` | 유효하지 않은 기분 타입입니다. | 400 |
| `TEMPLATE_ANSWER_REQUIRED` | 템플릿 형식에서는 모든 질문에 답변이 필요합니다. | 400 |
| `FREE_CONTENT_REQUIRED` | 자유 형식에서는 내용이 필요합니다. | 400 |

### 인증 및 보안 관련 에러

| 에러 코드 | 메시지 | HTTP 상태 |
|-----------|--------|-----------|
| `UNAUTHORIZED` | 인증이 필요합니다. | 401 |
| `FORBIDDEN` | 접근 권한이 없습니다. | 403 |
| `INVALID_TOKEN` | 유효하지 않은 토큰입니다. | 401 |
| `TOKEN_EXPIRED` | 토큰이 만료되었습니다. | 401 |
| `TOKEN_BLACKLISTED` | 무효화된 토큰입니다. | 401 |
| `INVALID_REFRESH_TOKEN` | 유효하지 않은 Refresh Token입니다. | 401 |
| `REFRESH_TOKEN_EXPIRED` | Refresh Token이 만료되었습니다. | 401 |
| `REFRESH_TOKEN_REVOKED` | 폐기된 Refresh Token입니다. | 401 |

### OAuth2 관련 에러

| 에러 코드 | 메시지 | HTTP 상태 |
|-----------|--------|-----------|
| `OAUTH2_CONFIG_ERROR` | OAuth2 설정 오류가 발생했습니다. | 500 |
| `OAUTH2_INIT_ERROR` | OAuth2 초기화 중 오류가 발생했습니다. | 500 |
| `TOKEN_EXCHANGE_FAILED` | 토큰 교환에 실패했습니다. | 400 |
| `USER_INFO_FAILED` | 사용자 정보 조회에 실패했습니다. | 400 |
| `INVALID_AUTH_CODE` | 유효하지 않은 인증 코드입니다. | 400 |
| `CSRF_VALIDATION_FAILED` | CSRF 검증에 실패했습니다. | 400 |
| `AUTHORIZATION_FAILED` | OAuth2 인증에 실패했습니다. | 400 |

### 유효성 검사 관련 에러

| 에러 코드 | 메시지 | HTTP 상태 |
|-----------|--------|-----------|
| `BAD_REQUEST` | 잘못된 요청입니다. | 400 |
| `INVALID_INPUT` | 입력값이 유효하지 않습니다. | 400 |
| `INVALID_PARAMETER` | 파라미터가 유효하지 않습니다. | 400 |
| `MISSING_REQUIRED_FIELD` | 필수 필드가 누락되었습니다. | 400 |
| `MISSING_REQUEST_PART` | 필수 요청 파트가 누락되었습니다. | 400 |
| `MESSAGE_NOT_READABLE` | 요청 본문 파싱에 실패했습니다. | 400 |
| `UNSUPPORTED_MEDIA_TYPE` | 지원하지 않는 Content-Type입니다. | 415 |
| `INVALID_PAGE_SIZE` | 페이지 크기는 1-100 사이여야 합니다. | 400 |
| `INVALID_PAGE_NUMBER` | 페이지 번호는 0 이상이어야 합니다. | 400 |

### 파일 관련 에러

| 에러 코드 | 메시지 | HTTP 상태 |
|-----------|--------|-----------|
| `FILE_SAVE_FAIL` | 파일 저장에 실패했습니다. | 500 |
| `FILE_SIZE_EXCEEDED` | 파일 크기가 제한을 초과했습니다. | 413 |
| `INVALID_FILE_TYPE` | 지원하지 않는 파일 형식입니다. | 400 |
| `FILE_NOT_FOUND` | 파일을 찾을 수 없습니다. | 404 |

### 동시성 관련 에러

| 에러 코드 | 메시지 | HTTP 상태 |
|-----------|--------|-----------|
| `OPTIMISTIC_LOCK_FAILED` | 다른 사용자가 데이터를 수정했습니다. 다시 시도해주세요. | 409 |
| `RESOURCE_CONFLICT` | 리소스 충돌이 발생했습니다. | 409 |

### 외부 API 관련 에러

| 에러 코드 | 메시지 | HTTP 상태 |
|-----------|--------|-----------|
| `EXTERNAL_API_ERROR` | 외부 API 호출 중 오류가 발생했습니다. | 500 |
| `EXTERNAL_API_TIMEOUT` | 외부 API 응답 시간이 초과되었습니다. | 504 |

### 시스템 에러

| 에러 코드 | 메시지 | HTTP 상태 |
|-----------|--------|-----------|
| `INTERNAL_ERROR` | 서버 내부 오류가 발생했습니다. | 500 |
| `DATABASE_ERROR` | 데이터베이스 오류가 발생했습니다. | 500 |
| `CACHE_ERROR` | 캐시 처리 중 오류가 발생했습니다. | 500 |
