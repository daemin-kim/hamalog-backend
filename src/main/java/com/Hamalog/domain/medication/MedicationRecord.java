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
    @Column(name = "medication_record_id")
    private Long medicationRecordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_schedule_id", nullable = false)
    private MedicationSchedule medicationSchedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_time_id", nullable = false)
    private MedicationTime medicationTime;

    @Column(name = "is_take_medication", nullable = false)
    private Boolean isTakeMedication;

    @Column(name = "real_take_time")
    private LocalDateTime realTakeTime;

    @Version
    @Column(name = "version")
    private Long version;

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
