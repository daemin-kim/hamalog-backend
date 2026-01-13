package com.Hamalog.service.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.Hamalog.domain.member.Member;
import com.Hamalog.domain.security.RefreshToken;
import com.Hamalog.exception.CustomException;
import com.Hamalog.repository.member.MemberRepository;
import com.Hamalog.repository.security.RefreshTokenRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("Refresh Token Service Tests")
class RefreshTokenServiceTest {

    private RefreshTokenService service;

    @Mock
    private RefreshTokenRepository repository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private Member mockMember;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new RefreshTokenService(repository, memberRepository);
        // refreshTokenExpiryMs 값을 604800000ms (7일)로 설정
        ReflectionTestUtils.setField(service, "refreshTokenExpiryMs", 604800000L);

        // Mock Member 설정
        when(mockMember.getMemberId()).thenReturn(1L);
    }

    @Test
    @DisplayName("RefreshToken 생성 성공")
    void testCreateRefreshToken() {
        Long memberId = 1L;

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(mockMember));
        when(repository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            token.setId(1L);
            return token;
        });

        RefreshToken refreshToken = service.createRefreshToken(memberId);

        assertNotNull(refreshToken);
        assertEquals(memberId, refreshToken.getMemberId());
        assertFalse(refreshToken.isRevoked());
        assertTrue(refreshToken.isValid());

        verify(repository).revokeAllByMemberId(memberId);  // 기존 토큰 폐지
    }

    @Test
    @DisplayName("RefreshToken 회전 성공 (Token Rotation)")
    void testRotateToken() {
        Long memberId = 1L;
        String oldTokenValue = "old-token-value";

        RefreshToken oldToken = RefreshToken.builder()
            .id(1L)
            .member(mockMember)
            .tokenValue(oldTokenValue)
            .createdAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusDays(7))
            .rotatedAt(LocalDateTime.now())
            .revoked(false)
            .build();

        when(repository.findByTokenValue(oldTokenValue))
            .thenReturn(Optional.of(oldToken));

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(mockMember));
        when(repository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            if (token.getId() == null) {
                token.setId(2L);
            }
            return token;
        });

        RefreshToken newToken = service.rotateToken(oldTokenValue);

        assertNotNull(newToken);
        assertTrue(oldToken.isRevoked());  // 기존 토큰 폐지됨
        assertNotEquals(oldToken.getTokenValue(), newToken.getTokenValue());
        verify(repository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("유효하지 않은 RefreshToken 회전 실패")
    void testRotateInvalidToken() {
        when(repository.findByTokenValue("invalid-token"))
            .thenReturn(Optional.empty());

        assertThrows(CustomException.class, () -> {
            service.rotateToken("invalid-token");
        });
    }

    @Test
    @DisplayName("만료된 RefreshToken 회전 실패")
    void testRotateExpiredToken() {
        RefreshToken expiredToken = RefreshToken.builder()
            .member(mockMember)
            .tokenValue("expired-token")
            .createdAt(LocalDateTime.now().minusDays(8))
            .expiresAt(LocalDateTime.now().minusHours(1))
            .rotatedAt(LocalDateTime.now().minusDays(8))
            .revoked(false)
            .build();

        when(repository.findByTokenValue("expired-token"))
            .thenReturn(Optional.of(expiredToken));

        assertThrows(CustomException.class, () -> {
            service.rotateToken("expired-token");
        });
    }

    @Test
    @DisplayName("사용자의 모든 RefreshToken 폐지")
    void testRevokeAllByMemberId() {
        Long memberId = 1L;

        service.revokeAllByMemberId(memberId);

        verify(repository).revokeAllByMemberId(memberId);
    }
}
