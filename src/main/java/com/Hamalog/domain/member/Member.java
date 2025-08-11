package com.Hamalog.domain.member;

import jakarta.persistence.*;
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
    private Long memberId;

    @Column(length = 20, nullable = false, unique = true)
    private String loginId;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(length = 13, nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private LocalDate birth;

    @Column(nullable = false)
    private LocalDateTime createdAt;

}
