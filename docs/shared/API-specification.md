# Hamalog API 명세서

> 📖 이 문서는 프론트엔드 개발자를 위한 REST API 명세서입니다.
> 인프라 구성, 데이터베이스 스키마, 변경 이력 등은 [API 참고 문서](../internal/API-reference.md)를 참조하세요.

## 프로젝트 문서

| 문서 | 설명 | 대상 |
|------|------|------|
| [API 명세서](./API-specification.md) | 현재 문서 - REST API 엔드포인트 상세 명세 | 프론트엔드 개발자 |
| [API 참고 문서](../internal/API-reference.md) | 인프라, 스키마, 변경 이력 | 백엔드/DevOps |
| [프로젝트 구조 설명서](./Project-Structure.md) | 프로젝트 디렉토리 구조, 아키텍처, 배포 구성 | 전체 |
| [바이브 코딩 가이드](../ai/VIBE-CODING-GUIDE.md) | AI 협업 개발 가이드 | AI/개발자 |
| [코딩 컨벤션](../internal/CODING-CONVENTIONS.md) | 코드 스타일 및 규칙 | 개발자 |
| [README](../README.md) | 프로젝트 소개 및 시작 가이드 | 전체 |

---


## 공통 사항

⚠️ **로그인 및 회원가입을 제외한 모든 엔드포인트는 JWT 토큰 기반 인증이 필요합니다.** ⚠️

- 토큰은 요청 헤더에 포함해야 합니다. (예: `Authorization: Bearer {token}`)
- EndPoint에 파라미터가 포함되는 경우(GET 등), 구별하기 위해 파라미터는 `{}`로 감싸져 있습니다.
- 모든 API는 리소스 소유권 검증을 통해 본인의 데이터만 접근 가능하도록 보안이 강화되었습니다.
- SPA 클라이언트는 `/auth/csrf-token` 으로 발급받은 CSRF 토큰을 `X-CSRF-TOKEN` 헤더에 포함해 안전하지 않은 메서드(POST, PUT, DELETE)에 함께 전송해야 합니다.

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
| **500** | 서버 에러 | 서버 내부 문제 |

---

## API 엔드포인트

### 인증 API (`/auth`, `/oauth2`)

| 기능 | EndPoint | Method | 비고 |
|------|----------|--------|------|
| 회원가입 | `/auth/signup` | `POST` | 이메일 형식 필수 |
| 일반 로그인 | `/auth/login` | `POST` | JWT 토큰 반환 |
| 토큰 갱신 | `/auth/refresh` | `POST` | Refresh Token 로테이션 |
| 로그아웃 | `/auth/logout` | `POST` | 토큰 블랙리스트 등록 |
| 회원 탈퇴 | `/auth/account` | `DELETE` | 모든 데이터 삭제 |
| CSRF 토큰 발급 | `/auth/csrf-token` | `GET` | 60분 TTL |
| CSRF 토큰 상태 | `/auth/csrf-status` | `GET` | - |
| 로그인 이력 조회 | `/auth/login-history` | `GET` | 페이지네이션 |
| 활성 세션 조회 | `/auth/sessions` | `GET` | 현재 세션 표시 |
| 세션 강제 종료 | `/auth/sessions/{session-id}` | `DELETE` | 다른 기기 로그아웃 |
| 모든 세션 종료 | `/auth/sessions` | `DELETE` | 전체 로그아웃 |
| 카카오 로그인 시작 | `/oauth2/auth/kakao` | `GET` | 카카오로 리다이렉션 |
| 카카오 로그인 콜백 | `/oauth2/auth/kakao/callback` | `GET` | JWT 발급 |

#### 인증 API 데이터 구조

##### 회원가입 요청
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

