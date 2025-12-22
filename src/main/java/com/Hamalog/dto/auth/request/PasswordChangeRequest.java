package com.Hamalog.dto.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "비밀번호 변경 요청")
public record PasswordChangeRequest(
        @NotBlank(message = "{password.current.notBlank}")
        @Schema(description = "현재 비밀번호", example = "currentPassword123!", requiredMode = Schema.RequiredMode.REQUIRED)
        String currentPassword,

        @NotBlank(message = "{password.new.notBlank}")
        @Size(min = 8, max = 20, message = "{password.new.size}")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,20}$",
                message = "{password.new.pattern}"
        )
        @Schema(description = "새 비밀번호 (영문+숫자+특수문자 8-20자)", example = "newPassword123!", requiredMode = Schema.RequiredMode.REQUIRED)
        String newPassword,

        @NotBlank(message = "{password.confirm.notBlank}")
        @Schema(description = "새 비밀번호 확인", example = "newPassword123!", requiredMode = Schema.RequiredMode.REQUIRED)
        String confirmPassword
) {
    /**
     * 새 비밀번호와 확인 비밀번호가 일치하는지 확인
     */
    public boolean isPasswordConfirmed() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }
}

