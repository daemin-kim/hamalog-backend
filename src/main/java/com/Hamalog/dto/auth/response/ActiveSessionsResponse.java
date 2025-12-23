package com.Hamalog.dto.auth.response;

import java.time.LocalDateTime;
import java.util.List;

public record ActiveSessionsResponse(
    int totalActiveSessions,
    List<SessionInfo> sessions
) {
    public record SessionInfo(
        Long loginHistoryId,
        String sessionId,
        LocalDateTime loginTime,
        String ipAddress,
        String deviceType,
        String userAgent,
        boolean isCurrentSession
    ) {}

    public static ActiveSessionsResponse of(int total, List<SessionInfo> sessions) {
        return new ActiveSessionsResponse(total, sessions);
    }
}
