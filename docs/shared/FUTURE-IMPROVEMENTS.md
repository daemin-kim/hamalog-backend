# Hamalog 향후 개선사항

> 📅 최종 업데이트: 2026년 1월 13일  
> 📌 미구현 기능 및 향후 로드맵

---

## 📊 현재 상태

Hamalog는 **Production Ready** 수준으로 구현되어 있습니다.

### ✅ 주요 완료 기능
- 인증/인가 (JWT + Refresh Token, OAuth2 카카오)
- 보안 (CSRF, Rate Limiting, 리소스 소유권 검증, **Brute Force IP 차단**)
- 푸시 알림 시스템 (FCM + Redis Stream 메시지 큐)
- 복약 관리 (스케줄, 기록, 통계, 그룹, 이미지)
- 마음 일기 (CRUD, 통계, 캘린더, 검색)
- 부작용 기록 (CRUD, **복용 후 리마인더 알림**)
- 데이터 내보내기 (JSON/CSV)
- 로그인 이력/세션 관리

---

## 🚀 미구현 기능 (향후 개선 예정)

### 1. Soft Delete 패턴 도입 (우선순위: ⭐⭐ 중간)

**필요 작업:**
- `BaseEntity`에 `deletedAt` 필드 추가
- JPA `@Where` 어노테이션 활용
- 휴지통 API (복구/영구삭제)
- 30일 후 자동 영구 삭제 스케줄러

**대상 엔티티:**
- `MoodDiary`, `MedicationSchedule`, `SideEffectRecord`

---

### 2. 2단계 인증 (우선순위: ⭐ 낮음)

**필요 작업:**
- TOTP (Google Authenticator)
- SMS 인증 (선택)

---

### 3. API 버전 관리 재도입 (우선순위: ⭐⭐ 중간)

**필요 작업:**
- ADR 문서: URI vs Header 기반 버전 관리
- `/v1/`, `/v2/` 경로 프리픽스 재도입
- Breaking Change 마이그레이션 가이드

---

## 📈 운영 고도화

### 4. Grafana 대시보드 구축

**필요 작업:**
- 대시보드 JSON 템플릿 (`docs/monitoring/grafana-dashboard.json`)
- 주요 메트릭: API 응답 시간, Rate Limiting, 캐시 히트율, 메시지 큐

---

### 5. E2E 테스트 추가

**필요 작업:**
- Playwright 또는 TestContainers 기반
- 주요 시나리오: 회원가입 → 로그인 → 복약 스케줄 → 복용 기록

---

### 6. 부하 테스트 (k6)

**필요 작업:**
- k6 스크립트 작성
- 목표 TPS 정의 및 병목 지점 식별

---

## 💡 추가 권장사항

### 문서화
- [ ] Swagger UI에 예제 요청/응답 추가

### 자동화
- [x] ~~CHANGELOG 자동 생성 (git-cliff 연동)~~ ✅ 완료 (2026-01-13)

### 운영
- [ ] API 사용량 대시보드
- [ ] 자동 스케일링 설정

---

> 📝 이 문서는 미구현 기능만 관리합니다. 완료된 기능은 CHANGELOG.md를 참조하세요.
