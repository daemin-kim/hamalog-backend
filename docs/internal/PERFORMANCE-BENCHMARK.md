# 🚀 Hamalog 성능 벤치마크 가이드

> **목적**: N+1 문제 개선 전후 성능 비교 및 병목 지점 분석
> 
> **생성일**: 2026-01-15
> 
> **도구**: Gatling 3.11.5

---

## 📋 목차

1. [개요](#1-개요)
2. [벤치마크 환경 설정](#2-벤치마크-환경-설정)
3. [테스트 시나리오](#3-테스트-시나리오)
4. [실행 방법](#4-실행-방법)
5. [결과 분석](#5-결과-분석)
6. [개선 전후 비교](#6-개선-전후-비교)
7. [병목 지점 분석](#7-병목-지점-분석)

---

## 1. 개요

### 1.1 벤치마크 목표

| 목표 | 설명 |
|------|------|
| N+1 문제 정량화 | `@EntityGraph` 적용 전후 쿼리 수 및 응답 시간 비교 |
| 동시 사용자 처리량 | 50/100/200명 동시 접속 시 시스템 안정성 |
| 병목 지점 식별 | CPU, 메모리, DB 커넥션 풀 등 리소스 사용률 |
| SLA 기준 검증 | 평균 응답 시간 500ms 미만, P95 1초 미만 |

### 1.2 측정 항목

- **응답 시간**: Mean, P50, P75, P95, P99, Max
- **처리량**: TPS (Transactions Per Second)
- **성공률**: 2xx 응답 비율
- **쿼리 수**: Hibernate Statistics 기반 SQL 실행 횟수

---

## 2. 벤치마크 환경 설정

### 2.1 Gradle 설정

`build.gradle`에 Gatling 플러그인이 추가되어 있습니다:

```groovy
plugins {
    id 'io.gatling.gradle' version '3.11.5'
}

gatling {
    logLevel = 'WARN'
    logHttp = 'NONE'
}
```

### 2.2 프로필 설정

벤치마크 API는 `dev`, `test`, `benchmark` 프로필에서만 활성화됩니다.

```bash
# 로컬 개발 환경
SPRING_PROFILES_ACTIVE=dev,benchmark

# 프로덕션 환경 (GitHub Actions 배포 시)
SPRING_PROFILES_ACTIVE=prod,benchmark
```

### 2.3 테스트 데이터 준비

```bash
# 벤치마크용 대량 데이터 생성 (500개 스케줄, 수천 개 기록)
mysql -u root -p hamalog < scripts/benchmark/load-test-data.sql
```

### 2.4 프로덕션 환경 벤치마크 (권장)

프로덕션 서버를 대상으로 벤치마크를 실행하려면:

#### Step 1: GitHub Secrets 설정
```
BENCHMARK_API_KEY: <랜덤 생성된 API Key>
```

#### Step 2: 벤치마크 모드 배포
1. GitHub Actions → "Deploy Benchmark Mode" 워크플로우 실행
2. 옵션 선택:
   - 벤치마크 유지 시간: 30분 (기본)
   - 테스트 데이터 자동 생성: true
   - 자동 롤백: true

#### Step 3: 로컬에서 벤치마크 실행
```bash
# API Key 설정
export BENCHMARK_API_KEY='your-api-key'

# 원격 벤치마크 실행
./scripts/benchmark/run-remote-benchmark.sh

# 특정 시뮬레이션만 실행
./scripts/benchmark/run-remote-benchmark.sh medication
```

#### Step 4: 롤백
자동 롤백이 설정되어 있으면 지정된 시간 후 자동으로 일반 프로덕션 모드로 복원됩니다.
수동 롤백이 필요한 경우: main 브랜치에 push 또는 "Deploy to Production" 워크플로우 실행

---

## 3. 테스트 시나리오

### 3.1 복약 스케줄 조회 (N+1 비교)

| 시나리오 | 엔드포인트 | 설명 |
|----------|------------|------|
| Before (Naive) | `GET /api/v1/benchmark/medication-schedules/list/{id}?optimized=false` | N+1 문제 발생 |
| After (Optimized) | `GET /api/v1/benchmark/medication-schedules/list/{id}?optimized=true` | @EntityGraph 최적화 |

**부하 패턴**:
- 50명 사용자, 30초 램프업
- 각 사용자 20회 반복 요청

### 3.2 인증 플로우

| 시나리오 | 설명 |
|----------|------|
| 로그인 부하 | 100명 동시 로그인 |
| 전체 플로우 | 로그인 → 토큰 갱신 → 로그아웃 |
| 스파이크 | 50명 동시 로그인 시도 |

---

## 4. 실행 방법

### 4.1 로컬 환경 (개발/테스트)

```bash
# 서버 시작 (별도 터미널)
./gradlew bootRun --args='--spring.profiles.active=dev,benchmark'

# 벤치마크 실행
./scripts/benchmark/run-benchmark.sh
```

### 4.2 프로덕션 환경 (권장)

```bash
# 1. GitHub Actions에서 벤치마크 모드 배포
# 2. 로컬에서 원격 벤치마크 실행
export BENCHMARK_API_KEY='your-api-key'
./scripts/benchmark/run-remote-benchmark.sh
```

### 4.3 개별 시뮬레이션 실행

```bash
# 복약 스케줄 시뮬레이션만
./gradlew gatlingRun-com.Hamalog.simulation.MedicationScheduleSimulation

# 인증 시뮬레이션만
./gradlew gatlingRun-com.Hamalog.simulation.AuthenticationSimulation
```

### 4.4 사용자 정의 파라미터

```bash
# 로컬 타겟
./gradlew gatlingRun \
    -DbaseUrl=http://localhost:8080 \
    -DtestUser=benchmark@test.com \
    -DtestPassword=Benchmark1234!

# 원격 타겟 (프로덕션)
./gradlew gatlingRun \
    -DbaseUrl=https://api.hamalog.shop \
    -DbenchmarkApiKey=your-api-key \
    -DtestMemberId=1
```

---

## 5. 결과 분석

### 5.1 리포트 위치

```
build/reports/gatling/
└── medicationschedulesimulation-{timestamp}/
    ├── index.html          # 메인 리포트 (브라우저에서 열기)
    ├── js/stats.json       # JSON 형식 통계
    └── simulation.log      # 상세 로그
```

### 5.2 핵심 지표 해석

| 지표 | 설명 | 목표 |
|------|------|------|
| Mean Response Time | 평균 응답 시간 | < 500ms |
| P95 Response Time | 95%의 요청이 이 시간 내 완료 | < 1000ms |
| P99 Response Time | 99%의 요청이 이 시간 내 완료 | < 3000ms |
| Success Rate | 성공한 요청 비율 | > 95% |
| TPS | 초당 처리 트랜잭션 수 | > 100 |

---

## 6. 개선 전후 비교

### 6.1 N+1 문제 개선 결과

> ⚠️ **아래 수치는 예상 값입니다. 실제 벤치마크 후 업데이트 필요**

#### 쿼리 수 비교

| 시나리오 | 스케줄 수 | Before (Naive) | After (Optimized) | 감소율 |
|----------|-----------|----------------|-------------------|--------|
| 목록 조회 | 10개 | 11 쿼리 | 1 쿼리 | **90.9%** |
| 목록 조회 | 50개 | 51 쿼리 | 1 쿼리 | **98.0%** |
| 목록 조회 | 100개 | 101 쿼리 | 1 쿼리 | **99.0%** |

#### 응답 시간 비교

| 시나리오 | Before (ms) | After (ms) | 개선율 |
|----------|-------------|------------|--------|
| Mean | ~850 | ~120 | **85.9%** |
| P95 | ~1500 | ~200 | **86.7%** |
| P99 | ~2500 | ~350 | **86.0%** |

### 6.2 측정 방법

```bash
# 쿼리 수 측정 API
curl "http://localhost:8080/api/v1/benchmark/query-count/1?optimized=false"
# {"queryType":"Naive","queryCount":101,"durationMs":245}

curl "http://localhost:8080/api/v1/benchmark/query-count/1?optimized=true"
# {"queryType":"Optimized","queryCount":1,"durationMs":23}
```

---

## 7. 병목 지점 분석

### 7.1 N+1 문제 원인

```java
// ❌ Before: LAZY 로딩으로 인한 N+1 문제
@Query("SELECT ms FROM MedicationSchedule ms WHERE ms.member.memberId = :memberId")
List<MedicationSchedule> findAllByMemberIdNaive(@Param("memberId") Long memberId);

// 이후 각 schedule.getMember().getName() 호출 시 추가 쿼리 발생
```

### 7.2 해결 방법

```java
// ✅ After: @EntityGraph로 Member 즉시 로딩
@EntityGraph(attributePaths = {"member"})
List<MedicationSchedule> findAllByMember_MemberId(Long memberId);

// 또는 JOIN FETCH
@Query("SELECT ms FROM MedicationSchedule ms JOIN FETCH ms.member WHERE ms.member.memberId = :memberId")
List<MedicationSchedule> findAllByMemberIdWithMember(@Param("memberId") Long memberId);
```

### 7.3 추가 최적화 기회

| 영역 | 현재 상태 | 개선 방안 | 우선순위 |
|------|-----------|-----------|----------|
| DTO Projection | 부분 적용 | 목록 조회에 Projection 확대 | 중 |
| 캐싱 | Redis 적용 | 자주 조회되는 데이터 캐싱 | 중 |
| 커넥션 풀 | HikariCP 기본값 | 부하 테스트 기반 튜닝 | 하 |
| 인덱스 | 기본 인덱스 | 느린 쿼리 분석 후 추가 | 하 |

---

## 📊 벤치마크 결과 기록

> 실제 벤치마크 실행 후 결과를 기록하세요.

### 2026-01-15 첫 벤치마크 (예정)

| 항목 | 값 |
|------|-----|
| 테스트 환경 | 프로덕션 서버 (Self-hosted, Windows Docker) |
| 데이터베이스 | MySQL 8.0 (Docker) |
| 테스트 데이터 | 500개 스케줄 / 사용자 |
| Mean (Naive) | _측정 예정_ |
| Mean (Optimized) | _측정 예정_ |
| 개선율 | _측정 예정_ |

---

## 🔗 관련 문서

- [JPA 성능 최적화 가이드](./patterns/JPA-PERFORMANCE.md)
- [개선 태스크 목록](./IMPROVEMENT-TASKS.md)
- [캐싱 패턴](./patterns/CACHING-PATTERNS.md)

