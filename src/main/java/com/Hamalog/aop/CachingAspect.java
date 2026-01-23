package com.Hamalog.aop;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 2-Tier 캐시 AOP Aspect
 *
 * L1: Caffeine (로컬 인메모리, 네트워크 RTT 없음, ~0.01ms)
 * L2: Redis (분산 캐시, 네트워크 RTT 있음, ~0.5-1ms)
 *
 * 조회 순서: L1 → L2 → DB
 * 저장 순서: L2 → L1 (Write-Through)
 */
@Slf4j
@Aspect
@Component
@ConditionalOnProperty(name = "app.aop.cache.enabled", matchIfMissing = true)
@Order(5)
public class CachingAspect {

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    
    // L1: Caffeine 로컬 캐시 (고성능, 네트워크 RTT 없음)
    private Cache<String, Object> l1Cache;

    // 로컬 캐시 통계
    private final ConcurrentHashMap<String, CacheStats> cacheStats = new ConcurrentHashMap<>();
    
    // L1 캐시 히트 통계 (L1 vs L2 성능 비교용)
    private final LongAdder l1Hits = new LongAdder();
    private final LongAdder l2Hits = new LongAdder();
    private final LongAdder cacheMisses = new LongAdder();

    @PostConstruct
    public void init() {
        // Caffeine L1 캐시 설정
        // - 최대 10,000개 항목
        // - 5분 후 만료 (Redis TTL보다 짧게)
        // - 통계 기록 활성화
        l1Cache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats()
                .build();

        log.info("[2-TIER CACHE] Caffeine L1 캐시 초기화 완료 (maxSize=10000, TTL=5min)");
    }

    @Around("@annotation(cacheable)")
    public Object handleCaching(ProceedingJoinPoint joinPoint, Cacheable cacheable) throws Throwable {
        String methodName = getMethodName(joinPoint);
        String cacheKey = generateCacheKey(joinPoint, cacheable);
        String cacheName = cacheable.value();
        long ttl = cacheable.ttl();
        boolean useLocalFallback = cacheable.useLocalFallback();

        String cacheId = java.util.UUID.randomUUID().toString().substring(0, 8);
        MDC.put("cache.id", cacheId);
        MDC.put("cache.method", methodName);
        MDC.put("cache.key", cacheKey);
        MDC.put("cache.name", cacheName);

        try {
            // 캐시에서 값 조회 시도
            Object cachedValue = getCachedValue(cacheKey, useLocalFallback);
            
            if (cachedValue != null) {
                // 캐시 히트
                updateCacheStats(cacheName, true);
                MDC.put("cache.result", "HIT");
                
                log.debug("CACHE_HIT: {} | Method: {} | Key: {} | Cache: {}", 
                         cacheId, methodName, shortenKey(cacheKey), cacheName);
                
                return cachedValue;
            }
            
            // 캐시 미스 - 실제 메서드 실행
            updateCacheStats(cacheName, false);
            MDC.put("cache.result", "MISS");
            
            log.debug("CACHE_MISS: {} | Method: {} | Key: {} | Cache: {} | Executing method", 
                     cacheId, methodName, shortenKey(cacheKey), cacheName);
            
            long startTime = System.currentTimeMillis();
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 결과를 캐시에 저장
            if (shouldCache(result, cacheable)) {
                cacheValue(cacheKey, result, ttl, useLocalFallback);
                
                log.info("CACHE_STORED: {} | Method: {} | Key: {} | Cache: {} | ExecutionTime: {}ms | TTL: {}s", 
                        cacheId, methodName, shortenKey(cacheKey), cacheName, executionTime, ttl);
            } else {
                log.debug("CACHE_NOT_STORED: {} | Method: {} | Key: {} | Reason: {}", 
                         cacheId, methodName, shortenKey(cacheKey), 
                         result == null ? "null_result" : "condition_not_met");
            }
            
            return result;
            
        } catch (Exception e) {
            MDC.put("cache.result", "ERROR");
            MDC.put("cache.error", e.getClass().getSimpleName());
            
            log.error("CACHE_ERROR: {} | Method: {} | Key: {} | Error: {} | Message: {}", 
                     cacheId, methodName, shortenKey(cacheKey), e.getClass().getSimpleName(), e.getMessage());
            
            throw e;
            
        } finally {
            clearCacheContext();
        }
    }

