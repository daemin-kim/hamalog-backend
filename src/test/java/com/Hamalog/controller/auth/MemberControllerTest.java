package com.Hamalog.controller.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.Hamalog.domain.member.Member;
import com.Hamalog.dto.auth.request.PasswordChangeRequest;
import com.Hamalog.dto.auth.request.ProfileUpdateRequest;
import com.Hamalog.dto.auth.response.MemberProfileResponse;
import com.Hamalog.security.CustomUserDetails;
import com.Hamalog.service.auth.MemberProfileService;
import com.Hamalog.service.i18n.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberController 테스트")
class MemberControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MemberProfileService memberProfileService;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private MemberController memberController;

    private ObjectMapper objectMapper;
    private MemberProfileResponse mockProfileResponse;

    @BeforeEach
    void setUp() {
        CharacterEncodingFilter filter = new CharacterEncodingFilter();
        filter.setEncoding("UTF-8");
        filter.setForceEncoding(true);

        mockMvc = MockMvcBuilders.standaloneSetup(memberController)
                .setCustomArgumentResolvers(new MockCustomUserDetailsArgumentResolver())
                .addFilters(filter)
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        mockProfileResponse = new MemberProfileResponse(
                1L,
                "test@example.com",
                "테스트 사용자",
                "테스트닉네임",
                "01012345678",
                LocalDate.of(1990, 1, 1),
                LocalDateTime.now()
        );
    }

    private static class MockCustomUserDetailsArgumentResolver implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(AuthenticationPrincipal.class) &&
                    CustomUserDetails.class.isAssignableFrom(parameter.getParameterType());
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                       NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
            Member mockMember = mock(Member.class);
            lenient().when(mockMember.getMemberId()).thenReturn(1L);
            lenient().when(mockMember.getLoginId()).thenReturn("test@example.com");

            CustomUserDetails mockUserDetails = mock(CustomUserDetails.class);
            lenient().when(mockUserDetails.getMember()).thenReturn(mockMember);
            return mockUserDetails;
        }
    }

    @Nested
    @DisplayName("프로필 조회")
    class GetMyProfile {

        @Test
        @DisplayName("성공: 내 프로필 조회")
        void success() throws Exception {
            when(memberProfileService.getProfile(1L)).thenReturn(mockProfileResponse);

            mockMvc.perform(get("/member/profile"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.memberId").value(1))
                    .andExpect(jsonPath("$.loginId").value("test@example.com"))
                    .andExpect(jsonPath("$.name").value("테스트 사용자"));

            verify(memberProfileService).getProfile(1L);
        }
    }

    @Nested
    @DisplayName("프로필 수정")
    class UpdateProfile {

        @Test
        @DisplayName("성공: 프로필 수정")
        void success() throws Exception {
            ProfileUpdateRequest request = new ProfileUpdateRequest(
                    "수정된 사용자",
                    "수정닉네임",
                    "01098765432",
                    null
            );

            MemberProfileResponse updatedResponse = new MemberProfileResponse(
                    1L,
                    "test@example.com",
                    "수정된 사용자",
                    "수정닉네임",
                    "01098765432",
                    LocalDate.of(1990, 1, 1),
                    LocalDateTime.now()
            );

            when(memberProfileService.updateProfile(eq(1L), any(ProfileUpdateRequest.class)))
                    .thenReturn(updatedResponse);

            mockMvc.perform(put("/member/profile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("수정된 사용자"))
                    .andExpect(jsonPath("$.nickName").value("수정닉네임"));

            verify(memberProfileService).updateProfile(eq(1L), any(ProfileUpdateRequest.class));
        }
    }

    @Nested
    @DisplayName("비밀번호 변경")
    class ChangePassword {

        @Test
        @DisplayName("성공: 비밀번호 변경")
        void success() throws Exception {
            PasswordChangeRequest request = new PasswordChangeRequest(
                    "currentPassword123",
                    "newPassword456!",
                    "newPassword456!"
            );

            doNothing().when(memberProfileService).changePassword(eq(1L), any(PasswordChangeRequest.class));
            when(messageService.getMessage("auth.password.change.success"))
                    .thenReturn("Password changed successfully");

            mockMvc.perform(put("/member/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding("UTF-8")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(memberProfileService).changePassword(eq(1L), any(PasswordChangeRequest.class));
        }
    }
}
