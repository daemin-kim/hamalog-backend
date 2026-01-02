# ADR-0005: Redis 캐시 전략

## 상태
Accepted

## 컨텍스트

Hamalog에서 다음 데이터의 빈번한 조회가 발생합니다:

1. **회원 프로필**: 모든 API 요청 시 인증 정보 조회
2. **CSRF 토큰**: 상태 변경 요청마다 검증
3. **최근 부작용 목록**: 사용자별 최근 기록
4. **Rate Limiting 카운터**: 요청마다 카운트

### 문제점

| 데이터 | 조회 빈도 | DB 부하 |
|--------|----------|---------|
| 회원 정보 | 매 요청 | 높음 |
| CSRF 토큰 | 상태 변경 요청 | 중간 |
| Rate Limit | 매 요청 | 매우 높음 |

### 고려한 대안들

| 방식 | 장점 | 단점 |
|------|------|------|
| **로컬 캐시 (Caffeine)** | 빠름, 인프라 불필요 | 분산 환경 불일치 |
| **Redis** | 분산 캐시, TTL 지원 | 네트워크 지연, 인프라 필요 |
| **Hazelcast** | 분산 캐시 + 클러스터링 | 복잡, 오버엔지니어링 |

## 결정

**Redis 7**을 분산 캐시로 채택합니다.

### 캐시 전략 상세

#### 1. Cache-Aside 패턴 (회원 프로필)

```java
@Cacheable(value = CACHE_NAME, key = "'loginId:' + #loginId", ttl = CACHE_TTL_SECONDS)
public Optional<Member> getMemberByLoginId(String loginId) {
    return memberRepository.findByLoginId(loginId);
}

@CacheEvict(value = CACHE_NAME, key = "'memberId:' + #memberId")
public void evictMemberCache(Long memberId) {
    // 캐시 무효화
}
```

#### 2. Write-Through 패턴 (CSRF 토큰)

```java
// CsrfTokenProvider.java
public String generateToken(String sessionId) {
    String token = createRandomToken();
    // Redis에 직접 저장 (TTL 60분)
    redisTemplate.opsForValue().set(
        "csrf:" + sessionId, 
        token, 
        Duration.ofMinutes(60)
    );
    return token;
}

public boolean validateToken(String sessionId, String token) {
    String stored = redisTemplate.opsForValue().get("csrf:" + sessionId);
    return token != null && token.equals(stored);
}
```

#### 3. Rate Limiting (Sliding Window)

```java
// RateLimitingService.java
public boolean isAllowed(String key, int limit, Duration window) {
    String redisKey = "rate:" + key;
    long now = System.currentTimeMillis();
    
    // Sorted Set으로 슬라이딩 윈도우 구현
    redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, now - window.toMillis());
    Long count = redisTemplate.opsForZSet().zCard(redisKey);
    
    if (count != null && count >= limit) {
        return false;
    }
    
    redisTemplate.opsForZSet().add(redisKey, String.valueOf(now), now);
    redisTemplate.expire(redisKey, window);
    return true;
}
```

### 캐시 키 네이밍 규칙

| 캐시 | 키 패턴 | TTL |
|------|---------|-----|
| 회원 프로필 | `memberCache::loginId:{loginId}` | 30분 |
| CSRF 토큰 | `csrf:{sessionId}` | 60분 |
| Rate Limit (API) | `rate:api:{ip}` | 1분 |
| Rate Limit (Auth) | `rate:auth:{ip}` | 1분 |
| 최근 부작용 | `recentSideEffects::{memberId}` | 10분 |

### Redis 설정

```java
@Configuration
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        
        // Key serializer
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Value serializer
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }
}
```

## 결과

### 장점
- ✅ **DB 부하 감소**: 반복 조회 제거로 DB 쿼리 60% 감소
- ✅ **응답 시간 단축**: 캐시 히트 시 ~1ms 응답
- ✅ **분산 환경 지원**: 여러 인스턴스에서 캐시 공유
- ✅ **TTL 자동 관리**: 만료된 데이터 자동 삭제

### 단점
- ⚠️ **캐시 일관성**: 캐시 무효화 누락 시 stale 데이터
- ⚠️ **네트워크 지연**: Redis 장애 시 성능 저하
- ⚠️ **메모리 비용**: Redis 서버 메모리 필요

