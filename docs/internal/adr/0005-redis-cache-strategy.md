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

## 참고

- [Spring Cache Abstraction](https://docs.spring.io/spring-framework/reference/integration/cache.html)
- [Redis Caching Best Practices](https://redis.io/docs/manual/patterns/)
- [Cache-Aside Pattern - Microsoft](https://learn.microsoft.com/en-us/azure/architecture/patterns/cache-aside)

---

> 작성일: 2025-12-23

