# 🏗️ Hamalog 아키텍처 복잡도 정당화

> 📅 작성일: 2026년 1월 2일  
> 🎯 목적: "왜 더 단순하게 안 했나?"에 대한 답변 준비

---

## 개요

Hamalog는 헬스케어 앱 규모 대비 **높은 수준의 아키텍처 패턴**을 적용했습니다.  
이 문서는 각 패턴의 **도입 이유, 트레이드오프, 규모별 권장 방식**을 정리합니다.

### 핵심 메시지

> **"과잉 엔지니어링임을 인지하고 있으며, 학습 목적과 확장성을 고려해 의도적으로 선택했습니다."**

---

## 패턴별 정당화

### 1. JWT + CSRF 이중 보호

| 항목 | 내용 |
|------|------|
| **ADR** | [0002-jwt-csrf-dual-protection.md](adr/0002-jwt-csrf-dual-protection.md) |
| **필요성** | HttpOnly Cookie에 JWT 저장 → CSRF 취약 → 이중 보호 필요 |
| **과잉인가?** | 사용자 ~100명 규모에서는 Session + Cookie가 더 단순 |
| **왜 선택?** | 모바일 앱 + 웹 SPA 동시 지원, MSA 전환 대비, 실무 패턴 학습 |

**면접 답변 예시:**
> "JWT를 HttpOnly Cookie에 저장해서 XSS를 방어했습니다. 이 경우 브라우저가 자동으로 쿠키를 전송하므로 CSRF 취약점이 생깁니다. 그래서 커스텀 CSRF 토큰을 추가했습니다. 규모가 작다면 Session 방식이 더 단순하지만, 향후 확장성과 학습 목적으로 이 방식을 선택했습니다."

---

### 2. Domain Event + Event Store

| 항목 | 내용 |
|------|------|
| **ADR** | [0004-domain-event-pattern.md](adr/0004-domain-event-pattern.md) |
| **필요성** | 회원 탈퇴 시 5개+ 도메인 연쇄 삭제 → 직접 호출 시 순환 의존성 |
| **과잉인가?** | 도메인 3~4개 수준에서는 직접 호출이 더 명확 |
| **왜 선택?** | 느슨한 결합, 감사 추적, 실무 패턴 학습 |

**더 단순한 대안:**
```java
// 직접 호출 방식 (소규모 권장)
@Service
public class MemberDeletionService {
    private final List<MemberDataCleanupService> cleanupServices;
    
    @Transactional
    public void deleteMember(Long memberId) {
        cleanupServices.forEach(s -> s.cleanup(memberId));
        memberRepository.deleteById(memberId);
    }
}
```

**면접 답변 예시:**
> "회원 탈퇴 시 여러 도메인의 데이터를 삭제해야 하는데, 직접 호출하면 MemberService가 모든 도메인을 알아야 합니다. 도메인 이벤트 패턴으로 결합도를 낮추고, 이벤트 스토어로 감사 추적도 구현했습니다. 물론 규모가 작다면 직접 호출이 더 나을 수 있습니다."

---

### 3. @RequireResourceOwnership (커스텀 AOP)

| 항목 | 내용 |
|------|------|
| **ADR** | [0003-aop-cross-cutting-concerns.md](adr/0003-aop-cross-cutting-concerns.md) |
| **필요성** | 20개+ 엔드포인트에 소유권 검증 필요 |
| **과잉인가?** | Spring Security `@PreAuthorize`가 표준 |
| **왜 선택?** | 의도가 명확한 어노테이션, 복잡한 검증 전략 지원, AOP 학습 |

**Spring Security 대안:**
```java
@PreAuthorize("@ownershipChecker.isOwner(#id, 'MEDICATION_SCHEDULE')")
public ResponseEntity<ScheduleResponse> getById(@PathVariable Long id)
```

**면접 답변 예시:**
> "@PreAuthorize도 고려했지만, SpEL 표현식이 복잡해지고 의도 파악이 어려웠습니다. 커스텀 어노테이션으로 resourceType과 idParam을 명시해서 자체 문서화되도록 했습니다. 다만 생태계 호환성 측면에서는 @PreAuthorize가 더 나을 수 있습니다."

---

### 4. Redis 캐싱

| 항목 | 내용 |
|------|------|
| **ADR** | [0005-redis-cache-strategy.md](adr/0005-redis-cache-strategy.md) |
| **필요성** | CSRF 토큰 분산 저장, Rate Limiting 인스턴스 간 공유 |
| **과잉인가?** | 단일 인스턴스에서는 로컬 캐시(Caffeine)로 충분 |
| **왜 선택?** | 분산 환경 대비, 실무 Redis 운영 경험 |

**로컬 캐시 대안:**
```java
@Bean
public CacheManager cacheManager() {
    return new CaffeineCacheManager("memberCache");
}
```

**면접 답변 예시:**
> "Redis는 CSRF 토큰과 Rate Limiting에 필수입니다. 여러 인스턴스에서 요청 카운트를 공유해야 하니까요. 회원 프로필 캐싱은 Caffeine으로도 가능하지만, 학습 목적으로 Redis를 일관되게 사용했습니다."

---

## 규모별 권장 아키텍처

| 규모 | 인증 | 이벤트 | 소유권 검증 | 캐싱 |
|------|------|--------|-------------|------|
| **MVP** (~100명) | Session + Cookie | 직접 호출 | 서비스에서 직접 | 없음 |
| **성장** (~1,000명) | JWT (localStorage) | 직접 호출 | @PreAuthorize | Caffeine |
| **스케일업** (1,000명+) | JWT + CSRF | Domain Event | 커스텀 AOP | Redis |
| **대규모** (10,000명+) | + OAuth2 | + Message Queue | + RBAC | + CDN |

**Hamalog는 "스케일업" 수준의 아키텍처를 적용했습니다.**

---

## 결론

### 과잉 엔지니어링을 인정하는 이유

1. **면접에서 솔직함 어필** - "이건 과잉이지만 학습 목적으로..."
2. **적절한 복잡도 판단력 증명** - 규모별 권장 방식을 설명할 수 있음
3. **실무 패턴 경험** - 대규모 서비스에서 실제로 사용하는 패턴

### 이 프로젝트가 증명하는 것

- ✅ 복잡한 패턴을 이해하고 구현할 수 있다
- ✅ 트레이드오프를 인지하고 문서화할 수 있다
- ✅ "더 단순한 대안"을 알고 있다
- ✅ 규모에 따른 적절한 선택을 설명할 수 있다

---

## 관련 문서

- [ADR-0002: JWT + CSRF 이중 보호](adr/0002-jwt-csrf-dual-protection.md)
- [ADR-0003: AOP 횡단 관심사](adr/0003-aop-cross-cutting-concerns.md)
- [ADR-0004: Domain Event 패턴](adr/0004-domain-event-pattern.md)
- [ADR-0005: Redis 캐시 전략](adr/0005-redis-cache-strategy.md)

---

> 이 문서는 면접 준비 및 코드 리뷰 시 참고용입니다.

