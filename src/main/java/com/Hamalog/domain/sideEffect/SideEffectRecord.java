package com.Hamalog.domain.sideEffect;

import com.Hamalog.domain.member.Member;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
public class SideEffectRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sideEffectRecordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private LocalDateTime createdAt;

}
