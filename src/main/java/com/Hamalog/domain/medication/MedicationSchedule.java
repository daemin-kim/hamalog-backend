package com.Hamalog.domain.medication;

import com.Hamalog.domain.member.Member;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
public class MedicationSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long medicationScheduleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(length = 20, nullable = false)
    private String name;

    @Column(length = 20, nullable = false)
    private String hospitalName;

    @Column(nullable = false)
    private LocalDate prescriptionDate;

    private String memo;

    @Column(nullable = false)
    private LocalDate startOfAd;

    @Column(nullable = false)
    private Integer prescriptionDays;

    @Column(nullable = false)
    private Integer perDay;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private AlarmType alarmType;

}
