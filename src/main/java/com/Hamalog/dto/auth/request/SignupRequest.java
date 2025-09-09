package com.Hamalog.dto.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Schema(description = "회원가입 요청 데이터")
public record SignupRequest(
        @Schema(description = "로그인 ID (이메일 형식)", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Email(message = "{member.loginId.email}") String loginId,
        
        @Schema(description = "비밀번호 (6~30자)", example = "password123", requiredMode = Schema.RequiredMode.REQUIRED, format = "password")
        @NotBlank @Size(min = 6, max = 30) String password,
        
        @Schema(description = "사용자 이름 (1~15자)", example = "홍길동", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(min = 1, max = 15) String name,
        
        @Schema(description = "닉네임 (한글/영어 1~10자)", example = "홍길동", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Pattern(regexp = "^[가-힣a-zA-Z]{1,10}$", message = "{member.nickname.pattern}") @Size(max = 10, message = "{member.nickname.size}") String nickName,
        
        @Schema(description = "전화번호 (010으로 시작하는 11자리 숫자)", example = "01012345678", requiredMode = Schema.RequiredMode.REQUIRED, pattern = "^010\\d{8}$")
        @NotBlank @Pattern(regexp = "^010\\d{8}$", message = "{member.phoneNumber.pattern}") String phoneNumber,
        
        @Schema(description = "생년월일", example = "1990-01-01", requiredMode = Schema.RequiredMode.REQUIRED, type = "string", format = "date")
        @NotNull 
        @Past(message = "{member.birth.past}")
        LocalDate birth
) {}
