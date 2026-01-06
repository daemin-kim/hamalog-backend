package com.Hamalog.service.auth;

import com.Hamalog.domain.member.Member;
import com.Hamalog.domain.security.LoginHistory;
import com.Hamalog.domain.security.LoginHistory.DeviceType;
import com.Hamalog.domain.security.LoginHistory.LoginStatus;
import com.Hamalog.dto.auth.response.ActiveSessionsResponse;
import com.Hamalog.dto.auth.response.ActiveSessionsResponse.SessionInfo;
import com.Hamalog.dto.auth.response.LoginHistoryListResponse;
import com.Hamalog.dto.auth.response.LoginHistoryResponse;
import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.repository.member.MemberRepository;
import com.Hamalog.repository.security.LoginHistoryRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class LoginHistoryService {

    private final LoginHistoryRepository loginHistoryRepository;
    private final MemberRepository memberRepository;

    /**
     * 로그인 성공 기록
     */
    @Transactional(rollbackFor = {Exception.class})
    public String recordLoginSuccess(Long memberId, String ipAddress, String userAgent) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        String sessionId = generateSessionId();
        DeviceType deviceType = parseDeviceType(userAgent);

        LoginHistory loginHistory = LoginHistory.builder()
                .member(member)
                .ipAddress(ipAddress)
                .userAgent(truncateUserAgent(userAgent))
                .deviceType(deviceType)
                .loginStatus(LoginStatus.SUCCESS)
                .sessionId(sessionId)
                .build();

        loginHistoryRepository.save(loginHistory);
        log.info("Login success recorded for member: {}, sessionId: {}", memberId, sessionId);

        return sessionId;
    }

    /**
     * 로그인 실패 기록
     */
    @Transactional(rollbackFor = {Exception.class})
    public void recordLoginFailure(Long memberId, String ipAddress, String userAgent, String failureReason) {
        Member member = memberRepository.findById(memberId).orElse(null);
        if (member == null) {
            log.warn("Login failure for unknown member, IP: {}", ipAddress);
            return;
        }

        DeviceType deviceType = parseDeviceType(userAgent);

        LoginHistory loginHistory = LoginHistory.builder()
                .member(member)
                .ipAddress(ipAddress)
                .userAgent(truncateUserAgent(userAgent))
                .deviceType(deviceType)
                .loginStatus(LoginStatus.FAILED)
                .failureReason(failureReason)
                .build();

        loginHistoryRepository.save(loginHistory);
        log.warn("Login failure recorded for member: {}, reason: {}", memberId, failureReason);
    }

    /**
     * 로그인 이력 조회 (페이지네이션)
     */
    public LoginHistoryListResponse getLoginHistory(Long memberId, int page, int size) {
        if (size > 100) size = 100;
        if (size < 1) size = 10;

        Pageable pageable = PageRequest.of(page, size);
        Page<LoginHistory> historyPage = loginHistoryRepository.findByMember_MemberIdOrderByLoginTimeDesc(memberId, pageable);

        Page<LoginHistoryResponse> responsePage = historyPage.map(LoginHistoryResponse::from);
        return LoginHistoryListResponse.from(responsePage);
    }

    /**
     * 활성 세션 조회
     */
    public ActiveSessionsResponse getActiveSessions(Long memberId, String currentSessionId) {
        List<LoginHistory> activeSessions = loginHistoryRepository.findActiveSessionsByMemberId(memberId);

        List<SessionInfo> sessionInfos = activeSessions.stream()
                .map(session -> new SessionInfo(
                        session.getLoginHistoryId(),
                        session.getSessionId(),
                        session.getLoginTime(),
                        session.getIpAddress(),
                        session.getDeviceType() != null ? session.getDeviceType().name() : "UNKNOWN",
                        session.getUserAgent(),
                        session.getSessionId() != null && session.getSessionId().equals(currentSessionId)
                ))
                .toList();

        return ActiveSessionsResponse.of(activeSessions.size(), sessionInfos);
    }

    /**
     * 특정 세션 종료
     */
    @Transactional(rollbackFor = {Exception.class})
    public void terminateSession(Long memberId, String sessionId) {
        LoginHistory session = loginHistoryRepository.findBySessionIdAndMember_MemberId(sessionId, memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN));

        session.deactivateSession();
        log.info("Session terminated: {} for member: {}", sessionId, memberId);
    }

    /**
     * 회원의 모든 세션 종료 (로그아웃 시)
     */
    @Transactional(rollbackFor = {Exception.class})
    public void terminateAllSessions(Long memberId) {
        int deactivated = loginHistoryRepository.deactivateAllSessionsByMemberId(memberId, LocalDateTime.now());
        log.info("All sessions terminated for member: {}, count: {}", memberId, deactivated);
    }

    /**
     * 세션 ID로 세션 종료 (다른 기기에서 로그아웃)
     */
    @Transactional(rollbackFor = {Exception.class})
    public void terminateSessionById(String sessionId) {
        int deactivated = loginHistoryRepository.deactivateSession(sessionId, LocalDateTime.now());
        if (deactivated > 0) {
            log.info("Session deactivated: {}", sessionId);
        }
    }

    private String generateSessionId() {
        return UUID.randomUUID().toString();
    }

    private DeviceType parseDeviceType(String userAgent) {
        if (userAgent == null) {
            return DeviceType.UNKNOWN;
        }

        String ua = userAgent.toLowerCase();
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) {
            return DeviceType.MOBILE;
        } else if (ua.contains("tablet") || ua.contains("ipad")) {
            return DeviceType.TABLET;
        } else if (ua.contains("windows") || ua.contains("macintosh") || ua.contains("linux")) {
            return DeviceType.DESKTOP;
        }
        return DeviceType.UNKNOWN;
    }

    private String truncateUserAgent(String userAgent) {
        if (userAgent == null) {
            return null;
        }
        return userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent;
    }
}
