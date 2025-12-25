package com.Hamalog.domain.notification;

import com.Hamalog.domain.member.Member;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * FCM 디바이스 토큰 엔티티
 * 사용자의 푸시 알림 수신 디바이스를 관리합니다.
 */
@Entity
@Table(name = "fcm_device_token",
        indexes = @Index(name = "idx_fcm_device_token_member", columnList = "member_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FcmDeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fcm_device_token_id")
    private Long fcmDeviceTokenId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // FCM 토큰 값
    @Column(name = "token", nullable = false, length = 500)
    private String token;

    // 디바이스 타입 (ANDROID, IOS, WEB)
    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 20)
    private DeviceType deviceType;

    // 디바이스 이름 (선택, 예: "Galaxy S24", "iPhone 15")
    @Column(name = "device_name", length = 100)
    private String deviceName;

    // 토큰 활성화 여부
    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    // 마지막 사용 시간 (알림 전송 성공 시 갱신)
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public FcmDeviceToken(Member member, String token, DeviceType deviceType, String deviceName) {
        this.member = member;
        this.token = token;
        this.deviceType = deviceType;
        this.deviceName = deviceName;
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 토큰 갱신
     */
    public void updateToken(String newToken) {
        this.token = newToken;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 마지막 사용 시간 갱신
     */
    public void markAsUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }

    /**
     * 토큰 비활성화 (만료 또는 등록 해제)
     */
    public void deactivate() {
        this.isActive = false;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 토큰 재활성화
     */
    public void activate() {
        this.isActive = true;
        this.updatedAt = LocalDateTime.now();
    }
}
