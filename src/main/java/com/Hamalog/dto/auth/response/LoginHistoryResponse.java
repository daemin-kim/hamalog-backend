package com.Hamalog.dto.auth.response;

import com.Hamalog.domain.security.LoginHistory;
import java.time.LocalDateTime;

public record LoginHistoryResponse(
    Long loginHistoryId,
    LocalDateTime loginTime,
    String ipAddress,
    String userAgent,
    String deviceType,
    String loginStatus,
    Boolean isActive,
    LocalDateTime logoutTime
) {
    public static LoginHistoryResponse from(LoginHistory loginHistory) {
        return new LoginHistoryResponse(
            loginHistory.getLoginHistoryId(),
            loginHistory.getLoginTime(),
            loginHistory.getIpAddress(),
            loginHistory.getUserAgent(),
            loginHistory.getDeviceType() != null ? loginHistory.getDeviceType().name() : null,
            loginHistory.getLoginStatus().name(),
            loginHistory.getIsActive(),
            loginHistory.getLogoutTime()
        );
    }
}
