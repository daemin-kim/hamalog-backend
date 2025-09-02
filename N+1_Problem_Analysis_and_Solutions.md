# N+1 문제 분석 및 해결방안 구현 보고서

## 🔍 발견된 N+1 문제들

### 1. MedicationSchedule 관련 N+1 문제
**위치**: `MedicationScheduleController`, `MedicationScheduleResponse`
**문제**: 
- `findAllByMember_MemberId()` 조회 후 각 MedicationSchedule의 Member 정보 접근 시 발생
- `MedicationScheduleResponse.from()` 메서드에서 `getMember().getMemberId()` 호출
- 컨트롤러에서 권한 확인 시 `getMember().getMemberId()` 호출

### 2. MedicationRecord 관련 N+1 문제  
**위치**: `MedicationRecordService`
**문제**:
- `isOwnerOfSchedule()` 메서드: `schedule.getMember().getLoginId()` 
- `isOwnerOfRecord()` 메서드: `record.getMedicationSchedule().getMember().getLoginId()`
- 연관된 MedicationSchedule과 MedicationTime 접근 시 발생

### 3. SideEffect 관련 N+1 문제
**위치**: `SideEffectService.createSideEffectRecord()`
**문제**:
- 반복문 내에서 각 SideEffect ID로 개별 조회: `sideEffectRepository.findById()`

## 🛠️ 구현한 해결방안

### 1. @EntityGraph 활용
**파일**: `MedicationScheduleRepository.java`
```java
@EntityGraph(attributePaths = {"member"})
List<MedicationSchedule> findAllByMember_MemberId(Long memberId);

@EntityGraph(attributePaths = {"member"})  
Page<MedicationSchedule> findByMember_MemberId(Long memberId, Pageable pageable);

@EntityGraph(attributePaths = {"member"})
Optional<MedicationSchedule> findById(Long id);
```

### 2. JOIN FETCH 쿼리 추가
**파일**: `MedicationScheduleRepository.java`
```java
@Query("SELECT ms FROM MedicationSchedule ms JOIN FETCH ms.member WHERE ms.member.memberId = :memberId")
List<MedicationSchedule> findAllByMemberIdWithMember(@Param("memberId") Long memberId);
```

**파일**: `MedicationRecordRepository.java`  
```java
@EntityGraph(attributePaths = {"medicationSchedule", "medicationSchedule.member", "medicationTime"})
List<MedicationRecord> findAllByMedicationSchedule_MedicationScheduleId(Long medicationScheduleId);

@Query("SELECT mr FROM MedicationRecord mr JOIN FETCH mr.medicationSchedule ms JOIN FETCH ms.member WHERE mr.medicationRecordId = :recordId")
Optional<MedicationRecord> findByIdWithMemberForOwnershipCheck(@Param("recordId") Long recordId);
```

### 3. IN 쿼리를 통한 일괄 조회
**파일**: `SideEffectService.java`
```java
// 기존: 반복문에서 개별 조회
// SideEffect sideEffect = sideEffectRepository.findById(item.sideEffectId())

// 해결: IN 쿼리로 일괄 조회
List<Long> sideEffectIds = request.sideEffects().stream()
    .map(item -> item.sideEffectId())
    .toList();
List<SideEffect> sideEffects = sideEffectRepository.findAllById(sideEffectIds);
Map<Long, SideEffect> sideEffectMap = sideEffects.stream()
    .collect(Collectors.toMap(SideEffect::getSideEffectId, Function.identity()));
```

### 4. JPA 배치 사이즈 설정 
**파일**: `application.properties`
```properties
# N+1 문제 예방을 위한 배치 사이즈 설정
spring.jpa.properties.hibernate.default_batch_fetch_size=100
spring.jpa.properties.hibernate.batch_fetch_style=LEGACY
spring.jpa.properties.hibernate.jdbc.batch_size=20
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
spring.jpa.properties.hibernate.jdbc.batch_versioned_data=true
```

## 🧪 테스트 및 검증

### 1. N+1 문제 재현 테스트
**파일**: `NPlusOneReproductionTest.java`
- N+1 문제 발생 시나리오 재현
- DTO 변환 과정에서 발생하는 추가 쿼리 확인
- 권한 확인 과정에서 발생하는 추가 쿼리 확인

### 2. N+1 문제 해결 검증 테스트  
**파일**: `NPlusOneSolutionTest.java`
- @EntityGraph 적용 후 추가 쿼리 없이 Member 정보 접근 확인
- JOIN FETCH 쿼리 적용 후 성능 개선 확인
- 권한 확인 시 추가 쿼리 발생하지 않음 확인

### 3. 회귀 테스트
- 기존 AuthService 테스트 통과 확인
- 변경사항이 기존 기능에 영향 없음 검증

## 📊 해결 효과

### Before (문제 발생 시)
- MedicationSchedule 10개 조회 시: 1(목록 조회) + 10(각 Member 조회) = **11개 쿼리**
- MedicationRecord 권한 확인 시: 1(레코드 조회) + 1(스케줄 조회) + 1(Member 조회) = **3개 쿼리**
- SideEffect 5개 생성 시: 1(레코드 저장) + 5(개별 SideEffect 조회) = **6개 쿼리**

### After (해결 후)  
- MedicationSchedule 10개 조회 시: **1개 쿼리** (JOIN으로 Member 함께 조회)
- MedicationRecord 권한 확인 시: **1개 쿼리** (JOIN FETCH로 모든 연관 엔티티 함께 조회)
- SideEffect 5개 생성 시: 1(레코드 저장) + 1(IN 쿼리로 일괄 조회) = **2개 쿼리**

## 🎯 권장사항

1. **코드 리뷰 시 N+1 문제 점검 항목 추가**
   - 반복문 내에서 엔티티 조회하는 패턴 주의
   - DTO 변환 시 LAZY 연관관계 접근 패턴 주의

2. **성능 모니터링**
   - `spring.jpa.properties.hibernate.generate_statistics=true` 설정 활용
   - 슬로우 쿼리 로깅으로 성능 문제 조기 발견

3. **추가 개선 고려사항**
   - 복잡한 조회의 경우 DTO 프로젝션 활용 검토  
   - 캐시 활용으로 반복 조회 최소화

## 📋 변경된 파일 목록

1. `src/main/java/com/Hamalog/repository/medication/MedicationScheduleRepository.java`
2. `src/main/java/com/Hamalog/repository/medication/MedicationRecordRepository.java`  
3. `src/main/java/com/Hamalog/service/sideEffect/SideEffectService.java`
4. `src/main/resources/application.properties`
5. `src/test/java/com/Hamalog/nplusone/NPlusOneReproductionTest.java` (신규)
6. `src/test/java/com/Hamalog/nplusone/NPlusOneSolutionTest.java` (신규)

모든 변경사항은 기존 API 인터페이스를 유지하면서 성능만 개선하도록 구현되었습니다.