##### 로그인 응답
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiJ9...",
  "refresh_token": "eyJhbGciOiJIUzI1NiJ9...",
  "expires_in": 900,
  "token_type": "Bearer"
}
```

##### 로그인 이력 응답
```json
{
  "histories": [
    {
      "loginHistoryId": 1,
      "loginTime": "2025-12-23T09:00:00",
      "ipAddress": "123.45.67.89",
      "userAgent": "Mozilla/5.0...",
      "deviceType": "MOBILE",
      "loginStatus": "SUCCESS",
      "isActive": true,
      "logoutTime": null
    }
  ],
  "totalPages": 5,
  "totalElements": 100,
  "currentPage": 0,
  "pageSize": 20,
  "hasNext": true,
  "hasPrevious": false
}
```

##### 활성 세션 응답
```json
{
  "totalActiveSessions": 3,
  "sessions": [
    {
      "loginHistoryId": 1,
      "sessionId": "uuid-session-id",
      "loginTime": "2025-12-23T09:00:00",
      "ipAddress": "123.45.67.89",
      "deviceType": "MOBILE",
      "isCurrentSession": true
    }
  ]
}
```

---

### 회원 프로필 API (`/member`)

| 기능 | EndPoint | Method | 비고 |
|------|----------|--------|------|
| 내 정보 조회 | `/member/profile` | `GET` | JWT 인증 필수 |
| 프로필 수정 | `/member/profile` | `PUT` | 변경할 필드만 전송 |
| 비밀번호 변경 | `/member/password` | `PUT` | 현재 비밀번호 확인 필요 |

#### 회원 프로필 데이터 구조

##### 프로필 응답
```json
{
  "memberId": 1,
  "loginId": "user@example.com",
  "name": "홍길동",
  "nickName": "길동이",
  "phoneNumber": "01012345678",
  "birth": "1990-01-01",
  "createdAt": "2025-01-01T12:00:00"
}
```

##### 비밀번호 변경 요청
```json
{
  "currentPassword": "currentPassword123",
  "newPassword": "newPassword456!",
  "confirmPassword": "newPassword456!"
}
```

---

### 복약 스케줄 API (`/medication-schedule`)

| 기능 | EndPoint | Method | 비고 |
|------|----------|--------|------|
| 목록 조회 | `/medication-schedule/list/{member-id}` | `GET` | 페이지네이션 |
| 상세 조회 | `/medication-schedule/{id}` | `GET` | - |
| 등록 | `/medication-schedule` | `POST` | multipart/form-data, 이미지 최대 5MB |
| 수정 | `/medication-schedule/{id}` | `PUT` | - |
| 삭제 | `/medication-schedule/{id}` | `DELETE` | 204 No Content |
| 검색 | `/medication-schedule/search/{member-id}` | `GET` | 약 이름 검색 |
| 필터링 | `/medication-schedule/filter/{member-id}` | `GET` | 활성 상태 필터 |
| 이미지 조회 | `/medication-schedule/{id}/image` | `GET` | 없으면 404 |
| 이미지 수정 | `/medication-schedule/{id}/image` | `PUT` | 이미지 교체 |
| 이미지 삭제 | `/medication-schedule/{id}/image` | `DELETE` | 이미지만 삭제 |
| 알림 시간 목록 | `/medication-schedule/{id}/times` | `GET` | 시간순 정렬 |
| 알림 시간 추가 | `/medication-schedule/{id}/times` | `POST` | - |

#### 복약 스케줄 데이터 구조

##### 등록 요청 (multipart/form-data)
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
formData.append('image', imageFile); // 선택
```

