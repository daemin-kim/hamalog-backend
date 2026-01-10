# 📚 Hamalog 기술적 도전과 해결 방식 - 심층 분석

> **Hamalog 프로젝트에서 마주한 9가지 핵심 기술적 도전과 그 해결 방식을 상세히 분석한 포트폴리오 보조 문서입니다.**

---

## 📋 문서 목록

| # | 문서명 | 핵심 주제 | 관련 기술 |
|---|--------|----------|----------|
| 01 | [JWT + CSRF 이중 보호](./01-jwt-csrf-protection.md) | SPA 환경 CSRF 공격 방어 | Spring Security, Redis, JWT |
| 02 | [AOP 기반 리소스 소유권 검증](./02-aop-resource-ownership.md) | 코드 중복 제거, 선언적 보안 | Spring AOP, 커스텀 어노테이션 |
| 03 | [JPA N+1 문제 해결](./03-jpa-n-plus-one.md) | 쿼리 최적화, 성능 개선 | EntityGraph, DTO Projection |
| 04 | [AES-256-GCM 민감정보 암호화](./04-sensitive-data-encryption.md) | 개인정보보호법 준수 | AES-GCM, JPA Converter |
| 05 | [낙관적 락 동시성 제어](./05-optimistic-locking.md) | 동시 수정 충돌 해결 | @Version, Exception Handler |
| 06 | [Redis Stream 비동기 메시지 큐](./06-redis-stream-queue.md) | 알림 발송 비동기화 | Redis Stream, Consumer Group |
| 07 | [Redis 캐시 일관성 전략](./07-redis-cache-consistency.md) | 캐시-DB 정합성 보장 | Cache-Aside, @Cacheable |
| 08 | [Redis Sorted Set Rate Limiting](./08-rate-limiting.md) | DDoS/Brute Force 방어 | Sliding Window, Fail-Open |
| 09 | [ArchUnit 아키텍처 테스트](./09-archunit-architecture-test.md) | 레이어드 아키텍처 강제 | ArchUnit, JUnit 5 |

---

## 📖 문서 구조 (공통 템플릿)

모든 문서는 다음 5단계 구조로 통일되어 있습니다:

```
1. 문제 상황 (Problem)
   - 실제 발생한 문제와 그 영향

2. 해결 전략 (Solution Strategy)
   - 고려한 대안들과 최종 선택 이유

3. 구현 상세 (Implementation)
   - 핵심 코드와 상세 주석/설명

4. 효과 및 검증 (Results)
   - 정량적/정성적 개선 효과

5. 면접 대비 Q&A (Interview Q&A)
   - 관련 기술 면접 질문 10개와 모범 답변
```

---

## 🎯 학습 포인트 요약

### 보안 (Security)
- **JWT + CSRF 이중 보호**: Stateless 아키텍처에서 CSRF 공격 방어
- **민감정보 암호화**: AES-256-GCM으로 필드 단위 암호화
- **리소스 소유권 검증**: AOP로 인가 로직 중앙화
- **Rate Limiting**: Sliding Window로 DDoS/Brute Force 방어

### 성능 (Performance)
- **N+1 문제 해결**: EntityGraph, DTO Projection, 배치 조회
- **비동기 처리**: Redis Stream으로 외부 API 호출 분리
- **캐싱 전략**: Cache-Aside 패턴으로 DB 부하 감소

### 데이터 정합성 (Data Integrity)
- **낙관적 락**: @Version으로 동시 수정 충돌 감지
- **캐시 일관성**: 수정 시 선제적 캐시 무효화

### 품질 보증 (Quality Assurance)
- **아키텍처 테스트**: ArchUnit으로 레이어드 아키텍처 강제
- **네이밍 컨벤션**: 자동 검증으로 일관성 유지

---

## 📎 관련 문서

| 문서 | 설명 |
|------|------|
| [PORTFOLIO.md](../PORTFOLIO.md) | 메인 포트폴리오 문서 |
| [ADR 목록](../internal/adr/) | 아키텍처 결정 기록 |
| [패턴 가이드](../internal/patterns/) | 구현 표준 문서 |

---

> 💡 **Tip**: 면접 준비 시 각 문서의 "면접 대비 Q&A" 섹션을 중점적으로 학습하세요.

