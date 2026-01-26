# ADR 템플릿

> **ADR (Architecture Decision Record)** 은 프로젝트에서 내린 중요한 아키텍처 결정을 기록하는 문서입니다.
> "왜 이렇게 구현했는지" 근거를 남기고, 면접에서 기술적 깊이를 증명합니다.

## 기본 형식

```markdown
# ADR-{번호}: {제목}

## 상태
{Draft | Proposed | Accepted | Deprecated | Superseded by ADR-XXX}

## 컨텍스트
{결정이 필요한 상황과 배경을 설명합니다}

## 결정
{내린 결정과 근거를 설명합니다}

## 결과

### 장점
- {장점 1}
- {장점 2}

### 단점
- {단점 1}
- {단점 2}

## 참고
- {관련 링크, 문서}
```

## 확장 형식 (권장)

> 💡 더 상세한 의사결정 기록이 필요할 때 사용하세요.
> 전체 템플릿: [DECISION-TROUBLESHOOTING-GUIDE.md](../DECISION-TROUBLESHOOTING-GUIDE.md#21-확장된-adr-템플릿)

```markdown
# ADR-{번호}: {제목}

## 상태
{Draft | Proposed | Accepted | Deprecated | Superseded by ADR-XXX}

## 1. 배경 (Context)

### 현재 상황
{해결해야 할 문제와 현재 상황}

### 제약 조건
- {제약 조건 1} (예: 개발 기간 3일)
- {제약 조건 2} (예: 이미 Redis 도입됨)

## 2. 고려한 대안 (Options)

### Option A: {대안명}
- **장점**: {장점들}
- **단점**: {단점들}
- **적합성**: ⭐⭐⭐☆☆

### Option B: {대안명}
- **장점**: {장점들}
- **단점**: {단점들}
- **적합성**: ⭐⭐⭐⭐☆

## 3. 결정 (Decision)

### 선택
**Option B: {대안명}**

### 선택 이유
{왜 이 옵션을 선택했는지 구체적으로}

## 4. 트레이드오프 & 후속 조치 (Consequences)

### 얻은 것
- {장점 1}

### 잃은 것
- {단점 1}

### 후속 조치
- {조치 1}

## 5. 참고 자료
- {관련 링크}
```

## ADR 목록

| # | 제목 | 상태 | 날짜 |
|---|------|------|------|
| 0001 | [ADR 템플릿](./0001-adr-template.md) | Accepted | 2025-12-23 |
| 0002 | [JWT + CSRF 이중 보호 채택](./0002-jwt-csrf-dual-protection.md) | Accepted | 2025-12-23 |
| 0003 | [AOP 기반 횡단 관심사 처리](./0003-aop-cross-cutting-concerns.md) | Accepted | 2025-12-23 |
| 0004 | [Domain Event 패턴 도입](./0004-domain-event-pattern.md) | Accepted | 2025-12-23 |
| 0005 | [Redis 캐시 전략](./0005-redis-cache-strategy.md) | Accepted | 2025-12-23 |
| 0006 | [민감 정보 AES 암호화](./0006-sensitive-data-encryption.md) | Accepted | 2025-12-23 |
| 0007 | [메시지 큐 도입 - Redis Stream 선택](./0007-message-queue-redis-stream.md) | Accepted | 2026-01-03 |

## 작성 가이드

1. **새 ADR 생성 시**: 다음 번호를 사용하여 파일 생성
2. **상태 변경 시**: 상태 필드 업데이트
3. **폐기 시**: Deprecated 상태로 변경하고 대체 ADR 링크

