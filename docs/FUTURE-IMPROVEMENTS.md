# Hamalog 향후 개선사항 분석 보고서

> 📅 분석일: 2025년 12월 22일  
> 📌 API 명세서 및 현재 코드 구조 기반 분석
> ✅ 2025/12/22 대규모 업데이트 완료 - 8개 기능 구현

---

## 📊 현재 상태 요약

### ✅ 이미 잘 구현된 부분
- **인증/인가**: JWT + Refresh Token 로테이션, OAuth2 카카오 로그인
- **보안**: CSRF 토큰, Rate Limiting, 리소스 소유권 검증 (AOP)
- **에러 처리**: 표준화된 ErrorResponse + traceId 추적
- **모니터링**: Prometheus 메트릭, 구조화된 로깅
- **코드 품질**: Spotless, JaCoCo, ArchUnit, 1400+ 테스트 케이스

### ✅ 2025/12/22 구현 완료 목록
| # | 기능 | 상태 |
|---|------|------|
| 1 | 마음 일기 수정 API | ✅ |
| 2 | 부작용 API 확장 (목록/상세/삭제) | ✅ |
| 3 | 복약 알림 시간 CRUD API | ✅ |
| 4 | 사용자 프로필 API | ✅ |
| 5 | 복약 통계 API | ✅ |
| 6 | 마음 일기 통계/캘린더 API | ✅ |
| 7 | 검색 기능 (일기/약 이름) | ✅ |
| 8 | 한글 메시지 파일 UTF-8 수정 | ✅ |

---

## 🚀 기능 개선 사항

### 1. 마음 일기 수정 기능 부재 (우선순위: ⭐⭐⭐ 높음) ✅ 구현 완료

**현재**: 생성(POST), 조회(GET), 삭제(DELETE)만 존재  
**구현됨**: 수정(PUT) 기능 추가

```
EndPoint: PUT /mood-diary/{mood-diary-id}
Request: MoodDiaryUpdateRequest
Response: MoodDiaryResponse
```

**구현된 파일**:
- `MoodDiaryUpdateRequest.java` (신규)
- `MoodDiary.java` - update 메서드 추가
- `MoodDiaryService.java` - updateMoodDiary() 추가
- `MoodDiaryController.java` - PUT 엔드포인트 추가
- `MoodDiaryServiceTest.java` - 테스트 케이스 추가

---

### 2. 부작용 API 기능 확장 (우선순위: ⭐⭐⭐ 높음) ✅ 구현 완료

**현재**: 최근 5개 조회 + 기록 생성만 존재  
**구현됨**:

| 기능 | EndPoint | 설명 |
|------|----------|------|
| 부작용 상세 조회 | `GET /side-effect/{record-id}` | 특정 기록 상세 보기 |
| 부작용 목록 조회 | `GET /side-effect/list/{member-id}` | 페이지네이션 포함 |
| 부작용 삭제 | `DELETE /side-effect/{record-id}` | 기록 삭제 |

**구현된 파일**:
- `SideEffectRecordResponse.java` (신규)
- `SideEffectRecordListResponse.java` (신규)
- `SideEffectRecordRepository.java` - 조회 메서드 추가
- `SideEffectSideEffectRecordRepository.java` - 조회/삭제 메서드 추가
- `SideEffectService.java` - 목록/상세/삭제 메서드 추가
- `SideEffectController.java` - 3개 엔드포인트 추가

---

### 3. 복약 스케줄 이미지 관리 API (우선순위: ⭐⭐ 중간)

**현재**: 생성 시 이미지 업로드 가능하지만 수정/삭제/조회 API 없음  
**필요**:

| 기능 | EndPoint | 설명 |
|------|----------|------|
| 이미지 조회 | `GET /medication-schedule/{id}/image` | 이미지 다운로드 |
| 이미지 수정 | `PUT /medication-schedule/{id}/image` | 이미지 교체 |
| 이미지 삭제 | `DELETE /medication-schedule/{id}/image` | 이미지만 삭제 |

---

### 4. 복약 알림 시간(MedicationTime) 관리 API (우선순위: ⭐⭐⭐ 높음) ✅ 구현 완료

**현재**: DB 스키마에 `medication_time` 테이블 존재하지만 API 없음  
**구현됨**:

| 기능 | EndPoint | 설명 |
|------|----------|------|
| 알림 시간 목록 | `GET /medication-schedule/{id}/times` | 스케줄별 알림 시간 조회 |
| 알림 시간 추가 | `POST /medication-schedule/{id}/times` | 새 알림 시간 추가 |
| 알림 시간 수정 | `PUT /medication-time/{time-id}` | 시간 변경 |
| 알림 시간 삭제 | `DELETE /medication-time/{time-id}` | 알림 삭제 |

