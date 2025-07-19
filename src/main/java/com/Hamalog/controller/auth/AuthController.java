package com.Hamalog.controller.auth;

import com.Hamalog.domain.member.Member;
import com.Hamalog.dto.auth.request.LoginRequest;
import com.Hamalog.dto.auth.response.LoginResponse;
import com.Hamalog.dto.auth.request.SignupRequest;
import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.repository.member.MemberRepository;
import com.Hamalog.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignupRequest request) {

        if (memberRepository.findByLoginId(request.loginId()).isPresent()) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }

        Member member = Member.builder()
                .loginId(request.loginId())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .phoneNumber(request.phoneNumber())
                .birth(request.birth())
                .createdAt(LocalDateTime.now())
                .build();

        memberRepository.save(member);

        return ResponseEntity.ok("회원가입 성공");
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.loginId(), request.password())
        );

        String token = jwtTokenProvider.createToken(authentication.getName());

        return ResponseEntity.ok(new LoginResponse(token));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        return ResponseEntity.ok("로그아웃 성공");
    }
}