    @Around("@annotation(cacheEvict)")
    public Object handleCacheEviction(ProceedingJoinPoint joinPoint, CacheEvict cacheEvict) throws Throwable {
        String methodName = getMethodName(joinPoint);
        
        try {
            // beforeInvocation이 true인 경우 메서드 실행 전에 캐시 무효화
            if (cacheEvict.beforeInvocation()) {
                evictCache(joinPoint, cacheEvict, "BEFORE");
            }
            
            Object result = joinPoint.proceed();
            
            // beforeInvocation이 false인 경우 메서드 실행 후 캐시 무효화
            if (!cacheEvict.beforeInvocation()) {
                evictCache(joinPoint, cacheEvict, "AFTER");
            }
            
            return result;
            
        } catch (Exception e) {
            // beforeInvocation이 false인 경우 예외 발생 시에도 캐시 무효화하지 않음
            if (!cacheEvict.beforeInvocation()) {
                // 예외가 발생해도 캐시 무효화하지 않음
            }
            throw e;
        }
    }

    private void evictCache(ProceedingJoinPoint joinPoint, CacheEvict cacheEvict, String timing) {
        String methodName = getMethodName(joinPoint);
        String cacheName = cacheEvict.value();
        
        if (cacheEvict.allEntries()) {
            // 전체 캐시 무효화
            try {
                String pattern = generateCacheKeyPrefix(cacheName) + "*";
                evictByPattern(pattern);
                
                log.info("CACHE_EVICT_ALL: {} | Method: {} | Cache: {} | Timing: {} | Pattern: {}", 
                        java.util.UUID.randomUUID().toString().substring(0, 8), 
                        methodName, cacheName, timing, pattern);
                
            } catch (Exception e) {
                log.error("CACHE_EVICT_ALL_ERROR: Method: {} | Cache: {} | Error: {}", 
                         methodName, cacheName, e.getMessage());
            }
        } else {
            // 특정 키 무효화
            String cacheKey = generateCacheKey(joinPoint, cacheName, cacheEvict.key());
            evictSingleKey(cacheKey);
            
            log.info("CACHE_EVICT_KEY: {} | Method: {} | Key: {} | Cache: {} | Timing: {}", 
                    java.util.UUID.randomUUID().toString().substring(0, 8), 
                    methodName, shortenKey(cacheKey), cacheName, timing);
        }
    }

    private Object getCachedValue(String key, boolean useLocalFallback) {
        // L1: Caffeine 로컬 캐시에서 먼저 조회 (네트워크 RTT 없음, ~0.01ms)
        if (useLocalFallback && l1Cache != null) {
            Object l1Value = l1Cache.getIfPresent(key);
            if (l1Value != null) {
                l1Hits.increment();
                log.trace("[L1 HIT] Key: {}", shortenKey(key));
                return l1Value;
            }
        }

        // L2: Redis에서 조회 (네트워크 RTT 있음, ~0.5-1ms)
        if (redisTemplate != null) {
            try {
                Object l2Value = redisTemplate.opsForValue().get(key);
                if (l2Value != null) {
                    l2Hits.increment();
                    log.trace("[L2 HIT] Key: {}", shortenKey(key));

                    // L2 히트 시 L1에도 저장 (Read-Through)
                    if (useLocalFallback && l1Cache != null) {
                        l1Cache.put(key, l2Value);
                    }
                    return l2Value;
                }
            } catch (Exception e) {
                log.warn("Redis L2 cache access failed for key: {} | Error: {}",
                        shortenKey(key), e.getMessage());
            }
        }
        
        cacheMisses.increment();
        return null;
    }

