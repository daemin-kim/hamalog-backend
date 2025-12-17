# Hamalog API 명세서

> 📖 이 문서는 프론트엔드 개발자를 위한 REST API 명세서입니다.
> 인프라 구성, 데이터베이스 스키마, 변경 이력 등은 [API 참고 문서](./API-reference.md)를 참조하세요.

## 프로젝트 문서

| 문서 | 설명 | 대상 |
|------|------|------|
| [API 명세서](./API-specification.md) | 현재 문서 - REST API 엔드포인트 상세 명세 | 프론트엔드 개발자 |
| [API 참고 문서](./API-reference.md) | 인프라, 스키마, 변경 이력 | 백엔드/DevOps |
| [프로젝트 구조 설명서](./Project-Structure.md) | 프로젝트 디렉토리 구조, 아키텍처, 배포 구성 | 전체 |
| [README](./README.md) | 프로젝트 소개 및 시작 가이드 | 전체 |

---

## API 버전 정보

> ⚠️ **2025-12-16 변경**: 모든 API 경로에 `/api/v1/` 프리픽스가 추가되었습니다.

| 버전 | 프리픽스 | 상태 |
|------|----------|------|
| v1 (현재) | `/api/v1/` | ✅ 활성 |

**예시:**
- 기존: `/auth/login` → 변경: `/api/v1/auth/login`
- 기존: `/medication-schedule` → 변경: `/api/v1/medication-schedule`

---

## 공통 사항

⚠️ **로그인 및 회원가입을 제외한 모든 엔드포인트는 JWT 토큰 기반 인증이 필요합니다. (`/api/v1/auth/csrf-token`, `/api/v1/auth/csrf-status` 포함)** ⚠️

- 토큰은 요청 헤더에 포함해야 합니다. (예: `Authorization: Bearer {token}`)
- 인증되지 않은 사용자가 API에 접근하는 것을 방지하기 위함입니다.
- EndPoint에 파라미터가 포함되는 경우(GET 등), 구별하기 위해 파라미터는 `{}`로 감싸져 있습니다.
- 가독성을 위해 엔드포인트 내 변수명은 케밥 케이스(띄어쓰기 대신에 하이픈 '-'이 들어간 형태)로 작성합니다.
- 모든 API는 리소스 소유권 검증을 통해 본인의 데이터만 접근 가능하도록 보안이 강화되었습니다.
- SPA 클라이언트는 `/api/v1/auth/csrf-token` 으로 발급받은 CSRF 토큰을 `X-CSRF-TOKEN` 헤더에 포함해 안전하지 않은 메서드(POST, PUT, DELETE)에 함께 전송해야 합니다.

### Base URL

| 환경 | URL | 비고 |
|------|-----|------|
| **Production** | `https://api.hamalog.shop` | Cloudflare Tunnel을 통한 보안 연결 |
| **Local Development** | `http://localhost:8080` | Docker Compose 로컬 환경 |

### 에러 응답 규칙

모든 에러 응답은 아래와 같이 일관된 형식을 따릅니다.

