# Hamalog 프로젝트 개선 계획

> 📅 작성일: 2025년 12월 25일  
> 📌 docs 폴더 분석 및 현 프로젝트 상태 기반

---

## 📊 현재 상태 요약

### 프로젝트 성숙도: **90/100** ⭐⭐⭐⭐⭐

Hamalog는 이미 **Production Ready** 수준의 성숙도를 갖추고 있습니다.

| 영역 | 점수 | 현재 상태 |
|------|------|-----------|
| **코드 구조** | 95/100 | 레이어드 아키텍처, 명확한 패키지 분리 ✅ |
| **테스트 커버리지** | 90/100 | 1,400+ 테스트 케이스, ArchUnit ✅ |
| **문서화** | 95/100 | API 명세서, ADR 6개, 패턴 문서 4개 ✅ |
| **AI 컨텍스트** | 95/100 | `.cursorrules`, `copilot-instructions.md` ✅ |
| **자동화** | 95/100 | CI/CD, Spotless, JaCoCo, git-cliff ✅ |
| **선언적 패턴** | 95/100 | AOP 기반 + 문서화 완료 ✅ |

---

## ✅ 이번 구현 완료 (2025-12-25)

### 1. 푸시 알림 시스템 구현

**기능 목록:**
| 기능 | EndPoint | 상태 |
|------|----------|------|
| FCM 토큰 등록 | `POST /notification/token` | ✅ |
| 알림 설정 조회 | `GET /notification/settings` | ✅ |
| 알림 설정 수정 | `PUT /notification/settings` | ✅ |
| 등록 디바이스 목록 | `GET /notification/devices` | ✅ |
| 디바이스 토큰 삭제 | `DELETE /notification/devices/{tokenId}` | ✅ |
| 현재 토큰 비활성화 | `DELETE /notification/token` | ✅ |

**생성된 파일:**
```
src/main/java/com/Hamalog/
├── domain/notification/
│   ├── NotificationSettings.java       # 알림 설정 엔티티
│   ├── FcmDeviceToken.java             # FCM 디바이스 토큰 엔티티
│   ├── DeviceType.java                 # 디바이스 타입 열거형
│   └── NotificationType.java           # 알림 타입 열거형
├── repository/notification/
│   ├── NotificationSettingsRepository.java
│   └── FcmDeviceTokenRepository.java
├── dto/notification/
│   ├── request/
│   │   ├── FcmTokenRegisterRequest.java
│   │   └── NotificationSettingsUpdateRequest.java
│   └── response/
│       ├── NotificationSettingsResponse.java
│       ├── FcmDeviceTokenResponse.java
│       └── FcmDeviceTokenListResponse.java
├── service/notification/
│   └── NotificationSettingsService.java
└── controller/notification/
    └── NotificationController.java

src/main/resources/db/migration/
└── V3__Add_notification_tables.sql     # Flyway 마이그레이션

src/test/java/com/Hamalog/service/notification/
└── NotificationSettingsServiceTest.java  # 단위 테스트
```

---

## 🚀 향후 개선 계획

### Phase 1: 핵심 기능 완성 (우선순위 높음)

#### 1.1 FCM 실제 전송 기능 구현
**필요 작업:**
- Firebase 프로젝트 설정
- Firebase Admin SDK 의존성 추가
- `FcmPushService` 구현 (실제 알림 전송)
- 스케줄러 기반 복약 알림 발송

**예상 파일:**
```java
@Service
public class FcmPushService {
    public void sendMedicationReminder(Long memberId, String medicineName, LocalTime takeTime);
    public void sendDiaryReminder(Long memberId);
}
```

#### 1.2 Soft Delete 패턴 도입
**필요 작업:**
- `BaseEntity` 추상 클래스에 `deletedAt` 필드 추가
- JPA `@Where` 어노테이션 활용
- 휴지통 API 구현 (복구/영구삭제)
- 30일 후 자동 영구 삭제 스케줄러

**대상 엔티티:**
- `MoodDiary`
- `MedicationSchedule`
- `SideEffectRecord`

---

### Phase 2: 운영 고도화 (우선순위 중간)

#### 2.1 Grafana 대시보드 구축
**필요 작업:**
- Grafana 대시보드 JSON 템플릿 제공
- 주요 메트릭 정의:
  - API 응답 시간 (P50, P95, P99)
  - Rate Limiting 차단/허용 비율
  - Redis 캐시 히트율
  - 복약 이행률 통계

**예상 경로:** `docs/monitoring/grafana-dashboard.json`

#### 2.2 Slack/Discord 알림 연동
**필요 작업:**
- 에러 알림 웹훅 구현
- 주요 비즈니스 이벤트 알림 (신규 가입, 연속 복약 달성 등)

---

### Phase 3: 테스트 강화 (우선순위 중간)

#### 3.1 E2E 테스트 추가
**필요 작업:**
- Playwright 또는 TestContainers 기반 E2E 테스트
- 주요 시나리오:
  - 회원가입 → 로그인 → 복약 스케줄 생성 → 복용 기록
  - OAuth2 카카오 로그인 플로우

#### 3.2 부하 테스트 (k6)
**필요 작업:**
- k6 스크립트 작성
- 목표 TPS 정의 및 병목 지점 식별

---

### Phase 4: API 버전 관리 (우선순위 낮음)

#### 4.1 API 버전 관리 전략 수립
**필요 작업:**
- ADR 문서 작성: URI vs Header 기반 버전 관리
- `/v1/`, `/v2/` 경로 프리픽스 재도입
- Breaking Change 발생 시 마이그레이션 가이드

---

## 📋 체크리스트

### 즉시 가능한 작업
- [x] 푸시 알림 설정 API 구현
- [x] FCM 토큰 관리 API 구현
- [x] Flyway 마이그레이션 스크립트 작성
- [x] 단위 테스트 작성
- [x] API 명세서 업데이트
- [ ] FCM 실제 전송 (Firebase 설정 필요)

### Firebase 설정 후 가능한 작업
- [ ] Firebase Admin SDK 연동
- [ ] 복약 알림 스케줄러 구현
- [ ] 일기 작성 리마인더 구현

### 인프라 작업
- [ ] Grafana 대시보드 템플릿
- [ ] Slack 웹훅 연동
- [ ] k6 부하 테스트 스크립트

---

## 🔗 관련 문서

- [API 명세서](./shared/API-specification.md)
- [코딩 컨벤션](./internal/CODING-CONVENTIONS.md)
- [에러 처리 패턴](./internal/patterns/ERROR-HANDLING.md)
- [캐싱 패턴](./internal/patterns/CACHING-PATTERNS.md)
- [바이브 코딩 가이드](./ai/VIBE-CODING-GUIDE.md)

---

> 이 문서는 프로젝트 개선 진행에 따라 업데이트됩니다.

