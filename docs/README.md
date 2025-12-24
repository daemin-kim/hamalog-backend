# 📚 Hamalog 문서 가이드

> 이 문서는 프로젝트의 모든 문서를 용도별 폴더로 분류하여 관리합니다.

---

## 📁 폴더 구조

```
📂 docs/
├── 📄 README.md                     📖 현재 문서 (문서 가이드)
│
├── 📂 shared/                       🌐 공유용 (프론트엔드, 기획자, 외부 공유)
│   ├── 📄 API-specification.md      REST API 명세서
│   ├── 📄 Project-Structure.md      프로젝트 구조, 기술 스택
│   └── 📄 FUTURE-IMPROVEMENTS.md    기능 현황, 로드맵
│
├── 📂 internal/                     🔧 개발용 (백엔드 내부)
│   ├── 📄 API-reference.md          인프라, DB 스키마, 변경 이력
│   ├── 📄 CODING-CONVENTIONS.md     코딩 컨벤션, 네이밍 규칙
│   ├── 📄 IMPROVEMENT-REPORT.md     리팩토링/개선 작업 기록
│   │
│   ├── 📂 adr/                      아키텍처 결정 기록 (ADR)
│   │   ├── 📄 0001-adr-template.md
│   │   ├── 📄 0002-jwt-csrf-dual-protection.md
│   │   ├── 📄 0003-aop-cross-cutting-concerns.md
│   │   ├── 📄 0004-domain-event-pattern.md
│   │   ├── 📄 0005-redis-cache-strategy.md
│   │   └── 📄 0006-sensitive-data-encryption.md
│   │
│   └── 📂 patterns/                 선언적 패턴 가이드
│       ├── 📄 ANNOTATION-GUIDE.md   커스텀 어노테이션 사용법
│       ├── 📄 ERROR-HANDLING.md     에러 처리 패턴
│       ├── 📄 SECURITY-PATTERNS.md  보안 패턴
│       └── 📄 CACHING-PATTERNS.md   캐싱 패턴
│
└── 📂 ai/                           🤖 AI용 (바이브 코딩)
    └── 📄 VIBE-CODING-GUIDE.md      AI 협업 가이드, 프롬프트
```

---

## 🏷️ 용도별 설명

### 🌐 `shared/` - 공유용

프론트엔드 개발자, 기획자, 외부 이해관계자와 공유하는 문서입니다.

| 문서 | 대상 | 설명 |
|------|------|------|
| `API-specification.md` | 프론트엔드 | REST API 엔드포인트, 요청/응답 형식 |
| `Project-Structure.md` | 전체 | 기술 스택, 디렉토리 구조, 배포 구성 |
| `FUTURE-IMPROVEMENTS.md` | 전체 | 기능 구현 현황, 로드맵 |

---

### 🔧 `internal/` - 개발용

백엔드 개발자가 코드 작성 시 참조하는 내부 문서입니다.

| 문서 | 용도 |
|------|------|
| `API-reference.md` | 인프라 구성, DB 스키마, 변경 이력 |
| `CODING-CONVENTIONS.md` | 네이밍 규칙, 클래스 구조, 코드 스타일 |
| `IMPROVEMENT-REPORT.md` | 완료된 리팩토링/개선 작업 기록 |

#### 📂 `internal/adr/` - 아키텍처 결정 기록

| 문서 | 내용 |
|------|------|
| `0001-adr-template.md` | ADR 작성 가이드 |
| `0002-jwt-csrf-dual-protection.md` | JWT + CSRF 이중 보호 |
| `0003-aop-cross-cutting-concerns.md` | AOP 횡단 관심사 |
| `0004-domain-event-pattern.md` | Domain Event 패턴 |
| `0005-redis-cache-strategy.md` | Redis 캐시 전략 |
| `0006-sensitive-data-encryption.md` | 민감 데이터 암호화 |

#### 📂 `internal/patterns/` - 선언적 패턴

| 문서 | 내용 |
|------|------|
| `ANNOTATION-GUIDE.md` | `@RequireResourceOwnership`, `@Retryable` 등 |
| `ERROR-HANDLING.md` | `ErrorCode`, `BusinessException` |
| `SECURITY-PATTERNS.md` | JWT, CSRF, 리소스 소유권 검증 |
| `CACHING-PATTERNS.md` | `@Cacheable`, Redis TTL 전략 |

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

> 📝 최종 업데이트: 2025년 12월 24일

