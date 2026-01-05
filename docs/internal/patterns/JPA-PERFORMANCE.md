# JPA 성능 최적화 가이드

> 이 문서는 Hamalog 프로젝트의 JPA ORM 성능 최적화 전략을 설명합니다.

## 📋 목차

1. [N+1 문제 해결](#1-n1-문제-해결)
2. [DTO Projection](#2-dto-projection)
3. [QueryDSL](#3-querydsl)
4. [Batch Size 설정](#4-batch-size-설정)
5. [성능 모니터링](#5-성능-모니터링)

---

## 1. N+1 문제 해결

### 1.1 @EntityGraph 사용

```java
// Repository에서 @EntityGraph로 연관 엔티티 함께 조회
@EntityGraph(attributePaths = {"member"})
List<MedicationSchedule> findAllByMember_MemberId(Long memberId);

@EntityGraph(attributePaths = {"medicationSchedule", "medicationSchedule.member", "medicationTime"})
Optional<MedicationRecord> findById(Long id);
```

### 1.2 JOIN FETCH 사용

```java
// JPQL에서 JOIN FETCH로 연관 엔티티 함께 조회
@Query("SELECT ms FROM MedicationSchedule ms JOIN FETCH ms.member WHERE ms.member.memberId = :memberId")
List<MedicationSchedule> findAllByMemberIdWithMember(@Param("memberId") Long memberId);
```

### 1.3 배치 조회

```java
// 여러 스케줄의 복약 기록을 한 번에 조회 (N+1 방지)
@Query("SELECT mr FROM MedicationRecord mr " +
       "JOIN FETCH mr.medicationSchedule ms " +
       "JOIN FETCH mr.medicationTime mt " +
       "WHERE ms.medicationScheduleId IN :scheduleIds")
List<MedicationRecord> findAllByScheduleIds(@Param("scheduleIds") List<Long> scheduleIds);
```

**사용 예시:**
```java
// Bad: N+1 발생
List<MedicationRecordExportData> recordData = schedules.stream()
    .flatMap(s -> recordRepository.findAllByMedicationSchedule_MedicationScheduleId(s.getId()).stream())
    .toList();

// Good: 한 번의 쿼리로 조회
List<Long> scheduleIds = schedules.stream().map(s -> s.getId()).toList();
List<MedicationRecord> allRecords = recordRepository.findAllByScheduleIds(scheduleIds);
```

---

## 2. DTO Projection

### 2.1 JPQL Constructor Expression

엔티티 전체가 아닌 필요한 필드만 조회하여 메모리 사용량과 네트워크 부하를 줄입니다.

**Projection DTO 정의:**
```java
// src/main/java/com/Hamalog/dto/medication/projection/MedicationScheduleProjection.java
public record MedicationScheduleProjection(
    Long medicationScheduleId,
    Long memberId,
    String name,
    String hospitalName,
    LocalDate prescriptionDate,
    String memo,
    LocalDate startOfAd,
    Integer prescriptionDays,
    Integer perDay,
    AlarmType alarmType,
    Boolean isActive
) {}
```

**Repository 메서드:**
```java
// JPQL Constructor Expression으로 DTO 직접 생성
@Query("SELECT new com.Hamalog.dto.medication.projection.MedicationScheduleProjection(" +
       "ms.medicationScheduleId, ms.member.memberId, ms.name, ms.hospitalName, " +
       "ms.prescriptionDate, ms.memo, ms.startOfAd, ms.prescriptionDays, ms.perDay, " +
       "ms.alarmType, ms.isActive) " +
       "FROM MedicationSchedule ms WHERE ms.member.memberId = :memberId")
Page<MedicationScheduleProjection> findProjectionsByMemberId(@Param("memberId") Long memberId, Pageable pageable);
```

### 2.2 사용 가이드라인

| 상황 | 권장 방식 |
|------|-----------|
| 목록 조회 (페이징) | DTO Projection |
| 상세 조회 (단건) | Entity with @EntityGraph |
| 수정/삭제 | Entity (변경 감지 필요) |
| 통계/집계 | DTO Projection 또는 네이티브 쿼리 |

---

## 3. QueryDSL

### 3.1 설정

**build.gradle:**
```groovy
dependencies {
    // QueryDSL
    implementation 'com.querydsl:querydsl-jpa:5.1.0:jakarta'
    annotationProcessor 'com.querydsl:querydsl-apt:5.1.0:jakarta'
    annotationProcessor 'jakarta.annotation:jakarta.annotation-api'
    annotationProcessor 'jakarta.persistence:jakarta.persistence-api'
}

// Q클래스 생성 경로
def querydslDir = "$buildDir/generated/querydsl"

sourceSets {
    main.java.srcDirs += querydslDir
}
```

**QuerydslConfig.java:**
```java
@Configuration
public class QuerydslConfig {
    @PersistenceContext
    private EntityManager entityManager;

    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(entityManager);
    }
}
```

### 3.2 Custom Repository 패턴

**인터페이스:**
```java
public interface MedicationScheduleRepositoryCustom {
    Page<MedicationScheduleProjection> searchWithConditions(
        Long memberId,
        String keyword,
        Boolean isActive,
        Pageable pageable
    );
}
```

**구현체:**
```java
@Repository
@RequiredArgsConstructor
public class MedicationScheduleRepositoryCustomImpl implements MedicationScheduleRepositoryCustom {
    
    private final JPAQueryFactory queryFactory;
    private static final QMedicationSchedule medicationSchedule = QMedicationSchedule.medicationSchedule;

    @Override
    public Page<MedicationScheduleProjection> searchWithConditions(...) {
        JPAQuery<MedicationScheduleProjection> query = queryFactory
            .select(Projections.constructor(MedicationScheduleProjection.class, ...))
            .from(medicationSchedule)
            .where(
                memberIdEq(memberId),
                keywordContains(keyword),
                isActiveEq(isActive)
            );
        // ...
    }

    // 동적 조건 메서드
    private BooleanExpression memberIdEq(Long memberId) {
        return memberId != null ? medicationSchedule.member.memberId.eq(memberId) : null;
    }
}
```

### 3.3 동적 쿼리 패턴

```java
// null-safe 동적 조건
private BooleanExpression keywordContains(String keyword) {
    if (!StringUtils.hasText(keyword)) {
        return null; // null 반환 시 where 절에서 무시됨
    }
    return medicationSchedule.name.containsIgnoreCase(keyword)
        .or(medicationSchedule.hospitalName.containsIgnoreCase(keyword));
}
```

---

## 4. Batch Size 설정

### 4.1 글로벌 설정

**application.properties:**
```properties
# Hibernate Batch Fetch Size - N+1 문제 완화를 위한 배치 조회 설정
# LAZY 로딩 시 IN 절로 한 번에 100개씩 배치 조회
spring.jpa.properties.hibernate.default_batch_fetch_size=100
```

### 4.2 동작 원리

배치 사이즈를 설정하면 LAZY 로딩 시 N번의 개별 쿼리 대신 IN 절을 사용한 배치 쿼리가 실행됩니다.

**Before (N+1):**
```sql
SELECT * FROM medication_schedule WHERE member_id = 1;  -- 1번
SELECT * FROM member WHERE member_id = 1;  -- N번
SELECT * FROM member WHERE member_id = 2;
SELECT * FROM member WHERE member_id = 3;
...
```

**After (Batch Size 100):**
```sql
SELECT * FROM medication_schedule WHERE member_id = 1;  -- 1번
SELECT * FROM member WHERE member_id IN (1, 2, 3, ..., 100);  -- 1번
```

### 4.3 권장 값

| 데이터 규모 | 권장 배치 사이즈 |
|-------------|------------------|
| 소규모 (< 1000) | 100 |
| 중규모 (1000 ~ 10000) | 100 ~ 500 |
| 대규모 (> 10000) | 500 ~ 1000 |

---

## 5. 성능 모니터링

### 5.1 쿼리 로깅 활성화 (개발환경)

**application-dev.properties:**
```properties
# SQL 로깅
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# 바인딩 파라미터 로깅
logging.level.org.hibernate.orm.jdbc.bind=trace
```

### 5.2 N+1 문제 탐지

테스트 코드에서 쿼리 수를 확인:

```java
@Test
void testNoPlusOneProblem() {
    // Hibernate statistics 활성화
    Statistics stats = entityManager.unwrap(Session.class).getSessionFactory().getStatistics();
    stats.setStatisticsEnabled(true);
    stats.clear();
    
    // 테스트 실행
    List<MedicationSchedule> schedules = repository.findAllByMember_MemberId(1L);
    schedules.forEach(s -> s.getMember().getName()); // LAZY 로딩 트리거
    
    // 쿼리 수 검증
    long queryCount = stats.getQueryExecutionCount();
    assertThat(queryCount).isLessThanOrEqualTo(2); // 1개 또는 2개 이하
}
```

---

## 📚 관련 문서

- [캐싱 패턴](./CACHING-PATTERNS.md)
- [에러 처리](./ERROR-HANDLING.md)
- [코딩 컨벤션](../CODING-CONVENTIONS.md)

---

> 📝 최종 업데이트: 2026년 1월 5일

