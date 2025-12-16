package com.Hamalog.dto.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "로그인 요청 데이터")
public record LoginRequest(
        @Schema(description = "사용자 로그인 ID", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{auth.loginId.notBlank}")
        @Size(max = 100, message = "{auth.loginId.size}")
        String loginId,
        
        @Schema(description = "사용자 비밀번호", example = "********", requiredMode = Schema.RequiredMode.REQUIRED, format = "password")
        @NotBlank(message = "{auth.password.notBlank}")
        @Size(min = 6, max = 100, message = "{auth.password.size}")
        String password
) {}