```json
{
  "code": "에러 코드",
  "message": "에러 메시지",
  "path": "/요청 경로",
  "violations": null,
  "timestamp": "2025-12-17T12:34:56.789",
  "traceId": "550e8400-e29b-41d4-a716-446655440000"
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `code` | String | 에러 코드 (예: `NOT_FOUND`, `BAD_REQUEST`) |
| `message` | String | 사용자 친화적 에러 메시지 |
| `path` | String | 요청 경로 |
| `violations` | Object | 유효성 검사 실패 시 필드별 에러 상세 |
| `timestamp` | DateTime | 에러 발생 시각 (ISO 8601 형식) |
| `traceId` | String | 요청 추적 ID (로그 상관관계 분석용) |

#### 유효성 검사 실패 시 응답 형식

Bean Validation 실패 시 `violations` 필드에 각 필드별 에러 상세 정보가 포함됩니다.

```json
{
  "code": "BAD_REQUEST",
  "message": "입력값이 유효하지 않습니다",
  "path": "/api/v1/auth/signup",
  "violations": {
    "loginId": "올바른 이메일 형식이어야 합니다",
    "nickName": "닉네임은 한글 또는 영어 1~10자여야 합니다"
  },
  "timestamp": "2025-12-17T12:34:56.789",
  "traceId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### HTTP 상태 코드

| 코드 | 의미 | 설명 |
|------|------|------|
| **200** | 성공 | 요청 처리 완료 |
| **201** | 생성 성공 | 리소스 생성 완료 |
| **204** | 삭제 성공 | 본문 없음 |
| **400** | 잘못된 요청 | 클라이언트 측 오류 |
| **401** | 인증 실패 | 유효하지 않은 토큰 또는 자격 증명 |
| **403** | 권한 없음 | 다른 사용자의 데이터 접근 시도 |
| **404** | 리소스 없음 | 요청한 리소스를 찾을 수 없음 |
| **409** | 중복 리소스 | 회원가입 중복 등 |
| **415** | 지원하지 않는 미디어 타입 | 잘못된 Content-Type |
| **500** | 서버 에러 | 서버 내부 문제 |

---

## 프론트엔드 테스트 가이드 🧪

### 필수 필드 테스트
- 필수 필드를 누락하거나 빈 문자열(`""`)로 전송하여 `400 BAD_REQUEST` 응답 확인
- `violations` 배열에서 해당 필드의 에러 메시지 확인

### 형식 검증 테스트
| 필드 타입 | 테스트 케이스 |
|-----------|---------------|
| 이메일 | `"test"`, `"test@"`, `"@test.com"` → 실패 |
| 전화번호 | `"01112345678"`, `"0101234567"`, `"010-1234-5678"` → 실패 |
| 날짜 | `"2025/12/01"`, `"12-01-2025"`, `"invalid"` → 실패 |
| 닉네임 | `"홍길동123"`, `"test_user"`, `"A"*11` → 실패 |

### 길이 제한 테스트
| 필드 | 최소 | 최대 | 테스트 방법 |
|------|------|------|-------------|
| password | 6자 | 30자 (회원가입), 100자 (로그인) | 5자 문자열, 31자 문자열 전송 |
| name | 1자 | 15자 | 빈 문자열, 16자 문자열 전송 |
| nickName | 1자 | 10자 | 빈 문자열, 11자 문자열 전송 |
| 약 이름 (name) | 1자 | 20자 | 21자 문자열 전송 |
| memo | - | 500자 | 501자 문자열 전송 |
| templateAnswer | - | 500자 | 501자 문자열 전송 |
| freeContent | - | 1500자 | 1501자 문자열 전송 |

### Enum 값 테스트
| 필드 | 유효한 값 | 테스트 방법 |
|------|-----------|-------------|
| alarmType | `SOUND`, `VIBE` | `"INVALID"`, `"sound"` (소문자) 전송 |
| moodType | `HAPPY`, `EXCITED`, `PEACEFUL`, `ANXIOUS`, `LETHARGIC`, `ANGRY`, `SAD` | `"INVALID"`, `"happy"` 전송 |
| diaryType | `TEMPLATE`, `FREE_FORM` | `"INVALID"`, `"template"` 전송 |

### 조건부 필수 필드 테스트 (마음 일기)
- `diaryType=TEMPLATE` + `templateAnswer1~4` 중 하나라도 누락 → `400 BAD_REQUEST`
- `diaryType=FREE_FORM` + `freeContent` 누락 → `400 BAD_REQUEST`

---

## API 엔드포인트

### 인증 (Authentication) / CSRF API (`/api/v1/auth`, `/api/v1/oauth2`)

| 기능 | EndPoint | Method | Request Data | Response Data | 비고 |
|------|----------|--------|--------------|---------------|------|
| 회원가입 | `/api/v1/auth/signup` | `POST` | 회원가입 요청 데이터 | `"회원가입이 성공적으로 완료되었습니다"` | loginId 이메일 형식 필수, nickName 한글/영어 1-10자, phoneNumber 010으로 시작하는 11자리 |
| 일반 로그인 | `/api/v1/auth/login` | `POST` | 로그인 요청 데이터 | 로그인 응답 데이터 | JWT Access Token, Refresh Token, expiresIn(초), tokenType=Bearer 포함 |
| 토큰 갱신 | `/api/v1/auth/refresh` | `POST` | 토큰 갱신 요청 데이터 | 토큰 갱신 응답 데이터 | Refresh Token 로테이션. 새 Access/Refresh Token 반환 |
| 로그아웃 | `/api/v1/auth/logout` | `POST` | Authorization 헤더 | `"로그아웃이 성공적으로 처리되었습니다"` | Access Token 블랙리스트 등록으로 즉시 무효화 |
| 회원 탈퇴 | `/api/v1/auth/account` | `DELETE` | Authorization 헤더 | `"회원 탈퇴가 완료되었습니다"` | 인증 필요. 관련 데이터 모두 삭제 |
| CSRF 토큰 발급 | `/api/v1/auth/csrf-token` | `GET` | Authorization 헤더 | CSRF 토큰 응답 데이터 | **JWT 인증 필수**. 60분 TTL |
| CSRF 토큰 상태 확인 | `/api/v1/auth/csrf-status` | `GET` | Authorization 헤더, `X-CSRF-TOKEN` | CSRF 상태 응답 데이터 | **JWT 인증 필수** |
| 카카오 로그인 시작 | `/api/v1/oauth2/auth/kakao` | `GET` | 없음 | 302 리다이렉션 | 카카오 인증 서버로 리다이렉션 |
| 카카오 로그인 콜백 | `/api/v1/oauth2/auth/kakao/callback` | `GET` | `?code={authorization_code}&state={state}` | 302 리다이렉션 | JWT 발급 후 RN 앱으로 리다이렉트 |

#### 인증 API 데이터 구조

##### 회원가입 요청 데이터
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

| 필드명 | 타입 | 필수 | 제약 조건 |
|--------|------|------|-----------|
| `loginId` | String | ✅ | 이메일 형식 필수 |
| `password` | String | ✅ | 6~30자 |
| `name` | String | ✅ | 1~15자 |
| `nickName` | String | ✅ | 한글/영어 1~10자 |
| `phoneNumber` | String | ✅ | 010으로 시작하는 11자리 |
| `birth` | LocalDate | ✅ | yyyy-MM-dd, 과거 날짜만 |

##### 로그인 요청 데이터
```json
{
  "loginId": "user@example.com",
  "password": "password123"
}
```

##### 로그인 응답 데이터
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiJ9...",
  "refresh_token": "eyJhbGciOiJIUzI1NiJ9...",
  "expires_in": 900,
  "token_type": "Bearer"
}
```

##### 토큰 갱신 요청 데이터
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

##### 토큰 갱신 응답 데이터
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiJ9.newAccessToken...",
  "refresh_token": "eyJhbGciOiJIUzI1NiJ9.newRefreshToken...",
  "expires_in": 900
}
```

