package com.Hamalog.domain.security;

import com.Hamalog.domain.member.Member;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "login_history", indexes = {
    @Index(name = "idx_login_history_member_id", columnList = "member_id"),
    @Index(name = "idx_login_history_login_time", columnList = "login_time"),
    @Index(name = "idx_login_history_session_id", columnList = "session_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "login_history_id")
    private Long loginHistoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "login_time", nullable = false)
    private LocalDateTime loginTime;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "device_type", length = 50)
    @Enumerated(EnumType.STRING)
    private DeviceType deviceType;

    @Column(name = "login_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private LoginStatus loginStatus;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "logout_time")
    private LocalDateTime logoutTime;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.loginTime == null) {
            this.loginTime = LocalDateTime.now();
        }
    }

    @Builder
    public LoginHistory(Member member, String ipAddress, String userAgent,
                       DeviceType deviceType, LoginStatus loginStatus,
                       String failureReason, String sessionId) {
        this.member = member;
        this.loginTime = LocalDateTime.now();
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.deviceType = deviceType;
        this.loginStatus = loginStatus;
        this.failureReason = failureReason;
        this.sessionId = sessionId;
        this.isActive = loginStatus == LoginStatus.SUCCESS;
    }

    public void deactivateSession() {
        this.isActive = false;
        this.logoutTime = LocalDateTime.now();
    }

    public enum DeviceType {
        MOBILE, DESKTOP, TABLET, UNKNOWN
    }

    public enum LoginStatus {
        SUCCESS, FAILED
    }
}
