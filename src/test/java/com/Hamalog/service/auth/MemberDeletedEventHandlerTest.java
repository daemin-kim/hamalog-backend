package com.Hamalog.service.auth;

import com.Hamalog.domain.events.member.MemberDeletedEvent;
import com.Hamalog.security.jwt.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberDeletedEventHandler Tests")
class MemberDeletedEventHandlerTest {

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private MemberDeletedEventHandler memberDeletedEventHandler;

    private MemberDeletedEvent mockEvent;

    @BeforeEach
    void setUp() {
        // Create a mock event that we can customize for each test
        mockEvent = mock(MemberDeletedEvent.class);
    }

    @Test
    @DisplayName("Should successfully handle member deletion with valid token")
    void handleMemberDeleted_ValidToken_Success() {
        // given
        String loginId = "test@example.com";
        Long memberId = 1L;
        String token = "valid.jwt.token";
        
        given(mockEvent.getLoginId()).willReturn(loginId);
        given(mockEvent.getMemberId()).willReturn(memberId);
        given(mockEvent.getToken()).willReturn(token);
        
        doNothing().when(tokenBlacklistService).blacklistToken(token);

        // when
        memberDeletedEventHandler.handleMemberDeleted(mockEvent);

        // then
        verify(tokenBlacklistService).blacklistToken(token);
        verify(mockEvent, atLeastOnce()).getLoginId();
        verify(mockEvent, atLeastOnce()).getMemberId();
        verify(mockEvent, atLeastOnce()).getToken();
    }

    @Test
    @DisplayName("Should handle member deletion when token is null")
    void handleMemberDeleted_NullToken_SkipsTokenBlacklisting() {
        // given
        String loginId = "test@example.com";
        Long memberId = 1L;
        String token = null;
        
        given(mockEvent.getLoginId()).willReturn(loginId);
        given(mockEvent.getMemberId()).willReturn(memberId);
        given(mockEvent.getToken()).willReturn(token);

        // when
        memberDeletedEventHandler.handleMemberDeleted(mockEvent);

        // then
        verify(tokenBlacklistService, never()).blacklistToken(anyString());
        verify(mockEvent, atLeastOnce()).getLoginId();
        verify(mockEvent, atLeastOnce()).getMemberId();
        verify(mockEvent, atLeastOnce()).getToken();
    }

    @Test
    @DisplayName("Should handle member deletion when token is empty")
    void handleMemberDeleted_EmptyToken_SkipsTokenBlacklisting() {
        // given
        String loginId = "test@example.com";
        Long memberId = 1L;
        String token = "";
        
        given(mockEvent.getLoginId()).willReturn(loginId);
        given(mockEvent.getMemberId()).willReturn(memberId);
        given(mockEvent.getToken()).willReturn(token);

        // when
        memberDeletedEventHandler.handleMemberDeleted(mockEvent);

        // then
        verify(tokenBlacklistService, never()).blacklistToken(anyString());
        verify(mockEvent, atLeastOnce()).getLoginId();
        verify(mockEvent, atLeastOnce()).getMemberId();
        verify(mockEvent, atLeastOnce()).getToken();
    }

    @Test
    @DisplayName("Should handle member deletion when token is blank")
    void handleMemberDeleted_BlankToken_SkipsTokenBlacklisting() {
        // given
        String loginId = "test@example.com";
        Long memberId = 1L;
        String token = "   ";
        
        given(mockEvent.getLoginId()).willReturn(loginId);
        given(mockEvent.getMemberId()).willReturn(memberId);
        given(mockEvent.getToken()).willReturn(token);

        // when
        memberDeletedEventHandler.handleMemberDeleted(mockEvent);

        // then
        verify(tokenBlacklistService, never()).blacklistToken(anyString());
        verify(mockEvent, atLeastOnce()).getLoginId();
        verify(mockEvent, atLeastOnce()).getMemberId();
        verify(mockEvent, atLeastOnce()).getToken();
    }

    @Test
    @DisplayName("Should handle exception during token blacklisting gracefully")
    void handleMemberDeleted_TokenBlacklistingFails_ContinuesWithoutThrowing() {
        // given
        String loginId = "test@example.com";
        Long memberId = 1L;
        String token = "valid.jwt.token";
        
        given(mockEvent.getLoginId()).willReturn(loginId);
        given(mockEvent.getMemberId()).willReturn(memberId);
        given(mockEvent.getToken()).willReturn(token);
        
        doThrow(new RuntimeException("Redis connection failed"))
                .when(tokenBlacklistService).blacklistToken(token);

        // when & then - should not throw exception
        memberDeletedEventHandler.handleMemberDeleted(mockEvent);

        // then
        verify(tokenBlacklistService).blacklistToken(token);
        verify(mockEvent, atLeastOnce()).getLoginId();
        verify(mockEvent, atLeastOnce()).getMemberId();
        verify(mockEvent, atLeastOnce()).getToken();
    }

    @Test
    @DisplayName("Should handle exception during event processing gracefully")
    void handleMemberDeleted_EventProcessingFails_ContinuesWithoutThrowing() {
        // given
        String loginId = "test@example.com";
        Long memberId = 1L;
        
        given(mockEvent.getLoginId()).willReturn(loginId);
        given(mockEvent.getMemberId()).willReturn(memberId);
        given(mockEvent.getToken()).willThrow(new RuntimeException("Event data access failed"));

        // when & then - should not throw exception
        memberDeletedEventHandler.handleMemberDeleted(mockEvent);

        // then
        verify(tokenBlacklistService, never()).blacklistToken(anyString());
        verify(mockEvent, atLeastOnce()).getLoginId();
        verify(mockEvent, atLeastOnce()).getMemberId();
        verify(mockEvent, atLeastOnce()).getToken();
    }

    @Test
    @DisplayName("Should handle member deletion with valid token containing special characters")
    void handleMemberDeleted_TokenWithSpecialCharacters_Success() {
        // given
        String loginId = "test+special@example.com";
        Long memberId = 123L;
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token-with_special.chars";
        
        given(mockEvent.getLoginId()).willReturn(loginId);
        given(mockEvent.getMemberId()).willReturn(memberId);
        given(mockEvent.getToken()).willReturn(token);
        
        doNothing().when(tokenBlacklistService).blacklistToken(token);

        // when
        memberDeletedEventHandler.handleMemberDeleted(mockEvent);

        // then
        verify(tokenBlacklistService).blacklistToken(token);
        verify(mockEvent, atLeastOnce()).getLoginId();
        verify(mockEvent, atLeastOnce()).getMemberId();
        verify(mockEvent, atLeastOnce()).getToken();
    }

    @Test
    @DisplayName("Should handle member deletion with minimal event data")
    void handleMemberDeleted_MinimalEventData_Success() {
        // given
        String loginId = null; // nullable field
        Long memberId = 1L;
        String token = "valid.token";
        
        given(mockEvent.getLoginId()).willReturn(loginId);
        given(mockEvent.getMemberId()).willReturn(memberId);
        given(mockEvent.getToken()).willReturn(token);
        
        doNothing().when(tokenBlacklistService).blacklistToken(token);

        // when
        memberDeletedEventHandler.handleMemberDeleted(mockEvent);

        // then
        verify(tokenBlacklistService).blacklistToken(token);
        verify(mockEvent, atLeastOnce()).getLoginId();
        verify(mockEvent, atLeastOnce()).getMemberId();
        verify(mockEvent, atLeastOnce()).getToken();
    }
}