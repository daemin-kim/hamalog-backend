package com.Hamalog.security.csrf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
@DisplayName("CSRF Token Provider Tests")
class CsrfTokenProviderTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private CsrfTokenProvider csrfTokenProvider;

    @BeforeEach
    void setUp() {
        csrfTokenProvider = new CsrfTokenProvider(redisTemplate, true);
    }

    @Nested
    @DisplayName("Redis 연동")
    class RedisBackedBehavior {

        @Test
        @DisplayName("Should store generated token in Redis with TTL")
        void generateToken_ValidSessionId_StoresInRedis() {
            // given
            String sessionId = "user123";
            AtomicReference<String> capturedToken = new AtomicReference<>();
            doAnswer(invocation -> {
                capturedToken.set(invocation.getArgument(1));
                return null;
            }).when(valueOperations).set(eq("csrf:" + sessionId), anyString(), eq(Duration.ofMinutes(60)));
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            // when
            String token = csrfTokenProvider.generateToken(sessionId);

            // then
            assertThat(token).isNotBlank();
            assertThat(capturedToken.get()).isEqualTo(token);
            verify(valueOperations).set(eq("csrf:" + sessionId), eq(token), eq(Duration.ofMinutes(60)));
        }

        @Test
        @DisplayName("Should validate token successfully when stored value matches")
        void validateToken_StoredValueMatches_ReturnsTrue() {
            // given
            String sessionId = "user123";
            String storedToken = "stored-token";
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("csrf:" + sessionId)).thenReturn(storedToken);

            // when
            boolean result = csrfTokenProvider.validateToken(sessionId, storedToken);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return remaining TTL when available")
        void getRemainingTtl_WithValue_ReturnsOptional() {
            when(redisTemplate.getExpire("csrf:user123")).thenReturn(120L);
            Optional<Long> ttl = csrfTokenProvider.getRemainingTtl("user123");
            assertThat(ttl).contains(120L);
            verify(redisTemplate).getExpire("csrf:user123");
        }
    }

    @Test
    @DisplayName("Should throw when sessionId is blank")
    void generateToken_BlankSession_ThrowsException() {
        assertThatThrownBy(() -> csrfTokenProvider.generateToken(null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> csrfTokenProvider.generateToken("  "))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should return false when stored value differs")
    void validateToken_DifferentValue_ReturnsFalse() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("csrf:user123")).thenReturn("stored-token");
        boolean result = csrfTokenProvider.validateToken("user123", "another-token");
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when session or token blank")
    void validateToken_BlankInputs_ReturnsFalse() {
        assertThat(csrfTokenProvider.validateToken(null, "token")).isFalse();
        assertThat(csrfTokenProvider.validateToken("user", null)).isFalse();
        assertThat(csrfTokenProvider.validateToken("", "token")).isFalse();
    }

    @Test
    @DisplayName("Should delete key when invalidating token")
    void invalidateToken_RemovesRedisKey() {
        csrfTokenProvider.invalidateToken("user123");
        verify(redisTemplate).delete("csrf:user123");
    }

    @Test
    @DisplayName("Should fall back to in-memory store when Redis is unavailable")
    void fallbackStoreUsedWhenRedisFails() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RedisConnectionFailureException("connection down"))
                .when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        String token = csrfTokenProvider.generateToken("user123");

        doThrow(new RedisConnectionFailureException("connection down"))
                .when(valueOperations).get(anyString());

        assertThat(csrfTokenProvider.validateToken("user123", token)).isTrue();
    }

    @Test
    @DisplayName("Should remove fallback entry on invalidate")
    void invalidateToken_RemovesFallback() {
        CsrfTokenProvider providerWithoutRedis = new CsrfTokenProvider(null, true);
        providerWithoutRedis.generateToken("user123");
        providerWithoutRedis.invalidateToken("user123");
        assertThat(providerWithoutRedis.validateToken("user123", "any"))
                .isFalse();
    }

    @Test
    @DisplayName("Should return fallback TTL when Redis unavailable")
    void getRemainingTtl_FromFallback() {
        CsrfTokenProvider providerWithoutRedis = new CsrfTokenProvider(null, true);
        providerWithoutRedis.generateToken("user123");
        Optional<Long> ttl = providerWithoutRedis.getRemainingTtl("user123");
        assertThat(ttl).isPresent();
    }

    @Test
    @DisplayName("Should handle redisTemplate returning non-string values gracefully")
    void validateToken_NonStringValue_ReturnsFalse() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("csrf:user123")).thenReturn(12345);
        assertThat(csrfTokenProvider.validateToken("user123", "token")).isFalse();
        verify(valueOperations).get("csrf:user123");
    }

    @Test
    @DisplayName("Should skip Redis delete when sessionId blank")
    void invalidateToken_BlankInput_NoOperation() {
        csrfTokenProvider.invalidateToken(" ");
        verify(redisTemplate, never()).delete(anyString());
    }
}