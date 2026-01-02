# ADR-0004: Domain Event 패턴 도입

## 상태
Accepted

## 컨텍스트

Hamalog에서 다음과 같은 **도메인 간 연쇄 작업**이 필요합니다:

1. **회원 탈퇴 시**: 모든 관련 데이터 삭제 (복약 스케줄, 일기, 부작용 기록, 토큰 등)
2. **복약 스케줄 삭제 시**: 관련 복약 기록, 알림 시간 삭제
3. **복약 스케줄 생성 시**: 알림 시간 자동 생성

### 문제점 (이벤트 도입 전)

```java
// 강결합 문제: MemberDeletionService가 모든 도메인을 알아야 함
@Service
public class MemberDeletionService {
    private final MemberRepository memberRepo;
    private final MoodDiaryRepository diaryRepo;
    private final MedicationScheduleRepository scheduleRepo;
    private final SideEffectRecordRepository sideEffectRepo;
    private final RefreshTokenRepository tokenRepo;
    // ... 더 많은 의존성
    
    @Transactional
    public void deleteMember(Long memberId) {
        diaryRepo.deleteByMemberId(memberId);
        scheduleRepo.deleteByMemberId(memberId);
        sideEffectRepo.deleteByMemberId(memberId);
        tokenRepo.deleteByMemberId(memberId);
        // ... 추가될 때마다 이 서비스 수정 필요
        memberRepo.deleteById(memberId);
    }
}
```

### 고려한 대안들

| 방식 | 장점 | 단점 |
|------|------|------|
| **직접 호출** | 단순, 명시적 | 강결합, 순환 의존성 위험 |
| **Cascade Delete** | JPA 표준 | 세밀한 제어 어려움 |
| **Domain Event** | 느슨한 결합, 확장성 | 복잡도 증가, 트랜잭션 관리 |
| **Message Queue** | 비동기, 확장성 | 인프라 복잡도, 최종 일관성 |

## 결정

**Spring Application Event** 기반의 **동기식 Domain Event 패턴**을 채택합니다.

### 구현 구조

```
이벤트 발행자                     이벤트 처리자
(Publisher)                    (Handler)

MemberDeletionService           MemberDeletedEventHandler
        |                              ↑
        | publish                      | @EventListener
        ↓                              |
   MemberDeletedEvent ─────────────────┘
                                       |
                               ┌───────┴───────┐
                               ↓               ↓
                    MoodDiaryService    MedicationService
                    (delete diaries)    (delete schedules)
```

### 핵심 구성요소

#### 1. 도메인 이벤트 인터페이스

```java
// domain/events/DomainEvent.java
public interface DomainEvent {
    Instant occurredAt();
}
```

#### 2. 이벤트 클래스

```java
// domain/events/MemberDeletedEvent.java
public record MemberDeletedEvent(
    Long memberId,
    String loginId,
    Instant occurredAt
) implements DomainEvent {
    
    public static MemberDeletedEvent of(Member member) {
        return new MemberDeletedEvent(
            member.getMemberId(),
            member.getLoginId(),
            Instant.now()
        );
    }
}
```

#### 3. 이벤트 발행

```java
@Service
@RequiredArgsConstructor
public class MemberDeletionService {
    private final ApplicationEventPublisher eventPublisher;
    private final MemberRepository memberRepository;
    
    @Transactional
    public void deleteMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(ErrorCode.MEMBER_NOT_FOUND::toException);
        
        // 이벤트 발행 (핸들러들이 관련 데이터 삭제)
        eventPublisher.publishEvent(MemberDeletedEvent.of(member));
        
        // 회원 삭제
        memberRepository.delete(member);
    }
}
```

