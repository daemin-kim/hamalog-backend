package com.Hamalog.service.sideEffect;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

class RecentSideEffectCacheServiceTest {

    private RecentSideEffectCacheService cacheService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ListOperations<String, Object> listOperations;

    private static final Long TEST_MEMBER_ID = 1L;
    private static final String CACHE_KEY = "recent_side_effects:" + TEST_MEMBER_ID;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        cacheService = new RecentSideEffectCacheService(redisTemplate);
    }

    @Test
    void getRecentSideEffects_WhenCacheExists_ShouldReturnList() {
        // Given
        List<Object> cachedItems = Arrays.asList("두통", "어지러움", "구토");
        when(listOperations.range(CACHE_KEY, 0, 4))
                .thenReturn(cachedItems);

        // When
        List<String> result = cacheService.getRecentSideEffects(TEST_MEMBER_ID);

        // Then
        assertEquals(3, result.size());
        assertEquals("두통", result.get(0));
        assertEquals("어지러움", result.get(1));
        assertEquals("구토", result.get(2));
    }

    @Test
    void getRecentSideEffects_WhenCacheEmpty_ShouldReturnEmptyList() {
        // Given
        when(listOperations.range(CACHE_KEY, 0, 4))
                .thenReturn(Collections.emptyList());

        // When
        List<String> result = cacheService.getRecentSideEffects(TEST_MEMBER_ID);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void getRecentSideEffects_WhenRedisError_ShouldReturnEmptyList() {
        // Given
        when(listOperations.range(CACHE_KEY, 0, 4))
                .thenThrow(new RuntimeException("Redis connection error"));

        // When
        List<String> result = cacheService.getRecentSideEffects(TEST_MEMBER_ID);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void addRecentSideEffect_ShouldAddToCache() {
        // Given
        String sideEffect = "두통";

        // When
        cacheService.addRecentSideEffect(TEST_MEMBER_ID, sideEffect);

        // Then
        verify(listOperations).remove(CACHE_KEY, 0, sideEffect);
        verify(listOperations).leftPush(CACHE_KEY, sideEffect);
        verify(listOperations).trim(CACHE_KEY, 0, 4);
        verify(redisTemplate).expire(CACHE_KEY, Duration.ofDays(30));
    }

    @Test
    void addRecentSideEffect_WhenRedisError_ShouldHandleGracefully() {
        // Given
        String sideEffect = "두통";
        when(listOperations.leftPush(CACHE_KEY, sideEffect))
                .thenThrow(new RuntimeException("Redis connection error"));

        // When & Then
        assertDoesNotThrow(() -> cacheService.addRecentSideEffect(TEST_MEMBER_ID, sideEffect));
    }

    @Test
    void refreshRecentSideEffects_ShouldRefreshCache() {
        // Given
        List<String> sideEffects = Arrays.asList("두통", "어지러움", "구토");

        // When
        cacheService.refreshRecentSideEffects(TEST_MEMBER_ID, sideEffects);

        // Then
        verify(redisTemplate).delete(CACHE_KEY);
        verify(listOperations).rightPushAll(eq(CACHE_KEY), any(Object[].class));
        verify(redisTemplate).expire(CACHE_KEY, Duration.ofDays(30));
    }

    @Test
    void refreshRecentSideEffects_WhenEmptyList_ShouldOnlyDeleteCache() {
        // Given
        List<String> emptyList = Collections.emptyList();

        // When
        cacheService.refreshRecentSideEffects(TEST_MEMBER_ID, emptyList);

        // Then
        verify(redisTemplate).delete(CACHE_KEY);
        verify(listOperations, never()).rightPushAll(eq(CACHE_KEY), any(Collection.class));
        verify(redisTemplate, never()).expire(eq(CACHE_KEY), any());
    }

    @Test
    void refreshRecentSideEffects_WhenRedisError_ShouldHandleGracefully() {
        // Given
        List<String> sideEffects = Arrays.asList("두통", "어지러움", "구토");
        when(redisTemplate.delete(CACHE_KEY))
                .thenThrow(new RuntimeException("Redis connection error"));

        // When & Then
        assertDoesNotThrow(() -> cacheService.refreshRecentSideEffects(TEST_MEMBER_ID, sideEffects));
    }
}