##### 목록 응답
```json
{
  "schedules": [
    {
      "medicationScheduleId": 101,
      "memberId": 1,
      "name": "혈압약",
      "hospitalName": "서울병원",
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

---

### 복약 알림 시간 API (`/medication-time`)

| 기능 | EndPoint | Method | 비고 |
|------|----------|--------|------|
| 수정 | `/medication-time/{time-id}` | `PUT` | - |
| 삭제 | `/medication-time/{time-id}` | `DELETE` | 204 No Content |

##### 알림 시간 생성/수정 요청
```json
{
  "takeTime": "09:00"
}
```

##### 알림 시간 응답
```json
{
  "medicationTimeId": 1,
  "medicationScheduleId": 101,
  "takeTime": "09:00:00"
}
```

---

### 복약 기록 API (`/medication-record`)

| 기능 | EndPoint | Method | 비고 |
|------|----------|--------|------|
| 목록 조회 | `/medication-record/list/{schedule-id}` | `GET` | 특정 스케줄의 기록 |
| 상세 조회 | `/medication-record/{id}` | `GET` | - |
| 생성 | `/medication-record` | `POST` | - |
| 수정 | `/medication-record/{id}` | `PUT` | - |
| 삭제 | `/medication-record/{id}` | `DELETE` | 204 No Content |
| 일괄 생성 | `/medication-record/batch` | `POST` | 최대 100개 |
| 일괄 수정 | `/medication-record/batch` | `PUT` | 최대 100개 |

#### 복약 기록 데이터 구조

##### 생성/수정 요청
```json
{
  "medicationScheduleId": 101,
  "medicationTimeId": 2,
  "isTakeMedication": true,
  "realTakeTime": "2025-08-11T20:00:15"
}
```

##### 배치 생성 요청
```json
{
  "records": [
    {
      "medicationScheduleId": 1,
      "medicationTimeId": 1,
      "isTakeMedication": true,
      "realTakeTime": "2025-12-23T09:00:00"
    }
  ]
}
```

##### 배치 응답
```json
{
  "totalRequested": 5,
  "successCount": 5,
  "failedCount": 0,
  "successRecords": [...],
  "failedItems": []
}
```

---

### 복약 통계 API (`/medication-stats`)

| 기능 | EndPoint | Method | 비고 |
|------|----------|--------|------|
| 이행률 조회 | `/medication-stats/{member-id}/adherence` | `GET` | 기간별 통계 |
| 현황 요약 | `/medication-stats/{member-id}/summary` | `GET` | 오늘/주간/월간 |

#### 복약 통계 데이터 구조

##### 이행률 응답
```json
{
  "startDate": "2025-12-01",
  "endDate": "2025-12-31",
  "totalScheduled": 90,
  "totalTaken": 82,
  "adherenceRate": 91.1,
  "missedDates": ["2025-12-15", "2025-12-22"],
  "dailyStats": [...]
}
```

##### 현황 요약 응답
```json
{
  "totalActiveSchedules": 5,
  "todayScheduled": 12,
  "todayTaken": 8,
  "todayAdherenceRate": 66.7,
  "weeklyAdherenceRate": 85.5,
  "monthlyAdherenceRate": 91.2,
  "scheduleSummaries": [...]
}
```

---

### 복약 스케줄 그룹 API (`/medication-group`)

| 기능 | EndPoint | Method | 비고 |
|------|----------|--------|------|
| 그룹 목록 조회 | `/medication-group` | `GET` | JWT 인증 필수 |
| 그룹 상세 조회 | `/medication-group/{group-id}` | `GET` | - |
| 그룹 생성 | `/medication-group` | `POST` | - |
| 그룹 수정 | `/medication-group/{group-id}` | `PUT` | - |
| 그룹 삭제 | `/medication-group/{group-id}` | `DELETE` | 204 No Content |

#### 그룹 데이터 구조

##### 그룹 생성 요청
```json
{
  "name": "아침약",
  "description": "아침에 먹는 약들",
  "color": "#FF5733"
}
```

##### 그룹 응답
```json
{
  "groupId": 1,
  "memberId": 1,
  "name": "아침약",
  "description": "아침에 먹는 약들",
  "color": "#FF5733",
  "createdAt": "2025-12-23T10:00:00"
}
```

---

### 부작용 API (`/side-effect`)

| 기능 | EndPoint | Method | 비고 |
|------|----------|--------|------|
| 최근 부작용 조회 | `/side-effect/recent` | `GET` | 최근 5개, Redis 캐시 |
| 부작용 기록 생성 | `/side-effect/record` | `POST` | 201 Created |
| 목록 조회 | `/side-effect/list/{member-id}` | `GET` | 페이지네이션 |
| 상세 조회 | `/side-effect/{record-id}` | `GET` | - |
| 삭제 | `/side-effect/{record-id}` | `DELETE` | 204 No Content |

#### 부작용 데이터 구조

##### 기록 생성 요청
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

##### 기록 상세 응답
```json
{
  "sideEffectRecordId": 1,
  "memberId": 1,
  "createdAt": "2025-08-29T10:30:00",
  "sideEffects": [
    { "sideEffectId": 1, "name": "두통", "degree": 3 },
    { "sideEffectId": 3, "name": "메스꺼움", "degree": 2 }
  ]
}
```

---

### 마음 일기 API (`/mood-diary`)

| 기능 | EndPoint | Method | 비고 |
|------|----------|--------|------|
| 생성 | `/mood-diary` | `POST` | 하루 1회 제한 |
| 상세 조회 | `/mood-diary/{id}` | `GET` | 소유자만 접근 |
| 목록 조회 | `/mood-diary/list/{member-id}` | `GET` | 최신순, 페이지네이션 |
| 날짜별 조회 | `/mood-diary/date/{member-id}` | `GET` | yyyy-MM-dd |
| 수정 | `/mood-diary/{id}` | `PUT` | 날짜 변경 불가 |
| 삭제 | `/mood-diary/{id}` | `DELETE` | 소유자만 삭제 |
| 통계 조회 | `/mood-diary/stats/{member-id}` | `GET` | 기분 분포, 연속 작성일 |
| 캘린더 조회 | `/mood-diary/calendar/{member-id}` | `GET` | 월별 작성 현황 |
| 검색 | `/mood-diary/search/{member-id}` | `GET` | 내용 검색 |

#### 마음 일기 데이터 구조

##### 생성 요청 (템플릿 형식)
```json
{
  "memberId": 1,
  "diaryDate": "2025-12-01",
  "moodType": "HAPPY",
  "diaryType": "TEMPLATE",
  "templateAnswer1": "오늘 친구와 만났습니다.",
  "templateAnswer2": "행복한 감정을 느꼈어요.",
  "templateAnswer3": "친구가 잘 들어줘서요.",
  "templateAnswer4": "자주 이런 날이 있었으면."
}
```

##### 생성 요청 (자유 형식)
```json
{
  "memberId": 1,
  "diaryDate": "2025-12-01",
  "moodType": "PEACEFUL",
  "diaryType": "FREE_FORM",
  "freeContent": "오늘은 전반적으로 평온한 하루였다..."
}
```

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

##### 통계 응답
```json
{
  "startDate": "2025-12-01",
  "endDate": "2025-12-31",
  "totalWrittenDays": 25,
  "totalDays": 31,
  "writingRate": 80.6,
  "consecutiveDays": 12,
  "moodDistribution": {
    "HAPPY": 8,
    "PEACEFUL": 5,
    "EXCITED": 4
  },
  "mostFrequentMood": "HAPPY"
}
```

##### 캘린더 응답
```json
{
  "year": 2025,
  "month": 12,
  "totalDays": 31,
  "writtenDays": 25,
  "writingRate": 80.6,
  "records": [
    {
      "day": 1,
      "date": "2025-12-01",
      "hasEntry": true,
      "moodType": "HAPPY",
      "moodDiaryId": 123
    }
  ]
}
```

---

### 데이터 내보내기 API (`/export`)

| 기능 | EndPoint | Method | 비고 |
|------|----------|--------|------|
| 내 데이터 내보내기 | `/export/my-data` | `GET` | JSON 객체 |
| JSON 파일 다운로드 | `/export/my-data/download` | `GET` | 파일 첨부 |
| 복약 기록 CSV | `/export/medication-records` | `GET` | 의사 상담용 |

#### 내보내기 응답
```json
{
  "meta": {
    "exportedAt": "2025-12-23T15:30:00",
    "format": "JSON",
    "version": "1.0"
  },
  "member": {
    "memberId": 1,
    "name": "홍길동",
    "nickName": "길동이"
  },
  "medicationSchedules": [...],
  "medicationRecords": [...],
  "moodDiaries": [...],
  "sideEffectRecords": [...]
}
```

---

### 알림 설정 API (`/notification`)

| 기능 | EndPoint | Method | 비고 |
|------|----------|--------|------|
| 알림 설정 조회 | `/notification/settings` | `GET` | 없으면 기본값 생성 |
| 알림 설정 수정 | `/notification/settings` | `PUT` | 변경할 필드만 전송 |
| FCM 토큰 등록 | `/notification/token` | `POST` | 디바이스 푸시 토큰 |
| 등록 디바이스 목록 | `/notification/devices` | `GET` | 모든 등록 디바이스 |
| 디바이스 토큰 삭제 | `/notification/devices/{tokenId}` | `DELETE` | 특정 디바이스 삭제 |
| 현재 토큰 비활성화 | `/notification/token` | `DELETE` | 로그아웃 시 호출 |

#### 알림 설정 데이터 구조

##### 알림 설정 응답
```json
{
  "notificationSettingsId": 1,
  "memberId": 1,
  "pushEnabled": true,
  "medicationReminderEnabled": true,
  "medicationReminderMinutesBefore": 10,
  "diaryReminderEnabled": false,
  "diaryReminderTime": "21:00:00",
  "quietHoursEnabled": false,
  "quietHoursStart": "23:00:00",
  "quietHoursEnd": "07:00:00",
  "createdAt": "2025-12-25T10:00:00",
  "updatedAt": null
}
```

##### 알림 설정 수정 요청
```json
{
  "pushEnabled": true,
  "medicationReminderEnabled": true,
  "medicationReminderMinutesBefore": 15,
  "diaryReminderEnabled": true,
  "diaryReminderTime": "20:00",
  "quietHoursEnabled": true,
  "quietHoursStart": "22:00",
  "quietHoursEnd": "08:00"
}
```

##### FCM 토큰 등록 요청
```json
{
  "token": "fcm-token-string...",
  "deviceType": "ANDROID",
  "deviceName": "Galaxy S24"
}
```

**디바이스 타입 (DeviceType):**
| 값 | 의미 |
|----|------|
| `ANDROID` | 안드로이드 기기 |
| `IOS` | iOS 기기 |
| `WEB` | 웹 브라우저 |

##### 등록 디바이스 목록 응답
```json
{
  "memberId": 1,
  "totalDevices": 2,
  "activeDevices": 2,
  "devices": [
    {
      "fcmDeviceTokenId": 1,
      "tokenPrefix": "fcm-token-12345678...",
      "deviceType": "ANDROID",
      "deviceName": "Galaxy S24",
      "isActive": true,
      "lastUsedAt": "2025-12-25T09:00:00",
      "createdAt": "2025-12-20T10:00:00"
    }
  ]
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

### 회원 관련
| 에러 코드 | 메시지 | HTTP |
|-----------|--------|------|
| `MEMBER_NOT_FOUND` | 회원을 찾을 수 없습니다. | 404 |
| `DUPLICATE_MEMBER` | 이미 존재하는 회원입니다. | 409 |
| `INVALID_CURRENT_PASSWORD` | 현재 비밀번호가 일치하지 않습니다. | 400 |
| `PASSWORD_CONFIRM_MISMATCH` | 비밀번호와 확인이 일치하지 않습니다. | 400 |

### 복약 관련
| 에러 코드 | 메시지 | HTTP |
|-----------|--------|------|
| `SCHEDULE_NOT_FOUND` | 복약 스케줄을 찾을 수 없습니다. | 404 |
| `RECORD_NOT_FOUND` | 복약 기록을 찾을 수 없습니다. | 404 |
| `TIME_NOT_FOUND` | 복약 시간 정보를 찾을 수 없습니다. | 404 |

### 마음 일기 관련
| 에러 코드 | 메시지 | HTTP |
|-----------|--------|------|
| `MOOD_DIARY_NOT_FOUND` | 마음 일기를 찾을 수 없습니다. | 404 |
| `DIARY_ALREADY_EXISTS` | 해당 날짜에 이미 일기가 존재합니다. | 409 |

### 파일 관련
| 에러 코드 | 메시지 | HTTP |
|-----------|--------|------|
| `FILE_SAVE_FAIL` | 파일 저장에 실패했습니다. | 500 |
| `FILE_SIZE_EXCEEDED` | 파일 크기가 제한을 초과했습니다. | 413 |
| `INVALID_FILE_TYPE` | 지원하지 않는 파일 형식입니다. | 400 |

### 알림 관련
| 에러 코드 | 메시지 | HTTP |
|-----------|--------|------|
| `NOTIFICATION_SETTINGS_NOT_FOUND` | 알림 설정을 찾을 수 없습니다. | 404 |
| `FCM_TOKEN_NOT_FOUND` | FCM 토큰을 찾을 수 없습니다. | 404 |
| `FCM_SEND_FAILED` | 푸시 알림 전송에 실패했습니다. | 500 |
| `INVALID_DEVICE_TYPE` | 유효하지 않은 디바이스 타입입니다. | 400 |

---

> 📖 더 자세한 정보(인프라 구성, DB 스키마, 변경 이력 등)는 [API 참고 문서](../internal/API-reference.md)를 참조하세요.
