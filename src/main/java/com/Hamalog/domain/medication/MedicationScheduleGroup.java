package com.Hamalog.domain.medication;

import com.Hamalog.domain.member.Member;
import jakarta.persistence.*;

@Entity
public class MedicationScheduleGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long medicationScheduleGroupId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(length = 20, nullable = false)
    private String name;

}
