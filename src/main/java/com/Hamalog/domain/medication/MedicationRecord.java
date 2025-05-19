package com.Hamalog.domain.medication;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
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

}
