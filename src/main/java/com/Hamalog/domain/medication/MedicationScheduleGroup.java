package com.Hamalog.domain.medication;

import com.Hamalog.domain.member.Member;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MedicationScheduleGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "medication_schedule_group_id")
    private Long medicationScheduleGroupId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(length = 20, nullable = false)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(length = 7)
    private String color;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public MedicationScheduleGroup(Member member, String name, String description, String color) {
        this.member = member;
        this.name = name;
        this.description = description;
        this.color = color;
    }

    public void update(String name, String description, String color) {
        this.name = name;
        this.description = description;
        this.color = color;
    }
}
