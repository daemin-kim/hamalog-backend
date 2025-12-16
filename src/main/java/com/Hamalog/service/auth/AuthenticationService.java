package com.Hamalog.service.auth;

import com.Hamalog.domain.member.Member;
import com.Hamalog.dto.auth.response.LoginResponse;
import com.Hamalog.dto.auth.response.TokenRefreshResponse;
import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.logging.SensitiveDataMasker;
import com.Hamalog.repository.member.MemberRepository;
import com.Hamalog.security.jwt.JwtTokenProvider;
import com.Hamalog.security.jwt.TokenBlacklistService;
import com.Hamalog.service.security.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 관련 서비스
 * 로그인, 로그아웃, 토큰 갱신 등의 책임을 담당합니다.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;

    /**
     * 사용자 인증 후 JWT 토큰을 생성합니다.
     *
     * @param loginId 로그인 ID (이메일)
     * @param password 비밀번호
     * @return 로그인 응답 (AccessToken, RefreshToken, 만료 시간)
     * @throws CustomException 회원을 찾을 수 없는 경우
     */
    public LoginResponse authenticateAndGenerateToken(String loginId, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginId, password)
        );

        Member member = memberRepository.findByLoginId(loginId)
            .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // AccessToken 생성
        String accessToken = jwtTokenProvider.createToken(authentication.getName(), member.getMemberId(), null);
        long expiresIn = 900;  // 15분

        // RefreshToken 생성
        var refreshToken = refreshTokenService.createRefreshToken(member.getMemberId());

        log.info("[AUTH] User logged in - loginId: {}, memberId: {}",
            SensitiveDataMasker.maskEmail(loginId),
            SensitiveDataMasker.maskUserId(member.getMemberId()));

        return new LoginResponse(
            accessToken,
            refreshToken.getTokenValue(),
            expiresIn
        );
    }

    /**
     * 사용자 로그아웃을 처리합니다.
     * 토큰을 블랙리스트에 등록하여 즉시 무효화합니다.
     *
     * @param token 무효화할 토큰
     */
    public void logoutUser(String token) {
        if (isValidTokenFormat(token)) {
            tokenBlacklistService.blacklistToken(token);
            log.debug("[AUTH] User logged out - token blacklisted");
        }
    }

    /**
     * RefreshToken을 사용하여 새 AccessToken을 발급합니다.
     *
     * @param refreshTokenValue 현재 RefreshToken 값
     * @return 새 AccessToken과 RefreshToken
     * @throws CustomException 회원을 찾을 수 없는 경우
     */
    public TokenRefreshResponse refreshAccessToken(String refreshTokenValue) {
        var refreshToken = refreshTokenService.rotateToken(refreshTokenValue);

        // 새 AccessToken 생성
        Member member = memberRepository.findById(refreshToken.getMemberId())
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        String newAccessToken = jwtTokenProvider.createToken(member.getLoginId(), member.getMemberId(), null);

        long expiresIn = 900;  // 15분

        log.debug("[AUTH] Access token refreshed - memberId: {}", refreshToken.getMemberId());

        return new TokenRefreshResponse(
            newAccessToken,
            refreshToken.getTokenValue(),
            expiresIn
        );
    }

    /**
     * 토큰 블랙리스트 등록 (회원 탈퇴 등에서 사용)
     *
     * @param token 블랙리스트에 등록할 토큰
     */
    public void blacklistToken(String token) {
        if (isValidTokenFormat(token)) {
            tokenBlacklistService.blacklistToken(token);
        }
    }

    /**
     * 토큰 형식이 유효한지 확인합니다.
     *
     * @param token 확인할 토큰
     * @return 유효하면 true
     */
    public boolean isValidTokenFormat(String token) {
        return token != null && !token.isBlank();
    }
}
