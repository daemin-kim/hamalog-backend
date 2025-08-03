package com.Hamalog.security.jwt;

import io.jsonwebtoken.*;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtTokenProvider {

    private final String secretKey = "매우_강력한_시크릿_키";
    private static final long validityInMilliseconds = 3600000;

    public String createToken(String loginId) {
        Claims claims = Jwts.claims().setSubject(loginId);

        Date now = new Date();
        Date expiry = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(expiry)
            .signWith(SignatureAlgorithm.HS256, secretKey)
            .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getLoginIdFromToken(String token) {
        return Jwts.parser().setSigningKey(secretKey)
            .parseClaimsJws(token).getBody().getSubject();
    }
}
