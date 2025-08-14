package com.Hamalog.dto.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Schema(description = "회원가입 요청 데이터")
public record SignupRequest(
        @Schema(description = "로그인 ID (4~20자)", example = "testuser", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(min = 4, max = 20) String loginId,
        
        @Schema(description = "비밀번호 (6~100자)", example = "password123", requiredMode = Schema.RequiredMode.REQUIRED, format = "password")
        @NotBlank @Size(min = 6, max = 100) String password,
        
        @Schema(description = "사용자 이름 (1~50자)", example = "홍길동", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(min = 1, max = 50) String name,
        
        @Schema(description = "전화번호 (10~13자 숫자)", example = "01012345678", requiredMode = Schema.RequiredMode.REQUIRED, pattern = "^\\d{10,13}$")
        @NotBlank @Pattern(regexp = "^\\d{10,13}$") String phoneNumber,
        
        @Schema(description = "생년월일", example = "1990-01-01", requiredMode = Schema.RequiredMode.REQUIRED, type = "string", format = "date")
        @NotNull LocalDate birth
) {}
