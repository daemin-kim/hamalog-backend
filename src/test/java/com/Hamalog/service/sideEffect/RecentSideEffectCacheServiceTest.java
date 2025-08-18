package com.Hamalog.service.sideEffect;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecentSideEffectCacheService Tests")
class RecentSideEffectCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ListOperations<String, Object> listOperations;

    private RecentSideEffectCacheService cacheService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForList()).thenReturn(listOperations);
        cacheService = new RecentSideEffectCacheService(redisTemplate);
    }

    @Test
    @DisplayName("최근 부작용 목록 조회 성공")
    void getRecentSideEffects_Success() {
        // Given
        Long memberId = 1L;
        List<Object> cachedItems = Arrays.asList("두통", "어지럼증", "구토");
        String expectedKey = "recent_side_effects:" + memberId;
        
        given(listOperations.range(expectedKey, 0, 4)).willReturn(cachedItems);

        // When
        List<String> result = cacheService.getRecentSideEffects(memberId);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).containsExactly("두통", "어지럼증", "구토");
        verify(listOperations).range(expectedKey, 0, 4);
    }

    @Test
    @DisplayName("캐시에 데이터가 없는 경우 빈 리스트 반환")
    void getRecentSideEffects_EmptyCache() {
        // Given
        Long memberId = 1L;
        String expectedKey = "recent_side_effects:" + memberId;
        
        given(listOperations.range(expectedKey, 0, 4)).willReturn(Collections.emptyList());

        // When
        List<String> result = cacheService.getRecentSideEffects(memberId);

        // Then
        assertThat(result).isEmpty();
        verify(listOperations).range(expectedKey, 0, 4);
    }

    @Test
    @DisplayName("Redis 오류 시 빈 리스트 반환")
    void getRecentSideEffects_RedisError() {
        // Given
        Long memberId = 1L;
        String expectedKey = "recent_side_effects:" + memberId;
        
        given(listOperations.range(expectedKey, 0, 4)).willThrow(new RuntimeException("Redis connection error"));

        // When
        List<String> result = cacheService.getRecentSideEffects(memberId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("새로운 부작용을 캐시에 추가")
    void addRecentSideEffect_Success() {
        // Given
        Long memberId = 1L;
        String sideEffectName = "두통";
        String expectedKey = "recent_side_effects:" + memberId;

        // When
        cacheService.addRecentSideEffect(memberId, sideEffectName);

        // Then
        verify(listOperations).remove(expectedKey, 0, sideEffectName); // 중복 제거
        verify(listOperations).leftPush(expectedKey, sideEffectName); // 리스트 앞에 추가
        verify(listOperations).trim(expectedKey, 0, 4); // 최대 5개 유지
        verify(redisTemplate).expire(expectedKey, Duration.ofDays(30)); // TTL 설정
    }

    @Test
    @DisplayName("캐시 초기화")
    void refreshRecentSideEffects_Success() {
        // Given
        Long memberId = 1L;
        List<String> sideEffectNames = Arrays.asList("두통", "어지럼증", "구토", "복통", "설사");
        String expectedKey = "recent_side_effects:" + memberId;

        // When
        cacheService.refreshRecentSideEffects(memberId, sideEffectNames);

        // Then
        verify(redisTemplate).delete(expectedKey); // 기존 캐시 삭제
        verify(listOperations).rightPushAll(expectedKey, sideEffectNames.toArray()); // 새 데이터 추가
        verify(redisTemplate).expire(expectedKey, Duration.ofDays(30)); // TTL 설정
    }

    @Test
    @DisplayName("캐시 초기화 - 5개 초과 데이터는 제한됨")
    void refreshRecentSideEffects_LimitToFive() {
        // Given
        Long memberId = 1L;
        List<String> sideEffectNames = Arrays.asList("두통", "어지럼증", "구토", "복통", "설사", "발열", "기침");
        String expectedKey = "recent_side_effects:" + memberId;

        // When
        cacheService.refreshRecentSideEffects(memberId, sideEffectNames);

        // Then
        verify(redisTemplate).delete(expectedKey);
        // 처음 5개만 추가되는지 확인
        Object[] expectedArray = sideEffectNames.stream().limit(5).toArray();
        verify(listOperations).rightPushAll(expectedKey, expectedArray);
        verify(redisTemplate).expire(expectedKey, Duration.ofDays(30));
    }

    @Test
    @DisplayName("빈 리스트로 캐시 초기화 시 아무것도 추가되지 않음")
    void refreshRecentSideEffects_EmptyList() {
        // Given
        Long memberId = 1L;
        List<String> sideEffectNames = Collections.emptyList();
        String expectedKey = "recent_side_effects:" + memberId;

        // When
        cacheService.refreshRecentSideEffects(memberId, sideEffectNames);

        // Then
        verify(redisTemplate).delete(expectedKey);
        // rightPushAll이나 expire가 호출되지 않아야 함
        verify(listOperations, org.mockito.Mockito.never()).rightPushAll(eq(expectedKey), any(Object[].class));
        verify(redisTemplate, org.mockito.Mockito.never()).expire(anyString(), any(Duration.class));
    }
}