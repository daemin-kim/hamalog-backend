package com.Hamalog.dto.notification.response;

import java.util.List;

/**
 * FCM 디바이스 토큰 목록 응답 DTO
 */
public record FcmDeviceTokenListResponse(
        Long memberId,
        int totalDevices,
        int activeDevices,
        List<FcmDeviceTokenResponse> devices
) {
    public static FcmDeviceTokenListResponse of(Long memberId, List<FcmDeviceTokenResponse> devices) {
        int activeCount = (int) devices.stream()
                .filter(FcmDeviceTokenResponse::isActive)
                .count();

        return new FcmDeviceTokenListResponse(
                memberId,
                devices.size(),
                activeCount,
                devices
        );
    }
}