**구현된 파일**:
- `MedicationTimeResponse.java` (신규)
- `MedicationTimeCreateRequest.java` (신규)
- `MedicationTimeUpdateRequest.java` (신규)
- `MedicationTimeService.java` (신규)
- `MedicationTimeController.java` (신규)
- `MedicationTime.java` - 생성자, update 메서드 추가
- `MedicationTimeRepository.java` - 조회/삭제 메서드 추가

---

### 5. 사용자 프로필 관리 API (우선순위: ⭐⭐ 중간) ✅ 구현 완료

**현재**: 회원가입/탈퇴만 존재, 프로필 조회/수정 없음  
**구현됨**:

| 기능 | EndPoint | 설명 |
|------|----------|------|
| 내 정보 조회 | `GET /member/profile` | 현재 로그인 사용자 정보 |
| 프로필 수정 | `PUT /member/profile` | 닉네임, 전화번호 등 수정 |
| 비밀번호 변경 | `PUT /member/password` | 현재 비밀번호 확인 후 변경 |

**구현된 파일**:
- `MemberProfileResponse.java` (신규)
- `ProfileUpdateRequest.java` (신규)
- `PasswordChangeRequest.java` (신규)
- `MemberProfileService.java` (신규)
- `MemberController.java` (신규)
- `Member.java` - updateProfile(), changePassword() 추가
- `ApiVersion.java` - MEMBER 경로 추가
- `ErrorCode.java` - 비밀번호 관련 에러 코드 추가

---

### 6. 복약 스케줄 그룹(Group) 관리 API (우선순위: ⭐ 낮음)

**현재**: DB에 `medication_schedule_group` 테이블 존재하지만 API 없음  
**필요**: 약 그룹화 기능 (예: "아침약", "저녁약", "혈압약" 등)

---

## 📈 통계 및 분석 기능

### 7. 복약 통계 API (우선순위: ⭐⭐⭐ 높음) ✅ 구현 완료

**구현된 엔드포인트**:

| 기능 | EndPoint | 설명 |
|------|----------|------|
| 복약 이행률 | `GET /medication-stats/{member-id}/adherence?startDate=&endDate=` | 기간별 복약 이행률 |
| 복약 현황 요약 | `GET /medication-stats/{member-id}/summary` | 오늘/주간/월간 이행률 + 스케줄별 통계 |

**구현된 파일**:
- `MedicationAdherenceResponse.java` (신규)
- `MedicationSummaryResponse.java` (신규)
- `MedicationStatsService.java` (신규)
- `MedicationStatsController.java` (신규)
- `MedicationRecordRepository.java` - 통계용 쿼리 메서드 추가

**응답 예시**:
```json
{
  "period": "2024-12",
  "totalScheduled": 90,
  "totalTaken": 82,
  "adherenceRate": 91.1,
  "missedDays": [15, 22, 28]
}
```

---

### 8. 마음 일기 통계 API (우선순위: ⭐⭐ 중간) ✅ 구현 완료

**구현된 엔드포인트**:

| 기능 | EndPoint | 설명 |
|------|----------|------|
| 기분 통계 | `GET /mood-diary/stats/{member-id}?startDate=&endDate=` | 기분 타입별 분포, 연속 작성일 |
| 월간 캘린더 | `GET /mood-diary/calendar/{member-id}?year=&month=` | 월별 일기 작성 현황 |

**구현된 파일**:
- `MoodDiaryStatsResponse.java` (신규)
- `MoodDiaryCalendarResponse.java` (신규)
- `MoodDiaryStatsService.java` (신규)
- `MoodDiaryController.java` - 통계/캘린더 엔드포인트 추가
- `MoodDiaryRepository.java` - 통계용 쿼리 메서드 추가

**응답 예시**:
```json
{
  "period": "2024-12",
  "moodDistribution": {
    "HAPPY": 8,
    "PEACEFUL": 5,
    "ANXIOUS": 3,
    "SAD": 2
  },
  "totalDays": 18,
  "writtenDays": 18,
  "consecutiveDays": 12
}
```

---

## 🔧 기술적 개선 사항

### 9. API 버전 관리 재도입 (우선순위: ⭐⭐ 중간)

**현재**: `/api/v1/` 프리픽스 제거됨  
**제안**: 향후 Breaking Change 시 버전 관리 필요
- URI 버전: `/v2/auth/login`
- 헤더 버전: `Accept: application/vnd.hamalog.v2+json`

---

### 10. 검색 기능 추가 (우선순위: ⭐⭐ 중간) ✅ 구현 완료

| 기능 | EndPoint | 설명 |
|------|----------|------|
| 일기 검색 | `GET /mood-diary/search/{member-id}?keyword=` | 키워드로 일기 내용 검색 |
| 약 이름 검색 | `GET /medication-schedule/search/{member-id}?keyword=` | 약 이름으로 검색 |

**구현된 파일**:
- `MoodDiaryRepository.java` - searchByKeyword() 추가
- `MedicationScheduleRepository.java` - searchByName() 추가
- `MoodDiaryService.java` - searchMoodDiaries() 추가
- `MedicationScheduleService.java` - searchMedicationSchedules() 추가
- 컨트롤러 검색 엔드포인트 추가

