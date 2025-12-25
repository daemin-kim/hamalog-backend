package com.Hamalog.domain.member;

import com.Hamalog.security.encryption.EncryptedLocalDateConverter;
import com.Hamalog.security.encryption.EncryptedStringConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode(of = "memberId")
@ToString(exclude = "password")
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "login_id", length = 100, nullable = false, unique = true)
    @Email(message = "{member.loginId.email}")
    private String loginId;

    @Column(nullable = false, length = 60)
    private String password;

    @Column(length = 15, nullable = false)
    @Size(max = 15, message = "{member.name.size}")
    private String name;

    @Column(name = "phone_number", length = 255, nullable = false)
    @Pattern(regexp = "^010\\d{8}$", message = "{member.phoneNumber.pattern}")
    @Convert(converter = EncryptedStringConverter.class)
    private String phoneNumber;

    @Column(name = "nickname", length = 10, nullable = false)
    @Pattern(regexp = "^[가-힣a-zA-Z]{1,10}$", message = "{member.nickname.pattern}")
    @Size(max = 10, message = "{member.nickname.size}")
    private String nickName;

    @Column(name = "birthday", nullable = false, length = 255)
    @Convert(converter = EncryptedLocalDateConverter.class)
    private LocalDate birth;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "deletion_requested_at")
    private LocalDateTime deletionRequestedAt;

    @Column(name = "deletion_due_at")
    private LocalDateTime deletionDueAt;

    @Column(name = "deletion_scheduled", nullable = false)
    private boolean deletionScheduled;

    @Version
    @Column(name = "version")
    private Long version;

    /**
     * 프로필 정보 수정
     */
    public void updateProfile(String name, String nickName, String phoneNumber, LocalDate birth) {
        if (name != null) {
            this.name = name;
        }
        if (nickName != null) {
            this.nickName = nickName;
        }
        if (phoneNumber != null) {
            this.phoneNumber = phoneNumber;
        }
        if (birth != null) {
            this.birth = birth;
        }
    }

    /**
     * 비밀번호 변경
     */
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public boolean isDeletionScheduled() {
        return deletionScheduled;
    }

    public void scheduleDeletion(LocalDateTime now, int daysUntilDeletion) {
        if (deletionScheduled) {
            return;
        }
        this.deletionScheduled = true;
        this.deletionRequestedAt = now;
        this.deletionDueAt = now.plusDays(daysUntilDeletion);
    }
}
