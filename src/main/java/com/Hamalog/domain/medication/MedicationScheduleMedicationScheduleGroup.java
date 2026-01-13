package com.Hamalog.domain.medication;

import com.Hamalog.domain.idClass.MedicationScheduleMedicationScheduleGroupId;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(MedicationScheduleMedicationScheduleGroupId.class)
@EqualsAndHashCode(of = {"medicationScheduleId", "medicationScheduleGroupId"})
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

    /**
     * 스케줄-그룹 연결 생성자
     * 애그리게이트 루트(MedicationScheduleGroup)를 통해서만 호출되어야 함
     */
    public MedicationScheduleMedicationScheduleGroup(
            Long medicationScheduleId,
            Long medicationScheduleGroupId,
            MedicationSchedule medicationSchedule,
            MedicationScheduleGroup medicationScheduleGroup
    ) {
        this.medicationScheduleId = medicationScheduleId;
        this.medicationScheduleGroupId = medicationScheduleGroupId;
        this.medicationSchedule = medicationSchedule;
        this.medicationScheduleGroup = medicationScheduleGroup;
    }
}
