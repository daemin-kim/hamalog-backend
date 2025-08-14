package com.Hamalog.controller.auth;

import com.Hamalog.dto.auth.request.LoginRequest;
import com.Hamalog.dto.auth.response.LoginResponse;
import com.Hamalog.dto.auth.request.SignupRequest;
import com.Hamalog.service.auth.AuthService;
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
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentication API", description = "사용자 인증 관련 API - 회원가입, 로그인, 로그아웃")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "회원가입",
            description = "새로운 사용자를 등록합니다. 로그인 ID, 비밀번호, 이름, 전화번호, 생년월일 정보가 필요합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "회원가입 성공",
                    content = @Content(
                            mediaType = "text/plain",
                            schema = @Schema(type = "string", example = "회원가입 성공")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 데이터 (유효성 검증 실패)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "ValidationError",
                                    value = "{\n  \"error\": \"Validation failed\",\n  \"message\": \"입력 데이터를 확인해주세요.\"\n}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "이미 존재하는 사용자 ID",
                    content = @Content
            )
    })
    @PostMapping("/signup")
    public ResponseEntity<String> signup(
            @Parameter(
                    description = "회원가입 요청 데이터",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SignupRequest.class)
                    )
            )
            @Valid @RequestBody SignupRequest request
    ) {
        authService.registerMember(request);
        return ResponseEntity.ok("회원가입 성공");
    }

    @Operation(
            summary = "로그인",
            description = "사용자 인증 후 JWT 토큰을 발급합니다. 로그인 ID와 비밀번호로 인증을 수행합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공 - JWT 토큰 발급",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LoginResponse.class),
                            examples = @ExampleObject(
                                    name = "LoginSuccess",
                                    value = "{\n  \"accessToken\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\",\n  \"tokenType\": \"Bearer\",\n  \"expiresIn\": 3600\n}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 데이터 (유효성 검증 실패)",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 - 잘못된 로그인 정보",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "AuthenticationFailed",
                                    value = "{\n  \"error\": \"Authentication failed\",\n  \"message\": \"로그인 정보가 올바르지 않습니다.\"\n}"
                            )
                    )
            )
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Parameter(
                    description = "로그인 요청 데이터",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LoginRequest.class),
                            examples = @ExampleObject(
                                    name = "LoginExample",
                                    value = "{\n  \"loginId\": \"testuser\",\n  \"password\": \"password123\"\n}"
                            )
                    )
            )
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse loginResponse = authService.authenticateAndGenerateToken(request.loginId(), request.password());
        return ResponseEntity.ok(loginResponse);
    }

    @Operation(
            summary = "로그아웃",
            description = "사용자를 로그아웃 처리하고 JWT 토큰을 무효화합니다. Authorization 헤더에 Bearer 토큰이 포함된 경우 해당 토큰을 블랙리스트에 추가합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "로그아웃 성공",
                    content = @Content(
                            mediaType = "text/plain",
                            schema = @Schema(type = "string"),
                            examples = {
                                    @ExampleObject(
                                            name = "LogoutWithToken",
                                            summary = "토큰과 함께 로그아웃",
                                            value = "로그아웃 성공 - 토큰이 무효화되었습니다"
                                    ),
                                    @ExampleObject(
                                            name = "LogoutWithoutToken",
                                            summary = "토큰 없이 로그아웃",
                                            value = "로그아웃 성공"
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 요청 (토큰이 유효하지 않음)",
                    content = @Content
            )
    })
    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @Parameter(hidden = true) HttpServletRequest request
    ) {
        String authorizationHeader = request.getHeader("Authorization");
        
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            authService.logoutUser(token);
            return ResponseEntity.ok("로그아웃 성공 - 토큰이 무효화되었습니다");
        }
        
        return ResponseEntity.ok("로그아웃 성공");
    }
}
