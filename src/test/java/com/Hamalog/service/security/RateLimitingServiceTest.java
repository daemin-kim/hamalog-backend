package com.Hamalog.service.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;

/**
 * RateLimitingService 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitingService 테스트")
class RateLimitingServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    private RateLimitingService rateLimitingService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        rateLimitingService = new RateLimitingService(redisTemplate);
    }

    @Test
    @DisplayName("인증 요청 속도 제한 - 정상 처리")
    void tryConsumeAuthRequest_Success() {
        // Given
        String key = "test-key";
        given(zSetOperations.count(anyString(), anyDouble(), anyDouble())).willReturn(0L);
        given(zSetOperations.add(anyString(), anyString(), anyDouble())).willReturn(true);

        // When
        boolean result = rateLimitingService.tryConsumeAuthRequest(key);

        // Then
        assertThat(result).isTrue();
        verify(zSetOperations, times(2)).removeRangeByScore(anyString(), eq(0.0), anyDouble());
        verify(zSetOperations, times(2)).add(anyString(), anyString(), anyDouble());
        verify(redisTemplate, times(2)).expire(anyString(), anyLong(), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("인증 요청 속도 제한 - 분당 제한 초과")
    void tryConsumeAuthRequest_ExceedsMinuteLimit() {
        // Given
        String key = "test-key";
        given(zSetOperations.count(anyString(), anyDouble(), anyDouble())).willReturn(5L);

        // When
        boolean result = rateLimitingService.tryConsumeAuthRequest(key);

        // Then
        assertThat(result).isFalse();
        verify(zSetOperations).removeRangeByScore(anyString(), eq(0.0), anyDouble());
    }

    @Test
    @DisplayName("API 요청 속도 제한 - 정상 처리")
    void tryConsumeApiRequest_Success() {
        // Given
        String key = "test-key";
        given(zSetOperations.count(anyString(), anyDouble(), anyDouble())).willReturn(30L);
        given(zSetOperations.add(anyString(), anyString(), anyDouble())).willReturn(true);

        // When
        boolean result = rateLimitingService.tryConsumeApiRequest(key);

        // Then
        assertThat(result).isTrue();
        verify(zSetOperations, times(2)).removeRangeByScore(anyString(), eq(0.0), anyDouble());
        verify(zSetOperations, times(2)).add(anyString(), anyString(), anyDouble());
        verify(redisTemplate, times(2)).expire(anyString(), anyLong(), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("API 요청 속도 제한 - 분당 제한 초과")
    void tryConsumeApiRequest_ExceedsMinuteLimit() {
        // Given
        String key = "test-key";
        given(zSetOperations.count(anyString(), anyDouble(), anyDouble())).willReturn(60L);

        // When
        boolean result = rateLimitingService.tryConsumeApiRequest(key);

        // Then
        assertThat(result).isFalse();
        verify(zSetOperations).removeRangeByScore(anyString(), eq(0.0), anyDouble());
    }

    @Test
    @DisplayName("남은 요청 수 확인 - 인증 엔드포인트")
    void getRemainingRequests_AuthEndpoint() {
        // Given
        String key = "test-key";
        given(zSetOperations.count(anyString(), anyDouble(), anyDouble())).willReturn(3L);

        // When
        long remaining = rateLimitingService.getRemainingRequests(key, true);

        // Then
        assertThat(remaining).isEqualTo(2L); // 5 - 3 = 2
    }

    @Test
    @DisplayName("남은 요청 수 확인 - API 엔드포인트")
    void getRemainingRequests_ApiEndpoint() {
        // Given
        String key = "test-key";
        given(zSetOperations.count(anyString(), anyDouble(), anyDouble())).willReturn(45L);

        // When
        long remaining = rateLimitingService.getRemainingRequests(key, false);

        // Then
        assertThat(remaining).isEqualTo(15L); // 60 - 45 = 15
    }

    @Test
    @DisplayName("속도 제한 정보 확인")
    void getRateLimitInfo_Success() {
        // Given
        String key = "test-key";
        given(zSetOperations.count(anyString(), anyDouble(), anyDouble())).willReturn(2L);

        // When
        RateLimitingService.RateLimitInfo info = rateLimitingService.getRateLimitInfo(key, true);

        // Then
        assertThat(info.maxRequestsPerMinute()).isEqualTo(5);
        assertThat(info.maxRequestsPerHour()).isEqualTo(20);
        assertThat(info.remainingRequestsThisMinute()).isEqualTo(3L);
    }

    @Test
    @DisplayName("IP 키 생성")
    void createIpKey() {
        // When
        String key = RateLimitingService.createIpKey("192.168.1.1");

        // Then
        assertThat(key).isEqualTo("ip:192.168.1.1");
    }

    @Test
    @DisplayName("사용자 키 생성")
    void createUserKey() {
        // When
        String key = RateLimitingService.createUserKey("user123");

        // Then
        assertThat(key).isEqualTo("user:user123");
    }

    @Test
    @DisplayName("Redis 오류 시 허용 (fail-open)")
    void tryConsumeAuthRequest_RedisError_FailOpen() {
        // Given
        String key = "test-key";
        given(zSetOperations.removeRangeByScore(anyString(), anyDouble(), anyDouble()))
            .willThrow(new RuntimeException("Redis connection error"));

        // When
        boolean result = rateLimitingService.tryConsumeAuthRequest(key);

        // Then
        assertThat(result).isTrue(); // fail-open behavior
    }
}