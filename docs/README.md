# 📚 Hamalog 문서 가이드

> 이 문서는 프로젝트의 모든 문서를 용도별로 분류합니다.

---

## 📊 문서 분류표

### 🌐 공유용 (외부/팀 간 협업)

프론트엔드 개발자, 기획자, 외부 이해관계자와 공유하는 문서입니다.

| 문서 | 경로 | 대상 | 설명 |
|------|------|------|------|
| **API 명세서** | `API-specification.md` | 프론트엔드 | REST API 엔드포인트, 요청/응답 형식 |
| **프로젝트 구조** | `Project-Structure.md` | 전체 | 기술 스택, 디렉토리 구조, 배포 구성 |
| **향후 개선사항** | `FUTURE-IMPROVEMENTS.md` | 전체 | 기능 구현 현황, 로드맵 |

---

### 🔧 개발용 (백엔드 내부)

백엔드 개발자가 코드 작성 시 참조하는 문서입니다.

| 문서 | 경로 | 용도 |
|------|------|------|
| **API 참고 문서** | `API-reference.md` | 인프라 구성, DB 스키마, 변경 이력 |
| **코딩 컨벤션** | `CODING-CONVENTIONS.md` | 네이밍 규칙, 클래스 구조, 코드 스타일 |
| **개선 보고서** | `IMPROVEMENT-REPORT.md` | 완료된 리팩토링/개선 작업 기록 |

---

### 🤖 AI/바이브 코딩용

AI 도구 (Cursor, GitHub Copilot)가 코드 생성 시 참조하는 문서입니다.

| 문서 | 경로 | 용도 |
|------|------|------|
| **바이브 코딩 가이드** | `VIBE-CODING-GUIDE.md` | AI 협업 전략, 프롬프트 가이드 |
| `.cursorrules` | 프로젝트 루트 | Cursor IDE AI 컨텍스트 |
| `copilot-instructions.md` | `.github/` | GitHub Copilot 컨텍스트 |

---

### 🏗️ 아키텍처 결정 기록 (ADR)

기술적 의사결정의 배경과 이유를 기록한 문서입니다.

| 문서 | 경로 | 내용 |
|------|------|------|
| **ADR 템플릿** | `adr/0001-adr-template.md` | ADR 작성 가이드 |
| **JWT+CSRF 이중 보호** | `adr/0002-jwt-csrf-dual-protection.md` | 인증 보안 설계 결정 |
| **AOP 횡단 관심사** | `adr/0003-aop-cross-cutting-concerns.md` | 로깅/재시도/캐싱 AOP 설계 |
| **Domain Event 패턴** | `adr/0004-domain-event-pattern.md` | 이벤트 기반 아키텍처 |
| **Redis 캐시 전략** | `adr/0005-redis-cache-strategy.md` | 캐시 설계 및 TTL 정책 |
| **민감 데이터 암호화** | `adr/0006-sensitive-data-encryption.md` | AES 암호화 전략 |

---

### 📐 선언적 패턴 문서

커스텀 어노테이션, 에러 처리 등 프로젝트 고유 패턴의 사용법입니다.

| 문서 | 경로 | 내용 |
|------|------|------|
| **어노테이션 가이드** | `patterns/ANNOTATION-GUIDE.md` | `@RequireResourceOwnership`, `@Retryable` 등 |
| **에러 처리 패턴** | `patterns/ERROR-HANDLING.md` | `ErrorCode`, `BusinessException` 사용법 |
| **보안 패턴** | `patterns/SECURITY-PATTERNS.md` | JWT, CSRF, 리소스 소유권 검증 |
| **캐싱 패턴** | `patterns/CACHING-PATTERNS.md` | `@Cacheable`, Redis TTL 전략 |

---

## 🎯 역할별 필독 문서

### 프론트엔드 개발자
1. `API-specification.md` - API 호출 방법
2. `Project-Structure.md` - 전체 구조 이해

### 백엔드 개발자 (신규 합류)
1. `Project-Structure.md` - 전체 구조
2. `CODING-CONVENTIONS.md` - 코드 스타일
3. `patterns/*.md` - 선언적 패턴
4. `adr/*.md` - 아키텍처 결정 배경

### AI 도구 설정
1. `.cursorrules` - Cursor IDE용
2. `.github/copilot-instructions.md` - GitHub Copilot용
3. `VIBE-CODING-GUIDE.md` - AI 협업 가이드

---

## 📁 폴더 구조

```
docs/
├── README.md                    ← 현재 문서 (문서 가이드)
├── API-specification.md         ← 🌐 공유용
├── API-reference.md             ← 🔧 개발용
├── CODING-CONVENTIONS.md        ← 🔧 개발용
├── Project-Structure.md         ← 🌐 공유용
├── FUTURE-IMPROVEMENTS.md       ← 🌐 공유용
├── IMPROVEMENT-REPORT.md        ← 🔧 개발용 (내부 기록)
├── VIBE-CODING-GUIDE.md         ← 🤖 AI용
├── adr/                         ← 🏗️ 아키텍처 결정
│   ├── 0001-adr-template.md
│   ├── 0002-jwt-csrf-dual-protection.md
│   ├── 0003-aop-cross-cutting-concerns.md
│   ├── 0004-domain-event-pattern.md
│   ├── 0005-redis-cache-strategy.md
│   └── 0006-sensitive-data-encryption.md
└── patterns/                    ← 📐 선언적 패턴
    ├── ANNOTATION-GUIDE.md
    ├── CACHING-PATTERNS.md
    ├── ERROR-HANDLING.md
    └── SECURITY-PATTERNS.md
```

---

> 📝 최종 업데이트: 2025년 12월 24일

