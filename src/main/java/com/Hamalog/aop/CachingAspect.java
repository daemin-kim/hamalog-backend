package com.Hamalog.aop;

import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 메서드 레벨 캐싱을 위한 AOP Aspect
 * @Cacheable 어노테이션이 적용된 메서드의 결과를 자동으로 캐시하고 관리합니다.
 */
@Slf4j
@Aspect
@Component
@ConditionalOnProperty(name = "app.aop.cache.enabled", matchIfMissing = true)
public class CachingAspect {

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    
    // 로컬 캐시 통계
    private final ConcurrentHashMap<String, CacheStats> cacheStats = new ConcurrentHashMap<>();
    
    // 메모리 기반 간단한 로컬 캐시 (Redis 장애 시 fallback)
    private final ConcurrentHashMap<String, CacheEntry> localCache = new ConcurrentHashMap<>();

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
            Object result = joinPoint.proceed();
            
            // 메서드 실행 후 캐시 무효화
            if (cacheEvict.beforeInvocation()) {
                evictCache(joinPoint, cacheEvict, "BEFORE");
            } else {
                evictCache(joinPoint, cacheEvict, "AFTER");
            }
            
            return result;
            
        } catch (Exception e) {
            // beforeInvocation이 false인 경우 예외 발생 시에도 캐시 무효화하지 않음
            if (cacheEvict.beforeInvocation()) {
                evictCache(joinPoint, cacheEvict, "AFTER_EXCEPTION");
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
        // Redis에서 먼저 조회 (Redis가 사용 가능한 경우에만)
        if (redisTemplate != null) {
            try {
                Object value = redisTemplate.opsForValue().get(key);
                if (value != null) {
                    return value;
                }
            } catch (Exception e) {
                log.warn("Redis cache access failed for key: {} | Error: {} | Using local fallback: {}", 
                        shortenKey(key), e.getMessage(), useLocalFallback);
            }
        }
        
        // Redis 실패 시 로컬 캐시 사용
        if (useLocalFallback) {
            CacheEntry entry = localCache.get(key);
            if (entry != null && !entry.isExpired()) {
                return entry.getValue();
            } else if (entry != null && entry.isExpired()) {
                localCache.remove(key);
            }
        }
        
        return null;
    }

    private void cacheValue(String key, Object value, long ttl, boolean useLocalFallback) {
        // Redis에 저장 (Redis가 사용 가능한 경우에만)
        if (redisTemplate != null) {
            try {
                if (ttl > 0) {
                    redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttl));
                } else {
                    redisTemplate.opsForValue().set(key, value);
                }
                return; // Redis 저장 성공 시 로컬 캐시에는 저장하지 않음
            } catch (Exception e) {
                log.warn("Redis cache store failed for key: {} | Error: {} | Using local fallback: {}", 
                        shortenKey(key), e.getMessage(), useLocalFallback);
            }
        }
        
        // Redis가 없거나 실패한 경우 로컬 캐시에 저장
        if (useLocalFallback) {
            LocalDateTime expireTime = ttl > 0 ? 
                LocalDateTime.now().plusSeconds(ttl) : null;
            localCache.put(key, new CacheEntry(value, expireTime));
        }
    }

    private void evictSingleKey(String key) {
        // Redis에서 삭제 (Redis가 사용 가능한 경우에만)
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(key);
            } catch (Exception e) {
                log.error("Redis cache eviction failed for key: {} | Error: {}", shortenKey(key), e.getMessage());
            }
        }
        
        // 로컬 캐시에서 삭제
        localCache.remove(key);
    }

    private void evictByPattern(String pattern) {
        // Redis에서 패턴 매칭으로 삭제 (Redis가 사용 가능한 경우에만)
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(redisTemplate.keys(pattern));
            } catch (Exception e) {
                log.error("Redis pattern eviction failed for pattern: {} | Error: {}", pattern, e.getMessage());
            }
        }
        
        // 로컬 캐시에서도 패턴 매칭으로 제거
        try {
            localCache.entrySet().removeIf(entry -> entry.getKey().matches(pattern.replace("*", ".*")));
        } catch (Exception e) {
            log.error("Local cache pattern eviction failed for pattern: {} | Error: {}", pattern, e.getMessage());
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
     * 로컬 캐시를 정리합니다 (만료된 항목 제거).
     */
    public void cleanupLocalCache() {
        localCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        log.info("Local cache cleanup completed");
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
     * 캐시 항목을 나타내는 클래스
     */
    private static class CacheEntry {
        private final Object value;
        private final LocalDateTime expireTime;

        public CacheEntry(Object value, LocalDateTime expireTime) {
            this.value = value;
            this.expireTime = expireTime;
        }

        public Object getValue() {
            return value;
        }

        public boolean isExpired() {
            return expireTime != null && LocalDateTime.now().isAfter(expireTime);
        }
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