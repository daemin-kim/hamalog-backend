# Changelog

Hamalog 프로젝트의 모든 주요 변경사항을 기록합니다.

이 형식은 [Keep a Changelog](https://keepachangelog.com/ko/1.0.0/)를 기반으로 하며,
[Semantic Versioning](https://semver.org/lang/ko/)을 따릅니다.

## [Unreleased]

### 추가됨 (Added)
- 바이브 코딩 가이드 문서 (`docs/VIBE-CODING-GUIDE.md`)
- AI 컨텍스트 파일 (`.cursorrules`, `.github/copilot-instructions.md`)
- ADR 문서 4개 (JWT+CSRF, AOP, Domain Event, 템플릿)
- 코딩 컨벤션 문서 (`docs/CODING-CONVENTIONS.md`)

---

## [1.2.0] - 2025-12-23

### 추가됨 (Added)

#### 복약 스케줄 그룹 관리 API
- `GET /medication-group` - 그룹 목록 조회
- `GET /medication-group/{group-id}` - 그룹 상세 조회
- `POST /medication-group` - 그룹 생성
- `PUT /medication-group/{group-id}` - 그룹 수정
- `DELETE /medication-group/{group-id}` - 그룹 삭제

#### 배치 작업 API
- `POST /medication-record/batch` - 복약 기록 일괄 생성
- `PUT /medication-record/batch` - 복약 기록 일괄 수정

#### 데이터 내보내기 API
- `GET /export/my-data` - 내 데이터 내보내기 (JSON)
- `GET /export/my-data/download` - JSON 파일 다운로드
- `GET /export/medication-records` - 복약 기록 CSV 내보내기

#### 로그인 이력 및 세션 관리
- `GET /auth/login-history` - 로그인 이력 조회
- `GET /auth/sessions` - 활성 세션 조회
- `DELETE /auth/sessions/{session-id}` - 세션 강제 종료
- `DELETE /auth/sessions` - 모든 세션 종료

#### 복약 스케줄 이미지 관리
- `GET /medication-schedule/{id}/image` - 이미지 조회
- `PUT /medication-schedule/{id}/image` - 이미지 수정
- `DELETE /medication-schedule/{id}/image` - 이미지 삭제

#### 정렬/필터링 옵션
- `GET /medication-schedule/filter/{member-id}?active=true` - 활성 상태 필터링

### 변경됨 (Changed)
- `MedicationScheduleGroup` 엔티티에 `description`, `color` 필드 추가
- `MedicationSchedule` 엔티티에 `imagePath`, `isActive` 필드 추가

### DB 마이그레이션
- `V2__Add_new_features.sql` 추가
  - `login_history` 테이블 생성
  - `notification_settings` 테이블 생성
  - `medication_schedule` 테이블에 `image_path`, `is_active` 컬럼 추가
  - `medication_schedule_group` 테이블에 `description`, `color` 컬럼 추가

---

## [1.1.0] - 2025-12-22

### 추가됨 (Added)

#### 마음 일기 API 확장
- `PUT /mood-diary/{mood-diary-id}` - 마음 일기 수정
- `GET /mood-diary/stats/{member-id}` - 기분 통계 조회
- `GET /mood-diary/calendar/{member-id}` - 월간 캘린더 조회
- `GET /mood-diary/search/{member-id}` - 일기 검색

#### 부작용 API 확장
- `GET /side-effect/{record-id}` - 부작용 상세 조회
- `GET /side-effect/list/{member-id}` - 부작용 목록 조회
- `DELETE /side-effect/{record-id}` - 부작용 삭제

#### 복약 알림 시간 CRUD
- `GET /medication-schedule/{id}/times` - 알림 시간 목록
- `POST /medication-schedule/{id}/times` - 알림 시간 추가
- `PUT /medication-time/{time-id}` - 알림 시간 수정
- `DELETE /medication-time/{time-id}` - 알림 시간 삭제

#### 사용자 프로필 API
- `GET /member/profile` - 내 정보 조회
- `PUT /member/profile` - 프로필 수정
- `PUT /member/password` - 비밀번호 변경

#### 복약 통계 API
- `GET /medication-stats/{member-id}/adherence` - 복약 이행률
- `GET /medication-stats/{member-id}/summary` - 복약 현황 요약

#### 검색 기능
- `GET /medication-schedule/search/{member-id}` - 약 이름 검색

### 수정됨 (Fixed)
- 한글 메시지 파일 UTF-8 인코딩 수정

---

## [1.0.0] - 2025-12-01

### 추가됨 (Added)

#### 인증/인가
- JWT + CSRF 이중 보호
- Refresh Token Rotation
- OAuth2 카카오 로그인
- Rate Limiting (Redis 기반)

#### 복약 관리
- 복약 스케줄 CRUD
- 복약 기록 CRUD
- 복약 시간 관리
- 복약 스케줄 그룹 관리

#### 마음 일기
- 마음 일기 CRUD
- 하루 1회 작성 제한
- 기분 타입 (HAPPY, NEUTRAL, SAD, ANGRY, ANXIOUS)

#### 부작용 관리
- 부작용 기록 생성
- 부작용 타입별 조회

#### 보안
- AOP 기반 리소스 소유권 검증 (`@RequireResourceOwnership`)
- 민감 정보 AES 암호화 (전화번호, 생년월일)
- CSRF 토큰 검증

#### 모니터링
- Prometheus 메트릭
- 구조화된 로깅 (Logstash JSON)
- 성능 모니터링 AOP

#### 코드 품질
- Spotless 코드 포맷팅
- JaCoCo 테스트 커버리지
- ArchUnit 아키텍처 테스트
- 1,400+ 테스트 케이스

---

## 버전 관리 정책

### 버전 번호 형식: `MAJOR.MINOR.PATCH`

| 변경 유형 | 버전 증가 | 설명 |
|-----------|----------|------|
| Breaking Change | MAJOR | API 호환성 깨지는 변경 |
| 새 기능 추가 | MINOR | 이전 버전과 호환되는 기능 추가 |
| 버그 수정 | PATCH | 이전 버전과 호환되는 버그 수정 |

### 커밋 메시지 형식 (Conventional Commits)

```
<type>(<scope>): <subject>

Types:
- feat: 새로운 기능
- fix: 버그 수정
- docs: 문서 변경
- style: 코드 스타일 (포맷팅)
- refactor: 리팩토링
- perf: 성능 개선
- test: 테스트 추가/수정
- chore: 빌드, 설정 변경
```

---

[Unreleased]: https://github.com/daemin-kim/Hamalog/compare/v1.2.0...HEAD
[1.2.0]: https://github.com/daemin-kim/Hamalog/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/daemin-kim/Hamalog/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/daemin-kim/Hamalog/releases/tag/v1.0.0

