# 📚 Hamalog 문서 가이드

> 이 문서는 프로젝트의 모든 문서를 용도별 폴더로 분류하여 관리합니다.

---

## 📁 폴더 구조

```
📂 docs/
├── 📄 README.md                     📖 현재 문서 (문서 가이드)
├── 📄 PORTFOLIO.md                  📋 메인 포트폴리오
├── 📄 PORTFOLIO-SUMMARY.md          📋 포트폴리오 요약 (1페이지)
├── 📄 GITHUB-PROJECT-CARD.md        📋 GitHub 프로젝트 카드
│
├── 📂 shared/                       🌐 공유용 (프론트엔드, 기획자, 외부 공유)
│   ├── 📄 API-specification.md      REST API 명세서
│   ├── 📄 Project-Structure.md      프로젝트 구조, 기술 스택
│   └── 📄 FUTURE-IMPROVEMENTS.md    미구현 기능, 로드맵
│
├── 📂 internal/                     🔧 개발용 (백엔드 내부)
│   ├── 📄 API-reference.md          인프라, DB 스키마, 변경 이력
│   ├── 📄 CODING-CONVENTIONS.md     코딩 컨벤션, 네이밍 규칙
│   ├── 📄 KOTLIN-GUIDE.md           Kotlin 문법 가이드 (Java 개발자용)
│   ├── 📄 PROJECT-AUDIT-GUIDE.md    프로젝트 전체 점검 가이드
│   ├── 📄 IMPROVEMENT-TASKS.md      개선 태스크 체크리스트
│   ├── 📄 AUDIT-REPORT-20260120.md  프로젝트 점검 보고서
│   ├── 📄 PRACTICAL-EVALUATION-REPORT.md  실무 평가 보고서
│   │
│   ├── 📂 adr/                      아키텍처 결정 기록 (ADR)
│   │   ├── 📄 0001-adr-template.md
│   │   ├── 📄 0002-jwt-csrf-dual-protection.md
│   │   ├── 📄 0003-aop-cross-cutting-concerns.md
│   │   ├── 📄 0004-domain-event-pattern.md
│   │   ├── 📄 0005-redis-cache-strategy.md
│   │   ├── 📄 0006-sensitive-data-encryption.md
│   │   └── 📄 0007-message-queue-redis-stream.md
│   │
│   ├── 📂 patterns/                 선언적 패턴 가이드
│   │   ├── 📄 ANNOTATION-GUIDE.md   커스텀 어노테이션 사용법
│   │   ├── 📄 ERROR-HANDLING.md     에러 처리 패턴
│   │   ├── 📄 SECURITY-PATTERNS.md  보안 패턴
│   │   ├── 📄 CACHING-PATTERNS.md   캐싱 패턴
│   │   ├── 📄 JPA-PERFORMANCE.md    JPA 성능 최적화
│   │   └── 📄 MESSAGE-QUEUE-PATTERNS.md  메시지 큐 패턴
│   │
│   └── 📊 성능 벤치마크 문서
│       ├── 📄 PERFORMANCE-BENCHMARK.md    벤치마크 결과
│       ├── 📄 BENCHMARK-COMMANDS.md       실행 명령어
│       ├── 📄 BENCHMARK-LOCAL-STRATEGY.md 로컬 전략
│       └── 📄 BENCHMARK-CACHE-PLAN.md     캐시 계획
│
├── 📂 portfolio/                    🎯 기술적 도전 심층 분석
│   ├── 📄 README.md                 포트폴리오 문서 목록
│   ├── 📄 01-jwt-csrf-protection.md
│   ├── 📄 02-aop-resource-ownership.md
│   ├── 📄 03-jpa-n-plus-one.md
│   ├── 📄 04-sensitive-data-encryption.md
│   ├── 📄 05-optimistic-locking.md
│   ├── 📄 06-redis-stream-queue.md
│   ├── 📄 07-redis-cache-consistency.md
│   ├── 📄 08-rate-limiting.md
│   └── 📄 09-archunit-architecture-test.md
│
└── 📂 ai/                           🤖 AI용 (바이브 코딩)
    └── 📄 VIBE-CODING-GUIDE.md      AI 협업 가이드, 프롬프트
```

---

## 🏷️ 용도별 설명

### 📋 루트 문서 - 포트폴리오

면접/채용 과정에서 제출하는 포트폴리오 문서입니다.

| 문서 | 대상 | 설명 |
|------|------|------|
| `PORTFOLIO.md` | 면접관 | 상세 포트폴리오 (576줄) |
| `PORTFOLIO-SUMMARY.md` | 면접관 | 1페이지 요약본 |
| `GITHUB-PROJECT-CARD.md` | 전체 | GitHub README용 프로젝트 카드 |

---

### 🌐 `shared/` - 공유용

프론트엔드 개발자, 기획자, 외부 이해관계자와 공유하는 문서입니다.

| 문서 | 대상 | 설명 |
|------|------|------|
| `API-specification.md` | 프론트엔드 | REST API 엔드포인트, 요청/응답 형식 |
| `Project-Structure.md` | 전체 | 기술 스택, 디렉토리 구조, 배포 구성 |
| `FUTURE-IMPROVEMENTS.md` | 전체 | 미구현 기능, 로드맵 |

---

### 🔧 `internal/` - 개발용

