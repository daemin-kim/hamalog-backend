package com.Hamalog.domain.security;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_member_id", columnList = "member_id"),
    @Index(name = "idx_token_value", columnList = "token_value"),
    @Index(name = "idx_expires_at", columnList = "expires_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false, unique = true, length = 500)
    private String tokenValue;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private LocalDateTime rotatedAt;

    @Column
    private LocalDateTime lastUsedAt;

    @Column
    private LocalDateTime reuseDetectedAt;

    @Column(length = 255)
    private String reuseClientFingerprint;

    @Column(nullable = false)
    @Builder.Default
    private boolean reuseDetected = false;

    @Column(nullable = false)
    @Builder.Default
    private long reuseCount = 0L;

    @Column(nullable = false)
    @Builder.Default
    private boolean revoked = false;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !revoked && !isExpired() && !reuseDetected;
    }

    public void markReuse(String fingerprint) {
        this.reuseDetected = true;
        this.reuseDetectedAt = LocalDateTime.now();
        this.reuseClientFingerprint = fingerprint;
    }

    public void touchUsage() {
        this.lastUsedAt = LocalDateTime.now();
    }

    public void incrementReuseCounter() {
        this.reuseCount += 1;
    }
}
