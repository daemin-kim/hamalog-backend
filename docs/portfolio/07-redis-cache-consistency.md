# 07. Redis 캐시 일관성 전략

> **Cache-Aside 패턴과 선제적 무효화로 캐시-DB 간 일관성을 보장하고, 커스텀 AOP로 유연한 캐싱을 구현하는 전략**

---

## 📋 목차

1. [문제 상황](#1-문제-상황-problem)
2. [해결 전략](#2-해결-전략-solution-strategy)
3. [구현 상세](#3-구현-상세-implementation)
4. [효과 및 검증](#4-효과-및-검증-results)
5. [면접 대비 Q&A](#5-면접-대비-qa)

---

## 1. 문제 상황 (Problem)

### 1.1 캐시 사용의 필요성

Hamalog에서 자주 조회되는 데이터:

| 데이터 | 조회 빈도 | 변경 빈도 | 캐싱 효과 |
|--------|----------|----------|----------|
| **회원 인증 정보** | 매 요청 | 드묾 | 🔥 매우 높음 |
| **알림 설정** | 알림 발송마다 | 드묾 | 🔥 높음 |
| **복약 통계** | 대시보드 접근 | 기록 변경 시 | 🔥 높음 |
| **일기 통계** | 대시보드 접근 | 일기 변경 시 | 🔥 높음 |

```
📊 캐싱 전후 비교 (회원 조회)

Before: 매 API 요청마다 DB 조회
  → 1000 요청/초 = 1000 DB 쿼리/초

After: Redis 캐시 활용 (HIT율 95%)
  → 1000 요청/초 = 50 DB 쿼리/초 + 950 캐시 조회
  → DB 부하 95% 감소!
```

### 1.2 캐시 불일치 문제

캐시를 사용하면 **캐시와 DB 간 데이터 불일치**가 발생할 수 있습니다.

```
┌─────────────────────────────────────────────────────────────────┐
│                    캐시 불일치 시나리오                          │
│                                                                  │
│  시간    사용자 A              Cache           DB                │
│  ────────────────────────────────────────────────────────────── │
│  T1      프로필 조회 ────────→ MISS ─────────→ name="김철수"     │
│  T2      ←───────────── name="김철수" (캐시 저장)                │
│  T3      프로필 수정 ─────────────────────────→ name="김영희"    │
│  T4                      name="김철수"         name="김영희"     │
│                          ↑ 불일치!             ↑ 실제 값         │
│  T5      프로필 조회 ────────→ HIT                               │
│  T6      ←───────────── name="김철수"  (잘못된 캐시 값!)         │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 1.3 문제 유형

| 문제 | 설명 | 영향 |
|------|------|------|
| **Stale Data** | 캐시에 오래된 데이터 | 사용자가 변경 후에도 이전 값 조회 |
| **Cache Stampede** | 캐시 만료 시 동시 DB 요청 | DB 과부하, 응답 지연 |
| **Cache Penetration** | 존재하지 않는 데이터 반복 조회 | DB 불필요한 부하 |
| **분산 환경 불일치** | 여러 서버의 캐시 동기화 | 서버마다 다른 데이터 반환 |

---

## 2. 해결 전략 (Solution Strategy)

### 2.1 고려한 캐시 전략

| 패턴 | 동작 | 장점 | 단점 |
|------|------|------|------|
| **Cache-Aside** | 애플리케이션이 캐시 관리 | 유연함, 제어 가능 | 코드 복잡도 |
| **Read-Through** | 캐시가 DB 조회 대행 | 투명함 | 캐시 라이브러리 의존 |
| **Write-Through** | 쓰기 시 캐시+DB 동시 갱신 | 일관성 보장 | 쓰기 지연 |
| **Write-Behind** | 캐시만 갱신, 비동기로 DB | 쓰기 성능 우수 | 데이터 유실 위험 |

### 2.2 최종 선택: Cache-Aside + 선제적 무효화

```
┌─────────────────────────────────────────────────────────────────┐
│                    Cache-Aside 패턴                              │
│                                                                  │
│  [읽기 흐름]                                                     │
│                                                                  │
│  요청 → @Cacheable ─→ 캐시 조회 ─→ HIT → 캐시 값 반환           │
│                          │                                       │
│                          └→ MISS → DB 조회 → 캐시 저장 → 반환   │
│                                                                  │
│  [쓰기 흐름 - 선제적 무효화]                                     │
│                                                                  │
│  요청 → @CacheEvict → 캐시 삭제 → DB 업데이트 → 완료            │
│                          │                                       │
│                          └→ 다음 조회 시 캐시 갱신               │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 2.3 왜 이 방식인가?

1. **명시적 제어**: 어떤 메서드가 캐싱되는지 코드로 명확
2. **선제적 무효화**: 데이터 변경 즉시 캐시 삭제로 불일치 최소화
3. **TTL 안전망**: 무효화 누락 시에도 TTL로 자동 갱신
4. **Redis 분산 캐시**: 여러 서버가 같은 캐시 공유

---

## 3. 구현 상세 (Implementation)

### 3.1 커스텀 캐싱 어노테이션

```java
/**
 * 커스텀 Cacheable 어노테이션
 * 
 * Spring @Cacheable 대신 커스텀 구현을 사용하는 이유:
 * 1. TTL을 어노테이션에서 직접 지정
 * 2. 로컬 캐시 fallback 지원
 * 3. 상세한 로깅 및 메트릭
 * 4. 캐시 조건 세밀한 제어
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Cacheable {
    
    /**
     * 캐시 이름 (네임스페이스)
     * Redis 키 접두사로 사용: "cache:{value}:{key}"
     */
    String value();
    
    /**
     * 캐시 키 생성 SpEL 표현식
     * 예: "'loginId:' + #loginId"
     *     "'memberId:' + #memberId"
     */
    String key() default "";
    
    /**
     * TTL (Time-To-Live) 초 단위
     * 0이면 만료 없음 (권장하지 않음)
     */
    long ttl() default 300;  // 기본 5분
    
    /**
     * Redis 장애 시 로컬 캐시 사용 여부
     */
    boolean useLocalFallback() default true;
    
    /**
     * null 결과도 캐싱할지 여부
     * true면 Cache Penetration 방지 가능
     */
    boolean cacheNull() default false;
}

/**
 * 캐시 무효화 어노테이션
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheEvict {
    
    /**
     * 무효화할 캐시 이름
     */
    String value();
    
    /**
     * 무효화할 캐시 키 SpEL 표현식
     */
    String key() default "";
    
    /**
     * 해당 캐시의 모든 항목 무효화
     */
    boolean allEntries() default false;
    
    /**
     * 메서드 실행 전에 무효화할지 (기본: 실행 후)
     */
    boolean beforeInvocation() default false;
}
```

### 3.2 캐싱 AOP Aspect 구현

```java
/**
 * 캐싱 AOP Aspect
 * 
 * @Cacheable, @CacheEvict 어노테이션을 처리하여
 * Redis 캐시 조회/저장/무효화를 자동 수행합니다.
 */
@Slf4j
@Aspect
@Component
@ConditionalOnProperty(name = "app.aop.cache.enabled", matchIfMissing = true)
@Order(5)  // 다른 Aspect보다 먼저 실행
public class CachingAspect {
    
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    
    // 캐시 통계 (모니터링용)
    private final ConcurrentHashMap<String, CacheStats> cacheStats = new ConcurrentHashMap<>();
    
    // 로컬 캐시 (Redis 장애 시 fallback)
    private final ConcurrentHashMap<String, CacheEntry> localCache = new ConcurrentHashMap<>();
    
    // ============================================================
    // @Cacheable 처리
    // ============================================================
    
    @Around("@annotation(cacheable)")
    public Object handleCaching(ProceedingJoinPoint joinPoint, Cacheable cacheable) throws Throwable {
        String methodName = getMethodName(joinPoint);
        String cacheKey = generateCacheKey(joinPoint, cacheable);
        String cacheName = cacheable.value();
        long ttl = cacheable.ttl();
        boolean useLocalFallback = cacheable.useLocalFallback();
        
        // 요청 추적 ID 설정 (로그 상관관계)
        String cacheId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("cache.id", cacheId);
        
        try {
            // ============================================================
            // Step 1: 캐시에서 값 조회
            // ============================================================
            
            Object cachedValue = getCachedValue(cacheKey, useLocalFallback);
            
            if (cachedValue != null) {
                // 캐시 히트: 캐시된 값 반환
                updateCacheStats(cacheName, true);  // HIT 통계 증가
                
                log.debug("CACHE_HIT: {} | Method: {} | Key: {} | Cache: {}", 
                         cacheId, methodName, shortenKey(cacheKey), cacheName);
                
                return cachedValue;
            }
            
            // ============================================================
            // Step 2: 캐시 미스 - 실제 메서드 실행
            // ============================================================
            
            updateCacheStats(cacheName, false);  // MISS 통계 증가
            
            log.debug("CACHE_MISS: {} | Method: {} | Key: {} | Cache: {} | Executing method", 
                     cacheId, methodName, shortenKey(cacheKey), cacheName);
            
            long startTime = System.currentTimeMillis();
            Object result = joinPoint.proceed();  // 실제 메서드 호출
            long executionTime = System.currentTimeMillis() - startTime;
            
            // ============================================================
            // Step 3: 결과를 캐시에 저장
            // ============================================================
            
            if (shouldCache(result, cacheable)) {
                cacheValue(cacheKey, result, ttl, useLocalFallback);
                
                log.info("CACHE_STORED: {} | Method: {} | Key: {} | TTL: {}s | ExecutionTime: {}ms", 
                        cacheId, methodName, shortenKey(cacheKey), ttl, executionTime);
            }
            
            return result;
            
        } finally {
            MDC.remove("cache.id");
        }
    }
    
    // ============================================================
    // @CacheEvict 처리
    // ============================================================
    
    @Around("@annotation(cacheEvict)")
    public Object handleCacheEviction(ProceedingJoinPoint joinPoint, CacheEvict cacheEvict) throws Throwable {
        String methodName = getMethodName(joinPoint);
        
        try {
            // beforeInvocation=true: 메서드 실행 전에 캐시 삭제
            // 사용 시나리오: 메서드가 실패해도 캐시는 무효화해야 할 때
            if (cacheEvict.beforeInvocation()) {
                evictCache(joinPoint, cacheEvict, "BEFORE");
            }
            
            Object result = joinPoint.proceed();
            
            // beforeInvocation=false (기본): 메서드 성공 후 캐시 삭제
            // 사용 시나리오: DB 업데이트 성공 후에만 캐시 무효화
            if (!cacheEvict.beforeInvocation()) {
                evictCache(joinPoint, cacheEvict, "AFTER");
            }
            
            return result;
            
        } catch (Exception e) {
            // 예외 발생 시 beforeInvocation=false면 캐시 유지
            // (DB 업데이트 실패 시 기존 캐시가 유효함)
            throw e;
        }
    }
    
    // ============================================================
    // 캐시 조회 (Redis → 로컬 fallback)
    // ============================================================
    
    private Object getCachedValue(String key, boolean useLocalFallback) {
        // 1. Redis에서 먼저 조회
        if (redisTemplate != null) {
            try {
                Object value = redisTemplate.opsForValue().get(key);
                if (value != null) {
                    return value;
                }
            } catch (Exception e) {
                log.warn("Redis cache access failed for key: {} | Using local fallback: {}", 
                        shortenKey(key), useLocalFallback);
            }
        }
        
        // 2. Redis 실패/미스 시 로컬 캐시 확인
        if (useLocalFallback) {
            CacheEntry entry = localCache.get(key);
            if (entry != null && !entry.isExpired()) {
                return entry.getValue();
            } else if (entry != null) {
                // 만료된 엔트리 삭제
                localCache.remove(key);
            }
        }
        
        return null;
    }
    
    // ============================================================
    // 캐시 저장 (Redis + 로컬)
    // ============================================================
    
    private void cacheValue(String key, Object value, long ttl, boolean useLocalFallback) {
        // 1. Redis에 저장
        if (redisTemplate != null) {
            try {
                if (ttl > 0) {
                    redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttl));
                } else {
                    redisTemplate.opsForValue().set(key, value);
                }
            } catch (Exception e) {
                log.warn("Redis cache store failed for key: {} | Using local fallback", 
                        shortenKey(key));
            }
        }
        
        // 2. 로컬 캐시에도 저장 (fallback용)
        if (useLocalFallback) {
            LocalDateTime expireAt = ttl > 0 
                ? LocalDateTime.now().plusSeconds(ttl) 
                : LocalDateTime.MAX;
            localCache.put(key, new CacheEntry(value, expireAt));
            
            // 로컬 캐시 크기 제한 (메모리 보호)
            if (localCache.size() > 10000) {
                cleanupLocalCache();
            }
        }
    }
    
    // ============================================================
    // 캐시 무효화
    // ============================================================
    
    private void evictCache(ProceedingJoinPoint joinPoint, CacheEvict cacheEvict, String timing) {
        String cacheName = cacheEvict.value();
        
        if (cacheEvict.allEntries()) {
            // 전체 캐시 무효화: 패턴으로 삭제
            String pattern = "cache:" + cacheName + ":*";
            evictByPattern(pattern);
            
            log.info("CACHE_EVICT_ALL: Cache: {} | Timing: {} | Pattern: {}", 
                    cacheName, timing, pattern);
        } else {
            // 특정 키 무효화
            String cacheKey = generateCacheKey(joinPoint, cacheName, cacheEvict.key());
            evictSingleKey(cacheKey);
            
            log.info("CACHE_EVICT_KEY: Key: {} | Cache: {} | Timing: {}", 
                    shortenKey(cacheKey), cacheName, timing);
        }
    }
    
    private void evictSingleKey(String key) {
        // Redis에서 삭제
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(key);
            } catch (Exception e) {
                log.warn("Redis cache evict failed for key: {}", shortenKey(key));
            }
        }
        
        // 로컬 캐시에서도 삭제
        localCache.remove(key);
    }
    
    // ============================================================
    // 캐시 키 생성
    // ============================================================
    
    private String generateCacheKey(ProceedingJoinPoint joinPoint, Cacheable cacheable) {
        String cacheName = cacheable.value();
        String keyExpression = cacheable.key();
        
        // SpEL 표현식 평가
        String evaluatedKey = evaluateSpelExpression(joinPoint, keyExpression);
        
        // 형식: "cache:{cacheName}:{evaluatedKey}"
        return String.format("cache:%s:%s", cacheName, evaluatedKey);
    }
}
```

### 3.3 Service에서의 사용

```java
/**
 * 회원 캐싱 서비스
 * 
 * 자주 조회되는 회원 정보를 Redis에 캐싱하여 DB 부하 감소
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class MemberCacheService {
    
    private static final String CACHE_NAME = "member";
    private static final long CACHE_TTL_SECONDS = 600;  // 10분
    
    private final MemberRepository memberRepository;
    
    // ============================================================
    // 캐시 조회 (Cache-Aside 패턴)
    // ============================================================
    
    /**
     * 로그인 ID로 회원 조회 (캐시 적용)
     * 
     * 동작:
     * 1. Redis에서 "cache:member:loginId:{loginId}" 키로 조회
     * 2. 캐시 히트 → 캐시된 Member 반환
     * 3. 캐시 미스 → DB 조회 → Redis에 저장 → 반환
     * 
     * TTL: 10분 (로그인 정보가 자주 바뀌지 않으므로 적절)
     */
    @Cacheable(value = CACHE_NAME, key = "'loginId:' + #loginId", ttl = CACHE_TTL_SECONDS)
    public Optional<Member> findByLoginId(String loginId) {
        log.debug("[MEMBER_CACHE] Cache miss - fetching member by loginId from DB");
        return memberRepository.findByLoginId(loginId);
    }
    
    /**
     * 회원 ID로 회원 조회 (캐시 적용)
     */
    @Cacheable(value = CACHE_NAME, key = "'memberId:' + #memberId", ttl = CACHE_TTL_SECONDS)
    public Optional<Member> findById(Long memberId) {
        log.debug("[MEMBER_CACHE] Cache miss - fetching member by memberId from DB");
        return memberRepository.findById(memberId);
    }
    
    // ============================================================
    // 캐시 무효화 (선제적 무효화 패턴)
    // ============================================================
    
    /**
     * 회원 정보 변경 시 캐시 무효화
     * 
     * 호출 시점:
     * - 회원 프로필 수정 후
     * - 비밀번호 변경 후
     * - 회원 탈퇴 시
     * 
     * 두 개의 키를 무효화해야 함:
     * - loginId 기반 캐시
     * - memberId 기반 캐시
     */
    @CacheEvict(value = CACHE_NAME, key = "'loginId:' + #loginId")
    public void evictByLoginId(String loginId, Long memberId) {
        log.info("[MEMBER_CACHE] Evicting cache for loginId: {}", loginId);
        // 참고: memberId 기반 캐시는 별도로 무효화 필요
    }
    
    @CacheEvict(value = CACHE_NAME, key = "'memberId:' + #memberId")
    public void evictByMemberId(Long memberId) {
        log.info("[MEMBER_CACHE] Evicting cache for memberId: {}", memberId);
    }
    
    /**
     * 회원 전체 캐시 무효화 (관리용)
     */
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void evictAll() {
        log.info("[MEMBER_CACHE] Evicting all member cache entries");
    }
}
```

### 3.4 알림 설정 캐싱

```java
@Service
@RequiredArgsConstructor
public class NotificationSettingsService {
    
    private final NotificationSettingsRepository repository;
    
    /**
     * 알림 설정 조회 (캐시 적용)
     * 
     * 알림 발송 시마다 호출되므로 캐싱 효과 높음
     * TTL: 30분 (설정 변경이 드묾)
     */
    @Cacheable(value = "notificationSettings", key = "#memberId", ttl = 1800)
    public Optional<NotificationSettings> findByMemberId(Long memberId) {
        return repository.findByMember_MemberId(memberId);
    }
    
    /**
     * 알림 설정 수정 (캐시 무효화)
     */
    @CacheEvict(value = "notificationSettings", key = "#memberId")
    @Transactional
    public NotificationSettings update(Long memberId, UpdateNotificationSettingsRequest request) {
        NotificationSettings settings = repository.findByMember_MemberId(memberId)
            .orElseThrow(() -> ErrorCode.SETTINGS_NOT_FOUND.toException());
        
        settings.update(
            request.pushEnabled(),
            request.medicationReminderEnabled(),
            request.diaryReminderEnabled(),
            request.quietHoursEnabled(),
            request.quietHoursStart(),
            request.quietHoursEnd()
        );
        
        // @CacheEvict가 메서드 완료 후 캐시 삭제
        return settings;
    }
}
```

### 3.5 캐시 설정 및 TTL 전략

```yaml
# application.yml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      timeout: 3000  # 연결 타임아웃 3초
      
# 캐시 설정
app:
  aop:
    cache:
      enabled: true
```

```java
// 캐시별 TTL 전략
public class CacheTTL {
    
    /**
     * 캐시별 권장 TTL
     * 
     * 원칙:
     * - 자주 변경되는 데이터 → 짧은 TTL
     * - 변경이 드문 데이터 → 긴 TTL
     * - 민감한 데이터 → 짧은 TTL (보안)
     */
    
    // 인증 관련 (민감 + 자주 조회)
    public static final long MEMBER_TTL = 600;        // 10분
    
    // 설정 관련 (드물게 변경)
    public static final long SETTINGS_TTL = 1800;     // 30분
    
    // 통계 관련 (계산 비용 높음, 변경 시 무효화)
    public static final long STATS_TTL = 1800;        // 30분
    
    // 목록 조회 (변경 빈번)
    public static final long LIST_TTL = 300;          // 5분
}
```

---

## 4. 효과 및 검증 (Results)

### 4.1 성능 개선

| 지표 | Before (캐시 없음) | After (캐시 적용) | 개선율 |
|------|-------------------|------------------|-------|
| **회원 조회 쿼리** | 1000/초 | 50/초 | 95% ↓ |
| **평균 응답 시간** | 50ms | 5ms | 90% ↓ |
| **DB 커넥션 사용** | 높음 | 낮음 | 70% ↓ |
| **Redis HIT율** | N/A | 95%+ | - |

### 4.2 캐시 일관성 보장

```
✅ 선제적 무효화로 데이터 변경 즉시 캐시 삭제
✅ TTL로 무효화 누락 시에도 자동 갱신
✅ Redis 분산 캐시로 다중 서버 일관성 보장
✅ 로컬 캐시 fallback으로 Redis 장애 대응
```

### 4.3 모니터링 메트릭

```
# 캐시 통계 로그
CACHE_STATS: member | Hits: 9500 | Misses: 500 | HitRate: 95.0%
CACHE_STATS: notificationSettings | Hits: 4800 | Misses: 200 | HitRate: 96.0%
CACHE_STATS: stats | Hits: 1900 | Misses: 100 | HitRate: 95.0%

# 캐시 이벤트 로그
CACHE_HIT: abc12345 | Method: findByLoginId | Key: cache:member:loginId:user@example.com
CACHE_MISS: def67890 | Method: findById | Key: cache:member:memberId:123 | Executing method
CACHE_STORED: def67890 | Method: findById | Key: cache:member:memberId:123 | TTL: 600s
CACHE_EVICT_KEY: ghi11111 | Key: cache:member:loginId:user@example.com | Timing: AFTER
```

### 4.4 검증 테스트

```java
@Test
@DisplayName("캐시 히트 시 DB 조회하지 않음")
void cacheable_shouldNotQueryDbOnCacheHit() {
    // given: 첫 번째 조회 (캐시 미스)
    memberCacheService.findByLoginId("test@example.com");
    verify(memberRepository, times(1)).findByLoginId("test@example.com");
    
    // when: 두 번째 조회 (캐시 히트)
    memberCacheService.findByLoginId("test@example.com");
    
    // then: DB 호출 횟수 변화 없음
    verify(memberRepository, times(1)).findByLoginId("test@example.com");
}

@Test
@DisplayName("데이터 수정 후 캐시 무효화")
void cacheEvict_shouldInvalidateCacheAfterUpdate() {
    // given: 캐시에 데이터 저장
    memberCacheService.findByLoginId("test@example.com");
    
    // when: 캐시 무효화
    memberCacheService.evictByLoginId("test@example.com", 1L);
    
    // then: 다음 조회 시 DB 다시 호출
    memberCacheService.findByLoginId("test@example.com");
    verify(memberRepository, times(2)).findByLoginId("test@example.com");
}

@Test
@DisplayName("TTL 만료 후 캐시 갱신")
void cacheable_shouldRefreshAfterTtlExpiry() throws Exception {
    // given: 캐시에 데이터 저장 (TTL: 1초)
    memberCacheService.findByLoginId("test@example.com");
    
    // when: TTL 만료 대기
    Thread.sleep(1500);
    
    // then: 캐시 미스, DB 다시 호출
    memberCacheService.findByLoginId("test@example.com");
    verify(memberRepository, times(2)).findByLoginId("test@example.com");
}
```

---

## 5. 면접 대비 Q&A

### Q1. Cache-Aside 패턴을 선택한 이유는?

> **모범 답변**
> 
> Cache-Aside는 **애플리케이션이 캐시를 직접 관리**하는 패턴입니다.
> 
> 선택 이유:
> 1. **유연성**: 어떤 데이터를 캐싱할지, TTL은 얼마로 할지 세밀하게 제어
> 2. **투명성**: 코드에서 캐싱 동작이 명확히 보임 (`@Cacheable`)
> 3. **장애 격리**: 캐시 장애 시 DB로 fallback 가능
> 4. **선택적 적용**: 필요한 메서드에만 캐싱 적용
> 
> Write-Through/Write-Behind는 쓰기 시 캐시도 갱신하므로 일관성은 좋지만:
> - 쓰기 성능 저하 (Write-Through)
> - 데이터 유실 위험 (Write-Behind)
> - 캐시 라이브러리 의존성 증가

### Q2. 캐시와 DB 간 불일치가 발생하는 시나리오와 해결책은?

> **모범 답변**
> 
> **시나리오 1: 캐시 무효화 누락**
> - 원인: 데이터 변경 시 `@CacheEvict`를 빠뜨림
> - 해결: TTL을 짧게 설정하여 자동 갱신 (안전망)
> 
> **시나리오 2: 무효화 실패**
> - 원인: Redis 장애로 삭제 명령 실패
> - 해결: 로컬 캐시도 함께 삭제, 재시도 로직
> 
> **시나리오 3: Race Condition**
> - 원인: A가 조회 중 B가 수정, A가 옛 값을 캐싱
> - 해결: 짧은 TTL, 필요 시 분산 락 사용
> 
> **시나리오 4: 분산 환경 지연**
> - 원인: 서버 A가 무효화해도 서버 B의 로컬 캐시 유지
> - 해결: Redis 분산 캐시 사용 (로컬 캐시는 짧은 TTL)

### Q3. TTL 값은 어떤 기준으로 정하나요?

> **모범 답변**
> 
> 세 가지 요소를 고려합니다:
> 
> 1. **변경 빈도**:
>    - 자주 변경: 짧은 TTL (1~5분)
>    - 드물게 변경: 긴 TTL (30분~1시간)
> 
> 2. **일관성 요구사항**:
>    - 즉시 반영 필수: 짧은 TTL + 선제적 무효화
>    - 지연 허용: 긴 TTL
> 
> 3. **데이터 민감도**:
>    - 인증 정보: 짧은 TTL (보안)
>    - 공개 정보: 긴 TTL 가능
> 
> Hamalog 예시:
> - 회원 정보: 10분 (인증에 사용, 변경 드묾)
> - 알림 설정: 30분 (변경 매우 드묾)
> - 통계 정보: 30분 (계산 비용 높음, 변경 시 무효화)

### Q4. Spring @Cacheable 대신 커스텀 어노테이션을 만든 이유는?

> **모범 답변**
> 
> Spring의 `@Cacheable`도 훌륭하지만, 추가 기능이 필요했습니다:
> 
> 1. **TTL 직접 지정**: Spring 기본은 CacheManager에서 전역 설정
>    ```java
>    @Cacheable(value = "member", key = "#id", ttl = 600)  // 커스텀
>    ```
> 
> 2. **로컬 캐시 Fallback**: Redis 장애 시 자동 전환
>    ```java
>    @Cacheable(..., useLocalFallback = true)
>    ```
> 
> 3. **상세 로깅**: HIT/MISS/EVICT 이벤트 추적
> 
> 4. **캐시 조건 세밀 제어**: null 캐싱 여부 등
> 
> 물론 Spring Cache Abstraction 위에 구축할 수도 있었지만, 학습 목적으로 직접 구현했습니다.

### Q5. 로컬 캐시와 Redis 캐시를 함께 사용하는 이유는?

> **모범 답변**
> 
> **다단계 캐싱 (Multi-Level Cache)** 전략입니다:
> 
> ```
> 요청 → 로컬 캐시 (L1) → Redis (L2) → DB
>         (0.01ms)         (1ms)       (10ms)
> ```
> 
> 장점:
> 1. **극한의 성능**: 로컬 캐시는 네트워크 없이 즉시 응답
> 2. **Redis 장애 대응**: Redis 다운 시 로컬 캐시로 서비스 지속
> 3. **Redis 부하 분산**: 자주 조회되는 데이터는 로컬에서 처리
> 
> 주의점:
> - 로컬 캐시는 서버별로 독립 → 일관성 문제 가능
> - 해결: 로컬 캐시 TTL을 Redis보다 짧게 설정 (예: 1분 vs 10분)

### Q6. Cache Stampede(캐시 쇄도)는 무엇이고 어떻게 방지하나요?

> **모범 답변**
> 
> Cache Stampede는 **캐시 만료 시 동시에 많은 요청이 DB로 몰리는 현상**입니다.
> 
> ```
> TTL 만료 → 1000개 요청이 동시에 캐시 미스
>         → 1000개 DB 쿼리 동시 실행 → DB 과부하
> ```
> 
> 방지 전략:
> 
> 1. **락 기반 갱신**:
>    ```java
>    if (cacheMiss) {
>        if (lock.tryLock()) {
>            result = db.query();  // 하나만 DB 조회
>            cache.set(result);
>            lock.unlock();
>        } else {
>            wait();  // 나머지는 대기
>        }
>    }
>    ```
> 
> 2. **TTL 분산**: 키마다 TTL에 랜덤 값 추가
>    ```java
>    long jitter = random.nextInt(60);  // 0~60초 랜덤
>    cache.set(key, value, baseTTL + jitter);
>    ```
> 
> 3. **미리 갱신**: TTL의 80% 시점에 백그라운드 갱신

### Q7. Cache Penetration(캐시 관통)은 무엇이고 어떻게 방지하나요?

> **모범 답변**
> 
> Cache Penetration은 **존재하지 않는 데이터를 반복 조회**하여 매번 DB까지 도달하는 문제입니다.
> 
> ```
> 악의적 요청: GET /users/9999999999 (존재하지 않는 ID)
> → 캐시 미스 → DB 조회 → 결과 없음 → 캐시 안 함
> → 반복하면 DB 부하 증가
> ```
> 
> 방지 전략:
> 
> 1. **Null 캐싱**: 없는 데이터도 캐싱 (짧은 TTL)
>    ```java
>    @Cacheable(..., cacheNull = true)
>    ```
>    주의: 메모리 사용 증가
> 
> 2. **Bloom Filter**: 존재 여부를 먼저 확인
>    ```java
>    if (!bloomFilter.mightContain(id)) {
>        return null;  // DB 조회 안 함
>    }
>    ```
> 
> 3. **입력 검증**: ID 형식 검증으로 명백히 잘못된 요청 차단

### Q8. 분산 환경에서 캐시 무효화를 어떻게 전파하나요?

> **모범 답변**
> 
> Redis를 사용하면 **자동으로 전파**됩니다:
> 
> ```
> 서버 A: cache.delete("member:123")
>     ↓
> Redis: 키 삭제
>     ↓
> 서버 B, C: 다음 조회 시 캐시 미스 → DB 조회 → 새 값 캐싱
> ```
> 
> 로컬 캐시가 있는 경우:
> 1. **Redis Pub/Sub**: 무효화 이벤트 브로드캐스트
>    ```java
>    redisTemplate.convertAndSend("cache-invalidation", key);
>    ```
> 
> 2. **짧은 TTL**: 로컬 캐시는 1분 등 짧게 설정
> 
> 3. **버전 기반**: 캐시 키에 버전 포함
>    ```
>    cache:member:v2:123
>    ```

### Q9. Redis 장애 시 서비스는 어떻게 되나요?

> **모범 답변**
> 
> **Graceful Degradation** 전략으로 서비스를 유지합니다:
> 
> 1. **로컬 캐시 Fallback**:
>    ```java
>    try {
>        return redis.get(key);
>    } catch (Exception e) {
>        return localCache.get(key);  // 로컬 캐시 사용
>    }
>    ```
> 
> 2. **DB 직접 조회**: 캐시 없이 DB만으로 동작
>    - 성능 저하 있지만 서비스는 유지
> 
> 3. **Circuit Breaker**: 연속 실패 시 Redis 호출 차단
>    ```java
>    if (circuitBreaker.isOpen()) {
>        return localCache.getOrDefault(key, dbQuery());
>    }
>    ```
> 
> 4. **알림**: Redis 장애 감지 시 운영팀 알림

### Q10. 캐시 워밍(Cache Warming)이란 무엇이고 언제 필요한가요?

> **모범 답변**
> 
> Cache Warming은 **서버 시작 시 미리 캐시를 채우는 것**입니다.
> 
> 필요한 상황:
> 1. 서버 재시작 후 초기 트래픽이 모두 DB로 향함
> 2. 캐시 의존도가 높아 성능 저하가 심각함
> 3. 특정 데이터(설정, 코드 테이블 등)가 자주 조회됨
> 
> 구현 예시:
> ```java
> @EventListener(ApplicationReadyEvent.class)
> public void warmUpCache() {
>     // 자주 조회되는 회원 목록 미리 캐싱
>     memberRepository.findAllActive().forEach(member -> {
>         cache.set("member:" + member.getId(), member, Duration.ofMinutes(10));
>     });
>     log.info("Cache warming completed: {} members cached", count);
> }
> ```
> 
> Hamalog에서는 현재 워밍 없이 **Lazy Loading**을 사용합니다. 트래픽이 적어 초기 캐시 미스가 문제되지 않기 때문입니다.

---

## 📎 관련 문서

- [ADR-0005: Redis 캐시 전략](../internal/adr/0005-redis-cache-strategy.md)
- [CACHING-PATTERNS.md](../internal/patterns/CACHING-PATTERNS.md)
- [CachingAspect.java](../../src/main/java/com/Hamalog/aop/CachingAspect.java)
- [MemberCacheService.java](../../src/main/java/com/Hamalog/service/auth/MemberCacheService.java)

