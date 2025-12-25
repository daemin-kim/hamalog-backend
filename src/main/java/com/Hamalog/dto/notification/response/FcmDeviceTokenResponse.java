package com.Hamalog.dto.notification.response;

import com.Hamalog.domain.notification.DeviceType;
import com.Hamalog.domain.notification.FcmDeviceToken;
import java.time.LocalDateTime;

/**
 * FCM 디바이스 토큰 응답 DTO
 */
public record FcmDeviceTokenResponse(
        Long fcmDeviceTokenId,
        String tokenPrefix,  // 보안을 위해 토큰 앞 20자만 노출
        DeviceType deviceType,
        String deviceName,
        boolean isActive,
        LocalDateTime lastUsedAt,
        LocalDateTime createdAt
) {
    /**
     * Entity → DTO 변환 팩토리 메서드
     */
    public static FcmDeviceTokenResponse from(FcmDeviceToken entity) {
        String tokenPrefix = entity.getToken().length() > 20
                ? entity.getToken().substring(0, 20) + "..."
                : entity.getToken();

        return new FcmDeviceTokenResponse(
                entity.getFcmDeviceTokenId(),
                tokenPrefix,
                entity.getDeviceType(),
                entity.getDeviceName(),
                entity.isActive(),
                entity.getLastUsedAt(),
                entity.getCreatedAt()
        );
    }
}
