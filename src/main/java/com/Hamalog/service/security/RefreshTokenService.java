package com.Hamalog.service.security;

import com.Hamalog.domain.member.Member;
import com.Hamalog.domain.security.RefreshToken;
import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.repository.member.MemberRepository;
import com.Hamalog.repository.security.RefreshTokenRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(rollbackFor = {Exception.class})
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberRepository memberRepository;

    @Value("${jwt.refresh-token.expiry:604800000}")
    private long refreshTokenExpiryMs;

    /**
     * 새로운 RefreshToken 생성 및 저장
     */
    public RefreshToken createRefreshToken(Long memberId) {
        // 기존 토큰 제거 (사용자당 1개만 유지)
        revokeAllByMemberId(memberId);

        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusSeconds(refreshTokenExpiryMs / 1000);

        RefreshToken token = RefreshToken.builder()
            .member(member)
            .tokenValue(generateSecureToken())
            .createdAt(now)
            .expiresAt(expiresAt)
            .rotatedAt(now)
            .revoked(false)
            .build();

        log.debug("[SECURITY] RefreshToken created - memberId: {}", memberId);
        return refreshTokenRepository.save(token);
    }

    /**
     * RefreshToken 검증 및 새로운 토큰 발급 (Token Rotation)
     */
    public RefreshToken rotateToken(String refreshToken) {
        RefreshToken token = refreshTokenRepository.findByTokenValue(refreshToken)
            .orElseThrow(() -> {
                log.warn("[SECURITY] Invalid refresh token attempt");
                return new CustomException(ErrorCode.UNAUTHORIZED);
            });

        if (!token.isValid()) {
            log.warn("[SECURITY] Refresh token is invalid or expired - memberId: {}", token.getMemberId());
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        // ✅ Token Rotation: 기존 토큰 폐지 및 새 토큰 발급
        token.setRevoked(true);
        refreshTokenRepository.save(token);

        log.debug("[SECURITY] Old refresh token revoked - memberId: {}", token.getMemberId());

        return createRefreshToken(token.getMemberId());
    }

    /**
     * 사용자의 모든 RefreshToken 폐지 (로그아웃)
     */
    public void revokeAllByMemberId(Long memberId) {
        refreshTokenRepository.revokeAllByMemberId(memberId);
        log.info("[SECURITY] All refresh tokens revoked - memberId: {}", memberId);
    }

    /**
     * 만료된 토큰 자동 정리 (매일 자정)
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        log.info("[SECURITY] Expired refresh tokens cleaned up");
    }

    /**
     * 안전한 토큰 생성 (256비트 무작위)
     */
    private String generateSecureToken() {
        return UUID.randomUUID().toString().replace("-", "") +
               UUID.randomUUID().toString().replace("-", "");
    }
}
