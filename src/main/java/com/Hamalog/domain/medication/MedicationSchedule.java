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
    @Column(name = "medication_schedule_id")
    private Long medicationScheduleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(length = 20, nullable = false)
    private String name;

    @Column(name = "hospital_name", length = 20, nullable = false)
    private String hospitalName;

    @Column(name = "prescription_date", nullable = false)
    private LocalDate prescriptionDate;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Column(name = "start_of_ad", nullable = false)
    private LocalDate startOfAd;

    @Column(name = "prescription_days", nullable = false)
    private Integer prescriptionDays;

    @Column(name = "per_day", nullable = false)
    private Integer perDay;

    @Enumerated(EnumType.STRING)
    @Column(name = "alarm_type", nullable = false)
    private AlarmType alarmType;

    @Version
    @Column(name = "version")
    private Long version;

    public MedicationSchedule(
            Member member,
            String name,
            String hospitalName,
            LocalDate prescriptionDate,
            String memo,
            LocalDate startOfAd,
            Integer prescriptionDays,
            Integer perDay,
            AlarmType alarmType
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
    }

    public void update(
            String name,
            String hospitalName,
            LocalDate prescriptionDate,
            String memo,
            LocalDate startOfAd,
            Integer prescriptionDays,
            Integer perDay,
            AlarmType alarmType
    ) {
        this.name = name;
        this.hospitalName = hospitalName;
        this.prescriptionDate = prescriptionDate;
        this.memo = memo;
        this.startOfAd = startOfAd;
        this.prescriptionDays = prescriptionDays;
        this.perDay = perDay;
        this.alarmType = alarmType;
    }
}
