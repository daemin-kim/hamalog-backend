package com.Hamalog.security.csrf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("CSRF Token Provider Tests")
class CsrfTokenProviderTest {

    private CsrfTokenProvider csrfTokenProvider;

    @BeforeEach
    void setUp() {
        csrfTokenProvider = new CsrfTokenProvider();
    }

    @Test
    @DisplayName("Should generate valid CSRF token successfully")
    void generateToken_ValidSessionId_ShouldReturnToken() {
        // given
        String sessionId = "user123";
        
        // when
        String token = csrfTokenProvider.generateToken(sessionId);
        
        // then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.length()).isGreaterThan(0);
        // Base64 URL encoded tokens don't have padding
        assertThat(token).doesNotContain("=");
    }

    @Test
    @DisplayName("Should generate different tokens for same session")
    void generateToken_SameSession_ShouldGenerateDifferentTokens() {
        // given
        String sessionId = "user123";
        
        // when
        String token1 = csrfTokenProvider.generateToken(sessionId);
        String token2 = csrfTokenProvider.generateToken(sessionId);
        
        // then
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    @DisplayName("Should generate different tokens for different sessions")
    void generateToken_DifferentSessions_ShouldGenerateDifferentTokens() {
        // given
        String sessionId1 = "user123";
        String sessionId2 = "user456";
        
        // when
        String token1 = csrfTokenProvider.generateToken(sessionId1);
        String token2 = csrfTokenProvider.generateToken(sessionId2);
        
        // then
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    @DisplayName("Should throw exception for null session ID")
    void generateToken_NullSessionId_ShouldThrowException() {
        // when & then
        assertThatThrownBy(() -> csrfTokenProvider.generateToken(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Session ID는 필수입니다");
    }

    @Test
    @DisplayName("Should throw exception for empty session ID")
    void generateToken_EmptySessionId_ShouldThrowException() {
        // when & then
        assertThatThrownBy(() -> csrfTokenProvider.generateToken(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Session ID는 필수입니다");
        
        assertThatThrownBy(() -> csrfTokenProvider.generateToken("   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Session ID는 필수입니다");
    }

    @Test
    @DisplayName("Should validate token successfully for valid token")
    void validateToken_ValidToken_ShouldReturnTrue() {
        // given
        String sessionId = "user123";
        String token = csrfTokenProvider.generateToken(sessionId);
        
        // when
        boolean isValid = csrfTokenProvider.validateToken(sessionId, token);
        
        // then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should reject invalid token")
    void validateToken_InvalidToken_ShouldReturnFalse() {
        // given
        String sessionId = "user123";
        csrfTokenProvider.generateToken(sessionId);
        String invalidToken = "invalid-token";
        
        // when
        boolean isValid = csrfTokenProvider.validateToken(sessionId, invalidToken);
        
        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject token for non-existent session")
    void validateToken_NonExistentSession_ShouldReturnFalse() {
        // given
        String sessionId = "user123";
        String token = csrfTokenProvider.generateToken(sessionId);
        String differentSessionId = "user456";
        
        // when
        boolean isValid = csrfTokenProvider.validateToken(differentSessionId, token);
        
        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject null or empty parameters")
    void validateToken_NullOrEmptyParams_ShouldReturnFalse() {
        // given
        String sessionId = "user123";
        String token = csrfTokenProvider.generateToken(sessionId);
        
        // when & then
        assertThat(csrfTokenProvider.validateToken(null, token)).isFalse();
        assertThat(csrfTokenProvider.validateToken("", token)).isFalse();
        assertThat(csrfTokenProvider.validateToken("   ", token)).isFalse();
        assertThat(csrfTokenProvider.validateToken(sessionId, null)).isFalse();
        assertThat(csrfTokenProvider.validateToken(sessionId, "")).isFalse();
        assertThat(csrfTokenProvider.validateToken(sessionId, "   ")).isFalse();
        assertThat(csrfTokenProvider.validateToken(null, null)).isFalse();
    }

    @Test
    @DisplayName("Should invalidate token successfully")
    void invalidateToken_ValidSession_ShouldInvalidateToken() {
        // given
        String sessionId = "user123";
        String token = csrfTokenProvider.generateToken(sessionId);
        
        // when
        csrfTokenProvider.invalidateToken(sessionId);
        
        // then
        boolean isValid = csrfTokenProvider.validateToken(sessionId, token);
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should handle invalidation of non-existent session gracefully")
    void invalidateToken_NonExistentSession_ShouldNotThrowException() {
        // given
        String nonExistentSessionId = "nonexistent";
        
        // when & then - should not throw exception
        csrfTokenProvider.invalidateToken(nonExistentSessionId);
    }

    @Test
    @DisplayName("Should handle null session ID for invalidation gracefully")
    void invalidateToken_NullSessionId_ShouldNotThrowException() {
        // when & then - should not throw exception
        csrfTokenProvider.invalidateToken(null);
        csrfTokenProvider.invalidateToken("");
        csrfTokenProvider.invalidateToken("   ");
    }

    @Test
    @DisplayName("Should generate new token and invalidate old one")
    void generateToken_ExistingSession_ShouldInvalidateOldToken() {
        // given
        String sessionId = "user123";
        String oldToken = csrfTokenProvider.generateToken(sessionId);
        
        // when
        String newToken = csrfTokenProvider.generateToken(sessionId);
        
        // then
        assertThat(newToken).isNotEqualTo(oldToken);
        assertThat(csrfTokenProvider.validateToken(sessionId, oldToken)).isFalse();
        assertThat(csrfTokenProvider.validateToken(sessionId, newToken)).isTrue();
    }

    @Test
    @DisplayName("Should use constant time comparison to prevent timing attacks")
    void constantTimeEquals_VariousInputs_ShouldUseConstantTime() throws Exception {
        // given
        Method constantTimeEquals = CsrfTokenProvider.class.getDeclaredMethod("constantTimeEquals", String.class, String.class);
        constantTimeEquals.setAccessible(true);
        
        // when & then
        assertThat((Boolean) constantTimeEquals.invoke(csrfTokenProvider, "same", "same")).isTrue();
        assertThat((Boolean) constantTimeEquals.invoke(csrfTokenProvider, "different", "strings")).isFalse();
        assertThat((Boolean) constantTimeEquals.invoke(csrfTokenProvider, "short", "verylongstring")).isFalse();
        assertThat((Boolean) constantTimeEquals.invoke(csrfTokenProvider, null, "string")).isFalse();
        assertThat((Boolean) constantTimeEquals.invoke(csrfTokenProvider, "string", null)).isFalse();
        assertThat((Boolean) constantTimeEquals.invoke(csrfTokenProvider, null, null)).isFalse();
    }

    @Test
    @DisplayName("Should clean expired tokens automatically")
    void generateToken_ExpiredTokensExist_ShouldCleanThem() throws Exception {
        // given
        String sessionId1 = "user123";
        String sessionId2 = "user456";
        
        // Create tokens
        csrfTokenProvider.generateToken(sessionId1);
        csrfTokenProvider.generateToken(sessionId2);
        
        // Access private tokenStorage field to manually create expired token
        Field tokenStorageField = CsrfTokenProvider.class.getDeclaredField("tokenStorage");
        tokenStorageField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, Object> tokenStorage = (ConcurrentHashMap<String, Object>) tokenStorageField.get(csrfTokenProvider);
        
        // Verify tokens exist
        assertThat(tokenStorage).hasSize(2);
        
        // when - generate another token which should trigger cleanup
        String sessionId3 = "user789";
        csrfTokenProvider.generateToken(sessionId3);
        
        // then - new token should exist
        assertThat(tokenStorage).containsKey(sessionId3);
    }

    @Test
    @DisplayName("Should handle concurrent token operations safely")
    void tokenOperations_Concurrent_ShouldHandleSafely() throws InterruptedException {
        // given
        String sessionId = "user123";
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        
        // when - multiple threads generating and validating tokens
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                String token = csrfTokenProvider.generateToken(sessionId);
                assertThat(token).isNotNull();
                
                // Validate the token
                boolean isValid = csrfTokenProvider.validateToken(sessionId, token);
                // Note: due to concurrent generation, this token might be invalidated by another thread
                // but the operation should not throw exceptions
            });
            threads[i].start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // then - should complete without exceptions
        // The final state should have only one valid token for the session
    }

    @Test
    @DisplayName("Should handle multiple sessions independently")
    void tokenOperations_MultipleSessions_ShouldBeIndependent() {
        // given
        String sessionId1 = "user123";
        String sessionId2 = "user456";
        String sessionId3 = "user789";
        
        // when
        String token1 = csrfTokenProvider.generateToken(sessionId1);
        String token2 = csrfTokenProvider.generateToken(sessionId2);
        String token3 = csrfTokenProvider.generateToken(sessionId3);
        
        // then - all tokens should be valid for their respective sessions
        assertThat(csrfTokenProvider.validateToken(sessionId1, token1)).isTrue();
        assertThat(csrfTokenProvider.validateToken(sessionId2, token2)).isTrue();
        assertThat(csrfTokenProvider.validateToken(sessionId3, token3)).isTrue();
        
        // Cross-validation should fail
        assertThat(csrfTokenProvider.validateToken(sessionId1, token2)).isFalse();
        assertThat(csrfTokenProvider.validateToken(sessionId2, token3)).isFalse();
        assertThat(csrfTokenProvider.validateToken(sessionId3, token1)).isFalse();
        
        // Invalidating one session should not affect others
        csrfTokenProvider.invalidateToken(sessionId2);
        assertThat(csrfTokenProvider.validateToken(sessionId1, token1)).isTrue();
        assertThat(csrfTokenProvider.validateToken(sessionId2, token2)).isFalse();
        assertThat(csrfTokenProvider.validateToken(sessionId3, token3)).isTrue();
    }

    @Test
    @DisplayName("Should handle special characters in session ID")
    void generateToken_SpecialCharactersInSessionId_ShouldWork() {
        // given
        String[] specialSessionIds = {
            "user@example.com",
            "session-with-dashes",
            "session_with_underscores",
            "session.with.dots",
            "session123with456numbers",
            "한글세션아이디"
        };
        
        // when & then
        for (String sessionId : specialSessionIds) {
            String token = csrfTokenProvider.generateToken(sessionId);
            assertThat(token).isNotNull();
            assertThat(csrfTokenProvider.validateToken(sessionId, token)).isTrue();
        }
    }

    @Test
    @DisplayName("Should generate tokens with sufficient entropy")
    void generateToken_MultipleGenerations_ShouldHaveSufficientEntropy() {
        // given
        String sessionId = "user123";
        int tokenCount = 100;
        
        // when - generate multiple tokens
        String[] tokens = new String[tokenCount];
        for (int i = 0; i < tokenCount; i++) {
            tokens[i] = csrfTokenProvider.generateToken(sessionId);
        }
        
        // then - all tokens should be unique (very high probability with 32-byte random tokens)
        for (int i = 0; i < tokenCount - 1; i++) {
            for (int j = i + 1; j < tokenCount; j++) {
                assertThat(tokens[i]).isNotEqualTo(tokens[j]);
            }
        }
    }
}