##### CSRF 토큰 응답 데이터
```json
{
  "csrfToken": "d1d9a7e2-...",
  "headerName": "X-CSRF-TOKEN",
  "expiryMinutes": 60,
  "storage": "redis",
  "timestamp": "2025-11-26T10:00:00Z"
}
```

---

### 복약 스케줄 (Medication Schedule) API (`/api/v1/medication-schedule`)

| 기능 | EndPoint | Method | Request Data | Response Data | 비고 |
|------|----------|--------|--------------|---------------|------|
| 목록 조회 | `/api/v1/medication-schedule/list/{member-id}` | `GET` | 쿼리: `page`, `size` (최대 100) | 복약 스케줄 목록 | 페이지네이션 |
| 상세 조회 | `/api/v1/medication-schedule/{medication-schedule-id}` | `GET` | 없음 | 복약 스케줄 상세 | - |
| 등록 | `/api/v1/medication-schedule` | `POST` (`multipart/form-data`) | data(JSON), image(선택) | 복약 스케줄 상세 | 이미지 최대 5MB |
| 수정 | `/api/v1/medication-schedule/{medication-schedule-id}` | `PUT` | 수정 요청 데이터 | 복약 스케줄 상세 | - |
| 삭제 | `/api/v1/medication-schedule/{medication-schedule-id}` | `DELETE` | 없음 | 204 No Content | - |

#### 복약 스케줄 데이터 구조

##### 목록 응답 데이터
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

##### 등록 요청 데이터 (multipart/form-data)

**React Native 예시:**
```javascript
const formData = new FormData();
formData.append('data', JSON.stringify({
  memberId: 1,
  name: "종합 비타민",
  hospitalName: "자가 처방",
  prescriptionDate: "2025-08-10",
  memo: "매일 아침 1정",
  startOfAd: "2025-08-11",
  prescriptionDays: 90,
  perDay: 1,
  alarmType: "VIBE"
}));

// 이미지가 있는 경우
if (imageFile) {
  formData.append('image', {
    uri: imageFile.uri,
    type: 'image/jpeg',
    name: 'image.jpg'
  });
}
```

| 필드명 | 타입 | 필수 | 제약 조건 |
|--------|------|------|-----------|
| `memberId` | Long | ✅ | - |
| `name` | String | ✅ | 최대 20자 |
| `hospitalName` | String | ✅ | 최대 20자 |
| `prescriptionDate` | String | ✅ | yyyy-MM-dd |
| `memo` | String | ❌ | 최대 500자 |
| `startOfAd` | String | ✅ | yyyy-MM-dd |
| `prescriptionDays` | Integer | ✅ | 1 이상 |
| `perDay` | Integer | ✅ | 1 이상 |
| `alarmType` | String | ✅ | `SOUND` 또는 `VIBE` |

