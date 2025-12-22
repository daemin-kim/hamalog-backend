package com.Hamalog.dto.auth.response;

import com.Hamalog.domain.member.Member;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "회원 프로필 응답")
public record MemberProfileResponse(
        @Schema(description = "회원 ID", example = "1")
        Long memberId,

        @Schema(description = "로그인 ID (이메일)", example = "user@example.com")
        String loginId,

        @Schema(description = "이름", example = "홍길동")
        String name,

        @Schema(description = "닉네임", example = "길동이")
        String nickName,

        @Schema(description = "전화번호", example = "01012345678")
        String phoneNumber,

        @Schema(description = "생년월일", example = "1990-01-01")
        LocalDate birth,

        @Schema(description = "가입일시", example = "2025-01-01T12:00:00")
        LocalDateTime createdAt
) {
    public static MemberProfileResponse from(Member member) {
        return new MemberProfileResponse(
                member.getMemberId(),
                member.getLoginId(),
                member.getName(),
                member.getNickName(),
                member.getPhoneNumber(),
                member.getBirth(),
                member.getCreatedAt()
        );
    }
}

