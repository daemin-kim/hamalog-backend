package com.Hamalog.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private SecretKey secretKey;
    private final String fallbackSecret;
    private final long validityInMilliseconds;
    private final TokenBlacklistService tokenBlacklistService;
    private final Environment environment;

    public JwtTokenProvider(
            @Value("${jwt.secret:}") String fallbackSecret,
            @Value("${jwt.expiry:3600000}") long validityInMilliseconds,
            TokenBlacklistService tokenBlacklistService,
            Environment environment) {
        this.fallbackSecret = fallbackSecret;
        this.validityInMilliseconds = validityInMilliseconds;
        this.tokenBlacklistService = tokenBlacklistService;
        this.environment = environment;
    }

    @PostConstruct
    protected void init() {
        // Check if running in production profile
        boolean isProduction = environment.getActiveProfiles().length > 0 && 
                              java.util.Arrays.asList(environment.getActiveProfiles()).contains("prod");
        
        // ✅ 환경 변수 우선순위: 1) JWT_SECRET 환경변수, 2) properties 파일
        String secret = System.getenv("JWT_SECRET");
        if (secret == null || secret.trim().isEmpty()) {
            secret = fallbackSecret;
        }

        if (secret != null && !secret.trim().isEmpty()) {
            log.info("[JWT_PROVIDER] Using JWT secret from configuration (length: {} bytes)", secret.length());
        }
        
        if (secret == null || secret.trim().isEmpty()) {
            if (isProduction) {
                String errorMessage = String.format(
                    "⛔ JWT 비밀키가 설정되지 않았습니다. 프로덕션 환경에서는 반드시 설정해야 합니다.\n" +
                    "현재 jwt.secret 상태: [%s]\n" +
                    "현재 jwt.secret 길이: %d\n" +
                    "해결 방법:\n" +
                    "1. 환경 변수 설정: export JWT_SECRET=$(openssl rand -base64 32)\n" +
                    "2. application-prod.properties: jwt.secret=<Base64 인코딩된 256비트 키>\n" +
                    "3. 키 생성 예시: openssl rand -base64 32",
                    secret == null ? "null" : "'" + secret + "'",
                    secret == null ? 0 : secret.length()
                );
                log.error("[JWT_PROVIDER] {}", errorMessage);
                throw new IllegalStateException(errorMessage);
            }
            
            // Generate secure random key for development
            log.warn("⚠️ [JWT_PROVIDER] JWT secret not configured. Generating random key for DEVELOPMENT ONLY.");
            log.warn("⚠️ [JWT_PROVIDER] This key will change on every restart. Set JWT_SECRET environment variable for persistent sessions.");

            byte[] randomKey = new byte[32]; // 256 bits
            new SecureRandom().nextBytes(randomKey);
            this.secretKey = Keys.hmacShaKeyFor(randomKey);
            return;
        }
        
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException e) {
            log.error("[JWT_PROVIDER] JWT secret must be Base64 encoded");
            throw new IllegalStateException("JWT 비밀키는 Base64로 인코딩되어야 합니다. 올바른 Base64 형식의 256비트 키를 사용하세요.", e);
        }
        
        if (keyBytes.length < 32) {
            log.error("[JWT_PROVIDER] JWT secret is too short: {} bits (minimum: 256 bits)", keyBytes.length * 8);
            throw new IllegalStateException("JWT 비밀키는 최소 256비트(32바이트)여야 합니다. 현재 키 길이: " + (keyBytes.length * 8) + "비트");
        }
        
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        
        if (!isProduction) {
            log.info("[JWT_PROVIDER] ✅ JWT configured successfully with {} bits key", keyBytes.length * 8);
        } else {
            log.info("[JWT_PROVIDER] ✅ JWT configured successfully for production");
        }
    }

    public String createToken(String loginId) {
        return createToken(loginId, null);
    }

    public String createToken(String loginId, Map<String, Object> extraClaims) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validityInMilliseconds);

        JwtBuilder builder = Jwts.builder()
                .setSubject(loginId)
                .setIssuedAt(now)
                .setExpiration(expiry);

        if (extraClaims != null && !extraClaims.isEmpty()) {
            builder.addClaims(extraClaims);
        }

        return builder
                .signWith(secretKey)
                .compact();
    }

    public boolean validateToken(String token) {
        if (tokenBlacklistService.isTokenBlacklisted(token)) {
            log.info("JWT 토큰이 블랙리스트에 있습니다");
            return false;
        }
        
        try {
            Jwts.parser()
                    .clockSkewSeconds(60)
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.info("JWT 만료됨");
        } catch (UnsupportedJwtException e) {
            log.warn("JWT 지원하지 않는 형식");
        } catch (MalformedJwtException e) {
            log.warn("JWT 위조 또는 변조 가능");
        } catch (SignatureException | IllegalArgumentException e) {
            log.warn("JWT 서명 오류 및 잘못된 토큰");
        }
        return false;
    }

    public String getLoginIdFromToken(String token) {
        return getAllClaims(token).getSubject();
    }

    public Claims getAllClaims(String token) {
        return Jwts.parser()
                .clockSkewSeconds(60)
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
