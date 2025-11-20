package com.Hamalog.dto.auth.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "토큰 갱신 응답 데이터")
public record TokenRefreshResponse(
    @JsonProperty("access_token")
    @Schema(description = "새로운 JWT 액세스 토큰")
    String accessToken,

    @JsonProperty("refresh_token")
    @Schema(description = "새로운 리프레시 토큰 (Token Rotation)")
    String refreshToken,

    @JsonProperty("expires_in")
    @Schema(description = "액세스 토큰 만료 시간 (초)")
    long expiresIn
) {}

