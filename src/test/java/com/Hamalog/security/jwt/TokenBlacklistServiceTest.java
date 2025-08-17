package com.Hamalog.security.jwt;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * TokenBlacklistService 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TokenBlacklistService 테스트")
class TokenBlacklistServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private Claims claims;

    private TokenBlacklistService tokenBlacklistService;

    private static final String TEST_TOKEN = "test.jwt.token";
    private static final String REDIS_KEY = "jwt_blacklist:" + TEST_TOKEN;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        tokenBlacklistService = new TokenBlacklistService(redisTemplate, jwtTokenProvider);
    }

    @Test
    @DisplayName("토큰 블랙리스트 추가 - Redis 사용")
    void blacklistToken_WithRedis_Success() {
        // Given
        Date expirationDate = new Date(System.currentTimeMillis() + 3600000); // 1 hour from now
        given(jwtTokenProvider.getAllClaims(TEST_TOKEN)).willReturn(claims);
        given(claims.getExpiration()).willReturn(expirationDate);

        // When
        tokenBlacklistService.blacklistToken(TEST_TOKEN);

        // Then
        verify(valueOperations).set(eq(REDIS_KEY), eq("blacklisted"), anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("토큰 블랙리스트 추가 - TTL 계산 실패 시 기본 TTL")
    void blacklistToken_FailedTtlCalculation_UseDefaultTtl() {
        // Given
        given(jwtTokenProvider.getAllClaims(TEST_TOKEN)).willThrow(new RuntimeException("Token parsing failed"));

        // When
        tokenBlacklistService.blacklistToken(TEST_TOKEN);

        // Then
        verify(valueOperations).set(eq(REDIS_KEY), eq("blacklisted"), eq(24L), eq(TimeUnit.HOURS));
    }

    @Test
    @DisplayName("토큰 블랙리스트 추가 - Redis 실패 시 메모리 저장소 사용")
    void blacklistToken_RedisFailure_UseMemoryStorage() {
        // Given
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis connection failed"));

        // When
        tokenBlacklistService.blacklistToken(TEST_TOKEN);

        // Then
        assertThat(tokenBlacklistService.isTokenBlacklisted(TEST_TOKEN)).isTrue();
    }

    @Test
    @DisplayName("토큰 블랙리스트 확인 - Redis에서 발견")
    void isTokenBlacklisted_FoundInRedis_ReturnTrue() {
        // Given
        given(redisTemplate.hasKey(REDIS_KEY)).willReturn(true);

        // When
        boolean result = tokenBlacklistService.isTokenBlacklisted(TEST_TOKEN);

        // Then
        assertThat(result).isTrue();
        verify(redisTemplate).hasKey(REDIS_KEY);
    }

    @Test
    @DisplayName("토큰 블랙리스트 확인 - Redis에서 찾지 못했지만 메모리에서 발견")
    void isTokenBlacklisted_NotInRedisButInMemory_ReturnTrue() {
        // Given
        given(redisTemplate.hasKey(REDIS_KEY)).willReturn(false);
        // 메모리에 직접 추가 (Redis 실패 시나리오를 위해)
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis failed"));
        tokenBlacklistService.blacklistToken(TEST_TOKEN); // 메모리에 저장됨

        // When
        boolean result = tokenBlacklistService.isTokenBlacklisted(TEST_TOKEN);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("토큰 블랙리스트 확인 - 둘 다에서 찾지 못함")
    void isTokenBlacklisted_NotFound_ReturnFalse() {
        // Given
        given(redisTemplate.hasKey(REDIS_KEY)).willReturn(false);

        // When
        boolean result = tokenBlacklistService.isTokenBlacklisted(TEST_TOKEN);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("토큰 블랙리스트 확인 - null 토큰")
    void isTokenBlacklisted_NullToken_ReturnFalse() {
        // When
        boolean result = tokenBlacklistService.isTokenBlacklisted(null);

        // Then
        assertThat(result).isFalse();
        verify(redisTemplate, never()).hasKey(anyString());
    }

    @Test
    @DisplayName("토큰 블랙리스트 확인 - 빈 토큰")
    void isTokenBlacklisted_EmptyToken_ReturnFalse() {
        // When
        boolean result = tokenBlacklistService.isTokenBlacklisted("   ");

        // Then
        assertThat(result).isFalse();
        verify(redisTemplate, never()).hasKey(anyString());
    }

    @Test
    @DisplayName("만료된 토큰 제거 - Redis와 메모리에서 모두 제거")
    void removeExpiredToken_Success() {
        // Given
        // 먼저 메모리에 토큰 추가
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis failed"));
        tokenBlacklistService.blacklistToken(TEST_TOKEN);

        // Redis 복구
        reset(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        tokenBlacklistService.removeExpiredToken(TEST_TOKEN);

        // Then
        verify(redisTemplate).delete(REDIS_KEY);
        assertThat(tokenBlacklistService.isTokenBlacklisted(TEST_TOKEN)).isFalse();
    }

    @Test
    @DisplayName("블랙리스트 크기 확인 - Redis와 메모리 합산")
    void getBlacklistSize_CombineRedisAndMemory() {
        // Given
        Set<String> redisKeys = Set.of("jwt_blacklist:token1", "jwt_blacklist:token2");
        given(redisTemplate.keys("jwt_blacklist:*")).willReturn(redisKeys);
        
        // 메모리에 토큰 추가
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis failed"));
        tokenBlacklistService.blacklistToken("memoryToken");

        // Redis 복구
        reset(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        given(redisTemplate.keys("jwt_blacklist:*")).willReturn(redisKeys);

        // When
        int size = tokenBlacklistService.getBlacklistSize();

        // Then
        assertThat(size).isEqualTo(3); // 2 from Redis + 1 from memory
    }

    @Test
    @DisplayName("블랙리스트 크기 확인 - Redis 오류 시 메모리만")
    void getBlacklistSize_RedisError_MemoryOnly() {
        // Given
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis failed"));
        tokenBlacklistService.blacklistToken(TEST_TOKEN);
        
        when(redisTemplate.keys(anyString())).thenThrow(new RuntimeException("Redis failed"));

        // When
        int size = tokenBlacklistService.getBlacklistSize();

        // Then
        assertThat(size).isEqualTo(1); // Only memory count
    }

    @Test
    @DisplayName("블랙리스트 완전 정리")
    void clearBlacklist_ClearBothRedisAndMemory() {
        // Given
        Set<String> redisKeys = Set.of("jwt_blacklist:token1", "jwt_blacklist:token2");
        given(redisTemplate.keys("jwt_blacklist:*")).willReturn(redisKeys);
        
        // 메모리에 토큰 추가
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis failed"));
        tokenBlacklistService.blacklistToken(TEST_TOKEN);

        // Redis 복구
        reset(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        given(redisTemplate.keys("jwt_blacklist:*")).willReturn(redisKeys);

        // When
        tokenBlacklistService.clearBlacklist();

        // Then
        verify(redisTemplate).delete(redisKeys);
        assertThat(tokenBlacklistService.getBlacklistSize()).isEqualTo(0);
    }

    @Test
    @DisplayName("TTL 계산 - 정상적인 토큰")
    void calculateTokenTtl_ValidToken_Success() {
        // Given
        long futureTime = System.currentTimeMillis() + 3600000; // 1 hour from now
        Date expirationDate = new Date(futureTime);
        given(jwtTokenProvider.getAllClaims(TEST_TOKEN)).willReturn(claims);
        given(claims.getExpiration()).willReturn(expirationDate);

        // When
        tokenBlacklistService.blacklistToken(TEST_TOKEN);

        // Then
        verify(valueOperations).set(eq(REDIS_KEY), eq("blacklisted"), anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("토큰 블랙리스트 추가 - null 토큰 무시")
    void blacklistToken_NullToken_Ignored() {
        // When
        tokenBlacklistService.blacklistToken(null);

        // Then
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("만료된 토큰 제거 - null 토큰 무시")
    void removeExpiredToken_NullToken_Ignored() {
        // When
        tokenBlacklistService.removeExpiredToken(null);

        // Then
        verify(redisTemplate, never()).delete(anyString());
    }
}