백엔드 개발자가 코드 작성 시 참조하는 내부 문서입니다.

| 문서 | 용도 |
|------|------|
| `API-reference.md` | 인프라 구성, DB 스키마, 변경 이력 |
| `CODING-CONVENTIONS.md` | 네이밍 규칙, 클래스 구조, 코드 스타일 |
| `KOTLIN-GUIDE.md` | Java 개발자를 위한 Kotlin 문법 가이드 |
| `PROJECT-AUDIT-GUIDE.md` | 프로젝트 전체 점검 체크리스트 |
| `IMPROVEMENT-TASKS.md` | 개선 태스크 우선순위별 체크리스트 |
| `AUDIT-REPORT-20260120.md` | 프로젝트 점검 결과 보고서 |
| `PRACTICAL-EVALUATION-REPORT.md` | 실무 평가 보고서 |

#### 📂 `internal/adr/` - 아키텍처 결정 기록

| 문서 | 내용 |
|------|------|
| `0001-adr-template.md` | ADR 작성 가이드 |
| `0002-jwt-csrf-dual-protection.md` | JWT + CSRF 이중 보호 |
| `0003-aop-cross-cutting-concerns.md` | AOP 횡단 관심사 |
| `0004-domain-event-pattern.md` | Domain Event 패턴 |
| `0005-redis-cache-strategy.md` | Redis 캐시 전략 |
| `0006-sensitive-data-encryption.md` | 민감 데이터 암호화 |
| `0007-message-queue-redis-stream.md` | 메시지 큐 (Redis Stream) |

#### 📂 `internal/patterns/` - 선언적 패턴

| 문서 | 내용 |
|------|------|
| `ANNOTATION-GUIDE.md` | `@RequireResourceOwnership`, `@Retryable` 등 |
| `ERROR-HANDLING.md` | `ErrorCode`, `CustomException` |
| `SECURITY-PATTERNS.md` | JWT, CSRF, 리소스 소유권 검증 |
| `CACHING-PATTERNS.md` | `@Cacheable`, Redis TTL 전략 |
| `JPA-PERFORMANCE.md` | N+1 해결, DTO Projection, QueryDSL |
| `MESSAGE-QUEUE-PATTERNS.md` | Redis Stream, Producer/Consumer, DLQ |

#### 📊 벤치마크 및 성능 문서

| 문서 | 내용 |
|------|------|
| `PERFORMANCE-BENCHMARK.md` | Gatling 성능 벤치마크 결과 |
| `BENCHMARK-COMMANDS.md` | 벤치마크 실행 명령어 |
| `BENCHMARK-LOCAL-STRATEGY.md` | 로컬 환경 벤치마크 전략 |
| `BENCHMARK-CACHE-PLAN.md` | 캐시 벤치마크 계획 |
| `GITHUB-SECRETS-GUIDE.md` | GitHub Secrets 설정 가이드 |

---

### 🎯 `portfolio/` - 기술적 도전 심층 분석

9가지 핵심 기술적 도전과 해결 방식을 상세히 분석한 문서입니다.  
면접 대비 Q&A(각 10문항)가 포함되어 있습니다.

| # | 문서 | 핵심 주제 |
|---|------|----------|
| 01 | `01-jwt-csrf-protection.md` | SPA 환경 CSRF 공격 방어 |
| 02 | `02-aop-resource-ownership.md` | 코드 중복 제거, 선언적 보안 |
| 03 | `03-jpa-n-plus-one.md` | 쿼리 최적화, 성능 개선 |
| 04 | `04-sensitive-data-encryption.md` | 개인정보보호법 준수 |
| 05 | `05-optimistic-locking.md` | 동시 수정 충돌 해결 |
| 06 | `06-redis-stream-queue.md` | 알림 발송 비동기화 |
| 07 | `07-redis-cache-consistency.md` | 캐시-DB 정합성 보장 |
| 08 | `08-rate-limiting.md` | DDoS/Brute Force 방어 |
| 09 | `09-archunit-architecture-test.md` | 레이어드 아키텍처 강제 |

---

### 🤖 `ai/` - AI용

AI 도구 (Cursor, GitHub Copilot)가 코드 생성 시 참조하는 문서입니다.

| 문서 | 용도 |
|------|------|
| `VIBE-CODING-GUIDE.md` | AI 협업 전략, 프롬프트 가이드 |

**관련 파일 (프로젝트 루트)**:
- `.cursorrules` - Cursor IDE AI 컨텍스트
- `.github/copilot-instructions.md` - GitHub Copilot 컨텍스트

---

## 🎯 역할별 필독 문서

### 프론트엔드 개발자
1. `shared/API-specification.md` - API 호출 방법
2. `shared/Project-Structure.md` - 전체 구조 이해

### 백엔드 개발자 (신규 합류)
1. `shared/Project-Structure.md` - 전체 구조
2. `internal/CODING-CONVENTIONS.md` - 코드 스타일
3. `internal/patterns/*.md` - 선언적 패턴
4. `internal/adr/*.md` - 아키텍처 결정 배경

### AI 도구 설정
1. `.cursorrules` (루트) - Cursor IDE용
2. `.github/copilot-instructions.md` - GitHub Copilot용
3. `ai/VIBE-CODING-GUIDE.md` - AI 협업 가이드

---

> 📝 최종 업데이트: 2026년 1월 22일

