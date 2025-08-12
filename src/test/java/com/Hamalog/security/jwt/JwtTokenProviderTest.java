package com.Hamalog.security.jwt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JWT Token Provider Unit Tests")
class JwtTokenProviderTest {

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Test
    @DisplayName("JWT Provider initializes successfully with valid Base64 secret")
    void jwtProviderInitializesWithValidSecret() {
        // Given a valid Base64-encoded 256-bit secret
        String validSecret = "DPy5yMHkeq4GfH0pRRUTmC7D7x3wFVegFLGmjDI7auo=";
        
        // When creating JwtTokenProvider
        assertDoesNotThrow(() -> {
            JwtTokenProvider provider = new JwtTokenProvider(validSecret, 3600000L, tokenBlacklistService);
            // This would trigger @PostConstruct init() method
            provider.init();
        });
    }

    @Test
    @DisplayName("JWT Provider throws IllegalStateException with empty secret")
    void jwtProviderThrowsExceptionWithEmptySecret() {
        // Given an empty secret (simulating the original error condition)
        String emptySecret = "";
        
        // When creating JwtTokenProvider with empty secret
        JwtTokenProvider provider = new JwtTokenProvider(emptySecret, 3600000L, tokenBlacklistService);
        
        // Then it should throw IllegalStateException during init
        IllegalStateException exception = assertThrows(IllegalStateException.class, provider::init);
        assertEquals("JWT 비밀키가 설정되지 않았습니다. jwt.secret에 256비트 Base64 값을 설정하세요.", exception.getMessage());
    }
}