# ADR-0003: AOP 기반 횡단 관심사 처리

## 상태
Accepted

## 컨텍스트

Hamalog 백엔드에서 다음 **횡단 관심사(Cross-Cutting Concerns)** 를 처리해야 합니다:

1. **로깅**: API 요청/응답, 서비스 메서드 호출 로깅
2. **보안**: 리소스 소유권 검증 (본인 데이터만 접근)
3. **성능 모니터링**: 메서드 실행 시간 측정
4. **재시도**: 낙관적 락 충돌 시 자동 재시도
5. **캐싱**: 자주 조회되는 데이터 캐싱
6. **감사(Audit)**: 비즈니스 이벤트 기록

### 고려한 대안들

| 방식 | 장점 | 단점 |
|------|------|------|
| **수동 구현** | 명시적, 디버깅 용이 | 코드 중복, 유지보수 어려움 |
| **Interceptor** | Spring 표준 | Controller에만 적용 가능 |
| **Filter** | 서블릿 표준 | HTTP 계층에만 적용 |
| **AOP (Aspect)** | 선언적, 레이어 무관 | 학습 곡선, 디버깅 복잡 |
| **Decorator 패턴** | 명시적 | 많은 Wrapper 클래스 필요 |

## 결정

**Spring AOP** 를 활용한 어노테이션 기반 횡단 관심사 처리를 채택합니다.

### 구현된 Aspect 목록

| Aspect | 적용 대상 | 역할 |
|--------|-----------|------|
| `ApiLoggingAspect` | `@RestController` 메서드 | API 요청/응답 로깅 |
| `ServiceLoggingAspect` | `@Service` 메서드 | 서비스 호출 로깅 |
| `PerformanceMonitoringAspect` | 서비스 레이어 | 실행 시간 측정, Prometheus 메트릭 |
| `BusinessAuditAspect` | create/update/delete 메서드 (Pointcut) | 비즈니스 이벤트 감사 로깅 |
| `RetryAspect` | `@Retryable` 어노테이션 | 재시도 로직 |
| `CachingAspect` | `@Cacheable` 어노테이션 | Redis 캐싱 |

### 핵심 커스텀 어노테이션

#### @RequireResourceOwnership

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireResourceOwnership {
    String resourceType();  // MEDICATION_SCHEDULE, MOOD_DIARY 등
    String idParam();       // 경로 변수명
}

// 사용 예시
@GetMapping("/{id}")
@RequireResourceOwnership(resourceType = "MEDICATION_SCHEDULE", idParam = "id")
public ResponseEntity<ScheduleResponse> getById(@PathVariable Long id) {
    return ResponseEntity.ok(service.findById(id));
}
```

**동작 방식**:
1. 메서드 실행 전 인터셉트
2. 경로 변수에서 리소스 ID 추출
3. JWT에서 현재 사용자 ID 추출
4. Repository를 통해 리소스 소유자 확인
5. 불일치 시 `ForbiddenException` 발생

#### @Retryable

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Retryable {
    int maxAttempts() default 3;
    long delay() default 1000;
    double backoffMultiplier() default 1.5;
    boolean randomizeDelay() default true;
}

// 사용 예시
@Retryable(maxAttempts = 3, delay = 500)
@Transactional
public void updateWithRetry(UpdateRequest request) {
    // 낙관적 락 충돌 시 자동 재시도
}
```

### 로깅 구조

```
logs/
├── application.log     # 일반 애플리케이션 로그
├── security.log        # 인증/인가 관련 로그
├── audit.log           # 비즈니스 감사 로그
└── performance.log     # 성능 모니터링 로그
```

### 코드 예시

```java
@Aspect
@Component
@Slf4j
public class PerformanceMonitoringAspect {
    
    @Around("@within(org.springframework.stereotype.Service)")
    public Object measureTime(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return pjp.proceed();
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            String method = pjp.getSignature().toShortString();
            
            // Prometheus 메트릭 기록
            meterRegistry.timer("service.method.time", 
                "method", method).record(elapsed, TimeUnit.MILLISECONDS);
            
            // 느린 쿼리 경고 (100ms 이상)
            if (elapsed > 100) {
                log.warn("Slow method: {} took {}ms", method, elapsed);
            }
        }
    }
}
```

## 결과

### 장점
- ✅ **선언적 프로그래밍**: 한 줄 어노테이션으로 복잡한 로직 적용
- ✅ **DRY 원칙**: 횡단 관심사 코드 중복 제거
- ✅ **관심사 분리**: 비즈니스 로직과 부가 기능 분리
- ✅ **유연성**: 포인트컷 조정으로 적용 범위 쉽게 변경
- ✅ **테스트 용이**: Aspect 단위 테스트 가능