---

### 11. 정렬/필터링 옵션 확장 (우선순위: ⭐⭐ 중간)

**현재**: 기본 페이지네이션만 제공  
**제안**:
```
GET /mood-diary/list/{member-id}?page=0&size=20&sort=createdAt,desc&moodType=HAPPY
GET /medication-schedule/list/{member-id}?sort=name,asc&active=true
```

---

### 12. 소프트 삭제(Soft Delete) 도입 (우선순위: ⭐⭐ 중간)

**현재**: 물리적 삭제 (CASCADE)  
**제안**: 
- `deleted_at` 컬럼 추가
- 휴지통 기능 제공
- 30일 후 자동 영구 삭제

---

### 13. 배치 작업 API (우선순위: ⭐ 낮음)

| 기능 | EndPoint | 설명 |
|------|----------|------|
| 복약 기록 일괄 생성 | `POST /medication-record/batch` | 여러 기록 한 번에 생성 |
| 복약 기록 일괄 수정 | `PUT /medication-record/batch` | 여러 기록 상태 일괄 변경 |

---

## 📱 프론트엔드 연동 개선

### 14. 푸시 알림 관리 API (우선순위: ⭐⭐⭐ 높음)

**현재**: 알람 타입(SOUND/VIBE)만 존재  
**필요**:

| 기능 | EndPoint | 설명 |
|------|----------|------|
| FCM 토큰 등록 | `POST /notification/token` | 푸시 토큰 저장 |
| 알림 설정 조회 | `GET /notification/settings` | 알림 활성화 여부 |
| 알림 설정 수정 | `PUT /notification/settings` | 알림 on/off, 시간대 설정 |

---

### 15. 데이터 내보내기/가져오기 (우선순위: ⭐ 낮음)

| 기능 | EndPoint | 설명 |
|------|----------|------|
| 내 데이터 내보내기 | `GET /export/my-data` | CSV/JSON 형식 다운로드 |
| 복약 기록 내보내기 | `GET /export/medication-records` | 의사 상담용 데이터 |

---

## 🔐 보안 개선

### 16. 2단계 인증 (우선순위: ⭐ 낮음)

- TOTP (Google Authenticator)
- SMS 인증

---

### 17. 로그인 이력 관리 (우선순위: ⭐⭐ 중간)

| 기능 | EndPoint | 설명 |
|------|----------|------|
| 로그인 이력 조회 | `GET /auth/login-history` | 최근 로그인 기록 |
| 활성 세션 조회 | `GET /auth/sessions` | 현재 활성화된 세션 목록 |
| 세션 강제 종료 | `DELETE /auth/sessions/{session-id}` | 특정 세션 로그아웃 |

---

## 📋 우선순위 정리

| 순위 | 개선사항 | 난이도 | 예상 소요 |
|------|----------|--------|----------|
| 1 | 마음 일기 수정 기능 | 쉬움 | 1일 |
| 2 | 사용자 프로필 조회/수정 | 쉬움 | 1일 |
| 3 | 복약 알림 시간 관리 API | 중간 | 2일 |
| 4 | 부작용 API 확장 | 중간 | 2일 |
| 5 | 복약 통계 API | 중간 | 2일 |
| 6 | 푸시 알림 관리 | 중간 | 2-3일 |
| 7 | 마음 일기 통계/캘린더 | 쉬움 | 1일 |
| 8 | 검색/필터링 확장 | 쉬움 | 1일 |
| 9 | 복약 이미지 관리 | 쉬움 | 1일 |
| 10 | 로그인 이력 관리 | 중간 | 1-2일 |

---

## 💡 추가 권장사항

### 문서화
- [ ] Swagger UI에 예제 요청/응답 추가
- [ ] API 변경 이력(CHANGELOG) 관리
- [ ] 에러 코드 상세 가이드 문서

### 테스트
- [ ] E2E 테스트 (Cypress/Playwright)
- [ ] 부하 테스트 (k6/Gatling)
- [ ] 계약 테스트 (Spring Cloud Contract)

### 운영
- [ ] API 사용량 대시보드
- [ ] 알림 (Slack/Discord) 연동
- [ ] 자동 스케일링 설정

---

## 마무리

현재 Hamalog는 **MVP 수준 이상**으로 잘 구현되어 있습니다. 
특히 보안, 에러 처리, 모니터링 측면에서 실무 수준의 품질을 갖추고 있습니다.

위 개선사항 중 **1~7번**을 우선 구현하면 기능 완성도가 크게 높아질 것입니다.
특히 마음 일기 수정, 사용자 프로필 관리, 통계 API는 사용자 경험에 직접적인 영향을 미치는 핵심 기능입니다.

---

> 이 문서는 현재 API 명세서와 코드 구조를 분석하여 작성되었습니다.
> 프론트엔드 요구사항에 따라 우선순위가 변경될 수 있습니다.

