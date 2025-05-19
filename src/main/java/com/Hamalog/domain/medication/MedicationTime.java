package com.Hamalog.domain.medication;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
public class MedicationTime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long medicationTimeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_schedule_id", nullable = false)
    private MedicationSchedule medicationSchedule;

    @Column(nullable = false)
    private LocalTime time;

}
