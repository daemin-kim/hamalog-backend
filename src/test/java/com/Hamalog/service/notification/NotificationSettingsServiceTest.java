package com.Hamalog.service.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.Hamalog.domain.events.DomainEventPublisher;
import com.Hamalog.domain.member.Member;
import com.Hamalog.domain.notification.DeviceType;
import com.Hamalog.domain.notification.FcmDeviceToken;
import com.Hamalog.domain.notification.NotificationSettings;
import com.Hamalog.dto.notification.request.FcmTokenRegisterRequest;
import com.Hamalog.dto.notification.request.NotificationSettingsUpdateRequest;
import com.Hamalog.dto.notification.response.FcmDeviceTokenListResponse;
import com.Hamalog.dto.notification.response.FcmDeviceTokenResponse;
import com.Hamalog.dto.notification.response.NotificationSettingsResponse;
import com.Hamalog.exception.CustomException;
import com.Hamalog.repository.member.MemberRepository;
import com.Hamalog.repository.notification.FcmDeviceTokenRepository;
import com.Hamalog.repository.notification.NotificationSettingsRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationSettingsService 테스트")
class NotificationSettingsServiceTest {

    @Mock
    private NotificationSettingsRepository notificationSettingsRepository;

    @Mock
    private FcmDeviceTokenRepository fcmDeviceTokenRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @InjectMocks
    private NotificationSettingsService notificationSettingsService;

    private Member member;
    private NotificationSettings notificationSettings;

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