---

### 복약 기록 (Medication Record) API (`/api/v1/medication-record`)

| 기능 | EndPoint | Method | Request Data | Response Data | 비고 |
|------|----------|--------|--------------|---------------|------|
| 목록 조회 | `/api/v1/medication-record/list/{medication-schedule-id}` | `GET` | 없음 | 복약 기록 목록 | 특정 스케줄의 모든 기록 |
| 상세 조회 | `/api/v1/medication-record/{medication-record-id}` | `GET` | 없음 | 복약 기록 상세 | - |
| 생성 | `/api/v1/medication-record` | `POST` | 생성 요청 데이터 | 복약 기록 상세 | - |
| 수정 | `/api/v1/medication-record/{medication-record-id}` | `PUT` | 수정 요청 데이터 | 복약 기록 상세 | - |
| 삭제 | `/api/v1/medication-record/{medication-record-id}` | `DELETE` | 없음 | 204 No Content | - |

#### 복약 기록 데이터 구조

##### 생성/수정 요청 데이터
```json
{
  "medicationScheduleId": 101,
  "medicationTimeId": 2,
  "isTakeMedication": true,
  "realTakeTime": "2025-08-11T20:00:15"
}
```

##### 응답 데이터
```json
{
  "medicationRecordId": 503,
  "medicationScheduleId": 101,
  "medicationTimeId": 2,
  "isTakeMedication": true,
  "realTakeTime": "2025-08-11T20:00:15"
}
```

---

### 부작용 (Side Effect) API (`/api/v1/side-effect`)

| 기능 | EndPoint | Method | Request Data | Response Data | 비고 |
|------|----------|--------|--------------|---------------|------|
| 최근 부작용 조회 | `/api/v1/side-effect/recent` | `GET` | 쿼리 `userId` | 최근 부작용 목록 | 최근 5개, Redis 캐시 |
| 부작용 기록 생성 | `/api/v1/side-effect/record` | `POST` | 부작용 기록 요청 | 201 Created | - |

#### 부작용 데이터 구조

##### 기록 생성 요청 데이터
```json
{
  "memberId": 1,
  "createdAt": "2025-08-29T10:30:00",
  "sideEffects": [
    { "sideEffectId": 1, "degree": 2 },
    { "sideEffectId": 3, "degree": 1 }
  ]
}
```

##### 최근 부작용 응답 데이터
```json
{
  "recentSideEffect": ["두통", "메스꺼움", "발진", "현기증", "복통"]
}
```

---

### 마음 일기 (Mood Diary) API (`/api/v1/mood-diary`)

| 기능 | EndPoint | Method | Request Data | Response Data | 비고 |
|------|----------|--------|--------------|---------------|------|
| 생성 | `/api/v1/mood-diary` | `POST` | 마음 일기 생성 요청 | 마음 일기 상세 | 하루 1회 제한 |
| 상세 조회 | `/api/v1/mood-diary/{mood-diary-id}` | `GET` | 없음 | 마음 일기 상세 | 소유자만 접근 가능 |
| 목록 조회 | `/api/v1/mood-diary/list/{member-id}` | `GET` | 쿼리: `page`, `size` | 마음 일기 목록 | 최신 순, 페이지네이션 |
| 날짜별 조회 | `/api/v1/mood-diary/date/{member-id}` | `GET` | 쿼리: `diaryDate` | 마음 일기 상세 | yyyy-MM-dd |
| 삭제 | `/api/v1/mood-diary/{mood-diary-id}` | `DELETE` | 없음 | 204 No Content | 소유자만 삭제 가능 |

#### 마음 일기 데이터 구조

##### 생성 요청 데이터 (템플릿 형식)
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

##### 생성 요청 데이터 (자유 형식)
```json
{
  "memberId": 1,
  "diaryDate": "2025-12-01",
  "moodType": "PEACEFUL",
  "diaryType": "FREE_FORM",
  "freeContent": "오늘은 전반적으로 평온한 하루였다..."
}
```

| 필드명 | 타입 | 필수 | 제약 조건 |
|--------|------|------|-----------|
| `memberId` | Long | ✅ | - |
| `diaryDate` | LocalDate | ✅ | yyyy-MM-dd, 동일 날짜 중복 불가 |
| `moodType` | MoodType | ✅ | HAPPY, EXCITED, PEACEFUL, ANXIOUS, LETHARGIC, ANGRY, SAD |
| `diaryType` | DiaryType | ✅ | TEMPLATE, FREE_FORM |
| `templateAnswer1~4` | String | 📌 | TEMPLATE일 때 필수, 각 최대 500자 |
| `freeContent` | String | 📌 | FREE_FORM일 때 필수, 최대 1500자 |

