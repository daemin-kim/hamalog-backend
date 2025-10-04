package com.Hamalog.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CachingAspect AOP Tests")
class CachingAspectTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private Method method;

    @InjectMocks
    private CachingAspect cachingAspect;

    private CachingAspect.Cacheable cacheableAnnotation;
    private CachingAspect.CacheEvict cacheEvictAnnotation;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Setup basic method signature mocking
        lenient().when(methodSignature.getDeclaringType()).thenReturn(TestService.class);
        lenient().when(methodSignature.getMethod()).thenReturn(method);
        lenient().when(method.getName()).thenReturn("testMethod");

        // Create mock annotations
        cacheableAnnotation = mock(CachingAspect.Cacheable.class);
        lenient().when(cacheableAnnotation.value()).thenReturn("testCache");
        lenient().when(cacheableAnnotation.key()).thenReturn("");
        lenient().when(cacheableAnnotation.ttl()).thenReturn(3600L);
        lenient().when(cacheableAnnotation.cacheNull()).thenReturn(false);
        lenient().when(cacheableAnnotation.condition()).thenReturn("");
        lenient().when(cacheableAnnotation.useLocalFallback()).thenReturn(true);

        cacheEvictAnnotation = mock(CachingAspect.CacheEvict.class);
    }

    @AfterEach
    void tearDown() {
        cachingAspect.clearCacheStats();
        cachingAspect.cleanupLocalCache();
    }

    @Test
    @DisplayName("Should cache result when Redis is available")
    void handleCaching_RedisAvailable_CachesResult() throws Throwable {
        // given
        String expectedResult = "test result";
        String cacheKey = "cache:testCache:testMethod";

        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[0]);

        when(valueOperations.get(cacheKey)).thenReturn(null); // Cache miss
        when(proceedingJoinPoint.proceed()).thenReturn(expectedResult);

        // when
        Object result = cachingAspect.handleCaching(proceedingJoinPoint, cacheableAnnotation);

        // then
        assertThat(result).isEqualTo(expectedResult);
        verify(valueOperations).set(eq(cacheKey), eq(expectedResult), eq(Duration.ofSeconds(3600)));
    }

    @Test
    @DisplayName("Should return cached result on cache hit")
    void handleCaching_CacheHit_ReturnsCachedResult() throws Throwable {
        // given
        String cachedResult = "cached result";
        String cacheKey = "cache:testCache:testMethod";

        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[0]);

        when(valueOperations.get(cacheKey)).thenReturn(cachedResult);

        // when
        Object result = cachingAspect.handleCaching(proceedingJoinPoint, cacheableAnnotation);

        // then
        assertThat(result).isEqualTo(cachedResult);
        verify(proceedingJoinPoint, never()).proceed(); // Should not call original method
    }

    @Test
    @DisplayName("Should use local fallback when Redis fails")
    void handleCaching_RedisFailure_UsesLocalFallback() throws Throwable {
        // given
        String expectedResult = "test result";

        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[0]);

        when(valueOperations.get(any())).thenThrow(new RuntimeException("Redis connection failed"));
        when(proceedingJoinPoint.proceed()).thenReturn(expectedResult);

        // when
        Object result = cachingAspect.handleCaching(proceedingJoinPoint, cacheableAnnotation);

        // then
        assertThat(result).isEqualTo(expectedResult);
        // Should fallback to local cache for subsequent calls
    }

    @Test
    @DisplayName("Should not cache null values when cacheNull is false")
    void handleCaching_NullResultAndCacheNullFalse_DoesNotCache() throws Throwable {
        // given
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[0]);

        when(cacheableAnnotation.cacheNull()).thenReturn(false);
        when(valueOperations.get(any())).thenReturn(null);
        when(proceedingJoinPoint.proceed()).thenReturn(null);

        // when
        Object result = cachingAspect.handleCaching(proceedingJoinPoint, cacheableAnnotation);

        // then
        assertThat(result).isNull();
        verify(valueOperations, never()).set(any(), any(), any(Duration.class));
    }

    @Test
    @DisplayName("Should cache null values when cacheNull is true")
    void handleCaching_NullResultAndCacheNullTrue_CachesNull() throws Throwable {
        // given
        String cacheKey = "cache:testCache:testMethod";

        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[0]);

        when(cacheableAnnotation.cacheNull()).thenReturn(true);
        when(valueOperations.get(cacheKey)).thenReturn(null);
        when(proceedingJoinPoint.proceed()).thenReturn(null);

        // when
        Object result = cachingAspect.handleCaching(proceedingJoinPoint, cacheableAnnotation);

        // then
        assertThat(result).isNull();
        verify(valueOperations).set(eq(cacheKey), eq(null), eq(Duration.ofSeconds(3600)));
    }

    @Test
    @DisplayName("Should generate cache key with custom key")
    void handleCaching_WithCustomKey_GeneratesCorrectCacheKey() throws Throwable {
        // given
        String customKey = "customKey";
        String expectedResult = "test result";
        String expectedCacheKey = "cache:testCache:" + customKey;

        when(cacheableAnnotation.key()).thenReturn(customKey);
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        lenient().when(proceedingJoinPoint.getArgs()).thenReturn(new Object[0]);

        when(valueOperations.get(expectedCacheKey)).thenReturn(null);
        when(proceedingJoinPoint.proceed()).thenReturn(expectedResult);

        // when
        Object result = cachingAspect.handleCaching(proceedingJoinPoint, cacheableAnnotation);

        // then
        assertThat(result).isEqualTo(expectedResult);
        verify(valueOperations).set(eq(expectedCacheKey), eq(expectedResult), eq(Duration.ofSeconds(3600)));
    }

    @Test
    @DisplayName("Should generate cache key with method arguments")
    void handleCaching_WithArguments_IncludesArgsInCacheKey() throws Throwable {
        // given
        Object[] args = {"arg1", 123, true};
        String expectedResult = "test result";

        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(proceedingJoinPoint.getArgs()).thenReturn(args);

        when(valueOperations.get(any(String.class))).thenReturn(null);
        when(proceedingJoinPoint.proceed()).thenReturn(expectedResult);

        // when
        Object result = cachingAspect.handleCaching(proceedingJoinPoint, cacheableAnnotation);

        // then
        assertThat(result).isEqualTo(expectedResult);
        verify(valueOperations).set(any(String.class), eq(expectedResult), eq(Duration.ofSeconds(3600)));
    }

    @Test
    @DisplayName("Should evict single cache entry")
    void handleCacheEviction_SingleKey_EvictsEntry() throws Throwable {
        // given
        String expectedResult = "test result";
        String cacheKey = "cache:testCache:testMethod";

        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[0]);
        when(proceedingJoinPoint.proceed()).thenReturn(expectedResult);

        when(cacheEvictAnnotation.value()).thenReturn("testCache");
        when(cacheEvictAnnotation.key()).thenReturn("");
        when(cacheEvictAnnotation.allEntries()).thenReturn(false);
        when(cacheEvictAnnotation.beforeInvocation()).thenReturn(false);

        // when
        Object result = cachingAspect.handleCacheEviction(proceedingJoinPoint, cacheEvictAnnotation);

        // then
        assertThat(result).isEqualTo(expectedResult);
        verify(redisTemplate).delete(cacheKey);
    }

    @Test
    @DisplayName("Should evict all cache entries when allEntries is true")
    void handleCacheEviction_AllEntries_EvictsAllEntries() throws Throwable {
        // given
        String expectedResult = "test result";
        Set<String> cacheKeys = Set.of("cache:testCache:key1", "cache:testCache:key2", "cache:testCache:key3");

        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(proceedingJoinPoint.proceed()).thenReturn(expectedResult);
        when(cacheEvictAnnotation.value()).thenReturn("testCache");
        when(cacheEvictAnnotation.allEntries()).thenReturn(true);
        when(cacheEvictAnnotation.beforeInvocation()).thenReturn(false);
        when(redisTemplate.keys("cache:testCache:*")).thenReturn(cacheKeys);

        // when
        Object result = cachingAspect.handleCacheEviction(proceedingJoinPoint, cacheEvictAnnotation);

        // then
        assertThat(result).isEqualTo(expectedResult);
        verify(redisTemplate).delete(cacheKeys);
    }

    @Test
    @DisplayName("Should evict before invocation when beforeInvocation is true")
    void handleCacheEviction_BeforeInvocation_EvictsBeforeMethodCall() throws Throwable {
        // given
        String expectedResult = "test result";
        String cacheKey = "cache:testCache:testMethod";

        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[0]);
        when(proceedingJoinPoint.proceed()).thenReturn(expectedResult);

        when(cacheEvictAnnotation.value()).thenReturn("testCache");
        when(cacheEvictAnnotation.key()).thenReturn("");
        when(cacheEvictAnnotation.beforeInvocation()).thenReturn(true);
        when(cacheEvictAnnotation.allEntries()).thenReturn(false);

        // when
        Object result = cachingAspect.handleCacheEviction(proceedingJoinPoint, cacheEvictAnnotation);

        // then
        assertThat(result).isEqualTo(expectedResult);
        verify(redisTemplate).delete(cacheKey);
    }

    @Test
    @DisplayName("Should track cache statistics")
    void handleCaching_MultipleCalls_TracksStatistics() throws Throwable {
        // given
        String cachedResult = "cached result";
        String cacheKey = "cache:testCache:testMethod";

        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[0]);

        // First call - cache miss, second call - cache hit
        when(valueOperations.get(cacheKey)).thenReturn(null).thenReturn(cachedResult);
        when(proceedingJoinPoint.proceed()).thenReturn(cachedResult);

        // when
        cachingAspect.handleCaching(proceedingJoinPoint, cacheableAnnotation); // miss
        cachingAspect.handleCaching(proceedingJoinPoint, cacheableAnnotation); // hit

        // then
        ConcurrentHashMap<String, CachingAspect.CacheStats> stats = cachingAspect.getCacheStats();
        assertThat(stats).containsKey("testCache");

        CachingAspect.CacheStats cacheStats = stats.get("testCache");
        assertThat(cacheStats.getTotal()).isEqualTo(2);
        assertThat(cacheStats.getHits()).isEqualTo(1);
        assertThat(cacheStats.getMisses()).isEqualTo(1);
        assertThat(cacheStats.getHitRatio()).isEqualTo(0.5);
    }

    @Test
    @DisplayName("Should clear cache statistics")
    void clearCacheStats_RemovesAllStatistics() throws Throwable {
        // given - generate some statistics first
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[0]);
        when(valueOperations.get(any())).thenReturn(null);
        when(proceedingJoinPoint.proceed()).thenReturn("result");

        cachingAspect.handleCaching(proceedingJoinPoint, cacheableAnnotation);

        // when
        cachingAspect.clearCacheStats();

        // then
        ConcurrentHashMap<String, CachingAspect.CacheStats> stats = cachingAspect.getCacheStats();
        assertThat(stats).isEmpty();
    }

    @Test
    @DisplayName("Should handle Redis exceptions gracefully")
    void handleCaching_RedisException_DoesNotThrow() throws Throwable {
        // given
        String expectedResult = "test result";

        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[0]);

        when(valueOperations.get(any())).thenThrow(new RuntimeException("Redis error"));
        when(proceedingJoinPoint.proceed()).thenReturn(expectedResult);

        // when & then
        assertThat(cachingAspect.handleCaching(proceedingJoinPoint, cacheableAnnotation))
            .isEqualTo(expectedResult);
    }

    // Test class for annotation testing
    private static class TestService {
        @CachingAspect.Cacheable(value = "testCache", ttl = 3600)
        public String testMethod() {
            return "test result";
        }

        @CachingAspect.CacheEvict(value = "testCache", allEntries = true)
        public void clearCache() {
            // Method to test cache eviction
        }
    }
}