package com.Hamalog.service.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.Hamalog.domain.member.Member;
import com.Hamalog.domain.notification.NotificationSettings;
import com.Hamalog.repository.notification.FcmDeviceTokenRepository;
import com.Hamalog.repository.notification.NotificationSettingsRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * FCM 푸시 알림 서비스 테스트
 */
@DisplayName("FCM 푸시 알림 서비스 테스트")
@ExtendWith(MockitoExtension.class)
class FcmPushServiceTest {

    @Mock
    private FcmDeviceTokenRepository fcmDeviceTokenRepository;

    @Mock
    private NotificationSettingsRepository notificationSettingsRepository;

    @InjectMocks
    private FcmPushService fcmPushService;

    private Long testMemberId;
    private Member testMember;
    private NotificationSettings testSettings;

    @BeforeEach
    void setUp() {
        testMemberId = 1L;

        testMember = Member.builder()
                .loginId("test@test.com")
                .password("password123")
                .name("테스트 사용자")
                .nickName("테스트")
                .phoneNumber("01012345678")
                .birth(LocalDate.of(1990, 1, 1))
                .build();
        ReflectionTestUtils.setField(testMember, "memberId", testMemberId);

        testSettings = new NotificationSettings(testMember);
    }

    @Nested
    @DisplayName("푸시 알림 발송")
    class SendPushNotification {

        @Test
        @DisplayName("성공: 푸시 비활성화 시 알림을 발송하지 않음")
        void shouldNotSendWhenPushDisabled() {
            // given
            NotificationSettings disabledSettings = new NotificationSettings(testMember);
            // pushEnabled를 false로 설정
            disabledSettings.updateSettings(false, null, null, null, null, null, null, null);

            given(notificationSettingsRepository.findByMember_MemberId(testMemberId))
                    .willReturn(Optional.of(disabledSettings));

            // when
            CompletableFuture<Void> result = fcmPushService.sendPushNotification(
                    testMemberId, "제목", "내용", Map.of("key", "value"));

            // then
            assertThat(result).isNotNull();
            verify(fcmDeviceTokenRepository, never()).findByMember_MemberIdAndIsActiveTrue(any());
        }

        @Test
        @DisplayName("성공: 활성화된 토큰이 없으면 발송하지 않음")
        void shouldNotSendWhenNoActiveTokens() {
            // given
            given(notificationSettingsRepository.findByMember_MemberId(testMemberId))
                    .willReturn(Optional.of(testSettings));
            given(fcmDeviceTokenRepository.findByMember_MemberIdAndIsActiveTrue(testMemberId))
                    .willReturn(Collections.emptyList());

            // when
            CompletableFuture<Void> result = fcmPushService.sendPushNotification(
                    testMemberId, "제목", "내용", null);

            // then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("성공: 조용한 시간에는 알림을 발송하지 않음")
        void shouldNotSendDuringQuietHours() {
            // given
            NotificationSettings quietSettings = new NotificationSettings(testMember);
            // 현재 시간이 항상 조용한 시간 내에 있도록 설정
            quietSettings.updateSettings(null, null, null, null, null, true, LocalTime.of(0, 0), LocalTime.of(23, 59));

            given(notificationSettingsRepository.findByMember_MemberId(testMemberId))
                    .willReturn(Optional.of(quietSettings));

            // when
            CompletableFuture<Void> result = fcmPushService.sendPushNotification(
                    testMemberId, "제목", "내용", null);

            // then
            assertThat(result).isNotNull();
            verify(fcmDeviceTokenRepository, never()).findByMember_MemberIdAndIsActiveTrue(any());
        }
    }

    @Nested
    @DisplayName("특수 알림 발송 메서드")
    class SpecialNotifications {

        @Test
        @DisplayName("성공: 심각한 부작용 알림 발송")
        void sendSevereSideEffectAlert() {
            // given
            given(notificationSettingsRepository.findByMember_MemberId(testMemberId))
                    .willReturn(Optional.of(testSettings));
            given(fcmDeviceTokenRepository.findByMember_MemberIdAndIsActiveTrue(testMemberId))
                    .willReturn(Collections.emptyList());

            // when
            fcmPushService.sendSevereSideEffectAlert(testMemberId, "두통", 5);

            // then - 토큰이 없으므로 에러 없이 완료
            verify(fcmDeviceTokenRepository).findByMember_MemberIdAndIsActiveTrue(testMemberId);
        }

        @Test
        @DisplayName("성공: 의료진 상담 권유 알림 발송")
        void sendMedicalConsultationReminder() {
            // given
            given(notificationSettingsRepository.findByMember_MemberId(testMemberId))
                    .willReturn(Optional.of(testSettings));
            given(fcmDeviceTokenRepository.findByMember_MemberIdAndIsActiveTrue(testMemberId))
                    .willReturn(Collections.emptyList());

            // when
            fcmPushService.sendMedicalConsultationReminder(testMemberId);

            // then
            verify(fcmDeviceTokenRepository).findByMember_MemberIdAndIsActiveTrue(testMemberId);
        }

