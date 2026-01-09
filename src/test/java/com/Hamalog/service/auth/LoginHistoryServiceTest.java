package com.Hamalog.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.Hamalog.domain.member.Member;
import com.Hamalog.domain.security.LoginHistory;
import com.Hamalog.domain.security.LoginHistory.DeviceType;
import com.Hamalog.domain.security.LoginHistory.LoginStatus;
import com.Hamalog.dto.auth.response.ActiveSessionsResponse;
import com.Hamalog.dto.auth.response.LoginHistoryListResponse;
import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.repository.member.MemberRepository;
import com.Hamalog.repository.security.LoginHistoryRepository;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginHistoryService 테스트")
class LoginHistoryServiceTest {

    @Mock
    private LoginHistoryRepository loginHistoryRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private LoginHistoryService loginHistoryService;

    private Member mockMember;
    private LoginHistory mockLoginHistory;

    @BeforeEach
    void setUp() {
        // Mock Member 설정
        mockMember = mock(Member.class);
        lenient().when(mockMember.getMemberId()).thenReturn(1L);
        lenient().when(mockMember.getLoginId()).thenReturn("test@example.com");

        // Mock LoginHistory 설정
        mockLoginHistory = mock(LoginHistory.class);
        lenient().when(mockLoginHistory.getLoginHistoryId()).thenReturn(1L);
        lenient().when(mockLoginHistory.getMember()).thenReturn(mockMember);
        lenient().when(mockLoginHistory.getSessionId()).thenReturn("test-session-id");
        lenient().when(mockLoginHistory.getIpAddress()).thenReturn("127.0.0.1");
        lenient().when(mockLoginHistory.getUserAgent()).thenReturn("Mozilla/5.0");
        lenient().when(mockLoginHistory.getDeviceType()).thenReturn(DeviceType.DESKTOP);
        lenient().when(mockLoginHistory.getLoginStatus()).thenReturn(LoginStatus.SUCCESS);
        lenient().when(mockLoginHistory.getLoginTime()).thenReturn(LocalDateTime.now());
    }

    @Nested
    @DisplayName("로그인 성공 기록")
    class RecordLoginSuccess {

        @Test
        @DisplayName("성공: 로그인 성공 기록 저장")
        void success() {
            // given
            Long memberId = 1L;
            String ipAddress = "192.168.1.1";
            String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)";

            when(memberRepository.findById(memberId)).thenReturn(Optional.of(mockMember));
            when(loginHistoryRepository.save(any(LoginHistory.class))).thenReturn(mockLoginHistory);

            // when
            String sessionId = loginHistoryService.recordLoginSuccess(memberId, ipAddress, userAgent);

            // then
            assertThat(sessionId).isNotNull();
            verify(loginHistoryRepository).save(any(LoginHistory.class));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 회원")
        void fail_memberNotFound() {
            // given
            Long memberId = 999L;

            when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                loginHistoryService.recordLoginSuccess(memberId, "127.0.0.1", "Mozilla/5.0"))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("로그인 실패 기록")
    class RecordLoginFailure {

        @Test
        @DisplayName("성공: 로그인 실패 기록 저장")
        void success() {
            // given
            Long memberId = 1L;
            String ipAddress = "192.168.1.1";
            String userAgent = "Mozilla/5.0";
            String failureReason = "비밀번호 불일치";

            when(memberRepository.findById(memberId)).thenReturn(Optional.of(mockMember));

            // when
            loginHistoryService.recordLoginFailure(memberId, ipAddress, userAgent, failureReason);

            // then
            verify(loginHistoryRepository).save(any(LoginHistory.class));
        }

