package com.Hamalog.domain.medication;

import com.Hamalog.domain.member.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor
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

    @Column(length = 500)
    private String imagePath;

    public MedicationSchedule(
            Member member,
            String name,
            String hospitalName,
            LocalDate prescriptionDate,
            String memo,
            LocalDate startOfAd,
            Integer prescriptionDays,
            Integer perDay,
            AlarmType alarmType,
            String imagePath
    ) {
        this.member = member;
        this.name = name;
        this.hospitalName = hospitalName;
        this.prescriptionDate = prescriptionDate;
        this.memo = memo;
        this.startOfAd = startOfAd;
        this.prescriptionDays = prescriptionDays;
        this.perDay = perDay;
        this.alarmType = alarmType;
        this.imagePath = imagePath;
    }

    public void update(
            String name,
            String hospitalName,
            LocalDate prescriptionDate,
            String memo,
            LocalDate startOfAd,
            Integer prescriptionDays,
            Integer perDay,
            AlarmType alarmType,
            String imagePath
    ) {
        this.name = name;
        this.hospitalName = hospitalName;
        this.prescriptionDate = prescriptionDate;
        this.memo = memo;
        this.startOfAd = startOfAd;
        this.prescriptionDays = prescriptionDays;
        this.perDay = perDay;
        this.alarmType = alarmType;
        this.imagePath = imagePath;
    }
}
