package com.Hamalog.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.Hamalog.domain.member.Member;
import com.Hamalog.domain.security.RefreshToken;
import com.Hamalog.dto.auth.response.LoginResponse;
import com.Hamalog.dto.auth.response.TokenRefreshResponse;
import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.repository.member.MemberRepository;
import com.Hamalog.security.jwt.JwtTokenProvider;
import com.Hamalog.security.jwt.TokenBlacklistService;
import com.Hamalog.service.security.RefreshTokenService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService 테스트")
class AuthenticationServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthenticationService authenticationService;

    private Member testMember;
    private RefreshToken testRefreshToken;

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
            .memberId(1L)
            .loginId("test@example.com")
            .password("encodedPassword")
            .name("테스트유저")
            .build();

        testRefreshToken = RefreshToken.builder()
            .id(1L)
            .member(testMember)
            .tokenValue("refresh-token-value")
            .createdAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusDays(7))
            .rotatedAt(LocalDateTime.now())
            .build();
    }

    @Nested
    @DisplayName("로그인 테스트")
    class AuthenticateAndGenerateTokenTest {

        @Test
        @DisplayName("로그인 성공 - JWT 토큰과 RefreshToken이 반환된다")
        void authenticateAndGenerateToken_Success() {
            // given
            String loginId = "test@example.com";
            String password = "password123";
            String expectedToken = "jwt-access-token";

            given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .willReturn(authentication);
            given(authentication.getName()).willReturn(loginId);
            given(memberRepository.findByLoginId(loginId))
                .willReturn(Optional.of(testMember));
            given(jwtTokenProvider.createToken(loginId, testMember.getMemberId(), null))
                .willReturn(expectedToken);
            given(refreshTokenService.createRefreshToken(testMember.getMemberId()))
                .willReturn(testRefreshToken);

            // when
            LoginResponse response = authenticationService.authenticateAndGenerateToken(loginId, password);

            // then
            assertThat(response.token()).isEqualTo(expectedToken);
            assertThat(response.refreshToken()).isEqualTo(testRefreshToken.getTokenValue());
            assertThat(response.expiresIn()).isEqualTo(900L);

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(jwtTokenProvider).createToken(loginId, testMember.getMemberId(), null);
            verify(refreshTokenService).createRefreshToken(testMember.getMemberId());
        }

        @Test
        @DisplayName("로그인 실패 - 잘못된 인증 정보로 예외 발생")
        void authenticateAndGenerateToken_BadCredentials_ThrowsException() {
            // given
            String loginId = "test@example.com";
            String password = "wrongPassword";

            given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .willThrow(new BadCredentialsException("Bad credentials"));

            // when & then
            assertThatThrownBy(() -> authenticationService.authenticateAndGenerateToken(loginId, password))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Bad credentials");

            verify(jwtTokenProvider, never()).createToken(anyString(), anyLong(), any());
            verify(refreshTokenService, never()).createRefreshToken(anyLong());
        }

        @Test
        @DisplayName("로그인 실패 - 회원을 찾을 수 없음")
        void authenticateAndGenerateToken_MemberNotFound_ThrowsException() {
            // given
            String loginId = "nonexistent@example.com";
            String password = "password123";

            given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .willReturn(authentication);
            given(memberRepository.findByLoginId(loginId))
                .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authenticationService.authenticateAndGenerateToken(loginId, password))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MEMBER_NOT_FOUND.getMessage());

            verify(jwtTokenProvider, never()).createToken(anyString(), anyLong(), any());
        }
    }

    @Nested
    @DisplayName("로그아웃 테스트")
    class LogoutUserTest {

        @Test
        @DisplayName("로그아웃 성공 - 유효한 토큰이 블랙리스트에 등록된다")
        void logoutUser_ValidToken_Success() {
            // given
            String token = "valid-jwt-token";

            // when
            authenticationService.logoutUser(token);

            // then
            verify(tokenBlacklistService).blacklistToken(token);
        }

        @Test
        @DisplayName("로그아웃 - null 토큰은 블랙리스트에 등록되지 않는다")
        void logoutUser_NullToken_NotBlacklisted() {
            // given
            String token = null;

            // when
            authenticationService.logoutUser(token);

            // then
            verify(tokenBlacklistService, never()).blacklistToken(anyString());
        }

        @Test
        @DisplayName("로그아웃 - 빈 토큰은 블랙리스트에 등록되지 않는다")
        void logoutUser_BlankToken_NotBlacklisted() {
            // given
            String token = "   ";

            // when
            authenticationService.logoutUser(token);

            // then
            verify(tokenBlacklistService, never()).blacklistToken(anyString());
        }

        @Test
        @DisplayName("로그아웃 - 빈 문자열 토큰은 블랙리스트에 등록되지 않는다")
        void logoutUser_EmptyToken_NotBlacklisted() {
            // given
            String token = "";

            // when
            authenticationService.logoutUser(token);

            // then
            verify(tokenBlacklistService, never()).blacklistToken(anyString());
        }
    }

    @Nested
    @DisplayName("토큰 갱신 테스트")
    class RefreshAccessTokenTest {

        @Test
        @DisplayName("토큰 갱신 성공 - 새 AccessToken과 RefreshToken이 반환된다")
        void refreshAccessToken_Success() {
            // given
            String oldRefreshTokenValue = "old-refresh-token";
            String newAccessToken = "new-access-token";

            RefreshToken rotatedToken = RefreshToken.builder()
                .id(1L)
                .member(testMember)
                .tokenValue("new-refresh-token")
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .rotatedAt(LocalDateTime.now())
                .build();

            given(refreshTokenService.rotateToken(oldRefreshTokenValue))
                .willReturn(rotatedToken);
            given(memberRepository.findById(1L))
                .willReturn(Optional.of(testMember));
            given(jwtTokenProvider.createToken(testMember.getLoginId(), testMember.getMemberId(), null))
                .willReturn(newAccessToken);

            // when
            TokenRefreshResponse response = authenticationService.refreshAccessToken(oldRefreshTokenValue);

            // then
            assertThat(response.accessToken()).isEqualTo(newAccessToken);
            assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
            assertThat(response.expiresIn()).isEqualTo(900L);

            verify(refreshTokenService).rotateToken(oldRefreshTokenValue);
            verify(memberRepository).findById(1L);
            verify(jwtTokenProvider).createToken(testMember.getLoginId(), testMember.getMemberId(), null);
        }

        @Test
        @DisplayName("토큰 갱신 실패 - 회원을 찾을 수 없음")
        void refreshAccessToken_MemberNotFound_ThrowsException() {
            // given
            String refreshTokenValue = "refresh-token";

            Member nonExistentMember = Member.builder()
                .memberId(999L)
                .loginId("nonexistent@example.com")
                .password("password")
                .name("없는유저")
                .build();

            RefreshToken rotatedToken = RefreshToken.builder()
                .id(1L)
                .member(nonExistentMember)  // 존재하지 않는 회원
                .tokenValue("new-refresh-token")
                .build();

            given(refreshTokenService.rotateToken(refreshTokenValue))
                .willReturn(rotatedToken);
            given(memberRepository.findById(999L))
                .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authenticationService.refreshAccessToken(refreshTokenValue))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MEMBER_NOT_FOUND.getMessage());

            verify(jwtTokenProvider, never()).createToken(anyString(), anyLong(), any());
        }
    }

    @Nested
    @DisplayName("토큰 블랙리스트 테스트")
    class BlacklistTokenTest {

        @Test
        @DisplayName("유효한 토큰을 블랙리스트에 등록한다")
        void blacklistToken_ValidToken_Success() {
            // given
            String token = "valid-token";

            // when
            authenticationService.blacklistToken(token);

            // then
            verify(tokenBlacklistService).blacklistToken(token);
        }

        @Test
        @DisplayName("null 토큰은 블랙리스트에 등록되지 않는다")
        void blacklistToken_NullToken_NotBlacklisted() {
            // given
            String token = null;

            // when
            authenticationService.blacklistToken(token);

            // then
            verify(tokenBlacklistService, never()).blacklistToken(anyString());
        }
    }

    @Nested
    @DisplayName("토큰 형식 검증 테스트")
    class IsValidTokenFormatTest {

        @Test
        @DisplayName("유효한 토큰 형식 - true 반환")
        void isValidTokenFormat_ValidToken_ReturnsTrue() {
            // given
            String token = "valid-jwt-token";

            // when
            boolean result = authenticationService.isValidTokenFormat(token);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("null 토큰 - false 반환")
        void isValidTokenFormat_NullToken_ReturnsFalse() {
            // given
            String token = null;

            // when
            boolean result = authenticationService.isValidTokenFormat(token);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("빈 토큰 - false 반환")
        void isValidTokenFormat_BlankToken_ReturnsFalse() {
            // given
            String token = "   ";

            // when
            boolean result = authenticationService.isValidTokenFormat(token);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("빈 문자열 토큰 - false 반환")
        void isValidTokenFormat_EmptyToken_ReturnsFalse() {
            // given
            String token = "";

            // when
            boolean result = authenticationService.isValidTokenFormat(token);

            // then
            assertThat(result).isFalse();
        }
    }
}
