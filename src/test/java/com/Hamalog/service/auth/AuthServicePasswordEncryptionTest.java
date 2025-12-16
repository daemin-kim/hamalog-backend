package com.Hamalog.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.Hamalog.domain.member.Member;
import com.Hamalog.dto.auth.request.SignupRequest;
import com.Hamalog.repository.medication.MedicationRecordRepository;
import com.Hamalog.repository.medication.MedicationScheduleRepository;
import com.Hamalog.repository.member.MemberRepository;
import com.Hamalog.repository.sideEffect.SideEffectRecordRepository;
import com.Hamalog.security.jwt.JwtTokenProvider;
import com.Hamalog.security.jwt.TokenBlacklistService;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 비밀번호 암호화 테스트")
class AuthServicePasswordEncryptionTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MedicationScheduleRepository medicationScheduleRepository;

    @Mock
    private MedicationRecordRepository medicationRecordRepository;

    @Mock
    private SideEffectRecordRepository sideEffectRecordRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private SignupRequest signupRequest;

    @BeforeEach
    void setUp() {
        signupRequest = new SignupRequest(
                "test@example.com",
                "plaintext123",
                "테스트유저",
                "테스터",
                "01012345678",
                LocalDate.of(1990, 1, 1)
        );
    }

    @Test
    @DisplayName("회원가입 시 비밀번호가 BCrypt로 암호화되어 저장된다")
    void registerMember_PasswordIsEncrypted() {
        // Given
        String plainPassword = "plaintext123";
        String encodedPassword = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        
        given(memberRepository.findByLoginId(signupRequest.loginId())).willReturn(Optional.empty());
        given(passwordEncoder.encode(plainPassword)).willReturn(encodedPassword);

        // When
        authService.registerMember(signupRequest);

        // Then
        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());
        
        Member savedMember = memberCaptor.getValue();
        
        // 비밀번호가 암호화되었는지 확인
        assertThat(savedMember.getPassword()).isEqualTo(encodedPassword);
        assertThat(savedMember.getPassword()).isNotEqualTo(plainPassword);
        assertThat(savedMember.getPassword()).startsWith("$2a$10$");
        assertThat(savedMember.getPassword().length()).isEqualTo(60);
        
        // 다른 필드들이 올바르게 설정되었는지 확인
        assertThat(savedMember.getLoginId()).isEqualTo(signupRequest.loginId());
        assertThat(savedMember.getName()).isEqualTo(signupRequest.name());
        assertThat(savedMember.getNickName()).isEqualTo(signupRequest.nickName());
        assertThat(savedMember.getPhoneNumber()).isEqualTo(signupRequest.phoneNumber());
        assertThat(savedMember.getBirth()).isEqualTo(signupRequest.birth());
        
        // PasswordEncoder의 encode 메서드가 호출되었는지 확인
        verify(passwordEncoder).encode(plainPassword);
    }

    @Test
    @DisplayName("PasswordEncoder.encode()가 회원가입시 호출되는지 확인")
    void registerMember_PasswordEncoderCalled() {
        // Given
        String plainPassword = "plaintext123";
        String encodedPassword = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        
        given(memberRepository.findByLoginId(signupRequest.loginId())).willReturn(Optional.empty());
        given(passwordEncoder.encode(plainPassword)).willReturn(encodedPassword);

        // When
        authService.registerMember(signupRequest);

        // Then
        // PasswordEncoder의 encode 메서드가 정확히 한 번 호출되었는지 확인
        verify(passwordEncoder).encode(plainPassword);
        
        // Member 저장이 호출되었는지 확인
        verify(memberRepository).save(any(Member.class));
    }
}