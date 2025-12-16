package com.Hamalog.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.Hamalog.domain.member.Member;
import com.Hamalog.dto.auth.request.SignupRequest;
import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.repository.member.MemberRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberRegistrationService 테스트")
class MemberRegistrationServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MemberRegistrationService memberRegistrationService;

    private SignupRequest validSignupRequest;

    @BeforeEach
    void setUp() {
        validSignupRequest = new SignupRequest(
            "test@example.com",
            "password123",
            "홍길동",
            "길동이",
            "01012345678",
            LocalDate.of(1990, 1, 1)
        );
    }

    @Nested
    @DisplayName("회원가입 테스트")
    class RegisterMemberTest {

        @Test
        @DisplayName("회원가입 성공 - 새 회원이 정상적으로 저장된다")
        void registerMember_Success() {
            // given
            given(memberRepository.findByLoginId(validSignupRequest.loginId()))
                .willReturn(Optional.empty());
            given(passwordEncoder.encode(validSignupRequest.password()))
                .willReturn("encodedPassword");

            // when
            memberRegistrationService.registerMember(validSignupRequest);

            // then
            ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
            verify(memberRepository).save(memberCaptor.capture());

            Member savedMember = memberCaptor.getValue();
            assertThat(savedMember.getLoginId()).isEqualTo(validSignupRequest.loginId());
            assertThat(savedMember.getName()).isEqualTo(validSignupRequest.name());
            assertThat(savedMember.getNickName()).isEqualTo(validSignupRequest.nickName());
            assertThat(savedMember.getPhoneNumber()).isEqualTo(validSignupRequest.phoneNumber());
            assertThat(savedMember.getBirth()).isEqualTo(validSignupRequest.birth());
            assertThat(savedMember.getPassword()).isEqualTo("encodedPassword");
            assertThat(savedMember.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("회원가입 실패 - 중복된 로그인 ID로 예외 발생")
        void registerMember_DuplicateLoginId_ThrowsException() {
            // given
            Member existingMember = Member.builder()
                .loginId(validSignupRequest.loginId())
                .build();
            given(memberRepository.findByLoginId(validSignupRequest.loginId()))
                .willReturn(Optional.of(existingMember));

            // when & then
            assertThatThrownBy(() -> memberRegistrationService.registerMember(validSignupRequest))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.DUPLICATE_MEMBER.getMessage());

            verify(memberRepository, never()).save(any());
        }

        @Test
        @DisplayName("회원가입 시 비밀번호가 암호화되어 저장된다")
        void registerMember_PasswordIsEncrypted() {
            // given
            String plainPassword = "password123";
            String encodedPassword = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

            given(memberRepository.findByLoginId(validSignupRequest.loginId()))
                .willReturn(Optional.empty());
            given(passwordEncoder.encode(plainPassword))
                .willReturn(encodedPassword);

            // when
            memberRegistrationService.registerMember(validSignupRequest);

            // then
            verify(passwordEncoder).encode(plainPassword);

            ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
            verify(memberRepository).save(memberCaptor.capture());

            Member savedMember = memberCaptor.getValue();
            assertThat(savedMember.getPassword()).isEqualTo(encodedPassword);
            assertThat(savedMember.getPassword()).isNotEqualTo(plainPassword);
        }

        @Test
        @DisplayName("회원가입 시 PasswordEncoder.encode()가 호출된다")
        void registerMember_PasswordEncoderCalled() {
            // given
            given(memberRepository.findByLoginId(validSignupRequest.loginId()))
                .willReturn(Optional.empty());
            given(passwordEncoder.encode(any()))
                .willReturn("encodedPassword");

            // when
            memberRegistrationService.registerMember(validSignupRequest);

            // then
            verify(passwordEncoder).encode(validSignupRequest.password());
        }
    }

    @Nested
    @DisplayName("다양한 입력값 테스트")
    class EdgeCaseTest {

        @Test
        @DisplayName("최소 길이 이름으로 회원가입 성공")
        void registerMember_MinLengthName_Success() {
            // given
            SignupRequest request = new SignupRequest(
                "test2@example.com",
                "password123",
                "홍",  // 최소 길이 이름
                "닉",
                "01012345678",
                LocalDate.of(1990, 1, 1)
            );

            given(memberRepository.findByLoginId(request.loginId()))
                .willReturn(Optional.empty());
            given(passwordEncoder.encode(request.password()))
                .willReturn("encodedPassword");

            // when
            memberRegistrationService.registerMember(request);

            // then
            ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
            verify(memberRepository).save(memberCaptor.capture());

            Member savedMember = memberCaptor.getValue();
            assertThat(savedMember.getName()).isEqualTo("홍");
        }

        @Test
        @DisplayName("생년월일이 과거 날짜일 때 회원가입 성공")
        void registerMember_PastBirthDate_Success() {
            // given
            SignupRequest request = new SignupRequest(
                "test3@example.com",
                "password123",
                "테스트",
                "테스터",
                "01012345678",
                LocalDate.of(1950, 1, 1)  // 과거 날짜
            );

            given(memberRepository.findByLoginId(request.loginId()))
                .willReturn(Optional.empty());
            given(passwordEncoder.encode(request.password()))
                .willReturn("encodedPassword");

            // when
            memberRegistrationService.registerMember(request);

            // then
            ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
            verify(memberRepository).save(memberCaptor.capture());

            Member savedMember = memberCaptor.getValue();
            assertThat(savedMember.getBirth()).isEqualTo(LocalDate.of(1950, 1, 1));
        }
    }
}