#### 4. 이벤트 처리

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class MemberDeletedEventHandler {
    private final MoodDiaryRepository moodDiaryRepository;
    private final MedicationScheduleRepository scheduleRepository;
    private final SideEffectRecordRepository sideEffectRepository;
    private final RefreshTokenRepository tokenRepository;
    
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleMemberDeleted(MemberDeletedEvent event) {
        Long memberId = event.memberId();
        log.info("Handling MemberDeletedEvent for memberId: {}", memberId);
        
        moodDiaryRepository.deleteByMember_MemberId(memberId);
        scheduleRepository.deleteByMember_MemberId(memberId);
        sideEffectRepository.deleteByMember_MemberId(memberId);
        tokenRepository.deleteByMemberId(memberId);
    }
}
```

### 트랜잭션 전략

| Phase | 설명 | 사용 시점 |
|-------|------|----------|
| `BEFORE_COMMIT` | 같은 트랜잭션 내 실행 | 데이터 일관성 필수 시 |
| `AFTER_COMMIT` | 커밋 후 실행 | 알림 발송 등 부가 작업 |
| `AFTER_ROLLBACK` | 롤백 후 실행 | 보상 트랜잭션 |

Hamalog에서는 **데이터 일관성**이 중요하므로 `BEFORE_COMMIT` 사용.

## 결과

### 장점
- ✅ **느슨한 결합**: 서비스 간 직접 의존성 제거
- ✅ **확장성**: 새 핸들러 추가만으로 기능 확장
- ✅ **단일 책임**: 각 서비스가 자신의 도메인만 관리
- ✅ **테스트 용이**: 이벤트 발행/처리 독립 테스트 가능
- ✅ **감사 추적**: 이벤트 로깅으로 변경 이력 추적

### 단점
- ⚠️ **디버깅 복잡**: 이벤트 흐름 추적 필요
- ⚠️ **순서 보장 어려움**: 여러 핸들러 실행 순서 불확정
- ⚠️ **트랜잭션 관리**: `@TransactionalEventListener` 이해 필요

### 적용된 이벤트

| 이벤트 | 발행 시점 | 처리 내용 |
|--------|----------|-----------|
| `MemberDeletedEvent` | 회원 탈퇴 | 관련 모든 데이터 삭제 |
| `MedicationScheduleCreatedEvent` | 스케줄 생성 | 알림 시간 자동 생성 |
| `MedicationScheduleDeletedEvent` | 스케줄 삭제 | 관련 기록 삭제 |

---

## 트레이드오프 분석

### 직접 호출 vs Domain Event 비교

| 기준 | 직접 호출 | Domain Event (현재) |
|------|-----------|---------------------|
| **이해 용이성** | ✅ 흐름이 명확 | ⚠️ 이벤트 추적 필요 |
| **결합도** | ⚠️ 강결합 | ✅ 느슨한 결합 |
| **확장성** | ⚠️ 수정 필요 | ✅ 핸들러 추가만 |
| **디버깅** | ✅ 쉬움 | ⚠️ 이벤트 흐름 추적 |
| **구현 복잡도** | ✅ 낮음 | ⚠️ 높음 |

**Hamalog에서 Domain Event가 필요한 이유:**
1. **회원 탈퇴 시 5개+ 도메인 연쇄 삭제** - 직접 호출 시 순환 의존성 발생
2. **복약 스케줄 생성 시 알림 자동 생성** - 핵심 도메인(Medication)이 부가 도메인(Notification)을 몰라도 됨
3. **감사 로그 자동 수집** - `EventPersistenceHandler`로 모든 이벤트 추적

### Event Store(StoredDomainEvent) 사용 판단 기준

| 규모/요구사항 | Event Store 필요? | 이유 |
|---------------|-------------------|------|
| **MVP** (사용자 ~100명) | ❌ 불필요 | 로그로 충분 |
| **감사 추적 필요** (의료/금융) | ✅ 필요 | 법적 요구사항 |
| **이벤트 소싱 아키텍처** | ✅ 필수 | 상태 복원 목적 |
| **장애 복구 필요** | ✅ 권장 | 실패한 이벤트 재처리 |
| **Hamalog** | ⚠️ 학습 목적 | 실무 패턴 경험 |

**현재 Hamalog의 Event Store 목적:**
1. **감사 추적** - 의료 앱 특성상 변경 이력 중요
2. **학습** - 이벤트 스토어 패턴 실습
3. **향후 확장** - 비동기 이벤트 처리, 재처리 기능 기반

### 과잉 엔지니어링 인정 및 대응

> ⚠️ **Hamalog 규모에서 Domain Event + Event Store는 과잉일 수 있습니다.**

**그럼에도 채택한 이유:**
1. 실무에서 많이 사용되는 패턴 학습
2. 면접에서 "도메인 이벤트 패턴 경험"으로 어필 가능
3. 추후 기능 추가 시 확장 용이

**더 단순한 대안 (소규모 시):**
```java
// 직접 호출 방식 - 규모가 작다면 이게 더 나음
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

### 만약 다시 선택한다면?

**동일한 선택을 하되:**
1. **Event Store 토글** 추가 - 개발 환경에서는 비활성화 옵션
2. **이벤트 흐름 다이어그램** 문서화 - 디버깅 용이성 확보
3. **@Order 어노테이션** 명시 - 핸들러 실행 순서 보장

## 참고

- [Spring Application Events](https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html#context-functionality-events)
- [Domain Events - Martin Fowler](https://martinfowler.com/eaaDev/DomainEvent.html)
- [Event-Driven Architecture](https://microservices.io/patterns/data/event-driven-architecture.html)

---

> 작성일: 2025-12-23

