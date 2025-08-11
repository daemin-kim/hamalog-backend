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

    public JwtTokenProvider(
            @Value("${jwt.secret:}") String secret,
            @Value("${jwt.expiry:3600000}") long validityInMilliseconds
    ) {
        this.secret = secret;
        this.validityInMilliseconds = validityInMilliseconds;
    }

    @PostConstruct
    protected void init() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret is not configured. Set jwt.secret as a Base64-encoded 256-bit key.");
        }
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("JWT secret must be Base64-encoded.");
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 256-bit.");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
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
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .clockSkewSeconds(60)
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.info("JWT 만료됨: {}", token);
        } catch (UnsupportedJwtException e) {
            log.warn("JWT 지원하지 않는 형식: {}", token);
        } catch (MalformedJwtException e) {
            log.warn("JWT 위조 또는 변조 가능: {}", token);
        } catch (SignatureException | IllegalArgumentException e) {
            log.warn("JWT 서명 오류 및 잘못된 토큰: {}", token);
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
