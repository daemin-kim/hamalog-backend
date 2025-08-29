package com.Hamalog.domain.member;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    @Column(name = "phone_number", length = 11, nullable = false)
    @Pattern(regexp = "^010\\d{8}$", message = "{member.phoneNumber.pattern}")
    private String phoneNumber;

    @Column(name = "nickname", length = 10, nullable = false)
    @Pattern(regexp = "^[가-힣a-zA-Z]{1,10}$", message = "{member.nickname.pattern}")
    @Size(max = 10, message = "{member.nickname.size}")
    private String nickName;

    @Column(name = "birthday", nullable = false)
    private LocalDate birth;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

}
