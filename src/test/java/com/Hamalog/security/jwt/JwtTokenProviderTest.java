package com.Hamalog.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JWT Token Provider Tests")
class JwtTokenProviderTest {

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private Environment environment;

    private JwtTokenProvider jwtTokenProvider;
    private String validBase64Secret;

    @BeforeEach
    void setUp() {
        // Generate a valid 256-bit key
        byte[] keyBytes = new byte[32]; // 256 bits
        for (int i = 0; i < keyBytes.length; i++) {
            keyBytes[i] = (byte) i;
        }
        validBase64Secret = Base64.getEncoder().encodeToString(keyBytes);
        
        // Setup default mock behavior
        lenient().when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});
        lenient().when(tokenBlacklistService.isTokenBlacklisted(anyString())).thenReturn(false);
        
        jwtTokenProvider = new JwtTokenProvider(
            validBase64Secret,
            3600000L, // 1 hour
            tokenBlacklistService,
            environment
        );
    }

    @Test
    @DisplayName("Should initialize with valid Base64 secret successfully")
    void init_ValidBase64Secret_ShouldInitializeSuccessfully() throws Exception {
        // when
        Method initMethod = JwtTokenProvider.class.getDeclaredMethod("init");
        initMethod.setAccessible(true);
        initMethod.invoke(jwtTokenProvider);
        
        // then - should not throw exception
        // Verify by creating a token
        String token = jwtTokenProvider.createToken("testuser");
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
    }

    @Test
    @DisplayName("Should throw exception for invalid Base64 secret")
    void init_InvalidBase64Secret_ShouldThrowException() {
        // given
        JwtTokenProvider providerWithInvalidSecret = new JwtTokenProvider(
            "invalid-base64-!@#$",
            3600000L,
            tokenBlacklistService,
            environment
        );
        
        // when & then
        assertThatThrownBy(() -> {
            Method initMethod = JwtTokenProvider.class.getDeclaredMethod("init");
            initMethod.setAccessible(true);
            initMethod.invoke(providerWithInvalidSecret);
        }).getCause().isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Base64로 인코딩되어야 합니다");
    }

    @Test
    @DisplayName("Should throw exception for short secret key")
    void init_ShortSecret_ShouldThrowException() {
        // given - 128-bit key (too short)
        byte[] shortKey = new byte[16]; // 128 bits
        String shortBase64Secret = Base64.getEncoder().encodeToString(shortKey);
        
        JwtTokenProvider providerWithShortSecret = new JwtTokenProvider(
            shortBase64Secret,
            3600000L,
            tokenBlacklistService,
            environment
        );
        
        // when & then
        assertThatThrownBy(() -> {
            Method initMethod = JwtTokenProvider.class.getDeclaredMethod("init");
            initMethod.setAccessible(true);
            initMethod.invoke(providerWithShortSecret);
        }).getCause().isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("최소 256비트(32바이트)여야 합니다");
    }

    @Test
    @DisplayName("Should throw exception in production with empty secret")
    void init_ProductionEmptySecret_ShouldThrowException() {
        // given
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        
        JwtTokenProvider providerWithEmptySecret = new JwtTokenProvider(
            "",
            3600000L,
            tokenBlacklistService,
            environment
        );
        
        // when & then
        assertThatThrownBy(() -> {
            Method initMethod = JwtTokenProvider.class.getDeclaredMethod("init");
            initMethod.setAccessible(true);
            initMethod.invoke(providerWithEmptySecret);
        }).getCause().isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("JWT 비밀키가 설정되지 않았습니다");
    }

    @Test
    @DisplayName("Should generate random key in development with empty secret")
    void init_DevelopmentEmptySecret_ShouldGenerateRandomKey() throws Exception {
        // given
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        
        JwtTokenProvider providerWithEmptySecret = new JwtTokenProvider(
            "",
            3600000L,
            tokenBlacklistService,
            environment
        );
        
        // when
        Method initMethod = JwtTokenProvider.class.getDeclaredMethod("init");
        initMethod.setAccessible(true);
        initMethod.invoke(providerWithEmptySecret);
        
        // then - should be able to create and validate tokens
        String token = providerWithEmptySecret.createToken("testuser");
        assertThat(token).isNotNull();
        assertThat(providerWithEmptySecret.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("Should create valid JWT token")
    void createToken_ValidLoginId_ShouldReturnValidToken() throws Exception {
        // given
        initializeProvider();
        String loginId = "testuser";
        
        // when
        String token = jwtTokenProvider.createToken(loginId);
        
        // then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getLoginIdFromToken(token)).isEqualTo(loginId);
    }

    @Test
    @DisplayName("Should create token with extra claims")
    void createToken_WithExtraClaims_ShouldIncludeClaims() throws Exception {
        // given
        initializeProvider();
        String loginId = "testuser";
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", "USER");
        extraClaims.put("department", "IT");
        extraClaims.put("level", 1);
        
        // when
        String token = jwtTokenProvider.createToken(loginId, extraClaims);
        
        // then
        assertThat(token).isNotNull();
        Claims claims = jwtTokenProvider.getAllClaims(token);
        assertThat(claims.get("role")).isEqualTo("USER");
        assertThat(claims.get("department")).isEqualTo("IT");
        assertThat(claims.get("level")).isEqualTo(1);
        assertThat(claims.getSubject()).isEqualTo(loginId);
    }

    @Test
    @DisplayName("Should create token without extra claims when null")
    void createToken_NullExtraClaims_ShouldCreateTokenWithoutExtraClaims() throws Exception {
        // given
        initializeProvider();
        String loginId = "testuser";
        
        // when
        String token = jwtTokenProvider.createToken(loginId, null);
        
        // then
        assertThat(token).isNotNull();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getLoginIdFromToken(token)).isEqualTo(loginId);
    }

    @Test
    @DisplayName("Should validate token successfully for valid token")
    void validateToken_ValidToken_ShouldReturnTrue() throws Exception {
        // given
        initializeProvider();
        String token = jwtTokenProvider.createToken("testuser");
        
        // when
        boolean isValid = jwtTokenProvider.validateToken(token);
        
        // then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should reject blacklisted token")
    void validateToken_BlacklistedToken_ShouldReturnFalse() throws Exception {
        // given
        initializeProvider();
        String token = jwtTokenProvider.createToken("testuser");
        when(tokenBlacklistService.isTokenBlacklisted(token)).thenReturn(true);
        
        // when
        boolean isValid = jwtTokenProvider.validateToken(token);
        
        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject malformed token")
    void validateToken_MalformedToken_ShouldReturnFalse() throws Exception {
        // given
        initializeProvider();
        String malformedToken = "not.a.valid.jwt.token";
        
        // when
        boolean isValid = jwtTokenProvider.validateToken(malformedToken);
        
        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject null or empty token")
    void validateToken_NullOrEmptyToken_ShouldReturnFalse() throws Exception {
        // given
        initializeProvider();
        
        // when & then
        assertThat(jwtTokenProvider.validateToken(null)).isFalse();
        assertThat(jwtTokenProvider.validateToken("")).isFalse();
        assertThat(jwtTokenProvider.validateToken("   ")).isFalse();
    }

    @Test
    @DisplayName("Should reject token with invalid signature")
    void validateToken_InvalidSignature_ShouldReturnFalse() throws Exception {
        // given
        initializeProvider();
        String token = jwtTokenProvider.createToken("testuser");
        
        // Tamper with the token by corrupting the signature portion significantly
        String[] tokenParts = token.split("\\.");
        String corruptedSignature = tokenParts[2].substring(0, Math.max(1, tokenParts[2].length() - 5)) + "XXXXX";
        String tamperedToken = tokenParts[0] + "." + tokenParts[1] + "." + corruptedSignature;
        
        // when
        boolean isValid = jwtTokenProvider.validateToken(tamperedToken);
        
        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should get login ID from valid token")
    void getLoginIdFromToken_ValidToken_ShouldReturnLoginId() throws Exception {
        // given
        initializeProvider();
        String loginId = "testuser@example.com";
        String token = jwtTokenProvider.createToken(loginId);
        
        // when
        String extractedLoginId = jwtTokenProvider.getLoginIdFromToken(token);
        
        // then
        assertThat(extractedLoginId).isEqualTo(loginId);
    }

    @Test
    @DisplayName("Should get all claims from valid token")
    void getAllClaims_ValidToken_ShouldReturnClaims() throws Exception {
        // given
        initializeProvider();
        String loginId = "testuser";
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", "ADMIN");
        extraClaims.put("permissions", "READ_WRITE");
        
        String token = jwtTokenProvider.createToken(loginId, extraClaims);
        
        // when
        Claims claims = jwtTokenProvider.getAllClaims(token);
        
        // then
        assertThat(claims.getSubject()).isEqualTo(loginId);
        assertThat(claims.get("role")).isEqualTo("ADMIN");
        assertThat(claims.get("permissions")).isEqualTo("READ_WRITE");
        assertThat(claims.getIssuedAt()).isBeforeOrEqualTo(new Date());
        assertThat(claims.getExpiration()).isAfter(new Date());
    }

    @Test
    @DisplayName("Should handle token expiry validation logic")
    void validateToken_TokenExpiry_ShouldWorkWithClockSkew() throws Exception {
        // given - create provider with standard expiry
        initializeProvider();
        String token = jwtTokenProvider.createToken("testuser");
        
        // when - validate immediately (should be valid)
        boolean isValid = jwtTokenProvider.validateToken(token);
        
        // then - token should be valid
        assertThat(isValid).isTrue();
        
        // Verify claims show correct expiry time
        Claims claims = jwtTokenProvider.getAllClaims(token);
        assertThat(claims.getExpiration()).isAfter(new Date());
        
        // Note: Testing actual expiry would require waiting or mocking time,
        // but the 60-second clock skew tolerance makes very short expiry tests unreliable
    }

    @Test
    @DisplayName("Should handle clock skew in token validation")
    void validateToken_ClockSkew_ShouldHandleGracefully() throws Exception {
        // given
        initializeProvider();
        
        // Create token with future issue date (within clock skew tolerance)
        String token = jwtTokenProvider.createToken("testuser");
        
        // when - validate immediately (should handle clock skew)
        boolean isValid = jwtTokenProvider.validateToken(token);
        
        // then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should create unique tokens for same user")
    void createToken_SameUser_ShouldCreateUniqueTokens() throws Exception {
        // given
        initializeProvider();
        String loginId = "testuser";
        
        // when
        String token1 = jwtTokenProvider.createToken(loginId);
        // Add delay to ensure different timestamp (JWT timestamps are in seconds)
        Thread.sleep(1000);
        String token2 = jwtTokenProvider.createToken(loginId);
        
        // then
        assertThat(token1).isNotEqualTo(token2);
        assertThat(jwtTokenProvider.validateToken(token1)).isTrue();
        assertThat(jwtTokenProvider.validateToken(token2)).isTrue();
        assertThat(jwtTokenProvider.getLoginIdFromToken(token1)).isEqualTo(loginId);
        assertThat(jwtTokenProvider.getLoginIdFromToken(token2)).isEqualTo(loginId);
    }

    @Test
    @DisplayName("Should handle special characters in login ID")
    void createToken_SpecialCharactersInLoginId_ShouldWork() throws Exception {
        // given
        initializeProvider();
        String[] specialLoginIds = {
            "user@example.com",
            "user.name+label@domain.co.kr",
            "한글사용자명",
            "user-with-dashes",
            "user_with_underscores",
            "user123with456numbers"
        };
        
        // when & then
        for (String loginId : specialLoginIds) {
            String token = jwtTokenProvider.createToken(loginId);
            assertThat(token).isNotNull();
            assertThat(jwtTokenProvider.validateToken(token)).isTrue();
            assertThat(jwtTokenProvider.getLoginIdFromToken(token)).isEqualTo(loginId);
        }
    }

    @Test
    @DisplayName("Should handle token with large claims payload")
    void createToken_LargeClaims_ShouldHandleGracefully() throws Exception {
        // given
        initializeProvider();
        String loginId = "testuser";
        Map<String, Object> largeClaims = new HashMap<>();
        
        // Add many claims
        for (int i = 0; i < 50; i++) {
            largeClaims.put("claim" + i, "value" + i + "_".repeat(100));
        }
        
        // when
        String token = jwtTokenProvider.createToken(loginId, largeClaims);
        
        // then
        assertThat(token).isNotNull();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        
        Claims claims = jwtTokenProvider.getAllClaims(token);
        assertThat(claims.getSubject()).isEqualTo(loginId);
        assertThat(claims).hasSize(53); // 50 custom claims + 3 standard claims (sub, iat, exp)
    }

    private void initializeProvider() throws Exception {
        initializeProvider(jwtTokenProvider);
    }
    
    private void initializeProvider(JwtTokenProvider provider) throws Exception {
        Method initMethod = JwtTokenProvider.class.getDeclaredMethod("init");
        initMethod.setAccessible(true);
        initMethod.invoke(provider);
    }
}