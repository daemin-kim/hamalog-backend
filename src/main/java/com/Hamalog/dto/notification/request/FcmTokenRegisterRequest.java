package com.Hamalog.dto.notification.request;

import com.Hamalog.domain.notification.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * FCM 토큰 등록 요청 DTO
 */
public record FcmTokenRegisterRequest(
        @NotBlank(message = "FCM 토큰은 필수입니다")
        @Size(max = 500, message = "FCM 토큰은 500자 이내여야 합니다")
        String token,

        @NotNull(message = "디바이스 타입은 필수입니다")
        DeviceType deviceType,

        @Size(max = 100, message = "디바이스 이름은 100자 이내여야 합니다")
        String deviceName
) {}
