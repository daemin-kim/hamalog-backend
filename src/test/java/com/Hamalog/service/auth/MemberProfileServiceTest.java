package com.Hamalog.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.Hamalog.domain.member.Member;
import com.Hamalog.dto.auth.request.PasswordChangeRequest;
import com.Hamalog.dto.auth.request.ProfileUpdateRequest;
import com.Hamalog.dto.auth.response.MemberProfileResponse;
import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.exception.member.MemberNotFoundException;
import com.Hamalog.repository.member.MemberRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberProfileService 테스트")
class MemberProfileServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MemberProfileService memberProfileService;

    private Member member;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .memberId(1L)
                .loginId("user@example.com")
                .password("encodedPassword")
                .name("홍길동")
                .nickName("길동이")
                .phoneNumber("01012345678")
                .birth(LocalDate.of(1990, 1, 1))
                .createdAt(LocalDateTime.now())
                .version(0L)
                .build();
    }

    @Nested
    @DisplayName("프로필 조회")
    class GetProfile {

        @Test
        @DisplayName("회원 ID로 프로필 조회 성공")
        void getProfile_Success() {
            // given
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

            // when
            MemberProfileResponse response = memberProfileService.getProfile(1L);

            // then
            assertThat(response.memberId()).isEqualTo(1L);
            assertThat(response.loginId()).isEqualTo("user@example.com");
            assertThat(response.name()).isEqualTo("홍길동");
            assertThat(response.nickName()).isEqualTo("길동이");
        }

        @Test
        @DisplayName("로그인 ID로 프로필 조회 성공")
        void getProfileByLoginId_Success() {
            // given
            when(memberRepository.findByLoginId("user@example.com")).thenReturn(Optional.of(member));

            // when
            MemberProfileResponse response = memberProfileService.getProfileByLoginId("user@example.com");

            // then
            assertThat(response.loginId()).isEqualTo("user@example.com");
        }

        @Test
        @DisplayName("존재하지 않는 회원 조회 시 예외 발생")
        void getProfile_NotFound() {
            // given
            when(memberRepository.findById(99L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberProfileService.getProfile(99L))
                    .isInstanceOf(MemberNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("프로필 수정")
    class UpdateProfile {

        @Test
        @DisplayName("프로필 수정 성공 - 닉네임만 변경")
        void updateProfile_OnlyNickName_Success() {
            // given
            ProfileUpdateRequest request = new ProfileUpdateRequest(
                    null, "새닉네임", null, null
            );
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

            // when
            MemberProfileResponse response = memberProfileService.updateProfile(1L, request);

            // then
            assertThat(response.nickName()).isEqualTo("새닉네임");
            assertThat(response.name()).isEqualTo("홍길동"); // 변경 안됨
        }

        @Test
        @DisplayName("프로필 수정 성공 - 전체 필드 변경")
        void updateProfile_AllFields_Success() {
            // given
            ProfileUpdateRequest request = new ProfileUpdateRequest(
                    "새이름", "새닉네임", "01098765432", LocalDate.of(1995, 5, 5)
            );
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

            // when
            MemberProfileResponse response = memberProfileService.updateProfile(1L, request);

            // then
            assertThat(response.name()).isEqualTo("새이름");
            assertThat(response.nickName()).isEqualTo("새닉네임");
            assertThat(response.phoneNumber()).isEqualTo("01098765432");
            assertThat(response.birth()).isEqualTo(LocalDate.of(1995, 5, 5));
        }

        @Test
        @DisplayName("수정할 데이터가 없으면 예외 발생")
        void updateProfile_NoData_ThrowsException() {
            // given
            ProfileUpdateRequest request = new ProfileUpdateRequest(
                    null, null, null, null
            );

            // when & then
            assertThatThrownBy(() -> memberProfileService.updateProfile(1L, request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NO_PROFILE_UPDATE_DATA);
        }

        @Test
        @DisplayName("존재하지 않는 회원 수정 시 예외 발생")
        void updateProfile_MemberNotFound() {
            // given
            ProfileUpdateRequest request = new ProfileUpdateRequest(
                    "새이름", null, null, null
            );
            when(memberRepository.findById(99L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberProfileService.updateProfile(99L, request))
                    .isInstanceOf(MemberNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("비밀번호 변경")
    class ChangePassword {

        @Test
        @DisplayName("비밀번호 변경 성공")
        void changePassword_Success() {
            // given
            PasswordChangeRequest request = new PasswordChangeRequest(
                    "currentPassword", "newPassword123!", "newPassword123!"
            );
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
            when(passwordEncoder.matches("currentPassword", "encodedPassword")).thenReturn(true);
            when(passwordEncoder.matches("newPassword123!", "encodedPassword")).thenReturn(false);
            when(passwordEncoder.encode("newPassword123!")).thenReturn("newEncodedPassword");

            // when
            memberProfileService.changePassword(1L, request);

            // then
            verify(passwordEncoder).encode("newPassword123!");
        }

        @Test
        @DisplayName("비밀번호 확인 불일치 시 예외 발생")
        void changePassword_ConfirmMismatch_ThrowsException() {
            // given
            PasswordChangeRequest request = new PasswordChangeRequest(
                    "currentPassword", "newPassword123!", "differentPassword!"
            );

            // when & then
            assertThatThrownBy(() -> memberProfileService.changePassword(1L, request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }

        @Test
        @DisplayName("현재 비밀번호 불일치 시 예외 발생")
        void changePassword_WrongCurrentPassword_ThrowsException() {
            // given
            PasswordChangeRequest request = new PasswordChangeRequest(
                    "wrongPassword", "newPassword123!", "newPassword123!"
            );
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
            when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> memberProfileService.changePassword(1L, request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CURRENT_PASSWORD);
        }

        @Test
        @DisplayName("새 비밀번호가 현재와 같으면 예외 발생")
        void changePassword_SameAsCurrent_ThrowsException() {
            // given
            PasswordChangeRequest request = new PasswordChangeRequest(
                    "currentPassword", "currentPassword", "currentPassword"
            );
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
            when(passwordEncoder.matches("currentPassword", "encodedPassword")).thenReturn(true);
            when(passwordEncoder.matches("currentPassword", "encodedPassword")).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> memberProfileService.changePassword(1L, request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SAME_AS_CURRENT_PASSWORD);
        }

        @Test
        @DisplayName("존재하지 않는 회원 비밀번호 변경 시 예외 발생")
        void changePassword_MemberNotFound() {
            // given
            PasswordChangeRequest request = new PasswordChangeRequest(
                    "currentPassword", "newPassword123!", "newPassword123!"
            );
            when(memberRepository.findById(99L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberProfileService.changePassword(99L, request))
                    .isInstanceOf(MemberNotFoundException.class);
        }
    }
}