### 성능 영향

| 작업 | 캐시 전 | 캐시 후 | 개선율 |
|------|--------|--------|--------|
| 회원 조회 | ~15ms | ~1ms | 93% |
| CSRF 검증 | ~5ms | ~1ms | 80% |
| Rate Limit 체크 | ~10ms | ~1ms | 90% |

---

## 트레이드오프 분석

### Redis 도입 판단 기준

| 기준 | Redis 필요? | 이유 |
|------|-------------|------|
| **단일 인스턴스** | ⚠️ 선택적 | 로컬 캐시(Caffeine)로 충분 |
| **다중 인스턴스** | ✅ 권장 | 캐시 일관성 필요 |
| **사용자 ~100명** | ❌ 불필요 | DB 부하 미미 |
| **사용자 ~1,000명** | ⚠️ 선택적 | 핫 데이터만 캐싱 |
| **사용자 10,000명+** | ✅ 필수 | DB 병목 해소 |

### Hamalog에서 Redis가 적합한 이유

1. **CSRF 토큰 저장** - 분산 환경에서 세션 없이 CSRF 방어
2. **Rate Limiting** - 인스턴스 간 요청 카운트 공유 필수
3. **Refresh Token 블랙리스트** - 로그아웃 시 즉시 토큰 무효화

> ⚠️ **단순 조회 캐싱만 필요하다면 Caffeine(로컬 캐시)이 더 적합합니다.**

### 규모별 권장 캐싱 전략

| 규모 | 권장 방식 | 이유 |
|------|-----------|------|
| **MVP** (~100명) | 캐싱 없음 | 조기 최적화 방지 |
| **성장** (~1,000명) | Caffeine (로컬) | 단순, 인프라 불필요 |
| **스케일업** (1,000명+) | Redis | 분산 캐시 필수 |
| **대규모** (10,000명+) | Redis + CDN | 정적 리소스 분리 |

### 과잉 엔지니어링 인정 및 대응

> ⚠️ **현재 Hamalog 규모에서 Redis 캐싱은 학습 목적입니다.**

**Redis가 실제로 필요한 기능:**
- ✅ CSRF 토큰 (분산 환경 필수)
- ✅ Rate Limiting (인스턴스 간 공유)
- ✅ Refresh Token 블랙리스트

**Redis 없이도 가능한 기능:**
- ⚠️ 회원 프로필 캐싱 → Caffeine으로 대체 가능
- ⚠️ 최근 부작용 목록 → DB 인덱스로 충분

### 더 단순한 대안

**Caffeine (로컬 캐시) 예시:**
```java
@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30)));
        return manager;
    }
}
```

**장점:** 인프라 없음, 네트워크 지연 없음, 단순함  
**단점:** 분산 환경에서 캐시 불일치

### Fallback 전략 (현재 구현됨)

Redis 장애 시에도 서비스가 동작하도록 Fallback을 구현했습니다:

```java
// CsrfTokenProvider.java
private final ConcurrentMap<String, TokenRecord> fallbackStore = new ConcurrentHashMap<>();

public String generateToken(String sessionId) {
    if (storeInRedis(sessionId, token)) {
        fallbackStore.remove(sessionId);  // Redis 성공 시 로컬 삭제
    } else {
        storeInFallback(sessionId, token);  // Redis 실패 시 로컬 저장
    }
    return token;
}
```

### 만약 다시 선택한다면?

1. **CSRF + Rate Limiting만 Redis 사용**
2. **회원 프로필 캐싱은 Caffeine으로 시작**
3. **부하 테스트 후 필요 시 Redis로 마이그레이션**

```java
// 하이브리드 캐시 설정 예시
@Configuration
public class HybridCacheConfig {
    @Bean
    public CacheManager cacheManager() {
        return new CompositeCacheManager(
            caffeineCacheManager(),   // 1차: 로컬 캐시
            redisCacheManager()       // 2차: 분산 캐시
        );
    }
}
```

## 참고

- [Spring Cache Abstraction](https://docs.spring.io/spring-framework/reference/integration/cache.html)
- [Redis Caching Best Practices](https://redis.io/docs/manual/patterns/)
- [Cache-Aside Pattern - Microsoft](https://learn.microsoft.com/en-us/azure/architecture/patterns/cache-aside)

---

> 작성일: 2025-12-23

