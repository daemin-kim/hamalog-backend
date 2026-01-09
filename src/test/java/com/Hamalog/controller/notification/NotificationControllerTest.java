package com.Hamalog.controller.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.Hamalog.domain.member.Member;
import com.Hamalog.domain.notification.DeviceType;
import com.Hamalog.dto.notification.request.FcmTokenRegisterRequest;
import com.Hamalog.dto.notification.request.NotificationSettingsUpdateRequest;
import com.Hamalog.dto.notification.response.FcmDeviceTokenListResponse;
import com.Hamalog.dto.notification.response.FcmDeviceTokenResponse;
import com.Hamalog.dto.notification.response.NotificationSettingsResponse;
import com.Hamalog.security.CustomUserDetails;
import com.Hamalog.service.notification.NotificationSettingsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
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
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationController 테스트")
class NotificationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private NotificationSettingsService notificationSettingsService;

    @InjectMocks
    private NotificationController notificationController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController)
                .setCustomArgumentResolvers(new MockCustomUserDetailsArgumentResolver())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
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

            CustomUserDetails mockUserDetails = mock(CustomUserDetails.class);
            lenient().when(mockUserDetails.getMember()).thenReturn(mockMember);
            return mockUserDetails;
        }
    }

    @Nested
    @DisplayName("알림 설정 조회")
    class GetNotificationSettings {

        @Test
        @DisplayName("성공: 알림 설정 조회")
        void success() throws Exception {
            NotificationSettingsResponse response = new NotificationSettingsResponse(
                    1L, 1L, true, true, 10, false, LocalTime.of(21, 0),
                    false, LocalTime.of(23, 0), LocalTime.of(7, 0),
                    LocalDateTime.now(), null
            );

            when(notificationSettingsService.getSettings(1L)).thenReturn(response);

            mockMvc.perform(get("/notification/settings"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.notificationSettingsId").value(1))
                    .andExpect(jsonPath("$.pushEnabled").value(true))
                    .andExpect(jsonPath("$.medicationReminderEnabled").value(true));

            verify(notificationSettingsService).getSettings(1L);
        }
    }

    @Nested
    @DisplayName("알림 설정 수정")
    class UpdateNotificationSettings {

        @Test
        @DisplayName("성공: 알림 설정 수정")
        void success() throws Exception {
            NotificationSettingsUpdateRequest request = new NotificationSettingsUpdateRequest(
                    true, true, 15, true, LocalTime.of(20, 0),
                    true, LocalTime.of(22, 0), LocalTime.of(8, 0)
            );

            NotificationSettingsResponse response = new NotificationSettingsResponse(
                    1L, 1L, true, true, 15, true, LocalTime.of(20, 0),
                    true, LocalTime.of(22, 0), LocalTime.of(8, 0),
                    LocalDateTime.now(), LocalDateTime.now()
            );

            when(notificationSettingsService.updateSettings(eq(1L), any(NotificationSettingsUpdateRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(put("/notification/settings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.medicationReminderMinutesBefore").value(15))
                    .andExpect(jsonPath("$.diaryReminderEnabled").value(true));

            verify(notificationSettingsService).updateSettings(eq(1L), any(NotificationSettingsUpdateRequest.class));
        }
    }

    @Nested
    @DisplayName("FCM 토큰 등록")
    class RegisterFcmToken {

        @Test
        @DisplayName("성공: FCM 토큰 등록")
        void success() throws Exception {
            FcmTokenRegisterRequest request = new FcmTokenRegisterRequest(
                    "fcm-token-string-abc123", DeviceType.ANDROID, "Galaxy S24"
            );

            FcmDeviceTokenResponse response = new FcmDeviceTokenResponse(
                    1L, "fcm-token-string-ab...", DeviceType.ANDROID,
                    "Galaxy S24", true, LocalDateTime.now(), LocalDateTime.now()
            );

            when(notificationSettingsService.registerFcmToken(eq(1L), any(FcmTokenRegisterRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/notification/token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.fcmDeviceTokenId").value(1))
                    .andExpect(jsonPath("$.deviceType").value("ANDROID"));

            verify(notificationSettingsService).registerFcmToken(eq(1L), any(FcmTokenRegisterRequest.class));
        }
    }

    @Nested
    @DisplayName("등록된 디바이스 목록 조회")
    class GetDevices {

        @Test
        @DisplayName("성공: 디바이스 목록 조회")
        void success() throws Exception {
            FcmDeviceTokenResponse device1 = new FcmDeviceTokenResponse(
                    1L, "fcm-token-12345678...", DeviceType.ANDROID, "Galaxy S24",
                    true, LocalDateTime.now(), LocalDateTime.now()
            );
            FcmDeviceTokenResponse device2 = new FcmDeviceTokenResponse(
                    2L, "fcm-token-87654321...", DeviceType.IOS, "iPhone 15",
                    true, LocalDateTime.now(), LocalDateTime.now()
            );

            FcmDeviceTokenListResponse response = new FcmDeviceTokenListResponse(
                    1L, 2, 2, Arrays.asList(device1, device2)
            );

            when(notificationSettingsService.getDeviceTokens(1L)).thenReturn(response);

            mockMvc.perform(get("/notification/devices"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalDevices").value(2))
                    .andExpect(jsonPath("$.devices").isArray())
                    .andExpect(jsonPath("$.devices.length()").value(2));

            verify(notificationSettingsService).getDeviceTokens(1L);
        }
    }

    @Nested
    @DisplayName("디바이스 토큰 삭제")
    class DeleteDeviceToken {

        @Test
        @DisplayName("성공: 디바이스 토큰 삭제")
        void success() throws Exception {
            Long tokenId = 1L;
            doNothing().when(notificationSettingsService).deleteDeviceToken(1L, tokenId);

            mockMvc.perform(delete("/notification/devices/{tokenId}", tokenId))
                    .andExpect(status().isNoContent());

            verify(notificationSettingsService).deleteDeviceToken(1L, tokenId);
        }
    }

    @Nested
    @DisplayName("현재 토큰 비활성화")
    class DeactivateCurrentToken {

        @Test
        @DisplayName("성공: 현재 FCM 토큰 비활성화")
        void success() throws Exception {
            String fcmToken = "current-fcm-token";
            doNothing().when(notificationSettingsService).deactivateFcmToken(1L, fcmToken);

            mockMvc.perform(delete("/notification/token")
                            .header("X-FCM-Token", fcmToken))
                    .andExpect(status().isNoContent());

            verify(notificationSettingsService).deactivateFcmToken(1L, fcmToken);
        }

        @Test
        @DisplayName("성공: FCM 토큰 없이 호출 시 무시")
        void success_noToken() throws Exception {
            mockMvc.perform(delete("/notification/token"))
                    .andExpect(status().isNoContent());

            verify(notificationSettingsService, never()).deactivateFcmToken(anyLong(), anyString());
        }
    }
}