### 단점
- ⚠️ **디버깅 복잡**: 실행 흐름 추적 어려움
- ⚠️ **학습 곡선**: AOP 개념 이해 필요
- ⚠️ **성능 오버헤드**: 프록시 생성 및 메서드 인터셉션 비용
- ⚠️ **Self-invocation 문제**: 같은 클래스 내 호출 시 AOP 미적용

### Self-invocation 해결책

```java
// 문제: 같은 클래스 내 호출 시 AOP 미적용
@Service
public class MyService {
    public void methodA() {
        methodB(); // AOP 미적용!
    }
    
    @Cacheable("cache")
    public void methodB() { }
}

// 해결: 자기 주입 또는 별도 서비스 분리
@Service
@RequiredArgsConstructor
public class MyService {
    private final MyService self; // 프록시 주입
    
    public void methodA() {
        self.methodB(); // AOP 적용됨
    }
}
```

### 성능 영향
- AOP 프록시 오버헤드: 메서드당 ~0.1ms
- 전체 요청 영향: 무시할 수 있는 수준

---

## 트레이드오프 분석

### @RequireResourceOwnership vs Spring Security @PreAuthorize

| 기준 | @RequireResourceOwnership (현재) | @PreAuthorize (Spring Security) |
|------|----------------------------------|----------------------------------|
| **가독성** | ✅ 의도가 명확 | ⚠️ SpEL 표현식 복잡 |
| **유연성** | ✅ 커스텀 로직 자유 | ⚠️ SpEL 제한적 |
| **생태계 호환** | ⚠️ 자체 구현 | ✅ Spring Security 표준 |
| **학습 곡선** | ⚠️ 내부 구현 이해 필요 | ✅ 문서/레퍼런스 풍부 |
| **테스트** | ⚠️ 별도 테스트 구성 | ✅ WithMockUser 등 지원 |

**현재 방식 예시:**
```java
@RequireResourceOwnership(resourceType = "MEDICATION_SCHEDULE", idParam = "id")
public ResponseEntity<ScheduleResponse> getById(@PathVariable Long id)
```

**Spring Security 대안:**
```java
@PreAuthorize("@ownershipChecker.isOwner(#id, 'MEDICATION_SCHEDULE')")
public ResponseEntity<ScheduleResponse> getById(@PathVariable Long id)
```

**Hamalog에서 커스텀 어노테이션을 선택한 이유:**
1. **명확한 의도 표현** - `resourceType`과 `idParam`이 자체 문서화
2. **복잡한 소유권 로직** - 다중 전략(DIRECT, VIA_SCHEDULE 등) 지원 필요
3. **학습 목적** - AOP Aspect 구현 경험

### 과잉 엔지니어링 인정 및 대응

> ⚠️ **소규모 프로젝트에서 커스텀 AOP는 과잉일 수 있습니다.**

**더 단순한 대안들:**

1. **서비스에서 직접 검증 (가장 단순)**
   ```java
   public ScheduleResponse getById(Long id) {
       var schedule = repository.findById(id).orElseThrow();
       if (!schedule.getMember().getLoginId().equals(getCurrentLoginId())) {
           throw ErrorCode.FORBIDDEN.toException();
       }
       return ScheduleResponse.from(schedule);
   }
   ```

2. **Spring Security @PreAuthorize**
   ```java
   @PreAuthorize("@scheduleService.isOwner(#id)")
   public ScheduleResponse getById(Long id) { ... }
   ```

3. **JPA Specification/Querydsl로 필터링**
   ```java
   repository.findByIdAndMember_LoginId(id, loginId);  // 없으면 404
   ```

**Hamalog에서 AOP를 유지하는 이유:**
1. 20개+ 엔드포인트에 일관된 소유권 검증 필요
2. 검증 로직 변경 시 한 곳만 수정
3. 실무에서 자주 사용되는 패턴 학습

### 규모별 권장 방식

| 규모 | 권장 방식 | 이유 |
|------|-----------|------|
| **MVP** (~10 엔드포인트) | 서비스 직접 검증 | 단순함, 명시적 |
| **성장** (10~50 엔드포인트) | @PreAuthorize | 표준, 생태계 지원 |
| **스케일업** (50+ 엔드포인트) | 커스텀 AOP | 복잡한 정책, 감사 로깅 |

### 만약 다시 선택한다면?

**@PreAuthorize로 시작하고**, 복잡도가 증가하면 커스텀 AOP로 마이그레이션하는 것을 권장합니다.

```java
// 1단계: @PreAuthorize로 시작
@PreAuthorize("@auth.canAccess(#id, 'SCHEDULE')")

// 2단계: 복잡해지면 커스텀 어노테이션으로 전환
@RequireResourceOwnership(resourceType = "SCHEDULE", strategy = "VIA_SCHEDULE")
```

## 참고

- [Spring AOP Documentation](https://docs.spring.io/spring-framework/reference/core/aop.html)
- [AspectJ @Around Advice](https://www.eclipse.org/aspectj/doc/released/progguide/semantics-advice.html)

---

> 작성일: 2025-12-23

