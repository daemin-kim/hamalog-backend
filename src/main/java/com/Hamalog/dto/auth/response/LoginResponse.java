package com.Hamalog.dto.auth.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 응답 데이터")
public record LoginResponse(
        @JsonProperty("access_token")
        @Schema(description = "JWT 액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0dXNlciIsImV4cCI6MTY5ODM2MDAwMH0.signature")
        String token,

        @JsonProperty("refresh_token")
        @Schema(description = "리프레시 토큰 (선택사항)", example = "abcdef1234567890...")
        String refreshToken,

        @JsonProperty("expires_in")
        @Schema(description = "액세스 토큰 만료 시간 (초)", example = "900")
        long expiresIn,

        @JsonProperty("token_type")
        @Schema(description = "토큰 타입", example = "Bearer")
        String tokenType,

        @JsonProperty("member_id")
        @Schema(description = "회원 ID", example = "1")
        Long memberId
) {
    public LoginResponse(String token, String refreshToken, long expiresIn, Long memberId) {
        this(token, refreshToken, expiresIn, "Bearer", memberId);
    }

    public LoginResponse(String token, String refreshToken, long expiresIn) {
        this(token, refreshToken, expiresIn, "Bearer", null);
    }

    // 하위 호환성을 위한 생성자
    public LoginResponse(String token) {
        this(token, null, 3600, "Bearer", null);
    }
}
