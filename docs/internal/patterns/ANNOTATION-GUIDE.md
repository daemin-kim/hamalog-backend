# 🏷️ Hamalog 커스텀 어노테이션 가이드

> 이 문서는 Hamalog 프로젝트에서 사용하는 커스텀 어노테이션의 사용법을 설명합니다.
> AI 코드 생성 시 이 문서를 참조하여 선언적 패턴을 적용할 수 있습니다.

---

## 📋 목차

1. [@RequireResourceOwnership](#1-requireresourceownership)
2. [@Retryable](#2-retryable)
3. [@Cacheable / @CacheEvict](#3-cacheable--cacheevict)
4. [로깅 어노테이션](#4-로깅-어노테이션)

---

## 1. @RequireResourceOwnership

### 1.1 개요

리소스 소유권을 AOP로 검증하는 어노테이션입니다. 메서드 실행 전에 현재 인증된 사용자가 해당 리소스의 소유자인지 자동으로 검증합니다.

### 1.2 위치

```
src/main/java/com/Hamalog/security/annotation/RequireResourceOwnership.java
```

### 1.3 지원 리소스 타입

| ResourceType | 설명 | 검증 방식 |
|--------------|------|-----------|
| `MEDICATION_RECORD` | 복약 기록 | 기록 → 스케줄 → 회원 |
| `MEDICATION_SCHEDULE` | 복약 스케줄 | 스케줄 → 회원 |
| `MEDICATION_SCHEDULE_BY_MEMBER` | 회원의 복약 스케줄 | 회원 ID 직접 비교 |
| `MEMBER` | 회원 | 회원 ID 직접 비교 |
| `MOOD_DIARY` | 마음 일기 | 일기 → 회원 |
| `MOOD_DIARY_BY_MEMBER` | 회원의 마음 일기 | 회원 ID 직접 비교 |

### 1.4 사용법

#### 기본 사용

```java
@GetMapping("/{id}")
@RequireResourceOwnership(
    resourceType = ResourceType.MEDICATION_SCHEDULE,
    paramName = "id"
)
public ResponseEntity<MedicationScheduleResponse> getById(@PathVariable Long id) {
    return ResponseEntity.ok(medicationScheduleService.findById(id));
}
```

#### 회원 ID 기반 검증

```java
@GetMapping("/member/{memberId}")
@RequireResourceOwnership(
    resourceType = ResourceType.MEDICATION_SCHEDULE_BY_MEMBER,
    paramName = "memberId"
)
public ResponseEntity<List<MedicationScheduleResponse>> getByMemberId(
    @PathVariable Long memberId
) {
    return ResponseEntity.ok(medicationScheduleService.findByMemberId(memberId));
}
```

#### 서비스 레이어에서 사용

```java
@Service
public class SideEffectService {

    @RequireResourceOwnership(
        resourceType = ResourceType.MEDICATION_SCHEDULE,
        paramName = "medicationScheduleId",
        parameterSource = ParameterSource.REQUEST_BODY
    )
    @Transactional(readOnly = true)
    public List<SideEffectResponse> findByScheduleId(Long medicationScheduleId) {
        // 소유권 검증이 AOP에서 자동으로 수행됨
        return sideEffectRepository.findByMedicationScheduleId(medicationScheduleId)
            .stream()
            .map(SideEffectResponse::from)
            .toList();
    }
}
```

### 1.5 속성

| 속성 | 타입 | 필수 | 기본값 | 설명 |
|------|------|------|--------|------|
| `resourceType` | ResourceType | ✅ | - | 검증할 리소스 타입 |
| `paramName` | String | ✅ | - | 리소스 ID를 가져올 파라미터명 |
| `parameterSource` | ParameterSource | ❌ | PATH_VARIABLE | 파라미터 추출 전략 |
| `ownershipStrategy` | OwnershipStrategy | ❌ | DIRECT | 소유권 검증 전략 |

### 1.6 동작 원리

```
1. 메서드 호출 전 AOP Aspect 실행
2. SecurityContext에서 현재 인증된 사용자 ID 추출
3. paramName으로 지정된 파라미터에서 리소스 ID 추출
4. resourceType에 따라 DB에서 리소스 조회
5. 리소스 소유자와 현재 사용자 비교
6. 불일치 시 ErrorCode.FORBIDDEN 예외 발생
7. 일치 시 원래 메서드 실행
```

### 1.7 주의사항

- ⚠️ 인증되지 않은 요청에서는 동작하지 않음 (Spring Security 필터에서 먼저 차단)
- ⚠️ 리소스가 존재하지 않으면 NOT_FOUND 예외 발생
- ⚠️ 테스트 시 `@WithMockUser` 또는 `SecurityContext` 설정 필요

---

## 2. @Retryable

### 2.1 개요

메서드 실행 중 예외 발생 시 자동으로 재시도하는 어노테이션입니다. 낙관적 락 충돌, 일시적인 네트워크 오류 등에 유용합니다.

### 2.2 위치

```
src/main/java/com/Hamalog/aop/RetryAspect.java (내부 @Retryable 정의)
```

### 2.3 사용법

#### 기본 사용

```java
@Retryable
@Transactional
public void updateWithRetry(Long id, UpdateRequest request) {
    // 낙관적 락 충돌 시 자동 재시도 (기본 3회)
    Entity entity = repository.findById(id)
        .orElseThrow(ErrorCode.NOT_FOUND::toException);
    entity.update(request);
}
```

#### 상세 설정

```java
@Retryable(
    maxAttempts = 5,
    delay = 500,
    backoffMultiplier = 2.0,
    retryFor = { OptimisticLockException.class, DataAccessException.class }
)
@Transactional
public void updateCriticalData(Long id, UpdateRequest request) {
    // 최대 5회 재시도, 500ms → 1000ms → 2000ms... 지수 백오프
}
```

### 2.4 속성

| 속성 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `maxAttempts` | int | 3 | 최대 재시도 횟수 |
| `delay` | long | 1000 | 재시도 간격 (ms) |
| `backoffMultiplier` | double | 1.5 | 지수 백오프 승수 |
| `randomizeDelay` | boolean | false | 지연 시간 랜덤화 |
| `retryFor` | Class[] | Exception.class | 재시도할 예외 타입 |

### 2.5 로깅

재시도 발생 시 자동으로 로깅됩니다:

```
RETRY_ATTEMPT: a1b2c3d4 | Method: updateWithRetry | Attempt: 2/3 | Delay: 1500ms
RETRY_SUCCESS: a1b2c3d4 | Method: updateWithRetry | Succeeded on attempt: 2/3
```

### 2.6 설정

`application.yml`에서 기본값 설정 가능:

```yaml
app:
  aop:
    retry:
      enabled: true
  retry:
    default-attempts: 3
    default-delay: 1000
    max-delay: 10000
```

---

## 3. @Cacheable / @CacheEvict

### 3.1 개요

Spring Cache + Redis를 사용한 캐싱 어노테이션입니다. 메서드 결과를 캐시하거나 캐시를 무효화합니다.

### 3.2 사용법

#### 캐시 저장

```java
@Cacheable(value = "memberProfile", key = "#memberId", ttl = 3600)
public MemberProfileResponse getProfile(Long memberId) {
    return memberRepository.findById(memberId)
        .map(MemberProfileResponse::from)
        .orElseThrow(ErrorCode.MEMBER_NOT_FOUND::toException);
}
```

#### 캐시 무효화

```java
@CacheEvict(value = "memberProfile", key = "#memberId")
@Transactional
public MemberProfileResponse updateProfile(Long memberId, ProfileUpdateRequest request) {
    Member member = memberRepository.findById(memberId)
        .orElseThrow(ErrorCode.MEMBER_NOT_FOUND::toException);
    member.updateProfile(request);
    return MemberProfileResponse.from(member);
}
```

#### 다중 키 캐시

```java
@Cacheable(value = "medicationStats", key = "'member:' + #memberId + ':year:' + #year")
public MedicationStatsResponse getYearlyStats(Long memberId, int year) {
    // ...
}
```

### 3.3 캐시 네이밍 규칙

| 캐시명 | 용도 | TTL |
|--------|------|-----|
| `memberProfile` | 회원 프로필 | 1시간 |
| `memberCache` | 회원 정보 (인증용) | 5분 |
| `medicationStats` | 복약 통계 | 30분 |
| `diaryStats` | 일기 통계 | 30분 |

### 3.4 주의사항

- ⚠️ 캐시 키는 고유해야 함 (memberId + 조건 조합)
- ⚠️ 데이터 변경 시 반드시 `@CacheEvict` 적용
- ⚠️ 복잡한 객체 캐싱 시 직렬화 고려

---

## 4. 로깅 어노테이션

### 4.1 개요

AOP 기반 자동 로깅 어노테이션입니다. 메서드 진입/종료, 실행 시간, 파라미터 등을 자동으로 로깅합니다.

### 4.2 종류

| 어노테이션 | 위치 | 로깅 내용 |
|------------|------|-----------|
| `@ApiLogging` | Controller | 요청/응답, 상태코드, 실행시간 |
| `@ServiceLogging` | Service | 메서드 호출, 파라미터, 결과 |
| `@PerformanceLogging` | 모든 레이어 | 실행 시간 측정 |

### 4.3 사용법

대부분 자동 적용됩니다. 패키지 기반 AOP로 `controller.*`, `service.*` 패키지의 public 메서드에 적용됩니다.

명시적으로 끄려면:

```java
@NoLogging  // 민감한 데이터 처리 시
public LoginResponse login(LoginRequest request) {
    // 로깅 비활성화
}
```

---

## 5. 새 어노테이션 추가 가이드

### 5.1 어노테이션 정의

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyCustomAnnotation {
    String value() default "";
    int timeout() default 5000;
}
```

### 5.2 Aspect 구현

```java
@Slf4j
@Aspect
@Component
@Order(10)  // 실행 순서
public class MyCustomAspect {

    @Around("@annotation(myAnnotation)")
    public Object around(ProceedingJoinPoint joinPoint, MyCustomAnnotation myAnnotation) 
            throws Throwable {
        // Before logic
        log.info("Before: {}", myAnnotation.value());
        
        try {
            Object result = joinPoint.proceed();
            // After returning logic
            return result;
        } catch (Exception e) {
            // After throwing logic
            throw e;
        } finally {
            // After logic
        }
    }
}
```

### 5.3 테스트

```java
@SpringBootTest
class MyCustomAspectTest {

    @Autowired
    private TestService testService;

    @Test
    @DisplayName("커스텀 어노테이션이 정상 동작한다")
    void customAnnotationWorks() {
        // given & when
        testService.methodWithAnnotation();
        
        // then - 로그 확인 또는 동작 검증
    }
}
```

---

## 📚 관련 문서

- [에러 처리 패턴](./ERROR-HANDLING.md)
- [보안 패턴](./SECURITY-PATTERNS.md)
- [캐싱 패턴](./CACHING-PATTERNS.md)
- [코딩 컨벤션](../CODING-CONVENTIONS.md)

---

> 📝 최종 업데이트: 2025년 12월 24일

