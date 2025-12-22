package com.Hamalog.controller.auth;

import com.Hamalog.config.ApiVersion;
import com.Hamalog.dto.auth.request.PasswordChangeRequest;
import com.Hamalog.dto.auth.request.ProfileUpdateRequest;
import com.Hamalog.dto.auth.response.MemberProfileResponse;
import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.security.CustomUserDetails;
import com.Hamalog.service.auth.MemberProfileService;
import com.Hamalog.service.i18n.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Member Profile API", description = "회원 프로필 관련 API")
@RestController
@RequestMapping(ApiVersion.MEMBER)
@RequiredArgsConstructor
public class MemberController {

    private final MemberProfileService memberProfileService;
    private final MessageService messageService;

    @Operation(summary = "내 프로필 조회",
            description = "현재 로그인한 사용자의 프로필 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "프로필 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MemberProfileResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음", content = @Content)
    })
    @GetMapping("/profile")
    public ResponseEntity<MemberProfileResponse> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = getAuthenticatedMemberId(userDetails);
        MemberProfileResponse response = memberProfileService.getProfile(memberId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "프로필 수정",
            description = "현재 로그인한 사용자의 프로필 정보를 수정합니다. 수정하지 않을 필드는 null로 전송합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "프로필 수정 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MemberProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검증 실패)", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음", content = @Content)
    })
    @PutMapping("/profile")
    public ResponseEntity<MemberProfileResponse> updateProfile(
            @Valid @RequestBody ProfileUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = getAuthenticatedMemberId(userDetails);
        MemberProfileResponse response = memberProfileService.updateProfile(memberId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "비밀번호 변경",
            description = "현재 로그인한 사용자의 비밀번호를 변경합니다. 현재 비밀번호 확인이 필요합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "비밀번호 변경 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (현재 비밀번호 불일치, 비밀번호 확인 불일치 등)", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음", content = @Content)
    })
    @PutMapping("/password")
    public ResponseEntity<String> changePassword(
            @Valid @RequestBody PasswordChangeRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = getAuthenticatedMemberId(userDetails);
        memberProfileService.changePassword(memberId, request);
        return ResponseEntity.ok(messageService.getMessage("auth.password.change.success"));
    }

    private Long getAuthenticatedMemberId(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getMember() == null
                || userDetails.getMember().getMemberId() == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return userDetails.getMember().getMemberId();
    }
}

