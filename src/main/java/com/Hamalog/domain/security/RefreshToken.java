package com.Hamalog.domain.security;

import com.Hamalog.domain.member.Member;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_refresh_tokens_member_id", columnList = "member_id"),
    @Index(name = "idx_refresh_tokens_token_value", columnList = "token_value"),
    @Index(name = "idx_refresh_tokens_expires_at", columnList = "expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 회원 연관관계 (FK 제약조건 적용)
     * 회원 삭제 시 RefreshToken도 함께 관리되어야 함
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

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

    /**
     * 회원 ID 조회 (하위 호환성 유지)
     * @return 회원 ID
     */
    public Long getMemberId() {
        return this.member != null ? this.member.getMemberId() : null;
    }
}
