# 🎯 Hamalog - 바이브 코딩 가이드

> **바이브 코딩(Vibe Coding)** 이란 AI와 자연어로 대화하며 코드를 생성하고 발전시키는 새로운 개발 패러다임입니다.
> 이 문서는 Hamalog 프로젝트를 AI와 효과적으로 협업할 수 있는 "바이브 코딩의 정수"로 만들기 위한 종합 가이드입니다.

---

## 📋 목차

1. [바이브 코딩 프로젝트란?](#1-바이브-코딩-프로젝트란)
2. [Hamalog 현재 상태](#2-hamalog-현재-상태)
3. [바이브 코딩 구현 로드맵](#3-바이브-코딩-구현-로드맵)
4. [AI 컨텍스트 최적화](#4-ai-컨텍스트-최적화)
5. [선언적 패턴 가이드](#5-선언적-패턴-가이드)
6. [문서화 전략](#6-문서화-전략)
7. [자동화 및 품질 관리](#7-자동화-및-품질-관리)
8. [실전 바이브 코딩 예제](#8-실전-바이브-코딩-예제)

---

## 1. 바이브 코딩 프로젝트란?

### 1.1 핵심 원칙

바이브 코딩의 정수가 되기 위해서는 다음 원칙을 따라야 합니다:

| 원칙 | 설명 | Hamalog 적용 |
|------|------|--------------|
| **🧠 AI 친화적 구조** | AI가 코드 컨텍스트를 빠르게 파악할 수 있는 명확한 구조 | 레이어드 아키텍처 + 일관된 네이밍 |
| **📝 자기 문서화 코드** | 코드 자체가 의도를 설명하는 선언적 패턴 | AOP 어노테이션 기반 횡단 관심사 |
| **🔄 일관된 패턴** | 반복되는 패턴으로 AI가 예측 가능한 코드 생성 | DTO/Entity/Service 표준 구조 |
| **📚 풍부한 컨텍스트 문서** | AI에게 프로젝트 맥락을 제공하는 메타 문서 | ADR, 코딩 컨벤션, API 명세서 |
| **✅ 자동화된 검증** | 생성된 코드의 품질을 자동으로 보장 | Spotless, JaCoCo, ArchUnit |

### 1.2 바이브 코딩의 장점

```
개발자 의도 (자연어)
       ↓
    AI 해석
       ↓
  코드 생성/수정
       ↓
  자동 검증 (테스트/린트)
       ↓
   즉시 피드백
       ↓
    반복 개선
```

---

## 2. Hamalog 현재 상태

### 2.1 프로젝트 성숙도 점수: **85/100** ⭐⭐⭐⭐

| 영역 | 점수 | 현재 상태 | 개선 필요 |
|------|------|-----------|-----------|
| **코드 구조** | 95/100 | 레이어드 아키텍처, 명확한 패키지 분리 | ✅ 완료 |
| **테스트 커버리지** | 90/100 | 1,400+ 테스트 케이스, ArchUnit | E2E 테스트 추가 |
| **문서화** | 95/100 | API 명세서, ADR 6개, 패턴 문서 4개 | ✅ 완료 |
| **AI 컨텍스트** | 95/100 | `.cursorrules`, `copilot-instructions.md` | ✅ 완료 |
| **자동화** | 95/100 | CI/CD, Spotless, JaCoCo, git-cliff | ✅ 완료 |
| **선언적 패턴** | 95/100 | AOP 기반 + 문서화 완료 | ✅ 완료 |

### 2.2 이미 갖춘 바이브 코딩 기반

#### ✅ 완료된 항목 (2025-12-24 업데이트)

| 항목 | 구현 내용 | 문서 |
|------|-----------|------|
| **선언적 보안** | `@RequireResourceOwnership` - 한 줄로 리소스 소유권 검증 | `docs/patterns/SECURITY-PATTERNS.md` |
| **선언적 로깅** | `@ServiceLogging`, `@ApiLogging` - AOP 기반 자동 로깅 | `docs/patterns/ANNOTATION-GUIDE.md` |
| **선언적 재시도** | `@Retryable` - 실패 시 자동 재시도 | `docs/patterns/ANNOTATION-GUIDE.md` |
| **선언적 캐싱** | `@Cacheable`, `@CacheEvict` - Redis 캐시 투명 적용 | `docs/patterns/CACHING-PATTERNS.md` |
| **표준화된 에러 처리** | `ErrorCode` Enum + `GlobalExceptionHandler` | `docs/patterns/ERROR-HANDLING.md` |
| **코드 스타일 강제** | Spotless - 자동 포맷팅 | `.github/workflows/ci.yml` |
| **아키텍처 강제** | ArchUnit - 레이어 의존성 규칙 테스트 | `src/test/java/.../architecture/` |
| **API 문서 자동화** | SpringDoc OpenAPI - Swagger UI 자동 생성 | `docs/API-specification.md` |
| **CHANGELOG 자동화** | git-cliff - Conventional Commits 기반 | `cliff.toml` |
| **CRUD 스캐폴딩** | 도메인 생성 스크립트 | `scripts/generate-crud.sh` |

---

## 3. 바이브 코딩 구현 로드맵

### ✅ Phase 1: AI 컨텍스트 기반 구축 (완료)

```
📁 프로젝트 루트
├── 📄 .cursorrules              ← AI IDE 컨텍스트 ✅
├── 📄 .github/
│   └── copilot-instructions.md  ← GitHub Copilot 컨텍스트 ✅
└── 📁 docs/
    ├── 📄 CODING-CONVENTIONS.md ← 코딩 컨벤션 문서 ✅
    └── 📁 adr/                  ← Architecture Decision Records ✅
        ├── 0001-adr-template.md
        ├── 0002-jwt-csrf-dual-protection.md
        ├── 0003-aop-cross-cutting-concerns.md
        ├── 0004-domain-event-pattern.md
        ├── 0005-redis-cache-strategy.md
        └── 0006-sensitive-data-encryption.md
```

### ✅ Phase 2: 선언적 패턴 문서화 (완료)

```
📁 docs/
└── 📁 patterns/
    ├── 📄 ANNOTATION-GUIDE.md      ← 커스텀 어노테이션 사용법 ✅
    ├── 📄 ERROR-HANDLING.md        ← 에러 처리 패턴 ✅
    ├── 📄 SECURITY-PATTERNS.md     ← 보안 패턴 ✅
    └── 📄 CACHING-PATTERNS.md      ← 캐싱 패턴 ✅
```

### ✅ Phase 3: 자동화 강화 (완료)

| 항목 | 도구 | 설명 |
|------|------|------|
| CHANGELOG 자동화 | `conventional-changelog` | Conventional Commits 기반 |
| 의존성 취약점 검사 | `OWASP Dependency Check` | 이미 설정됨, CI 연동 필요 |
| API 변경 감지 | OpenAPI diff | Breaking Change 자동 감지 |

### Phase 4: 코드 스캐폴딩 (2주)

```bash
# 새로운 도메인 CRUD 생성 예시
./scripts/generate-crud.sh --domain Notification --fields "title:String,content:String,isRead:Boolean"

# 생성되는 파일들:
# - domain/notification/Notification.java
# - dto/notification/request/NotificationCreateRequest.java
# - dto/notification/response/NotificationResponse.java
# - repository/notification/NotificationRepository.java
# - service/notification/NotificationService.java
# - controller/notification/NotificationController.java
# - test/service/notification/NotificationServiceTest.java
```

---

## 4. AI 컨텍스트 최적화

### 4.1 `.cursorrules` 파일 (권장)

```markdown
# Hamalog Project Context

## Project Overview
Hamalog는 Spring Boot 3.4.5 기반의 헬스케어 백엔드 시스템입니다.
복약 관리, 마음 일기, 부작용 기록 기능을 제공합니다.

## Tech Stack
- Java 21, Spring Boot 3.4.5, Spring Data JPA, MySQL 8.0
- Spring Security + JWT (jjwt 0.12.6)
- Redis 7 (캐시/세션), Flyway (DB 마이그레이션)
- Docker, Cloudflare Tunnel

## Architecture
- Controller → Service → Repository (Layered Architecture)
- AOP 기반 횡단 관심사 처리 (로깅, 보안, 캐싱, 재시도)
- Domain Event 기반 느슨한 결합

## Coding Conventions
1. DTO는 Java Record 사용
2. 엔티티는 JPA @Entity + Lombok 최소화 (생성자 직접 정의)
3. 서비스는 SRP 준수 (Auth → Registration/Authentication/Deletion 분리)
4. 모든 API는 @RequireResourceOwnership으로 리소스 소유권 검증
5. 에러는 ErrorCode Enum + BusinessException 사용

## Key Patterns
- @RequireResourceOwnership(resourceType, idParam) - 리소스 소유권 AOP 검증
- @Retryable(maxAttempts, delay) - 재시도 AOP
- ErrorCode.XXX.toException() - 표준 예외 생성

## File Locations
- Entities: src/main/java/com/Hamalog/domain/
- DTOs: src/main/java/com/Hamalog/dto/
- Services: src/main/java/com/Hamalog/service/
- Controllers: src/main/java/com/Hamalog/controller/
- Repositories: src/main/java/com/Hamalog/repository/
- Tests: src/test/java/com/Hamalog/

## Important Notes
- 한글 메시지는 messages_ko.properties에 정의
- Validation 메시지는 ValidationMessages.properties에 정의
- DB 스키마 변경은 Flyway 마이그레이션으로만 (db/migration/)
```

### 4.2 GitHub Copilot Instructions

`.github/copilot-instructions.md` 파일:

```markdown
# Hamalog Copilot Instructions

## 언어
- 코드 주석: 한글
- 변수/메서드명: 영어 camelCase
- 클래스명: 영어 PascalCase

## 코드 생성 규칙

### DTO 생성 시
```java
public record XxxRequest(
    @NotNull Long fieldName,
    @Size(max = 100) String description
) {}
```

### Service 메서드 생성 시
```java
@Transactional
public XxxResponse createXxx(XxxRequest request) {
    // 1. 유효성 검증
    // 2. 엔티티 생성
    // 3. 저장
    // 4. 응답 변환
    return XxxResponse.from(entity);
}
```

### 에러 처리 시
- throw ErrorCode.XXX_NOT_FOUND.toException();
- 새 에러 코드는 ErrorCode enum에 추가

### 테스트 생성 시
- @DisplayName 한글로 작성
- Given-When-Then 패턴 사용
- Nested 클래스로 테스트 그룹화
```

---

## 5. 선언적 패턴 가이드

### 5.1 리소스 소유권 검증 (@RequireResourceOwnership)

```java
// 사용 전: 수동 검증
@GetMapping("/{id}")
public ResponseEntity<?> get(@PathVariable Long id) {
    Resource resource = service.findById(id);
    if (!resource.getMemberId().equals(getCurrentUserId())) {
        throw ErrorCode.FORBIDDEN.toException();
    }
    return ResponseEntity.ok(resource);
}

// 사용 후: 선언적 검증
@GetMapping("/{id}")
@RequireResourceOwnership(resourceType = "MEDICATION_SCHEDULE", idParam = "id")
public ResponseEntity<?> get(@PathVariable Long id) {
    return ResponseEntity.ok(service.findById(id));
}
```

### 5.2 재시도 패턴 (@Retryable)

```java
@Retryable(maxAttempts = 3, delay = 1000, backoffMultiplier = 1.5)
@Transactional
public void updateWithRetry(UpdateRequest request) {
    // 낙관적 락 충돌 시 자동 재시도
}
```

### 5.3 캐싱 패턴

```java
@Cacheable(value = "memberProfile", key = "#memberId")
public MemberProfileResponse getProfile(Long memberId) { ... }

@CacheEvict(value = "memberProfile", key = "#memberId")
@Transactional
public void updateProfile(Long memberId, ProfileUpdateRequest request) { ... }
```

### 5.4 에러 처리 패턴

```java
// ErrorCode Enum 정의
public enum ErrorCode {
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "회원을 찾을 수 없습니다"),
    SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "복약 스케줄을 찾을 수 없습니다");
    
    public BusinessException toException() {
        return new BusinessException(this);
    }
}

// 사용
Member member = memberRepository.findById(id)
    .orElseThrow(ErrorCode.MEMBER_NOT_FOUND::toException);
```

---

## 6. 문서화 전략

### 6.1 ADR (Architecture Decision Records)

주요 설계 결정을 문서화하여 AI가 "왜 이렇게 구현했는지" 이해할 수 있게 합니다.

**ADR 템플릿 (MADR 형식):**

```markdown
# ADR-0001: JWT + CSRF 이중 보호 채택

## 상태
Accepted

## 컨텍스트
SPA 프론트엔드와 REST API 백엔드 구조에서 인증 및 CSRF 보호가 필요합니다.

## 결정
- JWT를 Authorization 헤더로 전송
- CSRF 토큰을 Redis에 저장하고 X-CSRF-TOKEN 헤더로 검증
- Refresh Token Rotation 적용

## 결과
### 장점
- Stateless JWT의 확장성
- Redis 기반 CSRF로 수평 확장 가능
- Refresh Token Rotation으로 토큰 탈취 위험 감소

### 단점
- Redis 의존성 추가
- 클라이언트 복잡도 증가

## 참고
- OWASP CSRF Prevention Cheat Sheet
```

### 6.2 작성 필요한 ADR 목록

| # | 제목 | 우선순위 |
|---|------|----------|
| 0001 | JWT + CSRF 이중 보호 채택 | 높음 |
| 0002 | AOP 기반 횡단 관심사 처리 | 높음 |
| 0003 | Domain Event 패턴 도입 | 중간 |
| 0004 | Flyway 마이그레이션 전략 | 중간 |
| 0005 | Redis 캐시 전략 | 중간 |
| 0006 | 민감 정보 AES 암호화 | 높음 |

---

## 7. 자동화 및 품질 관리

### 7.1 현재 자동화 파이프라인

```yaml
# .github/workflows/ci.yml
on: [push, pull_request]

jobs:
  build:
    steps:
      - Spotless Check    # 코드 스타일
      - Compile           # 컴파일
      - Test              # 1,400+ 테스트
      - JaCoCo Report     # 커버리지
      - ArchUnit          # 아키텍처 규칙
      - OWASP Check       # 의존성 취약점
      - Docker Build      # 이미지 빌드
```

### 7.2 추가 필요한 자동화

| 항목 | 도구 | 효과 |
|------|------|------|
| **CHANGELOG 자동 생성** | `git-cliff` 또는 `conventional-changelog` | 릴리즈 노트 자동화 |
| **API 문서 변경 감지** | `openapi-diff` | Breaking Change 경고 |
| **코드 복잡도 분석** | SonarQube 또는 Codacy | 리팩토링 대상 식별 |
| **성능 회귀 테스트** | k6 + GitHub Actions | 배포 전 성능 검증 |

### 7.3 Conventional Commits 가이드

```
<type>(<scope>): <subject>

Types:
- feat: 새로운 기능
- fix: 버그 수정
- docs: 문서 변경
- style: 코드 스타일 (포맷팅, 세미콜론 등)
- refactor: 리팩토링
- perf: 성능 개선
- test: 테스트 추가/수정
- chore: 빌드, 설정 변경

Examples:
feat(auth): 로그인 이력 관리 API 추가
fix(medication): 복약 기록 배치 생성 시 중복 체크 수정
docs: API 명세서 업데이트
```

---

## 8. 실전 바이브 코딩 예제

### 8.1 새로운 API 추가하기

**프롬프트 예시:**

> "알림 설정 API를 만들어줘. FCM 토큰 등록, 알림 설정 조회/수정이 필요해.
> 기존 패턴대로 Entity, DTO, Repository, Service, Controller 만들어줘."

**AI가 참조할 컨텍스트:**
1. `.cursorrules` - 프로젝트 구조, 패턴
2. 기존 `MemberProfileService.java` - 유사한 CRUD 패턴
3. `ErrorCode.java` - 새 에러 코드 추가 위치
4. `V2__Add_new_features.sql` - 마이그레이션 예시

### 8.2 버그 수정하기

**프롬프트 예시:**

> "복약 기록 배치 생성 시 동일한 schedule+time 조합이 중복 저장되는 버그가 있어.
> 중복 체크 로직을 추가해줘."

**AI가 참조할 컨텍스트:**
1. `MedicationRecordService.java` - 현재 배치 로직
2. `MedicationRecordRepository.java` - 중복 체크 쿼리 추가 위치
3. `MedicationRecordServiceTest.java` - 테스트 작성 패턴

### 8.3 리팩토링하기

**프롬프트 예시:**

> "MoodDiaryService가 너무 커졌어. 통계 관련 로직을 MoodDiaryStatsService로 분리해줘.
> 기존 테스트도 함께 분리해줘."

**AI가 참조할 컨텍스트:**
1. `AuthenticationService.java` - 서비스 분리 예시 (Auth 도메인)
2. `MoodDiaryService.java` - 현재 코드
3. ArchUnit 규칙 - 레이어 의존성 유지

---

## 📈 성공 지표

### 바이브 코딩 성숙도 체크리스트

- [x] AI 컨텍스트 파일 존재 (`.cursorrules`, `copilot-instructions.md`) ✅
- [x] ADR 문서 최소 3개 이상 (6개 완료) ✅
- [x] 코딩 컨벤션 문서 존재 (`docs/CODING-CONVENTIONS.md`) ✅
- [x] 선언적 패턴 문서 존재 (`docs/patterns/*.md`) ✅
- [x] Conventional Commits 적용 ✅
- [x] CHANGELOG 자동화 설정 (`cliff.toml`, `release.yml`) ✅
- [x] 90%+ 테스트 커버리지 (1,400+ 테스트) ✅
- [x] 아키텍처 테스트 (ArchUnit) 통과 ✅
- [x] API 문서 자동 생성 (OpenAPI) ✅
- [x] 코드 스캐폴딩 스크립트 존재 (`scripts/generate-crud.sh`) ✅
- [ ] E2E 통합 테스트 (다음 단계)

### 현재 점수: **94/100** 🎯

| 영역 | 현재 | 목표 | 상태 |
|------|------|------|------|
| 코드 구조 | 95 | 95 | ✅ 완료 |
| 테스트 | 90 | 95 | ⏳ E2E 추가 필요 |
| 문서화 | 95 | 95 | ✅ 완료 |
| AI 컨텍스트 | 95 | 95 | ✅ 완료 |
| 자동화 | 95 | 95 | ✅ 완료 |
| 선언적 패턴 | 95 | 95 | ✅ 완료 |

### 남은 작업

| 우선순위 | 작업 | 예상 기간 |
|----------|------|-----------|
| 높음 | E2E 통합 테스트 추가 | 1주 |
| 중간 | API 변경 감지 자동화 (openapi-diff) | 2일 |
| 낮음 | 성능 테스트 자동화 (k6) | 1주 |

---

## 📅 구현 일정

## 📅 구현 일정 (완료)

| Phase | 기간 | 주요 작업 | 산출물 | 상태 |
|-------|------|-----------|--------|------|
| **Phase 1** | 2025-12-23 | AI 컨텍스트 파일 생성 | `.cursorrules`, ADR 6개 | ✅ |
| **Phase 2** | 2025-12-24 | 선언적 패턴 문서화 | `docs/patterns/*` 4개 | ✅ |
| **Phase 3** | 2025-12-24 | 자동화 강화 | `cliff.toml`, `release.yml` | ✅ |
| **Phase 4** | - | 코드 스캐폴딩 | `scripts/generate-crud.sh` | ✅ |
| **Phase 5** | 진행 예정 | E2E 테스트 추가 | `src/test/java/.../e2e/` | ⏳ |

---

## 🔗 참고 자료

- [Vibe Coding 소개 (Andrej Karpathy)](https://twitter.com/karpathy/status/1886192184808149383)
- [Cursor IDE](https://cursor.sh/)
- [GitHub Copilot](https://github.com/features/copilot)
- [MADR - Markdown Architecture Decision Records](https://adr.github.io/madr/)
- [Conventional Commits](https://www.conventionalcommits.org/)
- [git-cliff - Changelog Generator](https://github.com/orhun/git-cliff)

---

> 📝 이 문서는 2025년 12월 24일 기준으로 작성되었습니다.
> Hamalog를 바이브 코딩의 모범 사례로 만들어 나가는 여정을 함께 합니다! 🚀
> Hamalog를 바이브 코딩의 모범 사례로 만들어 나가는 여정을 함께 합니다! 🚀

