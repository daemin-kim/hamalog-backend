# Hamalog 성능 최적화 보고서
## JPA N+1 문제 식별 및 해결

---

## 📌 Executive Summary

| 항목 | 내용 |
|------|------|
| **식별된 부하 지점** | 복약 스케줄 목록 조회 API (`GET /medication-schedules`) |
| **근본 원인** | JPA N+1 쿼리 문제 (LAZY 로딩으로 인한 추가 쿼리 폭증) |
| **개선 결과** | 응답시간 6.5배 향상, 쿼리 수 501.5배 감소 |
| **적용 기술** | @EntityGraph, JOIN FETCH, Hibernate Statistics |

---

## 1️⃣ 부하 지점 식별

### 1.1 문제 상황 인식

개발 중 Hibernate Statistics 로그에서 **비정상적인 쿼리 수**를 발견했습니다:

```
Session Metrics {
    1003 JDBC statements executed  ← 단일 API 호출에 1,003개 쿼리?!
    213ms spent executing queries
}
```

**의문점**: 단순히 사용자의 복약 스케줄 목록을 조회하는 API에서 왜 1,000개 이상의 쿼리가 발생하는가?

### 1.2 부하 테스트 목적

> **"이 API가 실제 서비스에서 병목이 될 수 있는가?"**

- 현재 데이터: 테스트 사용자 몇 명
- 예상 시나리오: 사용자가 수십~수백 개의 복약 스케줄을 등록하는 경우
- 검증 필요: 데이터 증가에 따른 성능 저하 패턴 확인

### 1.3 테스트 환경 구성

부하 지점을 정확히 측정하기 위해 **격리된 벤치마크 환경** 구축:

| 구성요소 | 설정 | 이유 |
|---------|------|------|
| **Docker 환경** | 독립 컨테이너 | 프로덕션 영향 없이 테스트 |
| **배치 패치 비활성화** | `batch_fetch_size: 1` | 순수 N+1 문제 측정 |
| **Hibernate Statistics** | `generate_statistics: true` | 정확한 쿼리 수 측정 |
| **테스트 데이터** | 1,000개 스케줄 × 3개 시간 | 실사용 시나리오 시뮬레이션 |

---

## 2️⃣ 원인 분석

### 2.1 N+1 문제란?

**JPA의 LAZY 로딩**으로 인해 연관 엔티티 접근 시 **추가 쿼리가 발생**하는 문제입니다.

```java
// 문제가 되는 코드 흐름
List<MedicationSchedule> schedules = repository.findAll();  // 1번 쿼리
for (Schedule schedule : schedules) {
    schedule.getMedicationTimes();  // 각 스케줄마다 1번씩 = N번 쿼리
}
// 총 쿼리: 1 + N개
```

### 2.2 Hamalog에서의 N+1 발생 구조

```
MedicationSchedule (1,000개)
    ├── medicationTimes (LAZY) → 접근 시 쿼리 발생
    └── member (LAZY) → 접근 시 쿼리 발생

API 호출 시:
├── 쿼리 1: SELECT * FROM medication_schedule WHERE member_id = ?
├── 쿼리 2: SELECT * FROM medication_time WHERE schedule_id = 1
├── 쿼리 3: SELECT * FROM medication_time WHERE schedule_id = 2
├── ...
└── 쿼리 1001: SELECT * FROM medication_time WHERE schedule_id = 1000
```

### 2.3 왜 문제인가? (스케일 예측)

| 데이터 규모 | 쿼리 수 | 예상 응답시간 | 서버 부하 |
|------------|--------|-------------|----------|
| 10개 스케줄 | 11개 | ~30ms | 낮음 |
| 100개 스케줄 | 101개 | ~100ms | 중간 |
| **1,000개 스케줄** | **1,001개** | **~300ms** | **높음** |
| 10,000개 스케줄 | 10,001개 | ~3초 | **서비스 불가** |

> ⚠️ **선형적으로 증가하는 쿼리 수** = 스케일링 불가능한 구조

---

## 3️⃣ 개선 방안 적용

### 3.1 해결: JOIN FETCH / @EntityGraph

연관 엔티티를 **한 번의 쿼리로 함께 조회**:

```java
// Before: N+1 발생
@Query("SELECT ms FROM MedicationSchedule ms WHERE ms.member.memberId = :memberId")
List<MedicationSchedule> findAllByMemberId(Long memberId);

// After: JOIN FETCH로 해결
@EntityGraph(attributePaths = {"medicationTimes"})
@Query("SELECT DISTINCT ms FROM MedicationSchedule ms WHERE ms.member.memberId = :memberId")
List<MedicationSchedule> findAllByMember_MemberId(Long memberId);
```

### 3.2 실행되는 쿼리 비교

**Before (N+1)**:
```sql
-- 1번째 쿼리
SELECT * FROM medication_schedule WHERE member_id = 1;
-- 2~1001번째 쿼리 (각 스케줄마다)
SELECT * FROM medication_time WHERE medication_schedule_id = ?;
SELECT * FROM medication_time WHERE medication_schedule_id = ?;
... (998번 더)
```

**After (JOIN FETCH)**:
```sql
-- 단 1번의 쿼리
SELECT DISTINCT ms.*, mt.* 
FROM medication_schedule ms
LEFT JOIN medication_time mt ON ms.id = mt.medication_schedule_id
WHERE ms.member_id = 1;
```

