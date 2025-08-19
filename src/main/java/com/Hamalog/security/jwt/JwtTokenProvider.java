package com.Hamalog.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private SecretKey secretKey;
    private final String secret;
    private final long validityInMilliseconds;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtTokenProvider(
            @Value("${jwt.secret:EzUuJwKK4vLnvk5r7yAgdNP/sa1dL87febZhlayPGjI=}") String secret,
            @Value("${jwt.expiry:3600000}") long validityInMilliseconds,
            TokenBlacklistService tokenBlacklistService
    ) {
        log.info("[DEBUG_LOG] JWT 생성자 호출됨 - secret 값 주입 확인");
        log.info("[DEBUG_LOG] JWT secret 길이: {}", secret != null ? secret.length() : "null");
        log.info("[DEBUG_LOG] JWT secret 값: '{}'", secret != null ? secret : "NULL");
        log.info("[DEBUG_LOG] JWT expiry: {}", validityInMilliseconds);
        
        this.secret = secret;
        this.validityInMilliseconds = validityInMilliseconds;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @PostConstruct
    protected void init() {
        log.info("[DEBUG_LOG] JWT @PostConstruct init() 메서드 시작");
        log.info("[DEBUG_LOG] Spring Profile 활성화 상태 확인 필요");
        log.info("[DEBUG_LOG] JWT secret 필드 값: '{}'", secret);
        log.info("[DEBUG_LOG] JWT secret null 체크: {}", secret == null);
        log.info("[DEBUG_LOG] JWT secret isBlank 체크: {}", secret != null ? secret.isBlank() : "secret is null");
        
        if (secret == null || secret.isBlank()) {
            log.error("[DEBUG_LOG] ❌ JWT 비밀키 검증 실패! secret='{}'", secret);
            log.error("[DEBUG_LOG] ❌ 환경변수 JWT_SECRET 또는 jwt.secret 프로퍼티가 제대로 설정되지 않음");
            throw new IllegalStateException("JWT 비밀키가 설정되지 않았습니다. jwt.secret에 256비트 Base64 값을 설정하세요.");
        }
        
        log.info("[DEBUG_LOG] ✅ JWT secret 검증 통과 - Base64 디코딩 시작");
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secret);
            log.info("[DEBUG_LOG] ✅ Base64 디코딩 성공 - 키 바이트 길이: {}", keyBytes.length);
        } catch (IllegalArgumentException e) {
            log.error("[DEBUG_LOG] ❌ Base64 디코딩 실패: {}", e.getMessage());
            log.error("[DEBUG_LOG] ❌ JWT secret 값이 올바른 Base64 형식이 아님: '{}'", secret);
            throw new IllegalStateException("JWT 비밀키는 Base64로 인코딩되어야 합니다.");
        }
        
        if (keyBytes.length < 32) {
            log.error("[DEBUG_LOG] ❌ JWT 키 길이 검증 실패! 현재 길이: {} bytes, 필요 길이: 32 bytes 이상", keyBytes.length);
            throw new IllegalStateException("JWT 비밀키는 최소 256비트여야 합니다.");
        }
        
        log.info("[DEBUG_LOG] ✅ JWT 키 길이 검증 통과: {} bytes", keyBytes.length);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        log.info("[DEBUG_LOG] ✅ JWT SecretKey 생성 완료 - JwtTokenProvider 초기화 성공");
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
        // First check if token is blacklisted
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
