# 🗄️ Hamalog 캐싱 패턴

> 이 문서는 Hamalog 프로젝트의 **2-Tier 캐시(Caffeine + Redis)** 기반 캐싱 전략과 패턴을 설명합니다.
> 성능 최적화와 일관성 유지를 위해 이 패턴을 따라야 합니다.

---

## 📋 목차

1. [캐싱 아키텍처](#1-캐싱-아키텍처)
2. [캐시 종류와 TTL](#2-캐시-종류와-ttl)
3. [캐싱 어노테이션 사용법](#3-캐싱-어노테이션-사용법)
4. [캐시 무효화 전략](#4-캐시-무효화-전략)
5. [분산 캐시 고려사항](#5-분산-캐시-고려사항)
6. [모니터링 및 디버깅](#6-모니터링-및-디버깅)
7. [2-Tier 캐시 전략](#7-2-tier-캐시-전략)

---

## 1. 캐싱 아키텍처

### 1.1 2-Tier 캐시 구조

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              Client                                      │
└─────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                         Application Server                               │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐                  │
│  │ Controller  │ →  │   Service   │ →  │ Repository  │                  │
│  └─────────────┘    └─────────────┘    └─────────────┘                  │
│                            ↓                   ↓                         │
│                    ┌──────────────┐                                      │
│                    │  L1: Caffeine │ ← 로컬 메모리 (~0.01ms)             │
│                    │  (10,000개)   │                                     │
│                    └──────────────┘                                      │
│                            ↓ MISS                                        │
│                    ┌──────────────┐     ┌───────────┐                    │
│                    │  L2: Redis   │     │   MySQL   │                    │
│                    │  (분산캐시)   │     │   (DB)    │                    │
│                    │  (~0.5-1ms)  │     │ (~2-3ms)  │                    │
│                    └──────────────┘     └───────────┘                    │
└─────────────────────────────────────────────────────────────────────────┘
```

### 1.2 캐시 조회 흐름

```
[조회 흐름]
1. L1 Caffeine 조회 → HIT → 즉시 반환 (네트워크 RTT 없음)
              ↓ MISS
2. L2 Redis 조회   → HIT → L1에 저장 후 반환
              ↓ MISS
3. DB 조회         → L2 + L1에 저장 후 반환
```

### 1.3 캐시 레이어 비교

| 레이어 | 특징 | 평균 응답시간 | 용도 |
|--------|------|---------------|------|
| **L1 (Caffeine)** | 로컬 메모리, 네트워크 RTT 없음 | ~0.01ms | Hot 데이터, 초고속 조회 |
| **L2 (Redis)** | 분산 캐시, 서버 간 공유 | ~0.5-1ms | Warm 데이터, 분산 환경 |
| **Database** | 영구 저장소 | ~2-3ms | Cold 데이터, Source of Truth |

---

## 2. 캐시 종류와 TTL

### 2.1 캐시 정의

```java
// CacheConfig.java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
            "memberCache", defaultConfig.entryTtl(Duration.ofMinutes(5)),
            "memberProfile", defaultConfig.entryTtl(Duration.ofHours(1)),
            "medicationStats", defaultConfig.entryTtl(Duration.ofMinutes(30)),
            "diaryStats", defaultConfig.entryTtl(Duration.ofMinutes(30))
        );

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigs)
            .build();
    }
}
```

### 2.2 캐시 목록

| 캐시명 | TTL | 용도 | 키 패턴 |
|--------|-----|------|---------|
| `memberCache` | 5분 | 인증용 회원 정보 | `loginId:{loginId}`, `memberId:{id}` |
| `memberProfile` | 1시간 | 회원 프로필 | `{memberId}` |
| `medicationStats` | 30분 | 복약 통계 | `member:{memberId}:year:{year}` |
| `diaryStats` | 30분 | 일기 통계 | `member:{memberId}:month:{month}` |
| `scheduleList` | 10분 | 복약 스케줄 목록 | `member:{memberId}` |

### 2.3 TTL 결정 기준

| 데이터 특성 | 권장 TTL | 이유 |
|-------------|----------|------|
| 자주 변경됨 | 1-5분 | 데이터 정합성 |
| 가끔 변경됨 | 10-30분 | 밸런스 |
| 거의 안 변함 | 1-24시간 | 성능 최적화 |
| 민감한 데이터 | 5분 이하 | 보안 |

---

## 3. 캐싱 어노테이션 사용법

### 3.1 @Cacheable - 캐시 저장/조회

```java
@Service
@RequiredArgsConstructor
public class MemberCacheService {

    private final MemberRepository memberRepository;

    // 캐시에 있으면 반환, 없으면 메서드 실행 후 캐시 저장
    @Cacheable(value = "memberCache", key = "'loginId:' + #loginId")
    public Member findByLoginId(String loginId) {
        return memberRepository.findByLoginId(loginId)
            .orElseThrow(ErrorCode.MEMBER_NOT_FOUND::toException);
    }

    // 복합 키 사용
    @Cacheable(value = "medicationStats", key = "'member:' + #memberId + ':year:' + #year")
    public MedicationStatsResponse getYearlyStats(Long memberId, int year) {
        // 복잡한 통계 쿼리 실행
        return calculateStats(memberId, year);
    }

    // 조건부 캐싱
    @Cacheable(value = "memberProfile", key = "#memberId", 
               condition = "#memberId != null",
               unless = "#result == null")
    public MemberProfileResponse getProfile(Long memberId) {
        return memberRepository.findById(memberId)
            .map(MemberProfileResponse::from)
            .orElse(null);
    }
}
```

### 3.2 @CacheEvict - 캐시 무효화

```java
@Service
@RequiredArgsConstructor
public class MemberProfileService {

    // 단일 키 무효화
    @CacheEvict(value = "memberProfile", key = "#memberId")
    @Transactional
    public MemberProfileResponse updateProfile(Long memberId, ProfileUpdateRequest request) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(ErrorCode.MEMBER_NOT_FOUND::toException);
        member.updateProfile(request);
        return MemberProfileResponse.from(member);
    }

    // 다중 캐시 무효화
    @Caching(evict = {
        @CacheEvict(value = "memberCache", key = "'memberId:' + #memberId"),
        @CacheEvict(value = "memberProfile", key = "#memberId")
    })
    @Transactional
    public void deleteAccount(Long memberId) {
        memberRepository.deleteById(memberId);
    }

    // 전체 캐시 무효화 (주의: 성능 영향)
    @CacheEvict(value = "scheduleList", allEntries = true)
    @Transactional
    public void bulkUpdateSchedules(List<Long> scheduleIds) {
        // 대량 업데이트
    }
}
```

### 3.3 @CachePut - 캐시 강제 갱신

```java
// 항상 메서드를 실행하고 결과를 캐시에 저장
@CachePut(value = "memberProfile", key = "#memberId")
@Transactional
public MemberProfileResponse forceRefreshProfile(Long memberId) {
    Member member = memberRepository.findById(memberId)
        .orElseThrow(ErrorCode.MEMBER_NOT_FOUND::toException);
    return MemberProfileResponse.from(member);
}
```

### 3.4 SpEL 표현식 활용

```java
// 메서드 파라미터 참조
@Cacheable(value = "cache", key = "#request.memberId")

// 결과 객체 참조 (unless에서)
@Cacheable(value = "cache", unless = "#result.isEmpty()")

// 정적 메서드 호출
@Cacheable(value = "cache", key = "T(java.util.UUID).randomUUID().toString()")

// 조건 조합
@Cacheable(value = "cache", 
           condition = "#memberId > 0", 
           unless = "#result == null || #result.size() == 0")
```

---

## 4. 캐시 무효화 전략

### 4.1 전략 비교

| 전략 | 장점 | 단점 | 사용 시기 |
|------|------|------|-----------|
| **TTL 기반** | 단순, 자동 정리 | 정합성 지연 | 통계, 목록 |
| **Write-Through** | 즉시 반영 | 구현 복잡 | 중요 데이터 |
| **Event 기반** | 느슨한 결합 | 이벤트 유실 가능 | 도메인 이벤트 |

### 4.2 Write-Through 패턴

```java
@Service
@Transactional
public class MedicationScheduleService {

    @CacheEvict(value = "scheduleList", key = "'member:' + #request.memberId()")
    public MedicationScheduleResponse create(MedicationScheduleCreateRequest request) {
        // 1. DB 저장
        MedicationSchedule saved = scheduleRepository.save(
            new MedicationSchedule(request, member)
        );
        
        // 2. 캐시는 @CacheEvict로 무효화 (다음 조회 시 갱신)
        return MedicationScheduleResponse.from(saved);
    }
}
```

### 4.3 이벤트 기반 무효화

```java
// 도메인 이벤트 발행
@Entity
public class MedicationSchedule extends AbstractAggregateRoot<MedicationSchedule> {
    
    public void update(UpdateRequest request) {
        this.name = request.name();
        registerEvent(new MedicationScheduleUpdatedEvent(this.id, this.member.getMemberId()));
    }
}

// 이벤트 리스너에서 캐시 무효화
@Component
public class CacheEvictionListener {

    private final CacheManager cacheManager;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onScheduleUpdated(MedicationScheduleUpdatedEvent event) {
        Cache cache = cacheManager.getCache("scheduleList");
        if (cache != null) {
            cache.evict("member:" + event.getMemberId());
        }
    }
}
```

---

## 5. 분산 캐시 고려사항

### 5.1 캐시 일관성

멀티 인스턴스 환경에서 Redis 캐시는 자동으로 공유됩니다.

```
┌─────────────┐     ┌─────────────┐
│  Server 1   │     │  Server 2   │
└──────┬──────┘     └──────┬──────┘
       │                   │
       └───────┬───────────┘
               ↓
        ┌─────────────┐
        │    Redis    │
        │   (공유)    │
        └─────────────┘
```

### 5.2 직렬화 주의사항

```java
// DTO는 직렬화 가능해야 함
public record MemberProfileResponse(
    Long memberId,
    String name,
    String email,
    LocalDateTime createdAt  // Jackson으로 직렬화 가능
) implements Serializable {
    
    public static MemberProfileResponse from(Member member) {
        return new MemberProfileResponse(
            member.getMemberId(),
            member.getName(),
            member.getEmail(),
            member.getCreatedAt()
        );
    }
}
```

### 5.3 캐시 키 충돌 방지

```java
// ❌ 충돌 가능
@Cacheable(value = "cache", key = "#id")
public A findA(Long id) { ... }

@Cacheable(value = "cache", key = "#id")  // 같은 캐시, 같은 키!
public B findB(Long id) { ... }

// ✅ 접두사로 구분
@Cacheable(value = "cache", key = "'a:' + #id")
public A findA(Long id) { ... }

@Cacheable(value = "cache", key = "'b:' + #id")
public B findB(Long id) { ... }

// ✅ 또는 별도 캐시 사용
@Cacheable(value = "cacheA", key = "#id")
@Cacheable(value = "cacheB", key = "#id")
```

---

## 6. 모니터링 및 디버깅

### 6.1 Redis CLI로 확인

```bash
# 모든 캐시 키 조회
redis-cli KEYS "*"

# 특정 캐시 값 확인
redis-cli GET "memberProfile::123"

# TTL 확인
redis-cli TTL "memberProfile::123"

# 캐시 삭제
redis-cli DEL "memberProfile::123"

# 패턴으로 삭제
redis-cli KEYS "medicationStats*" | xargs redis-cli DEL
```

### 6.2 Spring Actuator 메트릭

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: caches, metrics
```

```
GET /actuator/caches
GET /actuator/metrics/cache.gets
GET /actuator/metrics/cache.puts
GET /actuator/metrics/cache.evictions
```

### 6.3 로깅 활성화

```yaml
# application.yml
logging:
  level:
    org.springframework.cache: DEBUG
    org.springframework.data.redis: DEBUG
```

### 6.4 캐시 히트율 모니터링

```java
@Aspect
@Component
@Slf4j
public class CacheMonitoringAspect {

    private final MeterRegistry meterRegistry;

    @Around("@annotation(cacheable)")
    public Object monitorCacheHit(ProceedingJoinPoint joinPoint, Cacheable cacheable) 
            throws Throwable {
        String cacheName = cacheable.value()[0];
        
        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long duration = System.currentTimeMillis() - start;
        
        // 짧은 실행시간 = 캐시 히트로 추정
        if (duration < 10) {
            meterRegistry.counter("cache.hit", "cache", cacheName).increment();
        } else {
            meterRegistry.counter("cache.miss", "cache", cacheName).increment();
        }
        
        return result;
    }
}
```

---

## 7. 베스트 프랙티스

### 7.1 DO

```java
// ✅ 읽기 전용 데이터 캐싱
@Cacheable(value = "staticData", key = "#code")
public CodeResponse getCode(String code) { ... }

// ✅ 복합 키 사용
@Cacheable(value = "stats", key = "'member:' + #memberId + ':date:' + #date")

// ✅ 조건부 캐싱
@Cacheable(value = "cache", unless = "#result == null")

// ✅ 트랜잭션 커밋 후 캐시 무효화
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
```

### 7.2 DON'T

```java
// ❌ 너무 긴 TTL (데이터 불일치)
@Cacheable(value = "cache", key = "#id")  // TTL: 24시간

// ❌ 가변 객체 캐싱
@Cacheable(value = "cache", key = "#id")
public MutableEntity find(Long id) { ... }  // 엔티티 직접 캐싱 위험

// ❌ 트랜잭션 내 캐시 무효화 (롤백 시 문제)
@Transactional
public void update() {
    cache.evict(key);  // 트랜잭션 롤백해도 캐시는 이미 삭제됨
    // ...
}
```

---

## 8. 2-Tier 캐시 전략

### 8.1 아키텍처 개요

Hamalog는 **Caffeine(L1) + Redis(L2)** 2-Tier 캐시를 사용하여 극강의 성능을 달성합니다.

```java
// CachingAspect.java - 핵심 구현
private Object getCachedValue(String key, boolean useLocalFallback) {
    // L1: Caffeine 로컬 캐시에서 먼저 조회 (네트워크 RTT 없음)
    if (useLocalFallback && l1Cache != null) {
        Object l1Value = l1Cache.getIfPresent(key);
        if (l1Value != null) {
            l1Hits.increment();
            return l1Value;  // ~0.01ms
        }
    }
    
    // L2: Redis에서 조회 (네트워크 RTT 있음)
    if (redisTemplate != null) {
        Object l2Value = redisTemplate.opsForValue().get(key);
        if (l2Value != null) {
            l2Hits.increment();
            // L2 히트 시 L1에도 저장 (Read-Through)
            if (useLocalFallback && l1Cache != null) {
                l1Cache.put(key, l2Value);
            }
            return l2Value;  // ~0.5-1ms
        }
    }
    
    cacheMisses.increment();
    return null;  // DB 조회 필요
}
```

### 8.2 Caffeine 설정

```java
@PostConstruct
public void init() {
    l1Cache = Caffeine.newBuilder()
            .maximumSize(10_000)                    // 최대 10,000개 항목
            .expireAfterWrite(5, TimeUnit.MINUTES)  // 5분 후 만료 (L2보다 짧게)
            .recordStats()                          // 통계 기록 활성화
            .build();
}
```

### 8.3 성능 비교

| 캐시 레이어 | 평균 응답시간 | DB 대비 향상 | 특징 |
|-------------|---------------|--------------|------|
| **L1 (Caffeine)** | ~0.05ms | **~50x** | 네트워크 RTT 없음 |
| **L2 (Redis)** | ~0.8ms | ~3x | 분산 환경 공유 |
| **Database** | ~2.4ms | baseline | 디스크 I/O |

### 8.4 TTL 전략

| 레이어 | TTL | 이유 |
|--------|-----|------|
| L1 (Caffeine) | 5분 | L2보다 짧아 데이터 일관성 확보 |
| L2 (Redis) | 10분 | 적절한 밸런스 |

### 8.5 캐시 무효화

```java
// 단일 키 무효화 (L1 + L2 모두)
private void evictSingleKey(String key) {
    // L2: Redis에서 삭제
    if (redisTemplate != null) {
        redisTemplate.delete(key);
    }
    
    // L1: Caffeine에서 삭제
    if (l1Cache != null) {
        l1Cache.invalidate(key);
    }
}
```

### 8.6 벤치마크

```bash
# 2-Tier 캐시 성능 비교 API
curl "http://localhost:8080/api/v1/benchmark/cache/two-tier/1?iterations=10"
```

---

## 📚 관련 문서

- [ADR-0005: Redis 캐시 전략](../adr/0005-redis-cache-strategy.md)
- [커스텀 어노테이션 가이드](./ANNOTATION-GUIDE.md)
- [보안 패턴](./SECURITY-PATTERNS.md)
- [캐시 벤치마크 보고서](../../benchmark-results/CACHE-BENCHMARK-REPORT-20260118.md)

---

> 📝 최종 업데이트: 2026년 1월 24일

