package com.Hamalog.dto.auth.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 응답 데이터")
public record LoginResponse(
        @Schema(description = "JWT 액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0dXNlciIsImV4cCI6MTY5ODM2MDAwMH0.signature")
        String token
) {}
