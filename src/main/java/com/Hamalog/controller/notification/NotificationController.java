package com.Hamalog.controller.notification;

import com.Hamalog.config.ApiVersion;
import com.Hamalog.dto.notification.request.FcmTokenRegisterRequest;
import com.Hamalog.dto.notification.request.NotificationSettingsUpdateRequest;
import com.Hamalog.dto.notification.response.FcmDeviceTokenListResponse;
import com.Hamalog.dto.notification.response.FcmDeviceTokenResponse;
import com.Hamalog.dto.notification.response.NotificationSettingsResponse;
import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.security.CustomUserDetails;
import com.Hamalog.service.notification.NotificationSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 알림 설정 및 FCM 토큰 관리 Controller
 */
@Tag(name = "Notification API", description = "푸시 알림 설정 및 디바이스 토큰 관리 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping(ApiVersion.NOTIFICATION)
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationSettingsService notificationSettingsService;

    // ============= 알림 설정 API =============

    @Operation(summary = "알림 설정 조회",
            description = "현재 사용자의 알림 설정을 조회합니다. 설정이 없으면 기본값으로 생성됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = NotificationSettingsResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
    })
    @GetMapping("/settings")
    public ResponseEntity<NotificationSettingsResponse> getNotificationSettings(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = getAuthenticatedMemberId(userDetails);
        NotificationSettingsResponse response = notificationSettingsService.getSettings(memberId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "알림 설정 수정",
            description = "알림 설정을 수정합니다. 수정하지 않을 필드는 null로 전송합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = NotificationSettingsResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
    })
    @PutMapping("/settings")
    public ResponseEntity<NotificationSettingsResponse> updateNotificationSettings(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody NotificationSettingsUpdateRequest request
    ) {
        Long memberId = getAuthenticatedMemberId(userDetails);
        NotificationSettingsResponse response = notificationSettingsService.updateSettings(memberId, request);
        return ResponseEntity.ok(response);
    }

    // ============= FCM 토큰 관리 API =============

    @Operation(summary = "FCM 토큰 등록",
            description = "디바이스의 FCM 토큰을 등록합니다. 이미 등록된 토큰이면 갱신됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "등록 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = FcmDeviceTokenResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
    })
    @PostMapping("/token")
    public ResponseEntity<FcmDeviceTokenResponse> registerFcmToken(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody FcmTokenRegisterRequest request
    ) {
        Long memberId = getAuthenticatedMemberId(userDetails);
        FcmDeviceTokenResponse response = notificationSettingsService.registerFcmToken(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "등록된 디바이스 목록 조회",
            description = "현재 사용자에게 등록된 모든 디바이스 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = FcmDeviceTokenListResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
    })
    @GetMapping("/devices")
    public ResponseEntity<FcmDeviceTokenListResponse> getDevices(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = getAuthenticatedMemberId(userDetails);
        FcmDeviceTokenListResponse response = notificationSettingsService.getDeviceTokens(memberId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "디바이스 토큰 삭제",
            description = "특정 디바이스의 FCM 토큰을 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content),
            @ApiResponse(responseCode = "404", description = "토큰을 찾을 수 없음", content = @Content)
    })
    @DeleteMapping("/devices/{tokenId}")
    public ResponseEntity<Void> deleteDeviceToken(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long tokenId
    ) {
        Long memberId = getAuthenticatedMemberId(userDetails);
        notificationSettingsService.deleteDeviceToken(memberId, tokenId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "현재 디바이스 FCM 토큰 비활성화",
            description = "현재 디바이스의 FCM 토큰을 비활성화합니다. 로그아웃 시 호출하세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "비활성화 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
    })
    @DeleteMapping("/token")
    public ResponseEntity<Void> deactivateCurrentToken(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestHeader(value = "X-FCM-Token", required = false) String fcmToken
    ) {
        Long memberId = getAuthenticatedMemberId(userDetails);
        if (fcmToken != null && !fcmToken.isBlank()) {
            notificationSettingsService.deactivateFcmToken(memberId, fcmToken);
        }
        return ResponseEntity.noContent().build();
    }

    // ============= 헬퍼 메서드 =============

    private Long getAuthenticatedMemberId(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return userDetails.getMember().getMemberId();
    }
}
