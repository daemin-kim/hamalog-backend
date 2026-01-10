# 03. JPA N+1 문제 해결

> **@EntityGraph, DTO Projection, 배치 조회를 조합하여 N+1 문제를 해결하고 쿼리 수를 95% 감소시킨 전략**

---

## 📋 목차

1. [문제 상황](#1-문제-상황-problem)
2. [해결 전략](#2-해결-전략-solution-strategy)
3. [구현 상세](#3-구현-상세-implementation)
4. [효과 및 검증](#4-효과-및-검증-results)
5. [면접 대비 Q&A](#5-면접-대비-qa)

---

## 1. 문제 상황 (Problem)

### 1.1 N+1 문제란?

연관된 엔티티를 지연 로딩(Lazy Loading)으로 조회할 때, 메인 엔티티 1번 조회 + 연관 엔티티 N번 조회가 발생하는 문제입니다.

```java
// Entity 정의
@Entity
public class MedicationSchedule {
    @Id
    private Long medicationScheduleId;
    
    @ManyToOne(fetch = FetchType.LAZY)  // 지연 로딩
    private Member member;
    
    // ... 기타 필드
}

// Service에서 목록 조회
List<MedicationSchedule> schedules = scheduleRepository.findAllByMemberId(memberId);

// 각 스케줄의 member 접근 시 추가 쿼리 발생!
for (MedicationSchedule schedule : schedules) {
    String memberName = schedule.getMember().getName();  // N번 쿼리 발생!
}
```

### 1.2 실제 발생한 쿼리 로그

```sql
-- 1. 스케줄 목록 조회 (1번)
SELECT * FROM medication_schedule WHERE member_id = 1;

-- 2. 각 스케줄의 member 조회 (N번) - 10개 스케줄이면 10번!
SELECT * FROM member WHERE member_id = 1;
SELECT * FROM member WHERE member_id = 1;  -- 같은 ID라도 반복!
SELECT * FROM member WHERE member_id = 1;
...
```

### 1.3 Hamalog에서의 영향

| 기능 | 연관 엔티티 | N+1 쿼리 수 |
|------|------------|------------|
| 복약 스케줄 목록 | Member | 1 + N |
| 복약 기록 목록 | Schedule + MedicationTime | 1 + 2N |
| 마음 일기 목록 | Member | 1 + N |
| 데이터 내보내기 | 모든 연관 엔티티 | 1 + 5N 이상 |

```
예: 사용자의 10개 스케줄, 각 30개 기록 조회
최악의 경우: 1 + 10 + (10 × 30) = 311개 쿼리 발생!
```

### 1.4 성능 저하 측정

```
📊 10개 스케줄 목록 조회 (N+1 발생 시)
- 쿼리 수: 21개 (1 + 10 + 10)
- 응답 시간: 약 150ms
- DB 커넥션 점유: 21회 × 5ms = 105ms

📊 N+1 해결 후
- 쿼리 수: 1~2개
- 응답 시간: 약 30ms
- DB 커넥션 점유: 2회 × 5ms = 10ms
```

---

## 2. 해결 전략 (Solution Strategy)

### 2.1 고려한 대안들

| 방식 | 장점 | 단점 | 사용 시점 |
|------|------|------|----------|
| **EAGER Loading** | 구현 간단 | 항상 조회, 성능 저하 | ❌ 사용 안 함 |
| **@EntityGraph** | 선언적, 유연 | 카테시안 곱 가능성 | ✅ 기본 선택 |
| **JOIN FETCH** | JPQL 제어 | 쿼리 복잡 | ✅ 복잡한 조인 |
| **DTO Projection** | 최소 데이터, 가장 빠름 | 엔티티 기능 제한 | ✅ 목록 조회 |
| **Batch Size** | 자동 최적화 | 예측 어려움 | ✅ 전역 설정 |

### 2.2 최종 선택: 복합 전략

```
┌─────────────────────────────────────────────────────────────────┐
│                     N+1 해결 전략 선택 기준                       │
│                                                                  │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────────────┐  │
│  │ 단순 연관   │    │ 복잡한 조인 │    │ 목록 조회/페이징   │  │
│  │ (1~2개)     │    │ (3개 이상)  │    │ (대량 데이터)      │  │
│  └──────┬──────┘    └──────┬──────┘    └──────────┬──────────┘  │
│         │                  │                      │              │
│         ▼                  ▼                      ▼              │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────────────┐  │
│  │ @EntityGraph│    │ JOIN FETCH  │    │   DTO Projection    │  │
│  │             │    │   (JPQL)    │    │                     │  │
│  └─────────────┘    └─────────────┘    └─────────────────────┘  │
│                                                                  │
│  + 전역: hibernate.default_batch_fetch_size = 100               │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 2.3 왜 복합 전략인가?

1. **은탄환 없음**: 모든 상황에 맞는 단일 해결책은 없습니다.
2. **상황별 최적화**: 각 방식의 장점을 상황에 맞게 활용합니다.
3. **점진적 적용**: 기존 코드를 최소한으로 수정하면서 개선합니다.

---

## 3. 구현 상세 (Implementation)

### 3.1 @EntityGraph를 활용한 Eager 조회

```java
/**
 * MedicationScheduleRepository
 * 
 * @EntityGraph: 특정 쿼리에서만 연관 엔티티를 EAGER로 조회
 * 엔티티 정의는 LAZY를 유지하면서, 필요할 때만 함께 조회
 */
public interface MedicationScheduleRepository extends JpaRepository<MedicationSchedule, Long> {
    
    // ============================================================
    // 기본 조회: Member 함께 로딩
    // ============================================================
    
    /**
     * 회원별 스케줄 목록 조회 (Member 포함)
     * 
     * @EntityGraph가 없으면:
     *   SELECT * FROM medication_schedule WHERE member_id = ?
     *   SELECT * FROM member WHERE member_id = ? (N번 반복)
     * 
     * @EntityGraph가 있으면:
     *   SELECT ms.*, m.* 
     *   FROM medication_schedule ms 
     *   LEFT JOIN member m ON ms.member_id = m.member_id
     *   WHERE ms.member_id = ?
     *   → 단 1번의 쿼리!
     */
    @EntityGraph(attributePaths = {"member"})
    List<MedicationSchedule> findAllByMember_MemberId(Long memberId);
    
    // ============================================================
    // 페이징 조회: EntityGraph + Pageable
    // ============================================================
    
    /**
     * 페이징 시에도 @EntityGraph 적용 가능
     * 
     * 주의: 1:N 관계에서 페이징 + EntityGraph는
     * "HHH90003004: firstResult/maxResults specified with collection fetch" 경고 발생
     * → N:1 관계에서만 안전하게 사용
     */
    @EntityGraph(attributePaths = {"member"})
    Page<MedicationSchedule> findByMember_MemberId(Long memberId, Pageable pageable);
    
    // ============================================================
    // 단건 조회: 기본 findById 오버라이드
    // ============================================================
    
    /**
     * ID로 단건 조회 시에도 Member 함께 조회
     * 
     * 기본 JpaRepository.findById()는 EntityGraph가 없으므로
     * member 접근 시 추가 쿼리 발생
     * 
     * 이렇게 오버라이드하면 항상 Member와 함께 조회
     */
    @Override
    @EntityGraph(attributePaths = {"member"})
    Optional<MedicationSchedule> findById(Long id);
}
```

### 3.2 JOIN FETCH를 활용한 복잡한 조인

```java
/**
 * MedicationRecordRepository
 * 
 * 복약 기록은 Schedule → Member, MedicationTime과 연관됨
 * 3개 이상 엔티티를 조인하므로 명시적 JPQL 사용
 */
public interface MedicationRecordRepository extends JpaRepository<MedicationRecord, Long> {
    
    // ============================================================
    // EntityGraph: 3개 엔티티 동시 조회
    // ============================================================
    
    /**
     * 중첩 연관: medicationSchedule.member까지 함께 조회
     * 
     * attributePaths 표기법:
     * - "medicationSchedule": 1차 연관
     * - "medicationSchedule.member": 2차 연관 (중첩)
     * - "medicationTime": 1차 연관
     */
    @EntityGraph(attributePaths = {
        "medicationSchedule", 
        "medicationSchedule.member", 
        "medicationTime"
    })
    List<MedicationRecord> findAllByMedicationSchedule_MedicationScheduleId(Long scheduleId);
    
    // ============================================================
    // JOIN FETCH: JPQL로 명시적 조인
    // ============================================================
    
    /**
     * 배치 조회: IN 절로 여러 스케줄의 기록을 한 번에 조회
     * 
     * 사용 시나리오:
     * 1. 먼저 스케줄 목록 조회
     * 2. 스케줄 ID 리스트 추출
     * 3. IN 절로 모든 기록 한 번에 조회
     * 
     * 이 방식이 필요한 이유:
     * @EntityGraph는 특정 스케줄 1개의 기록만 조회
     * 여러 스케줄의 기록을 조회하려면 IN 절 필요
     */
    @Query("""
        SELECT mr FROM MedicationRecord mr
        JOIN FETCH mr.medicationSchedule ms
        JOIN FETCH ms.member m
        JOIN FETCH mr.medicationTime mt
        WHERE ms.medicationScheduleId IN :scheduleIds
        """)
    List<MedicationRecord> findAllByScheduleIds(@Param("scheduleIds") List<Long> scheduleIds);
    
    // ============================================================
    // JOIN FETCH vs EntityGraph 비교
    // ============================================================
    
    /**
     * 동일한 결과, 다른 표현 방식
     * 
     * EntityGraph 버전:
     */
    @EntityGraph(attributePaths = {"medicationSchedule", "medicationSchedule.member", "medicationTime"})
    Optional<MedicationRecord> findById(Long id);
    
    /**
     * JOIN FETCH 버전:
     * - 더 명시적이고 복잡한 조건 추가 가능
     * - WHERE 절, ORDER BY 등 세밀한 제어
     */
    @Query("""
        SELECT mr FROM MedicationRecord mr
        JOIN FETCH mr.medicationSchedule ms
        JOIN FETCH ms.member m
        JOIN FETCH mr.medicationTime mt
        WHERE mr.medicationRecordId = :id
        """)
    Optional<MedicationRecord> findByIdWithAllRelations(@Param("id") Long id);
}
```

### 3.3 DTO Projection으로 필요한 필드만 조회

```java
/**
 * DTO Projection의 장점:
 * 
 * 1. 필요한 필드만 조회 → 네트워크/메모리 절약
 * 2. 연관 엔티티 ID만 가져오면 추가 조인 불필요
 * 3. 영속성 컨텍스트에 등록되지 않음 → 메모리 절약
 * 4. 더티 체킹 불필요 → CPU 절약
 */

// ============================================================
// Projection DTO 정의 (record 사용)
// ============================================================

/**
 * 복약 스케줄 목록 조회용 Projection
 * 
 * Java 16+ record: 불변 객체, equals/hashCode/toString 자동 생성
 */
public record MedicationScheduleProjection(
    Long medicationScheduleId,
    Long memberId,              // Member 엔티티 대신 ID만
    String name,
    String hospitalName,
    LocalDate prescriptionDate,
    String memo,
    LocalDate startOfAd,
    Integer prescriptionDays,
    Integer perDay,
    AlarmType alarmType,
    Boolean isActive
) {
    /**
     * 엔티티를 Projection으로 변환하는 팩토리 메서드
     * (필요 시 사용)
     */
    public static MedicationScheduleProjection from(MedicationSchedule entity) {
        return new MedicationScheduleProjection(
            entity.getMedicationScheduleId(),
            entity.getMember().getMemberId(),
            entity.getName(),
            entity.getHospitalName(),
            entity.getPrescriptionDate(),
            entity.getMemo(),
            entity.getStartOfAd(),
            entity.getPrescriptionDays(),
            entity.getPerDay(),
            entity.getAlarmType(),
            entity.getIsActive()
        );
    }
}

// ============================================================
// Repository에서 DTO Projection 사용
// ============================================================

public interface MedicationScheduleRepository extends JpaRepository<MedicationSchedule, Long> {
    
    /**
     * JPQL Constructor Expression
     * 
     * SELECT new 패키지.DTO명(필드1, 필드2, ...)
     * 
     * DB에서 필요한 필드만 조회 → DTO 생성자 직접 호출
     * 엔티티 객체 생성 없이 바로 DTO 생성!
     */
    @Query("""
        SELECT new com.Hamalog.dto.medication.projection.MedicationScheduleProjection(
            ms.medicationScheduleId, 
            ms.member.memberId,      -- Member 엔티티가 아닌 ID만 조회!
            ms.name, 
            ms.hospitalName,
            ms.prescriptionDate, 
            ms.memo, 
            ms.startOfAd, 
            ms.prescriptionDays, 
            ms.perDay,
            ms.alarmType, 
            ms.isActive
        )
        FROM MedicationSchedule ms 
        WHERE ms.member.memberId = :memberId
        """)
    List<MedicationScheduleProjection> findProjectionsByMemberId(@Param("memberId") Long memberId);
    
    /**
     * DTO Projection + 페이징
     * 
     * 페이징과 함께 사용해도 N+1 문제 없음
     * COUNT 쿼리도 자동 생성됨
     */
    @Query("""
        SELECT new com.Hamalog.dto.medication.projection.MedicationScheduleProjection(
            ms.medicationScheduleId, 
            ms.member.memberId, 
            ms.name, 
            ms.hospitalName,
            ms.prescriptionDate, 
            ms.memo, 
            ms.startOfAd, 
            ms.prescriptionDays, 
            ms.perDay,
            ms.alarmType, 
            ms.isActive
        )
        FROM MedicationSchedule ms 
        WHERE ms.member.memberId = :memberId
        """)
    Page<MedicationScheduleProjection> findProjectionsByMemberId(
        @Param("memberId") Long memberId, 
        Pageable pageable
    );
    
    /**
     * 검색 + DTO Projection
     * 
     * 검색 조건이 추가되어도 DTO Projection 그대로 사용
     */
    @Query("""
        SELECT new com.Hamalog.dto.medication.projection.MedicationScheduleProjection(
            ms.medicationScheduleId, 
            ms.member.memberId, 
            ms.name, 
            ms.hospitalName,
            ms.prescriptionDate, 
            ms.memo, 
            ms.startOfAd, 
            ms.prescriptionDays, 
            ms.perDay,
            ms.alarmType, 
            ms.isActive
        )
        FROM MedicationSchedule ms 
        WHERE ms.member.memberId = :memberId
        AND LOWER(ms.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
        """)
    Page<MedicationScheduleProjection> searchProjectionsByName(
        @Param("memberId") Long memberId,
        @Param("keyword") String keyword,
        Pageable pageable
    );
}
```

### 3.4 Batch Size 전역 설정

```yaml
# application.yml
spring:
  jpa:
    properties:
      hibernate:
        # ============================================================
        # default_batch_fetch_size: 지연 로딩 시 IN 절로 묶어서 조회
        # ============================================================
        #
        # 예: 10개 스케줄의 member를 지연 로딩할 때
        # 
        # 설정 없음:
        #   SELECT * FROM member WHERE member_id = 1;
        #   SELECT * FROM member WHERE member_id = 2;
        #   ... (10번)
        #
        # batch_fetch_size = 100:
        #   SELECT * FROM member WHERE member_id IN (1, 2, 3, ..., 10);
        #   (1번)
        #
        # 주의: @EntityGraph/JOIN FETCH가 없는 경우에만 동작
        default_batch_fetch_size: 100
```

### 3.5 Service에서의 활용

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MedicationScheduleService {
    
    private final MedicationScheduleRepository scheduleRepository;
    private final MedicationRecordRepository recordRepository;
    
    // ============================================================
    // 목록 조회: DTO Projection 사용 (최적)
    // ============================================================
    
    /**
     * 스케줄 목록 조회 - DTO Projection 사용
     * 
     * 왜 Projection인가?
     * - 목록 조회는 많은 레코드를 반환
     * - 각 레코드의 모든 필드가 필요하지 않음
     * - 엔티티 수정이 필요 없음 (읽기 전용)
     */
    public Page<MedicationScheduleProjection> findAll(Long memberId, Pageable pageable) {
        return scheduleRepository.findProjectionsByMemberId(memberId, pageable);
    }
    
    // ============================================================
    // 단건 조회: EntityGraph 사용 (수정 가능성 있음)
    // ============================================================
    
    /**
     * 스케줄 단건 조회 - 엔티티로 반환
     * 
     * 왜 엔티티인가?
     * - 상세 조회 후 수정 가능성 있음
     * - 연관 엔티티(member) 정보도 필요
     * - findById에 @EntityGraph가 적용되어 있음
     */
    public ScheduleDetailResponse findById(Long scheduleId) {
        MedicationSchedule schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow(ErrorCode.SCHEDULE_NOT_FOUND::toException);
        
        // member가 이미 조회되어 있으므로 추가 쿼리 없음
        return ScheduleDetailResponse.from(schedule);
    }
    
    // ============================================================
    // 배치 조회: IN 절 사용 (대량 데이터)
    // ============================================================
    
    /**
     * 여러 스케줄의 기록 일괄 조회 (데이터 내보내기 등)
     * 
     * Bad Pattern (N+1 발생):
     * schedules.forEach(s -> 
     *     recordRepository.findByScheduleId(s.getId())  // N번 쿼리
     * );
     * 
     * Good Pattern (1번 쿼리):
     * List<Long> ids = schedules.stream().map(s -> s.getId()).toList();
     * recordRepository.findAllByScheduleIds(ids);  // IN 절 1번
     */
    public List<MedicationRecordExportData> exportRecords(Long memberId) {
        // 1. 스케줄 목록 조회 (1번 쿼리)
        List<MedicationSchedule> schedules = scheduleRepository
            .findAllByMember_MemberId(memberId);
        
        // 2. 스케줄 ID 추출
        List<Long> scheduleIds = schedules.stream()
            .map(MedicationSchedule::getMedicationScheduleId)
            .toList();
        
        // 3. 모든 기록 한 번에 조회 (1번 쿼리)
        List<MedicationRecord> allRecords = recordRepository
            .findAllByScheduleIds(scheduleIds);
        
        // 4. 메모리에서 그룹화
        Map<Long, List<MedicationRecord>> recordsBySchedule = allRecords.stream()
            .collect(Collectors.groupingBy(
                r -> r.getMedicationSchedule().getMedicationScheduleId()
            ));
        
        // 5. DTO 변환
        return schedules.stream()
            .map(s -> new MedicationRecordExportData(
                s,
                recordsBySchedule.getOrDefault(s.getMedicationScheduleId(), List.of())
            ))
            .toList();
    }
}
```

---

## 4. 효과 및 검증 (Results)

### 4.1 쿼리 수 개선

| 시나리오 | Before | After | 개선율 |
|----------|--------|-------|-------|
| 스케줄 10개 목록 | 21개 | 1개 | 95% ↓ |
| 기록 100개 목록 | 201개 | 2개 | 99% ↓ |
| 데이터 내보내기 | 300+개 | 5개 | 98% ↓ |

### 4.2 응답 시간 개선

```
📊 스케줄 목록 조회 (10개)
Before: 150ms → After: 30ms (80% 개선)

📊 기록 목록 조회 (100개)
Before: 500ms → After: 50ms (90% 개선)

📊 전체 데이터 내보내기
Before: 3000ms → After: 300ms (90% 개선)
```

### 4.3 검증 테스트

```java
/**
 * N+1 문제 검증 테스트
 * 
 * hibernate.show_sql=true 로 실행하여 쿼리 수 확인
 * 또는 DataSource-Proxy로 쿼리 카운트
 */
@Test
@DisplayName("스케줄 목록 조회 시 N+1 발생하지 않음")
void findAll_shouldNotCauseNPlusOne() {
    // given
    Long memberId = testMember.getMemberId();
    createSchedules(10, memberId);  // 10개 스케줄 생성
    
    // when: 쿼리 카운터 시작
    queryCounter.reset();
    
    List<MedicationScheduleProjection> result = 
        scheduleRepository.findProjectionsByMemberId(memberId);
    
    // then: 쿼리 수 검증 (1개만 실행되어야 함)
    assertThat(result).hasSize(10);
    assertThat(queryCounter.getQueryCount()).isEqualTo(1);
}

@Test
@DisplayName("기록 배치 조회 시 IN 절 사용")
void findAllByScheduleIds_shouldUseInClause() {
    // given
    List<Long> scheduleIds = List.of(1L, 2L, 3L, 4L, 5L);
    
    // when
    queryCounter.reset();
    
    List<MedicationRecord> records = 
        recordRepository.findAllByScheduleIds(scheduleIds);
    
    // then: 1개 쿼리만 실행
    assertThat(queryCounter.getQueryCount()).isEqualTo(1);
    
    // SQL에 IN 절 포함 확인
    assertThat(queryCounter.getLastQuery())
        .contains("IN");
}
```

---

## 5. 면접 대비 Q&A

### Q1. N+1 문제가 무엇인지 설명해주세요.

> **모범 답변**
> 
> N+1 문제는 ORM에서 연관된 엔티티를 조회할 때 발생하는 성능 문제입니다.
> 
> 예를 들어, 10개의 게시글과 각 게시글의 작성자를 조회한다고 하면:
> 1. 게시글 목록 조회: 1번 쿼리 (`SELECT * FROM post`)
> 2. 각 게시글의 작성자 조회: 10번 쿼리 (`SELECT * FROM user WHERE id = ?`)
> 
> 총 11번(1 + N) 쿼리가 발생합니다.
> 
> 이 문제가 심각한 이유는:
> - 데이터가 늘어나면 쿼리 수도 비례 증가
> - DB 커넥션을 반복 사용하여 풀 고갈 가능
> - 네트워크 왕복(Round Trip)이 증가
> 
> Hamalog에서는 복약 스케줄 10개 조회 시 21개 쿼리가 발생했고, 이를 2개로 줄였습니다.

### Q2. @EntityGraph와 JOIN FETCH의 차이점은?

> **모범 답변**
> 
> 둘 다 연관 엔티티를 함께 조회하지만, 사용 방식과 유연성이 다릅니다:
> 
> | 구분 | @EntityGraph | JOIN FETCH |
> |------|-------------|------------|
> | **위치** | 메서드 어노테이션 | JPQL 쿼리 내부 |
> | **유연성** | 선언적, 간단 | WHERE, ORDER BY 등 세밀한 제어 |
> | **가독성** | 쿼리와 분리 | 쿼리에 포함 |
> | **재사용성** | 여러 쿼리 메서드에 적용 가능 | 각 쿼리마다 작성 |
> 
> 선택 기준:
> - **1~2개 연관만 조회**: `@EntityGraph` (간결)
> - **3개 이상 또는 복잡한 조건**: `JOIN FETCH` (명시적)
> - **동적 쿼리 필요**: QueryDSL + `fetchJoin()`

### Q3. DTO Projection이 엔티티 조회보다 좋은 이유는?

> **모범 답변**
> 
> DTO Projection은 4가지 측면에서 우수합니다:
> 
> 1. **네트워크 비용**: 필요한 컬럼만 조회하여 전송량 감소
>    ```sql
>    -- 엔티티: SELECT * (20개 컬럼)
>    -- Projection: SELECT id, name, ... (10개 컬럼)
>    ```
> 
> 2. **메모리**: 영속성 컨텍스트에 등록되지 않아 메모리 사용량 감소
> 
> 3. **CPU**: 더티 체킹(변경 감지)이 불필요하여 스냅샷 비교 안 함
> 
> 4. **설계**: 읽기 전용임이 명확하여 의도치 않은 수정 방지
> 
> 단점은 엔티티 메서드(도메인 로직)를 사용할 수 없다는 것입니다. 따라서 목록 조회처럼 읽기 전용, 대량 데이터에 적합합니다.

### Q4. Batch Size 설정의 동작 원리는?

> **모범 답변**
> 
> `default_batch_fetch_size`는 지연 로딩 시 IN 절로 묶어서 조회하는 설정입니다.
> 
> ```java
> // 10개 스케줄의 member 지연 로딩 시
> 
> // batch_fetch_size 없음: 10번 쿼리
> SELECT * FROM member WHERE id = 1;
> SELECT * FROM member WHERE id = 2;
> ...
> 
> // batch_fetch_size = 100: 1번 쿼리
> SELECT * FROM member WHERE id IN (1, 2, 3, ..., 10);
> ```
> 
> 주의점:
> - `@EntityGraph`나 `JOIN FETCH`가 있으면 batch_fetch_size는 적용되지 않습니다 (이미 함께 조회됨)
> - 값이 너무 크면 IN 절이 길어져 DB 성능 저하 (100~1000 권장)
> - 값이 너무 작으면 여러 번 IN 쿼리 발생

### Q5. 1:N 관계에서 페이징과 EntityGraph를 함께 쓰면 문제가 있다던데?

> **모범 답변**
> 
> 맞습니다. **메모리 페이징(HHH90003004)** 경고가 발생합니다.
> 
> 문제 상황:
> ```java
> @EntityGraph(attributePaths = {"orders"})  // 1:N 관계
> Page<User> findAll(Pageable pageable);
> ```
> 
> Hibernate는 1:N 관계를 JOIN하면 row 수가 늘어나서 정확한 페이징이 불가능합니다. 그래서 **전체 데이터를 메모리에 로드한 후 페이징**합니다.
> 
> 해결 방법:
> 1. **N:1 관계만 EntityGraph 사용**: MedicationSchedule → Member (N:1, 안전)
> 2. **Batch Size 사용**: 1:N은 지연 로딩 + batch_fetch_size로 해결
> 3. **별도 쿼리 분리**: 목록 조회 후, ID로 연관 데이터 배치 조회
> 
> Hamalog에서는 N:1 관계(Schedule → Member)만 EntityGraph를 사용합니다.

### Q6. 왜 EAGER 대신 LAZY를 기본으로 사용하나요?

> **모범 답변**
> 
> EAGER는 **항상** 연관 엔티티를 조회하므로 위험합니다:
> 
> 1. **불필요한 조회**: 목록에서 연관 엔티티가 필요 없어도 조회
> 2. **연쇄 EAGER**: A → B (EAGER) → C (EAGER) → D (EAGER)...
> 3. **제어 불가**: 특정 쿼리에서만 빼는 것이 불가능
> 
> LAZY를 기본으로 하고, 필요할 때만 EntityGraph/JOIN FETCH로 함께 조회하면:
> - 명시적으로 의도를 표현
> - 각 쿼리별로 최적화 가능
> - N+1 문제를 인지하고 해결
> 
> 코딩 컨벤션으로 **`@ManyToOne`, `@OneToOne`에도 명시적으로 `FetchType.LAZY` 작성**을 강제합니다.

### Q7. 실제 프로젝트에서 N+1을 어떻게 발견했나요?

> **모범 답변**
> 
> 세 가지 방법을 사용했습니다:
> 
> 1. **hibernate.show_sql + format_sql**:
>    ```yaml
>    hibernate.show_sql: true
>    hibernate.format_sql: true
>    ```
>    개발 중 콘솔에서 쿼리 패턴 확인
> 
> 2. **P6Spy / DataSource-Proxy**:
>    쿼리 실행 횟수를 카운트하여 테스트에서 검증
>    ```java
>    assertThat(queryCount).isEqualTo(1);
>    ```
> 
> 3. **성능 테스트**:
>    데이터 양을 늘렸을 때 응답 시간이 선형 증가하면 N+1 의심
>    (10개: 100ms, 100개: 1000ms → N+1!)
> 
> 발견 후 `@EntityGraph` 또는 DTO Projection으로 수정하고, 테스트 코드로 재발 방지합니다.

### Q8. DTO Projection에서 연관 엔티티 전체가 필요하면 어떻게 하나요?

> **모범 답변**
> 
> 두 가지 선택지가 있습니다:
> 
> 1. **엔티티 조회 + EntityGraph** (권장):
>    ```java
>    @EntityGraph(attributePaths = {"member"})
>    Optional<MedicationSchedule> findById(Long id);
>    ```
>    연관 엔티티의 모든 필드가 필요하면 엔티티로 조회
> 
> 2. **Nested Projection**:
>    ```java
>    interface ScheduleProjection {
>        Long getId();
>        String getName();
>        MemberProjection getMember();  // 중첩 Projection
>        
>        interface MemberProjection {
>            Long getMemberId();
>            String getName();
>        }
>    }
>    ```
>    Spring Data JPA의 Interface-based Projection 활용
> 
> 목록에서 연관 엔티티 전체가 필요하다면, 정말 필요한지 다시 검토합니다. 대부분 ID + 이름 정도만 필요합니다.

### Q9. QueryDSL을 사용하지 않은 이유는?

> **모범 답변**
> 
> 현재 Hamalog의 쿼리 복잡도에서는 **JPQL + EntityGraph로 충분**했습니다.
> 
> QueryDSL이 필요한 시점:
> - 동적 검색 조건이 5개 이상
> - 복잡한 서브쿼리, 그룹화
> - 타입 세이프한 쿼리가 중요한 대규모 팀
> 
> 현재 상황:
> - 검색 조건이 간단 (이름 검색, 날짜 범위)
> - 대부분 memberId 기반 단순 조회
> - 1인 개발로 JPQL 문자열 관리 가능
> 
> **향후 개선 계획**으로 QueryDSL 도입을 고려하고 있으며, `Q` 클래스 생성 설정은 이미 build.gradle에 준비해두었습니다.

### Q10. EntityGraph로 여러 컬렉션을 조인하면 어떤 문제가 있나요?

> **모범 답변**
> 
> **MultipleBagFetchException** 또는 **Cartesian Product(카테시안 곱)** 문제가 발생합니다.
> 
> ```java
> // 위험한 예시
> @EntityGraph(attributePaths = {"orders", "reviews"})  // 2개 컬렉션
> List<User> findAll();
> ```
> 
> 문제:
> - User 1명, Order 10개, Review 5개 → 50개 row (10 × 5)
> - 중복 데이터로 메모리 낭비
> - Hibernate Bag 타입에서 예외 발생
> 
> 해결 방법:
> 1. **Set 사용**: `List` 대신 `Set`으로 선언 (중복 제거)
> 2. **분리 조회**: 첫 번째 컬렉션 조회 후, ID로 두 번째 컬렉션 조회
>    ```java
>    List<User> users = userRepo.findAllWithOrders();
>    List<Long> userIds = users.stream().map(User::getId).toList();
>    List<Review> reviews = reviewRepo.findByUserIdIn(userIds);
>    ```
> 3. **Batch Size**: 컬렉션은 지연 로딩 + batch_fetch_size로 해결
> 
> Hamalog에서는 1:N 컬렉션은 배치 조회 방식을 사용합니다.

---

## 📎 관련 문서

- [JPA-PERFORMANCE.md](../internal/patterns/JPA-PERFORMANCE.md)
- [MedicationScheduleRepository.java](../../src/main/java/com/Hamalog/repository/medication/MedicationScheduleRepository.java)
- [MedicationRecordRepository.java](../../src/main/java/com/Hamalog/repository/medication/MedicationRecordRepository.java)

