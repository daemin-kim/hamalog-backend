package com.Hamalog.domain.sideEffect;

import com.Hamalog.domain.member.Member;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
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