---

## 4️⃣ 성능 측정 결과

### 4.1 테스트 조건

| 항목 | 값 |
|------|-----|
| **테스트 일자** | 2026년 1월 16일 |
| **MedicationSchedule** | 1,000개 |
| **MedicationTime** | 3,000개 (스케줄당 3개) |
| **측정 횟수** | 10회 반복 |
| **측정 도구** | curl + Hibernate Statistics |

### 4.2 응답시간 비교

| 방식 | 평균 | 최소 | 최대 | P95 |
|------|------|------|------|-----|
| **최적화 (JOIN FETCH)** | **46ms** | 37ms | 90ms | ~80ms |
| **비최적화 (N+1)** | **297ms** | 153ms | 649ms | ~500ms |

```
응답시간 비교 (ms)
┌────────────────────────────────────────────────────────┐
│ JOIN FETCH  ████████ 46ms                              │
│ N+1 문제    ██████████████████████████████████ 297ms   │
└────────────────────────────────────────────────────────┘
                        🚀 6.5배 성능 향상
```

### 4.3 쿼리 수 비교 (Hibernate Statistics)

| 방식 | JDBC 쿼리 수 | DB 실행 시간 |
|------|-------------|-------------|
| **최적화** | **2개** | 14ms |
| **비최적화** | **1,003개** | 213ms |

```
쿼리 수 비교
┌────────────────────────────────────────────────────────┐
│ JOIN FETCH  ▮▮ (2개)                                   │
│ N+1 문제    ▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮... (1,003개)    │
└────────────────────────────────────────────────────────┘
                      🚀 501.5배 쿼리 감소
```

---

## 5️⃣ 비즈니스 임팩트

### 5.1 사용자 경험 개선

| 지표 | Before | After | 개선 |
|------|--------|-------|------|
| 목록 로딩 시간 | 300ms+ | 50ms 이하 | **체감 즉시 로딩** |
| 스크롤 반응성 | 끊김 가능 | 부드러움 | UX 향상 |

### 5.2 서버 리소스 효율화

| 지표 | Before | After | 개선 |
|------|--------|-------|------|
| DB 커넥션 점유 | 213ms | 14ms | **15배 감소** |
| 동시 처리량 | 제한적 | 확장 가능 | 처리량 증가 |

### 5.3 스케일 예측 (데이터 증가 시)

| 스케줄 수 | N+1 쿼리 수 | N+1 예상시간 | JOIN FETCH |
|----------|------------|-------------|-----------|
| 100개 | 101개 | ~30ms | ~10ms |
| 1,000개 | 1,001개 | ~300ms | ~50ms |
| 10,000개 | 10,001개 | **~3초** | ~200ms |
| 100,000개 | 100,001개 | **~30초** | ~1초 |

> ✅ **JOIN FETCH는 데이터 증가에도 일정한 성능 유지**

---

## 6️⃣ 추가 적용된 안전장치

### 6.1 프로덕션 설정

```yaml
# application.yml - 실수로 N+1 발생 시 완화
spring.jpa.properties.hibernate:
  default_batch_fetch_size: 100
```

### 6.2 모니터링

- **Hibernate Statistics**: 개발 환경에서 쿼리 수 상시 모니터링
- **로그 분석**: 비정상 쿼리 패턴 감지

---

## 7️⃣ 결론 및 학습

### 핵심 성과

| 지표 | 결과 |
|------|------|
| **응답시간 개선** | 297ms → 46ms (**6.5배**) |
| **쿼리 수 감소** | 1,003개 → 2개 (**501.5배**) |
| **스케일링** | 선형 증가 → 일정 유지 |

### 얻은 교훈

1. **부하 테스트의 중요성**: 개발 단계에서는 발견하기 어려운 성능 문제를 데이터 규모를 키워 사전에 발견
2. **JPA 사용 시 주의점**: LAZY 로딩의 편리함 이면에 N+1 위험 존재
3. **측정 기반 최적화**: "추측하지 말고 측정하라" - 정량적 데이터로 개선 효과 검증

---

## 📎 부록: 테스트 재현 방법

### 벤치마크 환경 실행

```bash
# 1. 환경 시작
docker-compose -f docker-compose-benchmark.yml up -d --build
sleep 60  # 데이터 생성 대기

# 2. 성능 테스트
# 최적화 쿼리
curl "http://localhost:8080/api/v1/benchmark/medication-schedules/list/1?optimized=true"
# N+1 쿼리
curl "http://localhost:8080/api/v1/benchmark/medication-schedules/list/1?optimized=false"

# 3. 쿼리 수 확인
docker-compose -f docker-compose-benchmark.yml logs app | grep "JDBC statements"

# 4. 환경 종료
docker-compose -f docker-compose-benchmark.yml down -v
```

### 관련 파일

| 파일 | 역할 |
|------|------|
| `MedicationScheduleRepository.java` | @EntityGraph 적용 쿼리 |
| `application-benchmark.yml` | 벤치마크 환경 설정 |
| `BenchmarkDataInitializer.java` | 1,000개 테스트 데이터 생성 |
| `docker-compose-benchmark.yml` | 격리된 테스트 환경 |

---

**작성일**: 2026년 1월 16일  
**프로젝트**: Hamalog - 복약 스케줄 관리 시스템

