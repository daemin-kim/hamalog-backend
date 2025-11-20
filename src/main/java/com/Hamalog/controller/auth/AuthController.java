package com.Hamalog.controller.auth;

import com.Hamalog.dto.auth.request.LoginRequest;
import com.Hamalog.dto.auth.response.LoginResponse;
import com.Hamalog.dto.auth.request.SignupRequest;import com.Hamalog.dto.auth.request.TokenRefreshRequest;
import com.Hamalog.dto.auth.response.TokenRefreshResponse;
import com.Hamalog.service.auth.AuthService;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final MessageService messageService;

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@Valid @RequestBody SignupRequest request) {
        authService.registerMember(request);
        return ResponseEntity.ok(messageService.getMessage("auth.signup.success"));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse loginResponse = authService.authenticateAndGenerateToken(request.loginId(), request.password());
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
        TokenRefreshResponse response = authService.refreshAccessToken(request.refreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            authService.logoutUser(token);
            return ResponseEntity.ok(messageService.getMessage("auth.logout.success"));
        }
        
        return ResponseEntity.ok(messageService.getMessage("auth.logout.success.simple"));
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

        authService.deleteMember(loginId, token);
        
        return ResponseEntity.ok(messageService.getMessage("auth.account.deletion.success"));
    }
}