        @Test
        @DisplayName("성공: 복약 미완료 알림 발송")
        void sendMissedMedicationReminder() {
            // given
            given(notificationSettingsRepository.findByMember_MemberId(testMemberId))
                    .willReturn(Optional.of(testSettings));
            given(fcmDeviceTokenRepository.findByMember_MemberIdAndIsActiveTrue(testMemberId))
                    .willReturn(Collections.emptyList());

            // when
            fcmPushService.sendMissedMedicationReminder(testMemberId, 3);

            // then
            verify(fcmDeviceTokenRepository).findByMember_MemberIdAndIsActiveTrue(testMemberId);
        }

        @Test
        @DisplayName("성공: 연속 복약 달성 알림 발송")
        void sendConsecutiveMedicationAchievement() {
            // given
            given(notificationSettingsRepository.findByMember_MemberId(testMemberId))
                    .willReturn(Optional.of(testSettings));
            given(fcmDeviceTokenRepository.findByMember_MemberIdAndIsActiveTrue(testMemberId))
                    .willReturn(Collections.emptyList());

            // when
            fcmPushService.sendConsecutiveMedicationAchievement(testMemberId, 7);

            // then
            verify(fcmDeviceTokenRepository).findByMember_MemberIdAndIsActiveTrue(testMemberId);
        }

        @Test
        @DisplayName("성공: 연속 일기 작성 달성 알림 발송")
        void sendConsecutiveDiaryAchievement() {
            // given
            given(notificationSettingsRepository.findByMember_MemberId(testMemberId))
                    .willReturn(Optional.of(testSettings));
            given(fcmDeviceTokenRepository.findByMember_MemberIdAndIsActiveTrue(testMemberId))
                    .willReturn(Collections.emptyList());

            // when
            fcmPushService.sendConsecutiveDiaryAchievement(testMemberId, 5);

            // then
            verify(fcmDeviceTokenRepository).findByMember_MemberIdAndIsActiveTrue(testMemberId);
        }

        @Test
        @DisplayName("성공: 부작용 기록 권유 알림 발송")
        void sendSideEffectRecordReminder() {
            // given
            given(notificationSettingsRepository.findByMember_MemberId(testMemberId))
                    .willReturn(Optional.of(testSettings));
            given(fcmDeviceTokenRepository.findByMember_MemberIdAndIsActiveTrue(testMemberId))
                    .willReturn(Collections.emptyList());

            // when
            fcmPushService.sendSideEffectRecordReminder(testMemberId, "부작용 기록을 권장합니다.");

            // then
            verify(fcmDeviceTokenRepository).findByMember_MemberIdAndIsActiveTrue(testMemberId);
        }

        @Test
        @DisplayName("성공: 일기 작성 알림 발송")
        void sendDiaryReminder() {
            // given
            given(notificationSettingsRepository.findByMember_MemberId(testMemberId))
                    .willReturn(Optional.of(testSettings));
            given(fcmDeviceTokenRepository.findByMember_MemberIdAndIsActiveTrue(testMemberId))
                    .willReturn(Collections.emptyList());

            // when
            fcmPushService.sendDiaryReminder(testMemberId);

            // then
            verify(fcmDeviceTokenRepository).findByMember_MemberIdAndIsActiveTrue(testMemberId);
        }

        @Test
        @DisplayName("성공: 부정적 기분 지속 알림 발송")
        void sendNegativeMoodAlert() {
            // given
            given(notificationSettingsRepository.findByMember_MemberId(testMemberId))
                    .willReturn(Optional.of(testSettings));
            given(fcmDeviceTokenRepository.findByMember_MemberIdAndIsActiveTrue(testMemberId))
                    .willReturn(Collections.emptyList());

            // when
            fcmPushService.sendNegativeMoodAlert(testMemberId, 3);

            // then
            verify(fcmDeviceTokenRepository).findByMember_MemberIdAndIsActiveTrue(testMemberId);
        }
    }

    @Nested
    @DisplayName("설정 확인")
    class SettingsCheck {

        @Test
        @DisplayName("성공: 설정이 없으면 기본값(활성화)으로 처리")
        void shouldUseDefaultWhenNoSettings() {
            // given
            given(notificationSettingsRepository.findByMember_MemberId(testMemberId))
                    .willReturn(Optional.empty());
            given(fcmDeviceTokenRepository.findByMember_MemberIdAndIsActiveTrue(testMemberId))
                    .willReturn(Collections.emptyList());

            // when
            CompletableFuture<Void> result = fcmPushService.sendPushNotification(
                    testMemberId, "제목", "내용", null);

            // then
            assertThat(result).isNotNull();
            verify(fcmDeviceTokenRepository).findByMember_MemberIdAndIsActiveTrue(testMemberId);
        }

        @Test
        @DisplayName("성공: 조용한 시간이 자정을 넘어가는 경우 처리")
        void shouldHandleQuietHoursAcrossMidnight() {
            // given
            NotificationSettings midnightSettings = new NotificationSettings(testMember);
            midnightSettings.updateSettings(null, null, null, null, null, true, LocalTime.of(22, 0), LocalTime.of(6, 0));

            given(notificationSettingsRepository.findByMember_MemberId(testMemberId))
                    .willReturn(Optional.of(midnightSettings));

            // when
            CompletableFuture<Void> result = fcmPushService.sendPushNotification(
                    testMemberId, "제목", "내용", null);

            // then
            assertThat(result).isNotNull();
        }
    }
}
