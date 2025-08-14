package com.Hamalog.dto.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "로그인 요청 데이터")
public record LoginRequest(
        @Schema(description = "사용자 로그인 ID", example = "testuser", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "로그인 ID는 필수입니다")
        String loginId,
        
        @Schema(description = "사용자 비밀번호", example = "password123", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "비밀번호는 필수입니다")
        String password
) {}
