package com.Hamalog.domain.medication;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class MedicationRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long medicationRecordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_schedule_id", nullable = false)
    private MedicationSchedule medicationSchedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_time_id", nullable = false)
    private MedicationTime medicationTime;

    @Column(nullable = false)
    private Boolean isTakeMedication;

    @Column(nullable = false)
    private LocalDateTime realTakeTime;

    public MedicationRecord(
            MedicationSchedule medicationSchedule,
            MedicationTime medicationTime,
            Boolean isTakeMedication,
            LocalDateTime realTakeTime
    ) {
        this.medicationSchedule = medicationSchedule;
        this.medicationTime = medicationTime;
        this.isTakeMedication = isTakeMedication;
        this.realTakeTime = realTakeTime;
    }

    public void update(
            Boolean isTakeMedication,
            LocalDateTime realTakeTime
    ) {
        this.isTakeMedication = isTakeMedication;
        this.realTakeTime = realTakeTime;
    }

}