        @Test
        @DisplayName("경고: 존재하지 않는 회원은 기록 생략")
        void skip_unknownMember() {
            // given
            Long memberId = 999L;

            when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

            // when
            loginHistoryService.recordLoginFailure(memberId, "127.0.0.1", "Mozilla/5.0", "이유");

            // then
            verify(loginHistoryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("로그인 이력 조회")
    class GetLoginHistory {

        @Test
        @DisplayName("성공: 페이지네이션으로 이력 조회")
        void success() {
            // given
            Long memberId = 1L;
            int page = 0;
            int size = 10;

            Page<LoginHistory> historyPage = new PageImpl<>(
                    Arrays.asList(mockLoginHistory),
                    PageRequest.of(page, size),
                    1
            );

            when(loginHistoryRepository.findByMember_MemberIdOrderByLoginTimeDesc(eq(memberId), any()))
                    .thenReturn(historyPage);

            // when
            LoginHistoryListResponse result = loginHistoryService.getLoginHistory(memberId, page, size);

            // then
            assertThat(result.totalElements()).isEqualTo(1);
            assertThat(result.histories()).hasSize(1);
        }

        @Test
        @DisplayName("성공: 최대 사이즈 제한 적용")
        void success_sizeLimit() {
            // given
            Long memberId = 1L;
            int page = 0;
            int size = 200; // 100 초과

            Page<LoginHistory> historyPage = new PageImpl<>(
                    List.of(mockLoginHistory),
                    PageRequest.of(page, 100),
                    1
            );

            when(loginHistoryRepository.findByMember_MemberIdOrderByLoginTimeDesc(eq(memberId), any()))
                    .thenReturn(historyPage);

            // when
            LoginHistoryListResponse result = loginHistoryService.getLoginHistory(memberId, page, size);

            // then
            assertThat(result).isNotNull();
            verify(loginHistoryRepository).findByMember_MemberIdOrderByLoginTimeDesc(
                    eq(memberId),
                    eq(PageRequest.of(0, 100))
            );
        }
    }

    @Nested
    @DisplayName("활성 세션 조회")
    class GetActiveSessions {

        @Test
        @DisplayName("성공: 활성 세션 목록 조회")
        void success() {
            // given
            Long memberId = 1L;
            String currentSessionId = "test-session-id";

            when(loginHistoryRepository.findActiveSessionsByMemberId(memberId))
                    .thenReturn(Arrays.asList(mockLoginHistory));

            // when
            ActiveSessionsResponse result = loginHistoryService.getActiveSessions(memberId, currentSessionId);

            // then
            assertThat(result.totalActiveSessions()).isEqualTo(1);
            assertThat(result.sessions().get(0).isCurrentSession()).isTrue();
        }

        @Test
        @DisplayName("성공: 활성 세션 없음")
        void success_noActiveSessions() {
            // given
            Long memberId = 1L;

            when(loginHistoryRepository.findActiveSessionsByMemberId(memberId))
                    .thenReturn(List.of());

            // when
            ActiveSessionsResponse result = loginHistoryService.getActiveSessions(memberId, "any-session");

            // then
            assertThat(result.totalActiveSessions()).isEqualTo(0);
            assertThat(result.sessions()).isEmpty();
        }
    }

    @Nested
    @DisplayName("세션 종료")
    class TerminateSession {

        @Test
        @DisplayName("성공: 특정 세션 종료")
        void success_terminateSession() {
            // given
            Long memberId = 1L;
            String sessionId = "test-session-id";

            when(loginHistoryRepository.findBySessionIdAndMember_MemberId(sessionId, memberId))
                    .thenReturn(Optional.of(mockLoginHistory));

            // when
            loginHistoryService.terminateSession(memberId, sessionId);

            // then
            verify(mockLoginHistory).deactivateSession();
        }

        @Test
        @DisplayName("실패: 존재하지 않는 세션")
        void fail_sessionNotFound() {
            // given
            Long memberId = 1L;
            String sessionId = "invalid-session";

            when(loginHistoryRepository.findBySessionIdAndMember_MemberId(sessionId, memberId))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> loginHistoryService.terminateSession(memberId, sessionId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TOKEN);
        }

        @Test
        @DisplayName("성공: 모든 세션 종료")
        void success_terminateAllSessions() {
            // given
            Long memberId = 1L;

            when(loginHistoryRepository.deactivateAllSessionsByMemberId(eq(memberId), any()))
                    .thenReturn(3);

            // when
            loginHistoryService.terminateAllSessions(memberId);

            // then
            verify(loginHistoryRepository).deactivateAllSessionsByMemberId(eq(memberId), any());
        }

        @Test
        @DisplayName("성공: 세션 ID로 세션 종료")
        void success_terminateSessionById() {
            // given
            String sessionId = "test-session-id";

            when(loginHistoryRepository.deactivateSession(eq(sessionId), any())).thenReturn(1);

            // when
            loginHistoryService.terminateSessionById(sessionId);

            // then
            verify(loginHistoryRepository).deactivateSession(eq(sessionId), any());
        }
    }
}
