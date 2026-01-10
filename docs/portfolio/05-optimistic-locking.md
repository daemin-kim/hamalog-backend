# 05. 낙관적 락 동시성 제어

> **@Version 기반 낙관적 락으로 동시 수정 충돌을 감지하고, 글로벌 예외 핸들러로 일관된 응답을 제공하는 패턴**

---

## 📋 목차

1. [문제 상황](#1-문제-상황-problem)
2. [해결 전략](#2-해결-전략-solution-strategy)
3. [구현 상세](#3-구현-상세-implementation)
4. [효과 및 검증](#4-효과-및-검증-results)
5. [면접 대비 Q&A](#5-면접-대비-qa)

---

## 1. 문제 상황 (Problem)

### 1.1 동시성 문제란?

여러 사용자(또는 디바이스)가 동시에 같은 데이터를 수정할 때 발생하는 문제입니다.

```
┌─────────────────────────────────────────────────────────────────┐
│                    Lost Update 시나리오                          │
│                                                                  │
│  시간    사용자 A (모바일)        사용자 A (웹)          DB       │
│  ────────────────────────────────────────────────────────────── │
│  T1      스케줄 조회 ─────────────────────────────→ name="약A"  │
│  T2      ───────────────────── 스케줄 조회 ───────→ name="약A"  │
│  T3      name="아침약"으로 수정 ────────────────→               │
│  T4      ───────────────────── name="저녁약"으로 수정 ─────────→│
│  T5                                                   name="저녁약"│
│                                                                  │
│  결과: 사용자 A의 "아침약" 수정이 유실됨! (Lost Update)          │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2 Hamalog에서의 실제 시나리오

| 시나리오 | 위험도 | 영향 |
|----------|-------|------|
| **복약 스케줄 수정** | 🔴 높음 | 복용 시간/용량 정보 유실 |
| **복약 기록 중복 저장** | 🟡 중간 | 이행률 통계 왜곡 |
| **알림 설정 변경** | 🟡 중간 | 알림 누락/중복 |
| **마음 일기 동시 저장** | 🟡 중간 | 내용 유실 |

### 1.3 문제가 발생하는 환경

```
┌─────────────────────────────────────────────────────────────────┐
│                    다중 디바이스 환경                            │
│                                                                  │
│  ┌────────────┐    ┌────────────┐    ┌────────────┐             │
│  │  모바일 앱  │    │    웹     │    │  태블릿   │             │
│  │  (Android) │    │ (Chrome)  │    │  (iPad)   │             │
│  └─────┬──────┘    └─────┬──────┘    └─────┬──────┘             │
│        │                 │                 │                    │
│        └────────────────┼─────────────────┘                    │
│                         │                                       │
│                         ▼                                       │
│              ┌────────────────────┐                             │
│              │   Hamalog Server   │                             │
│              │   (동시 요청 처리)  │                             │
│              └────────────────────┘                             │
│                                                                  │
│  같은 사용자가 여러 디바이스에서 동시에 수정 가능!               │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. 해결 전략 (Solution Strategy)

### 2.1 고려한 대안들

| 방식 | 장점 | 단점 | 채택 여부 |
|------|------|------|----------|
| **비관적 락 (Pessimistic)** | 충돌 완전 방지 | DB 락 경합, 성능 저하, 데드락 위험 | ❌ |
| **낙관적 락 (Optimistic)** | 성능 우수, 구현 단순 | 충돌 시 재시도 필요 | ✅ |
| **분산 락 (Distributed)** | 분산 환경 지원 | 인프라 복잡, Redis 의존 | ❌ (과도함) |
| **CQRS** | 읽기/쓰기 분리 | 아키텍처 복잡 | ❌ (과도함) |

### 2.2 최종 선택: 낙관적 락

```
┌─────────────────────────────────────────────────────────────────┐
│                    낙관적 락 동작 원리                           │
│                                                                  │
│  1. 엔티티에 version 필드 추가                                   │
│  2. 조회 시 version 값도 함께 조회                               │
│  3. 수정 시 version 조건을 WHERE에 추가                          │
│  4. UPDATE된 row가 0이면 → 충돌 발생 → 예외 던짐                 │
│                                                                  │
│  SQL 예시:                                                       │
│  UPDATE medication_schedule                                     │
│  SET name = '아침약', version = 2                               │
│  WHERE id = 1 AND version = 1;  ← 충돌 감지 조건                │
│                                                                  │
│  → affected rows = 0이면 OptimisticLockException 발생           │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 2.3 왜 낙관적 락인가?

1. **충돌 빈도가 낮음**: 같은 사용자가 동시에 수정하는 경우는 드뭄
2. **성능**: DB 락을 잡지 않아 처리량 우수
3. **구현 단순**: `@Version` 어노테이션 하나로 적용
4. **JPA 표준**: 벤더 독립적, Hibernate가 자동 처리

---

## 3. 구현 상세 (Implementation)

### 3.1 Entity에 @Version 필드 추가

```java
/**
 * 복약 스케줄 엔티티
 * 
 * @Version: JPA 낙관적 락 지원
 * - 엔티티 조회 시 version 값 함께 조회
 * - 엔티티 수정 시 version 조건으로 UPDATE
 * - 충돌 시 OptimisticLockException 발생
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MedicationSchedule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "medication_schedule_id")
    private Long medicationScheduleId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
    
    @Column(length = 20, nullable = false)
    private String name;
    
    @Column(name = "hospital_name", length = 20, nullable = false)
    private String hospitalName;
    
    // ... 기타 필드들
    
    // ============================================================
    // @Version: 낙관적 락 핵심
    // ============================================================
    
    /**
     * 버전 필드 (낙관적 락)
     * 
     * - 타입: Long, Integer, Short, Timestamp 가능
     * - 초기값: JPA가 자동으로 0 또는 null에서 시작
     * - 증가: UPDATE 시 자동으로 +1
     * 
     * 주의:
     * - 애플리케이션에서 직접 수정하면 안 됨
     * - @Setter 없이 getter만 제공
     */
    @Version
    @Column(name = "version")
    private Long version;
    
    // ============================================================
    // 수정 메서드 (Dirty Checking 활용)
    // ============================================================
    
    /**
     * 스케줄 정보 수정
     * 
     * JPA Dirty Checking:
     * 1. 영속 상태의 엔티티 필드 변경
     * 2. 트랜잭션 커밋 시 자동 UPDATE 쿼리 생성
     * 3. @Version이 있으면 version 조건 자동 추가
     */
    public void update(String name, String hospitalName, LocalDate prescriptionDate, 
                       String memo, LocalDate startOfAd, Integer prescriptionDays, 
                       Integer perDay, AlarmType alarmType) {
        this.name = name;
        this.hospitalName = hospitalName;
        this.prescriptionDate = prescriptionDate;
        this.memo = memo;
        this.startOfAd = startOfAd;
        this.prescriptionDays = prescriptionDays;
        this.perDay = perDay;
        this.alarmType = alarmType;
        // version은 건드리지 않음! JPA가 자동 증가
    }
}
```

### 3.2 실제 생성되는 SQL

```sql
-- 조회 시: version도 함께 조회
SELECT 
    medication_schedule_id, member_id, name, hospital_name, 
    prescription_date, memo, start_of_ad, prescription_days, 
    per_day, alarm_type, is_active, version  -- version 포함
FROM medication_schedule 
WHERE medication_schedule_id = 1;

-- 수정 시: version 조건 추가 + version 증가
UPDATE medication_schedule 
SET 
    name = '아침약',
    hospital_name = '서울병원',
    -- ... 기타 필드들
    version = 2              -- 증가된 버전
WHERE 
    medication_schedule_id = 1 
    AND version = 1;         -- 현재 버전 조건 (핵심!)

-- affected rows = 0이면:
-- → 다른 트랜잭션이 먼저 수정함
-- → OptimisticLockException 발생
```

### 3.3 글로벌 예외 핸들러 (GlobalExceptionHandler.java)

```java
/**
 * 전역 예외 처리기
 * 
 * @RestControllerAdvice: 모든 Controller의 예외를 처리
 * 낙관적 락 충돌 시 일관된 응답 형식 제공
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    // ============================================================
    // 동시성/데이터 예외 처리
    // ============================================================
    
    /**
     * 낙관적 락 충돌 처리
     * 
     * 발생 상황:
     * - 두 트랜잭션이 같은 엔티티를 동시에 수정
     * - 먼저 커밋한 트랜잭션이 version을 증가시킴
     * - 나중 트랜잭션의 UPDATE가 영향받은 row 0
     * - Hibernate가 OptimisticLockException 던짐
     * 
     * 두 가지 예외 타입:
     * 1. OptimisticLockException: JPA 표준 예외
     * 2. OptimisticLockingFailureException: Spring Data 예외
     */
    @ExceptionHandler({
        OptimisticLockException.class, 
        OptimisticLockingFailureException.class
    })
    public ResponseEntity<ErrorResponse> handleOptimisticLock(
            Exception ex, 
            HttpServletRequest request
    ) {
        // 로깅: 충돌 발생 기록 (디버깅 및 모니터링용)
        log.warn("[OPTIMISTIC_LOCK] path={} | message={}", 
                request.getRequestURI(), ex.getMessage());
        
        // 409 Conflict 응답
        // 클라이언트에게 "충돌 발생, 다시 시도하라"는 의미
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(
                ErrorCode.OPTIMISTIC_LOCK_FAILED.getCode(),   // "C001"
                ErrorCode.OPTIMISTIC_LOCK_FAILED.getMessage() // "다른 사용자가 먼저 수정했습니다. 새로고침 후 다시 시도해주세요."
            ));
    }
    
    /**
     * 데이터 무결성 위반 처리
     * 
     * 발생 상황:
     * - UNIQUE 제약 조건 위반
     * - FK 제약 조건 위반
     * - NOT NULL 제약 조건 위반
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, 
            HttpServletRequest request
    ) {
        log.warn("[DATA_INTEGRITY] path={} | message={}", 
                request.getRequestURI(), ex.getMessage());
        
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(
                ErrorCode.RESOURCE_CONFLICT.getCode(),
                ErrorCode.RESOURCE_CONFLICT.getMessage()
            ));
    }
}
```

### 3.4 ErrorCode 정의

```java
/**
 * 에러 코드 열거형
 * 
 * 모든 에러 응답에서 일관된 코드 체계 사용
 */
public enum ErrorCode {
    
    // ============================================================
    // 동시성 관련 에러 (C: Concurrency)
    // ============================================================
    
    /**
     * 낙관적 락 충돌
     * 
     * HTTP 409 Conflict:
     * - 클라이언트의 요청이 서버의 현재 상태와 충돌
     * - 재시도가 가능함을 암시
     */
    OPTIMISTIC_LOCK_FAILED(
        HttpStatus.CONFLICT, 
        "C001", 
        "다른 사용자가 먼저 수정했습니다. 새로고침 후 다시 시도해주세요."
    ),
    
    /**
     * 리소스 충돌 (UNIQUE 위반 등)
     */
    RESOURCE_CONFLICT(
        HttpStatus.CONFLICT, 
        "C002", 
        "리소스 충돌이 발생했습니다."
    ),
    
    // ... 기타 에러 코드들
    
    private final HttpStatus status;
    private final String code;
    private final String message;
    
    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
    
    /**
     * CustomException 생성 헬퍼
     */
    public CustomException toException() {
        return new CustomException(this);
    }
}
```

### 3.5 Service에서의 사용

```java
@Service
@RequiredArgsConstructor
@Transactional
public class MedicationScheduleService {
    
    private final MedicationScheduleRepository scheduleRepository;
    
    /**
     * 스케줄 수정
     * 
     * 낙관적 락이 적용된 수정 흐름:
     * 1. findById로 엔티티 조회 (version 포함)
     * 2. 엔티티 필드 수정 (Dirty Checking)
     * 3. 트랜잭션 커밋 시 UPDATE 실행
     * 4. version 조건 불일치 → OptimisticLockException
     * 5. GlobalExceptionHandler가 409 응답 반환
     */
    public ScheduleResponse update(Long scheduleId, UpdateScheduleRequest request) {
        // 조회 (version 필드도 함께 조회됨)
        MedicationSchedule schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow(ErrorCode.SCHEDULE_NOT_FOUND::toException);
        
        // 수정 (Dirty Checking - version은 자동 증가)
        schedule.update(
            request.name(),
            request.hospitalName(),
            request.prescriptionDate(),
            request.memo(),
            request.startOfAd(),
            request.prescriptionDays(),
            request.perDay(),
            request.alarmType()
        );
        
        // 트랜잭션 커밋 시:
        // - UPDATE ... WHERE id = ? AND version = ?
        // - 충돌 시 OptimisticLockException 발생
        
        return ScheduleResponse.from(schedule);
    }
}
```

### 3.6 클라이언트 처리 가이드

```typescript
// 프론트엔드 (React/TypeScript 예시)

async function updateSchedule(id: number, data: UpdateRequest): Promise<Schedule> {
    try {
        const response = await api.put(`/medication-schedule/${id}`, data);
        return response.data;
    } catch (error) {
        if (error.response?.status === 409) {
            // 409 Conflict: 낙관적 락 충돌
            const retry = confirm(
                "다른 기기에서 이미 수정되었습니다.\n" +
                "최신 데이터를 불러올까요?"
            );
            
            if (retry) {
                // 최신 데이터 다시 조회
                const latestData = await fetchSchedule(id);
                // 사용자에게 최신 데이터 표시
                displaySchedule(latestData);
            }
        } else {
            throw error;
        }
    }
}
```

---

## 4. 효과 및 검증 (Results)

### 4.1 동시성 문제 해결

| 시나리오 | Before | After |
|----------|--------|-------|
| **두 디바이스 동시 수정** | 나중 수정이 덮어씀 (Lost Update) | 충돌 감지, 사용자에게 알림 |
| **같은 복약 기록 중복 저장** | 중복 저장됨 | UNIQUE 제약 + 예외 처리 |
| **트랜잭션 경합** | 예측 불가 | 명확한 충돌 감지 |

### 4.2 성능 특성

```
📊 낙관적 락 성능 (vs 비관적 락)

낙관적 락:
- 조회: 추가 비용 없음
- 수정: WHERE 조건 1개 추가 (무시할 수준)
- 락 대기: 없음

비관적 락:
- 조회: SELECT ... FOR UPDATE (락 획득)
- 수정: 락 해제 대기
- 동시성: 순차 처리로 처리량 저하

결론: 충돌이 드문 경우 낙관적 락이 월등히 우수
```

### 4.3 검증 테스트

```java
@Test
@DisplayName("동시 수정 시 낙관적 락 충돌 발생")
void concurrentUpdate_shouldThrowOptimisticLockException() throws Exception {
    // given: 스케줄 생성
    Long scheduleId = createSchedule("원본 약");
    
    // when: 두 스레드가 동시에 수정 시도
    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch latch = new CountDownLatch(2);
    AtomicInteger conflictCount = new AtomicInteger(0);
    
    Runnable updateTask = () -> {
        try {
            // 같은 스케줄을 다른 이름으로 수정
            scheduleService.update(scheduleId, 
                new UpdateRequest("수정된 약 " + Thread.currentThread().getName()));
        } catch (OptimisticLockException e) {
            conflictCount.incrementAndGet();
        } finally {
            latch.countDown();
        }
    };
    
    executor.submit(updateTask);
    executor.submit(updateTask);
    latch.await();
    
    // then: 하나는 성공, 하나는 충돌
    assertThat(conflictCount.get()).isEqualTo(1);
}

@Test
@DisplayName("409 Conflict 응답 형식 검증")
void optimisticLock_shouldReturn409WithErrorCode() throws Exception {
    // given: version 1인 스케줄
    Long scheduleId = createSchedule("테스트 약");
    
    // 다른 트랜잭션에서 먼저 수정 (version → 2)
    updateInAnotherTransaction(scheduleId);
    
    // when: 구버전으로 수정 시도
    mockMvc.perform(put("/medication-schedule/{id}", scheduleId)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\": \"충돌될 수정\"}"))
        
        // then: 409 Conflict + 에러 코드
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("C001"))
        .andExpect(jsonPath("$.message").exists());
}
```

---

## 5. 면접 대비 Q&A

### Q1. 낙관적 락과 비관적 락의 차이점은?

> **모범 답변**
> 
> | 구분 | 낙관적 락 | 비관적 락 |
> |------|----------|----------|
> | **철학** | "충돌은 드물다" 가정 | "충돌이 자주 발생한다" 가정 |
> | **락 시점** | 커밋 시점에 검증 | 조회 시점에 락 획득 |
> | **구현** | @Version 필드 | SELECT ... FOR UPDATE |
> | **충돌 시** | 예외 발생, 재시도 필요 | 대기 후 순차 처리 |
> | **성능** | 충돌 적으면 우수 | 충돌 많으면 안정적 |
> | **데드락** | 없음 | 가능성 있음 |
> 
> Hamalog에서 낙관적 락을 선택한 이유:
> 1. 같은 사용자의 동시 수정은 드문 케이스
> 2. 읽기가 쓰기보다 훨씬 많음
> 3. 데드락 관리 부담 없음

### Q2. @Version 필드에 Long과 Timestamp 중 무엇을 선택해야 하나요?

> **모범 답변**
> 
> | 타입 | 장점 | 단점 |
> |------|------|------|
> | **Long/Integer** | 간단, 예측 가능 | 단순 카운터 |
> | **Timestamp** | 시간 정보 포함 | 동시성 이슈 가능, 시계 동기화 필요 |
> 
> **Long을 권장**합니다:
> 
> 1. **단순성**: 1씩 증가하는 카운터, 이해하기 쉬움
> 2. **안전성**: Timestamp는 동일 밀리초에 두 수정이 있으면 문제
> 3. **디버깅**: "version 5"가 "5번 수정됨"을 의미
> 
> Timestamp가 필요한 경우는 "마지막 수정 시간"을 별도로 알고 싶을 때인데, 이는 `updatedAt` 필드로 분리하는 것이 좋습니다.

### Q3. 낙관적 락 충돌 시 자동 재시도를 구현하지 않은 이유는?

> **모범 답변**
> 
> **사용자에게 충돌을 알리는 것이 더 중요**하기 때문입니다.
> 
> 자동 재시도의 문제:
> 1. **데이터 유실 위험**: 사용자 A가 입력한 "아침 8시"를 "저녁 6시"로 덮어쓸 수 있음
> 2. **의도 확인 불가**: 시스템이 어떤 값이 "올바른"지 판단할 수 없음
> 3. **충돌 은폐**: 사용자가 충돌을 인지하지 못함
> 
> 올바른 접근:
> 1. 409 응답으로 충돌 알림
> 2. 클라이언트가 최신 데이터 표시
> 3. 사용자가 확인 후 다시 수정
> 
> 다만, **멱등성이 보장된 작업**(예: 같은 값으로 재설정)은 자동 재시도가 적합할 수 있습니다.

### Q4. 읽기 작업에서는 낙관적 락이 동작하나요?

> **모범 답변**
> 
> **읽기만 하는 경우에는 동작하지 않습니다.**
> 
> 낙관적 락은 UPDATE/DELETE 시에만 version 조건을 검사합니다.
> 
> ```sql
> -- 읽기: version 조건 없음
> SELECT * FROM schedule WHERE id = 1;
> 
> -- 쓰기: version 조건 있음
> UPDATE schedule SET name = ? WHERE id = 1 AND version = 1;
> ```
> 
> "읽기 중에 다른 트랜잭션이 수정했는지"를 알고 싶다면:
> - 읽기 시작 시점의 version 저장
> - 수정 전에 다시 조회하여 version 비교
> - 또는 JPQL의 `OPTIMISTIC_FORCE_INCREMENT` 사용

### Q5. @Version 필드를 직접 수정하면 어떻게 되나요?

> **모범 답변**
> 
> **하면 안 됩니다.** JPA의 버전 관리가 깨집니다.
> 
> ```java
> // 잘못된 예
> schedule.setVersion(schedule.getVersion() + 1);  // ❌ 절대 금지
> ```
> 
> 이렇게 하면:
> 1. JPA가 version을 다시 증가시킴 (+2가 됨)
> 2. 충돌 감지 로직이 올바르게 동작하지 않음
> 3. 예상치 못한 OptimisticLockException 발생 가능
> 
> **해결책**: `@Version` 필드에는 setter를 제공하지 않습니다.
> ```java
> @Version
> @Getter  // getter만
> private Long version;
> ```

### Q6. 엔티티가 삭제될 때도 낙관적 락이 적용되나요?

> **모범 답변**
> 
> **네, DELETE에도 적용됩니다.**
> 
> ```sql
> -- JPA가 생성하는 DELETE
> DELETE FROM schedule WHERE id = 1 AND version = 5;
> ```
> 
> 시나리오:
> 1. 사용자 A가 스케줄(version=5) 조회
> 2. 사용자 B가 스케줄 수정 (version→6)
> 3. 사용자 A가 삭제 시도 → version=5 조건 불일치 → 예외
> 
> 이는 "다른 사람이 수정한 내용을 실수로 삭제"하는 것을 방지합니다.

### Q7. 벌크 연산(JPQL UPDATE/DELETE)에서는 낙관적 락이 동작하나요?

> **모범 답변**
> 
> **기본적으로 동작하지 않습니다.**
> 
> ```java
> // 벌크 연산: 영속성 컨텍스트를 거치지 않음
> @Modifying
> @Query("UPDATE Schedule s SET s.isActive = false WHERE s.member.id = :memberId")
> int deactivateAll(@Param("memberId") Long memberId);
> ```
> 
> 벌크 연산은 영속성 컨텍스트를 우회하므로:
> - @Version 조건이 추가되지 않음
> - version도 증가하지 않음
> 
> **해결책**:
> 1. 쿼리에 직접 version 조건 추가
>    ```sql
>    UPDATE Schedule s SET s.isActive = false, s.version = s.version + 1 
>    WHERE s.member.id = :memberId
>    ```
> 2. 벌크 연산 후 영속성 컨텍스트 초기화
>    ```java
>    entityManager.clear();
>    ```

### Q8. 연관된 엔티티가 수정되면 부모의 version이 증가하나요?

> **모범 답변**
> 
> **아니오, 자동으로는 증가하지 않습니다.**
> 
> ```java
> // Schedule의 version은 변경되지 않음
> MedicationTime time = schedule.getMedicationTimes().get(0);
> time.setTime(LocalTime.of(9, 0));  // 자식만 수정
> ```
> 
> 자식 엔티티 수정 시 부모 version을 증가시키려면:
> 
> 1. **@OptimisticLocking(cascade = true)** (Hibernate 전용)
>    ```java
>    @OptimisticLocking(cascade = true)
>    public class Schedule { ... }
>    ```
> 
> 2. **명시적 터치**
>    ```java
>    public void touchSchedule() {
>        entityManager.lock(schedule, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
>    }
>    ```
> 
> Hamalog에서는 각 엔티티가 독립적인 version을 가지며, 필요 시 명시적 터치를 사용합니다.

### Q9. 낙관적 락과 데이터베이스 격리 수준의 관계는?

> **모범 답변**
> 
> **낙관적 락은 격리 수준과 별개로 동작합니다.**
> 
> | 격리 수준 | 해결하는 문제 | 낙관적 락 역할 |
> |----------|--------------|---------------|
> | READ_COMMITTED | Dirty Read 방지 | 보완 (Lost Update 방지) |
> | REPEATABLE_READ | Non-repeatable Read 방지 | 보완 |
> | SERIALIZABLE | 모든 이상현상 방지 | 불필요 (하지만 성능 문제) |
> 
> MySQL InnoDB의 기본 격리 수준은 REPEATABLE_READ이지만, Lost Update는 여전히 발생할 수 있습니다. 낙관적 락이 이를 보완합니다.
> 
> SERIALIZABLE을 쓰면 낙관적 락이 불필요하지만, 성능이 크게 저하되어 실무에서 거의 사용하지 않습니다.

### Q10. 프론트엔드에서 version을 관리해야 하나요?

> **모범 답변**
> 
> **구현 방식에 따라 다릅니다.**
> 
> **방식 1: 서버에서 전체 관리 (현재 Hamalog)**
> - 조회 → 수정 요청 사이에 서버가 version 확인
> - 프론트엔드는 version을 몰라도 됨
> - 단, 사용자 확인 없이 덮어쓰기 가능 (UX 고려 필요)
> 
> **방식 2: 프론트엔드가 version 포함 (권장)**
> ```typescript
> // 조회 응답에 version 포함
> { "id": 1, "name": "약A", "version": 5 }
> 
> // 수정 요청에 version 포함
> PUT /schedules/1
> { "name": "약B", "version": 5 }
> ```
> - If-Match 헤더로 ETag 사용도 가능
> - 더 명시적이고 HTTP 표준에 부합
> 
> Hamalog는 현재 서버 관리 방식이며, 향후 ETag 기반으로 개선 계획이 있습니다.

---

## 📎 관련 문서

- [GlobalExceptionHandler.java](../../src/main/java/com/Hamalog/handler/GlobalExceptionHandler.java)
- [MedicationSchedule.java](../../src/main/java/com/Hamalog/domain/medication/MedicationSchedule.java)
- [ErrorCode.java](../../src/main/java/com/Hamalog/exception/ErrorCode.java)

