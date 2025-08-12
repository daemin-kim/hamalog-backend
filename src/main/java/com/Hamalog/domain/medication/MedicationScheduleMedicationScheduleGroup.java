package com.Hamalog.domain.medication;

import com.Hamalog.domain.idClass.MedicationScheduleMedicationScheduleGroupId;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
@IdClass(MedicationScheduleMedicationScheduleGroupId.class)
public class MedicationScheduleMedicationScheduleGroup {
    @Id
    @Column(name = "medication_schedule_id")
    private Long medicationScheduleId;

    @Id
    @Column(name = "medication_schedule_group_id")
    private Long medicationScheduleGroupId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_schedule_id", insertable = false, updatable = false)
    private MedicationSchedule medicationSchedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_schedule_group_id", insertable = false, updatable = false)
    private MedicationScheduleGroup medicationScheduleGroup;

}
