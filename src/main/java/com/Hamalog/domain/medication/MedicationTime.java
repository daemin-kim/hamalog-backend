package com.Hamalog.domain.medication;

import jakarta.persistence.*;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MedicationTime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "medication_time_id")
    private Long medicationTimeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_schedule_id", nullable = false)
    private MedicationSchedule medicationSchedule;

    @Column(name = "take_time", nullable = false)
    private LocalTime takeTime;

    @Version
    private Long version;

    public MedicationTime(MedicationSchedule medicationSchedule, LocalTime takeTime) {
        this.medicationSchedule = medicationSchedule;
        this.takeTime = takeTime;
    }

    public void updateTime(LocalTime takeTime) {
        this.takeTime = takeTime;
    }
}
