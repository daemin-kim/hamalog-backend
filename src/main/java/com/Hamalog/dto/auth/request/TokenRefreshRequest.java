package com.Hamalog.dto.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "토큰 갱신 요청 데이터")
public record TokenRefreshRequest(
    @NotBlank(message = "{auth.refreshToken.notBlank}")
    @Schema(description = "리프레시 토큰", requiredMode = Schema.RequiredMode.REQUIRED)
    String refreshToken
) {}