    private void cacheValue(String key, Object value, long ttl, boolean useLocalFallback) {
        // L2: Redis에 저장 (분산 캐시, 모든 인스턴스 공유)
        boolean redisSuccess = false;
        if (redisTemplate != null) {
            try {
                if (ttl > 0) {
                    redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttl));
                } else {
                    redisTemplate.opsForValue().set(key, value);
                }
                redisSuccess = true;
            } catch (Exception e) {
                log.warn("Redis L2 cache store failed for key: {} | Error: {}",
                        shortenKey(key), e.getMessage());
            }
        }
        
        // L1: Caffeine에도 저장 (Write-Through 패턴)
        // L1 TTL은 Redis보다 짧게 설정되어 있으므로 자동 관리됨
        if (useLocalFallback && l1Cache != null) {
            l1Cache.put(key, value);
            log.trace("[L1+L2 STORED] Key: {} | Redis: {}", shortenKey(key), redisSuccess);
        }
    }

    private void evictSingleKey(String key) {
        // L2: Redis에서 삭제
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(key);
            } catch (Exception e) {
                log.error("Redis L2 cache eviction failed for key: {} | Error: {}", shortenKey(key), e.getMessage());
            }
        }
        
        // L1: Caffeine에서 삭제
        if (l1Cache != null) {
            l1Cache.invalidate(key);
        }
    }

    private void evictByPattern(String pattern) {
        // L2: Redis에서 패턴 매칭으로 삭제
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(redisTemplate.keys(pattern));
            } catch (Exception e) {
                log.error("Redis L2 pattern eviction failed for pattern: {} | Error: {}", pattern, e.getMessage());
            }
        }

        // L1: Caffeine에서 전체 무효화 (패턴 매칭 미지원, 안전하게 전체 삭제)
        if (l1Cache != null) {
            l1Cache.invalidateAll();
            log.debug("[L1 INVALIDATE_ALL] Pattern eviction triggered full L1 cache clear");
        }
    }

    private boolean shouldCache(Object result, Cacheable cacheable) {
        if (!cacheable.cacheNull() && result == null) {
            return false;
        }
        
        String condition = cacheable.condition();
        if (!condition.isEmpty()) {
            // 간단한 조건 체크 (실제로는 SpEL 파서 등을 사용할 수 있음)
            return evaluateCondition(result, condition);
        }
        
        return true;
    }

    private boolean evaluateCondition(Object result, String condition) {
        // 간단한 조건 평가 (확장 가능)
        switch (condition.toLowerCase()) {
            case "result != null":
                return result != null;
            case "result == null":
                return result == null;
            default:
                return true;
        }
    }

    private String generateCacheKey(ProceedingJoinPoint joinPoint, Cacheable cacheable) {
        return generateCacheKey(joinPoint, cacheable.value(), cacheable.key());
    }

    private String generateCacheKey(ProceedingJoinPoint joinPoint, String cacheName, String customKey) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(generateCacheKeyPrefix(cacheName));
        
        if (!customKey.isEmpty()) {
            keyBuilder.append(customKey);
        } else {
            // 메서드명과 파라미터로 키 생성
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            keyBuilder.append(signature.getMethod().getName());
            
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                keyBuilder.append(":").append(generateArgsHash(args));
            }
        }
        
        return keyBuilder.toString();
    }

    private String generateCacheKeyPrefix(String cacheName) {
        return "cache:" + cacheName + ":";
    }

    private String generateArgsHash(Object[] args) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(Arrays.deepToString(args).getBytes());
            byte[] digest = md.digest();
            
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(Arrays.deepHashCode(args));
        }
    }

    private String getMethodName(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getDeclaringType().getSimpleName() + "." + signature.getName();
    }

    private String shortenKey(String key) {
        return key.length() > 50 ? key.substring(0, 47) + "..." : key;
    }

    private void updateCacheStats(String cacheName, boolean hit) {
        cacheStats.computeIfAbsent(cacheName, k -> new CacheStats()).update(hit);
    }

    private void clearCacheContext() {
        MDC.remove("cache.id");
        MDC.remove("cache.method");
        MDC.remove("cache.key");
        MDC.remove("cache.name");
        MDC.remove("cache.result");
        MDC.remove("cache.error");
    }

    /**
     * 캐시 통계를 가져옵니다.
     */
    public ConcurrentHashMap<String, CacheStats> getCacheStats() {
        return new ConcurrentHashMap<>(cacheStats);
    }

    /**
     * 캐시 통계를 초기화합니다.
     */
    public void clearCacheStats() {
        cacheStats.clear();
        log.info("Cache statistics cleared");
    }

    /**
     * 로컬 캐시를 정리합니다 (Caffeine은 자동 관리되므로 수동 정리 필요 없음).
     */
    public void cleanupLocalCache() {
        if (l1Cache != null) {
            l1Cache.cleanUp();
            log.info("[L1 CLEANUP] Caffeine cache cleanup completed");
        }
    }

    /**
     * 2-Tier 캐시 전체 무효화
     */
    public void invalidateAll() {
        if (l1Cache != null) {
            l1Cache.invalidateAll();
        }
        log.info("[2-TIER CACHE] All caches invalidated");
    }

    /**
     * 2-Tier 캐시 통계를 가져옵니다.
     */
    public TwoTierCacheStats getTwoTierStats() {
        com.github.benmanes.caffeine.cache.stats.CacheStats caffeineStats =
                l1Cache != null ? l1Cache.stats() : null;

        return new TwoTierCacheStats(
                l1Hits.sum(),
                l2Hits.sum(),
                cacheMisses.sum(),
                caffeineStats != null ? caffeineStats.hitRate() : 0.0,
                l1Cache != null ? l1Cache.estimatedSize() : 0
        );
    }

    /**
     * L1 캐시 통계를 초기화합니다.
     */
    public void resetTwoTierStats() {
        l1Hits.reset();
        l2Hits.reset();
        cacheMisses.reset();
        log.info("[2-TIER CACHE] Statistics reset");
    }

    /**
     * 2-Tier 캐시 통계 레코드
     */
    public record TwoTierCacheStats(
            long l1Hits,
            long l2Hits,
            long misses,
            double l1HitRate,
            long l1Size
    ) {
        public long totalHits() {
            return l1Hits + l2Hits;
        }

        public long totalRequests() {
            return l1Hits + l2Hits + misses;
        }

        public double overallHitRate() {
            long total = totalRequests();
            return total > 0 ? (double) totalHits() / total : 0.0;
        }

        @Override
        public String toString() {
            return String.format(
                    "TwoTierCacheStats{L1=%d (%.1f%%), L2=%d, Miss=%d, Overall=%.1f%%, L1Size=%d}",
                    l1Hits, l1HitRate * 100, l2Hits, misses, overallHitRate() * 100, l1Size
            );
        }
    }

    /**
     * 메서드 결과를 캐시하는 어노테이션
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Cacheable {
        
        /**
         * 캐시 이름
         */
        String value();
        
        /**
         * 캐시 키 (비어있으면 메서드명과 파라미터로 자동 생성)
         */
        String key() default "";
        
        /**
         * TTL (초 단위, 0이면 무제한)
         */
        long ttl() default 300;
        
        /**
         * null 결과도 캐시할지 여부
         */
        boolean cacheNull() default false;
        
        /**
         * 캐시 조건 (간단한 표현식)
         */
        String condition() default "";
        
        /**
         * Redis 장애 시 로컬 캐시 사용 여부
         */
        boolean useLocalFallback() default true;
    }

    /**
     * 캐시를 무효화하는 어노테이션
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface CacheEvict {
        
        /**
         * 캐시 이름
         */
        String value();
        
        /**
         * 무효화할 키 (비어있으면 메서드명과 파라미터로 자동 생성)
         */
        String key() default "";
        
        /**
         * 전체 캐시 무효화 여부
         */
        boolean allEntries() default false;
        
        /**
         * 메서드 실행 전에 무효화할지 여부
         */
        boolean beforeInvocation() default false;
    }

    /**
     * 캐시 통계를 저장하는 클래스
     */
    public static class CacheStats {
        private final LongAdder hits = new LongAdder();
        private final LongAdder misses = new LongAdder();

        public void update(boolean hit) {
            if (hit) {
                hits.increment();
            } else {
                misses.increment();
            }
        }

        public long getHits() {
            return hits.sum();
        }

        public long getMisses() {
            return misses.sum();
        }

        public long getTotal() {
            return getHits() + getMisses();
        }

        public double getHitRatio() {
            long total = getTotal();
            return total > 0 ? (double) getHits() / total : 0.0;
        }

        @Override
        public String toString() {
            return String.format("CacheStats{hits=%d, misses=%d, hitRatio=%.2f%%}", 
                               getHits(), getMisses(), getHitRatio() * 100);
        }
    }
}