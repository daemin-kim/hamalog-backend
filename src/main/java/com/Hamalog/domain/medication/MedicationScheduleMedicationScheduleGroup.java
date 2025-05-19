package com.Hamalog.domain.medication;

import com.Hamalog.domain.idClass.MedicationScheduleMedicationScheduleGroupId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

@Entity
@IdClass(MedicationScheduleMedicationScheduleGroupId.class)
public class MedicationScheduleMedicationScheduleGroup {
    @Id
    private Long medicationScheduleId;

    @Id
    private Long medicationScheduleGroupId;

}
