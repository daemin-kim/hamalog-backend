package com.Hamalog.dto.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Schema(description = "회원 프로필 수정 요청")
public record ProfileUpdateRequest(
        @Size(max = 15, message = "{member.name.size}")
        @Schema(description = "이름 (최대 15자)", example = "홍길동")
        String name,

        @Pattern(regexp = "^[가-힣a-zA-Z]{1,10}$", message = "{member.nickname.pattern}")
        @Size(max = 10, message = "{member.nickname.size}")
        @Schema(description = "닉네임 (한글/영문 1-10자)", example = "길동이")
        String nickName,

        @Pattern(regexp = "^010\\d{8}$", message = "{member.phoneNumber.pattern}")
        @Schema(description = "전화번호 (010XXXXXXXX 형식)", example = "01012345678")
        String phoneNumber,

        @Schema(description = "생년월일", example = "1990-01-01")
        LocalDate birth
) {
    /**
     * 수정할 필드가 하나도 없는지 확인
     */
    public boolean hasNoUpdates() {
        return name == null && nickName == null && phoneNumber == null && birth == null;
    }
}