> 📌 = `diaryType`에 따라 조건부 필수

**템플릿 질문:**
1. 오늘 나에게 가장 인상 깊었던 사건은 무엇이었나요?
2. 그 순간, 나는 어떤 감정을 느꼈나요?
3. 그 감정을 느낀 이유는 무엇이라고 생각하나요?
4. 지금 이 감정에 대해 내가 해주고 싶은 말은 무엇인가요?

**기분 타입 (MoodType):**
| 값 | 의미 |
|----|------|
| `HAPPY` | 행복 |
| `EXCITED` | 신남 |
| `PEACEFUL` | 평온 |
| `ANXIOUS` | 불안&긴장 |
| `LETHARGIC` | 무기력 |
| `ANGRY` | 분노 |
| `SAD` | 슬픔 |

##### 응답 데이터
```json
{
  "moodDiaryId": 1,
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
```

##### 목록 응답 데이터
```json
{
  "diaries": [...],
  "totalCount": 15,
  "currentPage": 0,
  "pageSize": 20,
  "hasNext": false,
  "hasPrevious": false
}
```

---

## 에러 코드 목록

### 인증 관련

| 에러 코드 | 메시지 | HTTP |
|-----------|--------|------|
| `UNAUTHORIZED` | 인증이 필요합니다. | 401 |
| `FORBIDDEN` | 접근 권한이 없습니다. | 403 |
| `INVALID_TOKEN` | 유효하지 않은 토큰입니다. | 401 |
| `TOKEN_EXPIRED` | 토큰이 만료되었습니다. | 401 |
| `INVALID_REFRESH_TOKEN` | 유효하지 않은 Refresh Token입니다. | 401 |

### 회원 관련

| 에러 코드 | 메시지 | HTTP |
|-----------|--------|------|
| `MEMBER_NOT_FOUND` | 회원을 찾을 수 없습니다. | 404 |
| `DUPLICATE_MEMBER` | 이미 존재하는 회원입니다. | 409 |

### 복약 관련

| 에러 코드 | 메시지 | HTTP |
|-----------|--------|------|
| `SCHEDULE_NOT_FOUND` | 복약 스케줄을 찾을 수 없습니다. | 404 |
| `RECORD_NOT_FOUND` | 복약 기록을 찾을 수 없습니다. | 404 |
| `TIME_NOT_FOUND` | 복약 시간 정보를 찾을 수 없습니다. | 404 |

### 부작용 관련

| 에러 코드 | 메시지 | HTTP |
|-----------|--------|------|
| `SIDE_EFFECT_NOT_FOUND` | 부작용 정보를 찾을 수 없습니다. | 404 |
| `INVALID_DEGREE` | 부작용 정도는 1-5 사이여야 합니다. | 400 |

### 마음 일기 관련

| 에러 코드 | 메시지 | HTTP |
|-----------|--------|------|
| `MOOD_DIARY_NOT_FOUND` | 마음 일기를 찾을 수 없습니다. | 404 |
| `DIARY_ALREADY_EXISTS` | 해당 날짜에 이미 일기가 작성되어 있습니다. | 409 |
| `INVALID_DIARY_TYPE` | 유효하지 않은 일기 형식입니다. | 400 |
| `TEMPLATE_ANSWER_REQUIRED` | 템플릿 형식에서는 모든 질문에 답변이 필요합니다. | 400 |
| `FREE_CONTENT_REQUIRED` | 자유 형식에서는 내용이 필요합니다. | 400 |

### 유효성 검사 관련

| 에러 코드 | 메시지 | HTTP |
|-----------|--------|------|
| `BAD_REQUEST` | 잘못된 요청입니다. | 400 |
| `INVALID_INPUT` | 입력값이 유효하지 않습니다. | 400 |
| `MISSING_REQUIRED_FIELD` | 필수 필드가 누락되었습니다. | 400 |
| `UNSUPPORTED_MEDIA_TYPE` | 지원하지 않는 Content-Type입니다. | 415 |

### 파일 관련

| 에러 코드 | 메시지 | HTTP |
|-----------|--------|------|
| `FILE_SAVE_FAIL` | 파일 저장에 실패했습니다. | 500 |
| `FILE_SIZE_EXCEEDED` | 파일 크기가 제한을 초과했습니다. | 413 |
| `INVALID_FILE_TYPE` | 지원하지 않는 파일 형식입니다. | 400 |

---

> 📖 더 자세한 정보(인프라 구성, DB 스키마, 변경 이력 등)는 [API 참고 문서](./API-reference.md)를 참조하세요.
