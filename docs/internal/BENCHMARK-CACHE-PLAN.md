# Redis 캐시 성능 벤치마크 계획

> **목적**: Redis 캐시 적용 전후 성능 차이를 정량적으로 측정하여 캐싱 전략의 효과를 검증

---

## 📌 개요

| 항목 | 내용 |
|------|------|
| **테스트 대상** | 회원 인증 정보 조회 (매 API 요청 시 발생) |
| **비교 항목** | Redis 캐시 HIT vs DB 직접 조회 |
| **예상 개선율** | 90%+ 응답시간 단축 |
| **측정 도구** | curl + Spring Actuator + Hibernate Statistics |

---

## 1️⃣ 테스트 시나리오

### 1.1 테스트 대상 API

현재 `MemberCacheService.java`에 `@Cacheable` 적용된 메서드:

```java
@Cacheable(value = CACHE_NAME, key = "'loginId:' + #loginId", ttl = CACHE_TTL_SECONDS)
public Optional<Member> findByLoginId(String loginId)

@Cacheable(value = CACHE_NAME, key = "'memberId:' + #memberId", ttl = CACHE_TTL_SECONDS)
public Optional<Member> findById(Long memberId)
```

### 1.2 시나리오 설계

| 시나리오 | 설명 | 측정 항목 |
|----------|------|----------|
| **캐시 HIT** | 캐시에 데이터 존재 시 조회 | Redis 조회 시간 (~1ms) |
| **캐시 MISS + DB** | 캐시 없이 DB 직접 조회 | DB 쿼리 시간 (~15ms) |
| **캐시 워밍업** | 첫 조회 후 캐시 저장 | 전체 시간 |

### 1.3 예상 결과

| 방식 | 예상 응답시간 | 근거 |
|------|-------------|------|
| **캐시 HIT** | ~1-3ms | Redis 네트워크 RTT |
| **DB 직접 조회** | ~10-20ms | MySQL 쿼리 + 네트워크 |
| **개선율** | 80-95% | 기존 ADR-0005 문서 참조 |

---

## 2️⃣ 구현 완료 항목

### 2.1 DTO 클래스 (`MemberBenchmarkResponse.java`)

```java
public record MemberBenchmarkResponse(
    Long memberId,
    String loginId,
    DataSource source,  // REDIS_CACHE, DATABASE, CACHE_MISS_THEN_DB
    long queryTimeNanos,
    double queryTimeMs
) {
    public static MemberBenchmarkResponse fromCache(...) { ... }
    public static MemberBenchmarkResponse fromDatabase(...) { ... }
    public static MemberBenchmarkResponse fromCacheMiss(...) { ... }
}
```

### 2.2 BenchmarkService 확장

| 메서드 | 역할 |
|--------|------|
| `getMemberWithCache(Long)` | 캐시 경유 조회 |
| `getMemberDirectFromDb(Long)` | DB 직접 조회 |
| `evictMemberCache(Long)` | 캐시 무효화 |
| `warmupMemberCache(Long)` | 캐시 워밍업 |

### 2.3 BenchmarkController 엔드포인트

| HTTP | 경로 | 설명 |
|------|------|------|
| GET | `/api/v1/benchmark/member/cache/{id}` | 캐시 조회 |
| GET | `/api/v1/benchmark/member/db/{id}` | DB 직접 조회 |
| POST | `/api/v1/benchmark/member/cache/warmup/{id}` | 캐시 워밍업 |
| DELETE | `/api/v1/benchmark/member/cache/{id}` | 캐시 삭제 |

### 2.4 벤치마크 스크립트

`scripts/benchmark/run-cache-benchmark.sh`:
- 10회 반복 측정
- 평균/최소/최대 통계 계산
- 결과 Markdown 파일 자동 생성

---

## 3️⃣ 실행 방법

```bash
# 1. 벤치마크 환경 시작
docker-compose -f docker-compose-benchmark.yml up -d --build

# 2. 앱 시작 대기 (약 30초)
sleep 30

# 3. 벤치마크 실행
./scripts/benchmark/run-cache-benchmark.sh

# 4. 환경 종료
docker-compose -f docker-compose-benchmark.yml down -v
```

---

## 4️⃣ 파일 변경 목록

| 파일 | 상태 |
|------|------|
| `src/main/java/com/Hamalog/dto/benchmark/MemberBenchmarkResponse.java` | ✅ 신규 |
| `src/main/java/com/Hamalog/service/benchmark/BenchmarkService.java` | ✅ 수정 |
| `src/main/java/com/Hamalog/controller/benchmark/BenchmarkController.java` | ✅ 수정 |
| `scripts/benchmark/run-cache-benchmark.sh` | ✅ 신규 |
| `docs/internal/BENCHMARK-CACHE-PLAN.md` | ✅ 신규 |

---

## 5️⃣ 실행 순서

1. [x] 계획 문서 작성
2. [x] DTO 클래스 생성 (`MemberBenchmarkResponse.java`)
3. [x] BenchmarkService 확장 (캐시/DB 조회 메서드)
4. [x] BenchmarkController 확장 (4개 엔드포인트)
5. [x] 벤치마크 스크립트 작성 (`run-cache-benchmark.sh`)
6. [ ] Docker 환경에서 테스트 실행
7. [ ] 결과 문서화

---

## 6️⃣ 성능 테스트 완료 보고서 (템플릿)

> **테스트 완료 후 아래 내용을 작성합니다.**

### 6.1 테스트 환경

| 항목 | 값 |
|------|-----|
| 테스트 일시 | YYYY-MM-DD HH:MM |
| Docker 환경 | `docker-compose-benchmark.yml` |
| Spring 프로파일 | `benchmark` |
| JVM 옵션 | `-Xms512m -Xmx1024m` |
| MySQL | 8.0 |
| Redis | 7.x |

### 6.2 테스트 조건

| 항목 | 값 |
|------|-----|
| 반복 횟수 | 10회 |
| 대상 회원 ID | 1 |
| 워밍업 | 캐시 무효화 후 1회 조회 |

### 6.3 측정 결과

| 방식 | 평균 (ms) | 최소 (ms) | 최대 (ms) |
|------|-----------|-----------|-----------|
| **DB 직접 조회** | - | - | - |
| **캐시 HIT** | - | - | - |

### 6.4 개선 효과

| 지표 | 값 |
|------|-----|
| 응답시간 단축 | -% |
| 성능 향상 | -배 |

### 6.5 분석 및 결론

#### ✅ 주요 성과
- Redis 캐시 적용으로 회원 조회 응답시간 **-% 개선**
- DB 부하 감소로 전체 시스템 처리량 향상 예상

#### 📊 병목 지점 분석
| 단계 | Before | After | 개선 |
|------|--------|-------|------|
| 회원 인증 정보 조회 | ~15ms (DB) | ~1ms (Redis) | 93% ↓ |

#### 💡 추가 개선 방안
1. 캐시 TTL 최적화 (현재 30분)
2. 캐시 워밍업 전략 (서버 시작 시 인기 데이터 프리로드)
3. 2차 캐시(Local Cache) 적용 검토

### 6.6 Raw Data

```
# DB 직접 조회 (ms)
[측정값 배열]

# 캐시 HIT (ms)
[측정값 배열]
```

---

**작성일**: 2026년 1월 18일
