package com.Hamalog.service.notification;

import com.Hamalog.domain.events.DomainEventPublisher;
import com.Hamalog.domain.events.notification.NotificationSettingsUpdated;
import com.Hamalog.domain.member.Member;
import com.Hamalog.domain.notification.FcmDeviceToken;
import com.Hamalog.domain.notification.NotificationSettings;
import com.Hamalog.dto.notification.request.FcmTokenRegisterRequest;
import com.Hamalog.dto.notification.request.NotificationSettingsUpdateRequest;
import com.Hamalog.dto.notification.response.FcmDeviceTokenListResponse;
import com.Hamalog.dto.notification.response.FcmDeviceTokenResponse;
import com.Hamalog.dto.notification.response.NotificationSettingsResponse;
import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.repository.member.MemberRepository;
import com.Hamalog.repository.notification.FcmDeviceTokenRepository;
import com.Hamalog.repository.notification.NotificationSettingsRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림 설정 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class NotificationSettingsService {

    private final NotificationSettingsRepository notificationSettingsRepository;
    private final FcmDeviceTokenRepository fcmDeviceTokenRepository;
    private final MemberRepository memberRepository;
    private final DomainEventPublisher domainEventPublisher;

    /**
     * 알림 설정 조회
     * 설정이 없으면 기본값으로 생성하여 반환
     */
    @Cacheable(value = "notificationSettings", key = "#memberId")
    public NotificationSettingsResponse getSettings(Long memberId) {
        NotificationSettings settings = notificationSettingsRepository.findByMember_MemberId(memberId)
                .orElseGet(() -> createDefaultSettings(memberId));

        return NotificationSettingsResponse.from(settings);
    }

    /**
     * 기본 알림 설정 생성
     */
    @Transactional
    public NotificationSettings createDefaultSettings(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        NotificationSettings settings = new NotificationSettings(member);
        return notificationSettingsRepository.save(settings);
    }

    /**
     * 알림 설정 수정
     */
    @Transactional
    @CacheEvict(value = "notificationSettings", key = "#memberId")
    public NotificationSettingsResponse updateSettings(Long memberId, NotificationSettingsUpdateRequest request) {
        // 1. 설정 조회 또는 생성
        NotificationSettings settings = notificationSettingsRepository.findByMember_MemberId(memberId)
                .orElseGet(() -> createDefaultSettings(memberId));

        // 2. 설정 업데이트
        settings.updateSettings(
                request.pushEnabled(),
                request.medicationReminderEnabled(),
                request.medicationReminderMinutesBefore(),
                request.diaryReminderEnabled(),
                request.diaryReminderTime(),
                request.quietHoursEnabled(),
                request.quietHoursStart(),
                request.quietHoursEnd()
        );

        // 3. 저장 및 응답 반환
        NotificationSettings saved = notificationSettingsRepository.save(settings);
        log.info("알림 설정 업데이트 완료: memberId={}", memberId);

        // 4. 도메인 이벤트 발행
        Member member = settings.getMember();
        NotificationSettingsUpdated event = new NotificationSettingsUpdated(
                saved.getNotificationSettingsId(),
                memberId,
                member.getLoginId(),
                saved.isPushEnabled(),
                saved.isMedicationReminderEnabled(),
                saved.getMedicationReminderMinutesBefore(),
                saved.isDiaryReminderEnabled(),
                saved.getDiaryReminderTime(),
                saved.isQuietHoursEnabled(),
                saved.getQuietHoursStart(),
                saved.getQuietHoursEnd()
        );
        domainEventPublisher.publish(event);
        log.debug("Published NotificationSettingsUpdated event for memberId: {}", memberId);

        return NotificationSettingsResponse.from(saved);
    }

    /**
     * FCM 토큰 등록 (신규 등록 또는 기존 갱신)
     */
    @Transactional
    public FcmDeviceTokenResponse registerFcmToken(Long memberId, FcmTokenRegisterRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 기존 토큰이 있으면 갱신, 없으면 신규 생성
        FcmDeviceToken deviceToken = fcmDeviceTokenRepository.findByToken(request.token())
                .map(existing -> {
                    // 다른 사용자의 토큰이면 기존 것을 비활성화하고 새로 생성
                    if (!existing.getMember().getMemberId().equals(memberId)) {
                        existing.deactivate();
                        fcmDeviceTokenRepository.save(existing);
                        return createNewFcmToken(member, request);
                    }
                    // 같은 사용자면 활성화
                    existing.activate();
                    return fcmDeviceTokenRepository.save(existing);
                })
                .orElseGet(() -> createNewFcmToken(member, request));

        log.info("FCM 토큰 등록 완료: memberId={}, deviceType={}", memberId, request.deviceType());

        return FcmDeviceTokenResponse.from(deviceToken);
    }

    private FcmDeviceToken createNewFcmToken(Member member, FcmTokenRegisterRequest request) {
        FcmDeviceToken newToken = new FcmDeviceToken(
                member,
                request.token(),
                request.deviceType(),
                request.deviceName()
        );
        return fcmDeviceTokenRepository.save(newToken);
    }

    /**
     * 사용자의 등록된 디바이스 목록 조회
     */
    public FcmDeviceTokenListResponse getDeviceTokens(Long memberId) {
        List<FcmDeviceTokenResponse> devices = fcmDeviceTokenRepository.findByMember_MemberId(memberId)
                .stream()
                .map(FcmDeviceTokenResponse::from)
                .toList();

        return FcmDeviceTokenListResponse.of(memberId, devices);
    }

    /**
     * FCM 토큰 비활성화 (로그아웃, 디바이스 삭제)
     */
    @Transactional
    public void deactivateFcmToken(Long memberId, String token) {
        fcmDeviceTokenRepository.findByMember_MemberIdAndToken(memberId, token)
                .ifPresent(deviceToken -> {
                    deviceToken.deactivate();
                    fcmDeviceTokenRepository.save(deviceToken);
                    log.info("FCM 토큰 비활성화: memberId={}", memberId);
                });
    }

    /**
     * 특정 디바이스 토큰 삭제
     */
    @Transactional
    public void deleteDeviceToken(Long memberId, Long tokenId) {
        FcmDeviceToken token = fcmDeviceTokenRepository.findById(tokenId)
                .orElseThrow(() -> new CustomException(ErrorCode.FCM_TOKEN_NOT_FOUND));

        // 소유자 검증
        if (!token.getMember().getMemberId().equals(memberId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        fcmDeviceTokenRepository.delete(token);
        log.info("FCM 디바이스 토큰 삭제: memberId={}, tokenId={}", memberId, tokenId);
    }

    /**
     * 모든 FCM 토큰 비활성화 (전체 로그아웃)
     */
    @Transactional
    public void deactivateAllFcmTokens(Long memberId) {
        fcmDeviceTokenRepository.deactivateAllByMemberId(memberId, java.time.LocalDateTime.now());
        log.info("모든 FCM 토큰 비활성화: memberId={}", memberId);
    }
}
