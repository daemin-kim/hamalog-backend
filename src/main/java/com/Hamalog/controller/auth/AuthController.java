package com.Hamalog.controller.auth;

import com.Hamalog.config.ApiVersion;
import com.Hamalog.dto.auth.request.LoginRequest;
import com.Hamalog.dto.auth.request.SignupRequest;
import com.Hamalog.dto.auth.request.TokenRefreshRequest;
import com.Hamalog.dto.auth.response.ActiveSessionsResponse;
import com.Hamalog.dto.auth.response.LoginHistoryListResponse;
import com.Hamalog.dto.auth.response.LoginResponse;
import com.Hamalog.dto.auth.response.TokenRefreshResponse;
import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.security.CustomUserDetails;
import com.Hamalog.security.jwt.JwtTokenProvider;
import com.Hamalog.service.auth.AuthenticationService;
import com.Hamalog.service.auth.LoginHistoryService;
import com.Hamalog.service.auth.MemberDeletionService;
import com.Hamalog.service.auth.MemberRegistrationService;
import com.Hamalog.service.i18n.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentication API", description = "인증 관련 API (회원가입, 로그인, 로그아웃, 토큰 갱신, 로그인 이력)")
@RestController
@RequestMapping(ApiVersion.AUTH)
@RequiredArgsConstructor
public class AuthController {

    private final MemberRegistrationService memberRegistrationService;
    private final AuthenticationService authenticationService;
    private final MemberDeletionService memberDeletionService;
    private final LoginHistoryService loginHistoryService;
    private final MessageService messageService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@Valid @RequestBody SignupRequest request) {
        memberRegistrationService.registerMember(request);
        return ResponseEntity.ok(messageService.getMessage("auth.signup.success"));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse loginResponse = authenticationService.authenticateAndGenerateToken(request.loginId(), request.password());
        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/refresh")
    @Operation(summary = "토큰 갱신", description = "RefreshToken을 사용하여 새로운 AccessToken을 발급받습니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "토큰 갱신 성공",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = TokenRefreshResponse.class))),
        @ApiResponse(responseCode = "401", description = "유효하지 않은 RefreshToken"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<TokenRefreshResponse> refreshToken(
            @Valid @RequestBody TokenRefreshRequest request) {
        TokenRefreshResponse response = authenticationService.refreshAccessToken(request.refreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "현재 로그인된 사용자를 로그아웃하고 토큰을 무효화합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
        @ApiResponse(responseCode = "401", description = "유효하지 않은 토큰")
    })
    public ResponseEntity<String> logout(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        
        // Bearer 토큰 형식 검증
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        String token = authorizationHeader.substring(7);

        // 토큰 유효성 검증
        if (!jwtTokenProvider.validateToken(token)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        authenticationService.logoutUser(token);
        return ResponseEntity.ok(messageService.getMessage("auth.logout.success"));
    }

    @DeleteMapping("/account")
    @Operation(summary = "회원 탈퇴", description = "현재 로그인된 회원의 계정을 삭제합니다. 모든 관련 데이터(복용 기록, 부작용 기록 등)도 함께 삭제됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원 탈퇴 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "회원 탈퇴가 완료되었습니다"))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    public ResponseEntity<String> deleteAccount(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || 
            "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(401).body(messageService.getMessage("auth.authentication.required"));
        }

        String loginId = authentication.getName();
        
        String token = null;
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            token = authorizationHeader.substring(7);
        }

        memberDeletionService.deleteMember(loginId, token);

        return ResponseEntity.ok(messageService.getMessage("auth.account.deletion.success"));
    }

    @GetMapping("/login-history")
    @Operation(summary = "로그인 이력 조회", description = "현재 로그인된 사용자의 로그인 이력을 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = LoginHistoryListResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<LoginHistoryListResponse> getLoginHistory(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기 (최대 100)", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = userDetails.getMember().getMemberId();
        LoginHistoryListResponse response = loginHistoryService.getLoginHistory(memberId, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sessions")
    @Operation(summary = "활성 세션 조회", description = "현재 활성화된 로그인 세션 목록을 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ActiveSessionsResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<ActiveSessionsResponse> getActiveSessions(
            HttpServletRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = userDetails.getMember().getMemberId();
        String currentSessionId = extractSessionIdFromToken(request);
        ActiveSessionsResponse response = loginHistoryService.getActiveSessions(memberId, currentSessionId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/sessions/{session-id}")
    @Operation(summary = "세션 강제 종료", description = "특정 세션을 강제로 종료합니다. (다른 기기에서 로그아웃)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "세션 종료 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음")
    })
    public ResponseEntity<String> terminateSession(
            @Parameter(description = "종료할 세션 ID", required = true)
            @PathVariable("session-id") String sessionId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = userDetails.getMember().getMemberId();
        loginHistoryService.terminateSession(memberId, sessionId);
        return ResponseEntity.ok("세션이 종료되었습니다.");
    }

    @DeleteMapping("/sessions")
    @Operation(summary = "모든 세션 종료", description = "현재 사용자의 모든 세션을 종료합니다. (모든 기기에서 로그아웃)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "모든 세션 종료 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<String> terminateAllSessions(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = userDetails.getMember().getMemberId();
        loginHistoryService.terminateAllSessions(memberId);
        return ResponseEntity.ok("모든 세션이 종료되었습니다.");
    }

    private String extractSessionIdFromToken(HttpServletRequest request) {
        // 현재 JWT에는 세션 ID가 포함되어 있지 않음
        // 향후 JWT에 세션 ID를 포함하도록 확장 가능
        return null;
    }
}
