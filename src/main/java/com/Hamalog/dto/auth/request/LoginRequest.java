package com.Hamalog.dto.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "로그인 요청 데이터")
public record LoginRequest(
        @Schema(description = "사용자 로그인 ID", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "로그인 ID는 필수입니다")
        @Size(max = 100, message = "로그인 ID는 100자를 초과할 수 없습니다")
        String loginId,
        
        @Schema(description = "사용자 비밀번호", example = "********", requiredMode = Schema.RequiredMode.REQUIRED, format = "password")
        @NotBlank(message = "비밀번호는 필수입니다")
        @Size(min = 6, max = 100, message = "비밀번호는 6자 이상 100자 이하여야 합니다")
        String password
) {}