        notificationSettings = new NotificationSettings(member);
    }

    @Nested
    @DisplayName("알림 설정 조회")
    class GetSettings {

        @Test
        @DisplayName("성공: 기존 설정이 있는 경우")
        void getSettings_ExistingSettings_Success() {
            // given
            when(notificationSettingsRepository.findByMember_MemberId(1L))
                    .thenReturn(Optional.of(notificationSettings));

            // when
            NotificationSettingsResponse response = notificationSettingsService.getSettings(1L);

            // then
            assertThat(response.memberId()).isEqualTo(1L);
            assertThat(response.pushEnabled()).isTrue();
            assertThat(response.medicationReminderEnabled()).isTrue();
            assertThat(response.medicationReminderMinutesBefore()).isEqualTo(10);
        }

        @Test
        @DisplayName("성공: 설정이 없으면 기본값으로 생성")
        void getSettings_NoSettings_CreatesDefault() {
            // given
            when(notificationSettingsRepository.findByMember_MemberId(1L))
                    .thenReturn(Optional.empty());
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
            when(notificationSettingsRepository.save(any(NotificationSettings.class)))
                    .thenReturn(notificationSettings);

            // when
            NotificationSettingsResponse response = notificationSettingsService.getSettings(1L);

            // then
            assertThat(response.pushEnabled()).isTrue();
            assertThat(response.medicationReminderEnabled()).isTrue();
            verify(notificationSettingsRepository).save(any(NotificationSettings.class));
        }
    }

    @Nested
    @DisplayName("알림 설정 수정")
    class UpdateSettings {

        @Test
        @DisplayName("성공: 설정 업데이트")
        void updateSettings_Success() {
            // given
            NotificationSettingsUpdateRequest request = new NotificationSettingsUpdateRequest(
                    false, // pushEnabled
                    true,  // medicationReminderEnabled
                    15,    // medicationReminderMinutesBefore
                    true,  // diaryReminderEnabled
                    LocalTime.of(20, 0), // diaryReminderTime
                    true,  // quietHoursEnabled
                    LocalTime.of(22, 0), // quietHoursStart
                    LocalTime.of(8, 0)   // quietHoursEnd
            );

            when(notificationSettingsRepository.findByMember_MemberId(1L))
                    .thenReturn(Optional.of(notificationSettings));
            when(notificationSettingsRepository.save(any(NotificationSettings.class)))
                    .thenReturn(notificationSettings);

            // when
            NotificationSettingsResponse response = notificationSettingsService.updateSettings(1L, request);

            // then
            verify(notificationSettingsRepository).save(any(NotificationSettings.class));
        }
    }

    @Nested
    @DisplayName("FCM 토큰 등록")
    class RegisterFcmToken {

        @Test
        @DisplayName("성공: 새 토큰 등록")
        void registerFcmToken_NewToken_Success() {
            // given
            FcmTokenRegisterRequest request = new FcmTokenRegisterRequest(
                    "test-fcm-token-12345",
                    DeviceType.ANDROID,
                    "Galaxy S24"
            );

            FcmDeviceToken newToken = new FcmDeviceToken(
                    member,
                    request.token(),
                    request.deviceType(),
                    request.deviceName()
            );

            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
            when(fcmDeviceTokenRepository.findByToken(request.token()))
                    .thenReturn(Optional.empty());
            when(fcmDeviceTokenRepository.save(any(FcmDeviceToken.class)))
                    .thenReturn(newToken);

            // when
            FcmDeviceTokenResponse response = notificationSettingsService.registerFcmToken(1L, request);

            // then
            assertThat(response.deviceType()).isEqualTo(DeviceType.ANDROID);
            assertThat(response.deviceName()).isEqualTo("Galaxy S24");
            assertThat(response.isActive()).isTrue();
            verify(fcmDeviceTokenRepository).save(any(FcmDeviceToken.class));
        }

        @Test
        @DisplayName("실패: 회원을 찾을 수 없음")
        void registerFcmToken_MemberNotFound_Fail() {
            // given
            FcmTokenRegisterRequest request = new FcmTokenRegisterRequest(
                    "test-fcm-token",
                    DeviceType.IOS,
                    "iPhone 15"
            );

            when(memberRepository.findById(1L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> notificationSettingsService.registerFcmToken(1L, request))
                    .isInstanceOf(CustomException.class);
        }
    }

    @Nested
    @DisplayName("디바이스 목록 조회")
    class GetDeviceTokens {

        @Test
        @DisplayName("성공: 디바이스 목록 조회")
        void getDeviceTokens_Success() {
            // given
            FcmDeviceToken token1 = new FcmDeviceToken(member, "token1", DeviceType.ANDROID, "Galaxy");
            FcmDeviceToken token2 = new FcmDeviceToken(member, "token2", DeviceType.IOS, "iPhone");

            when(fcmDeviceTokenRepository.findByMember_MemberId(1L))
                    .thenReturn(List.of(token1, token2));

            // when
            FcmDeviceTokenListResponse response = notificationSettingsService.getDeviceTokens(1L);

            // then
            assertThat(response.totalDevices()).isEqualTo(2);
            assertThat(response.activeDevices()).isEqualTo(2);
            assertThat(response.devices()).hasSize(2);
        }

        @Test
        @DisplayName("성공: 등록된 디바이스가 없는 경우")
        void getDeviceTokens_Empty_Success() {
            // given
            when(fcmDeviceTokenRepository.findByMember_MemberId(1L))
                    .thenReturn(List.of());

            // when
            FcmDeviceTokenListResponse response = notificationSettingsService.getDeviceTokens(1L);

            // then
            assertThat(response.totalDevices()).isEqualTo(0);
            assertThat(response.devices()).isEmpty();
        }
    }

    @Nested
    @DisplayName("디바이스 토큰 삭제")
    class DeleteDeviceToken {

        @Test
        @DisplayName("성공: 토큰 삭제")
        void deleteDeviceToken_Success() {
            // given
            FcmDeviceToken token = new FcmDeviceToken(member, "token", DeviceType.ANDROID, "Galaxy");

            when(fcmDeviceTokenRepository.findById(1L))
                    .thenReturn(Optional.of(token));

            // when
            notificationSettingsService.deleteDeviceToken(1L, 1L);

            // then
            verify(fcmDeviceTokenRepository).delete(token);
        }

        @Test
        @DisplayName("실패: 다른 사용자의 토큰 삭제 시도")
        void deleteDeviceToken_ForbiddenUser_Fail() {
            // given
            Member otherMember = Member.builder()
                    .memberId(2L)
                    .loginId("other@example.com")
                    .build();
            FcmDeviceToken token = new FcmDeviceToken(otherMember, "token", DeviceType.ANDROID, "Galaxy");

            when(fcmDeviceTokenRepository.findById(1L))
                    .thenReturn(Optional.of(token));

            // when & then
            assertThatThrownBy(() -> notificationSettingsService.deleteDeviceToken(1L, 1L))
                    .isInstanceOf(CustomException.class);
        }
    }
}
