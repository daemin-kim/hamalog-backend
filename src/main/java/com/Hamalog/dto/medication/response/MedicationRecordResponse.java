package com.Hamalog.dto.medication.response;

import com.Hamalog.domain.medication.MedicationRecord;
import java.time.LocalDateTime;

public record MedicationRecordResponse(
        Long medicationRecordId,
        Long medicationScheduleId,
        Long medicationTimeId,
        Boolean isTakeMedication,
        LocalDateTime realTakeTime
) {
    public static MedicationRecordResponse from(MedicationRecord medicationRecord) {
        return new MedicationRecordResponse(
                medicationRecord.getMedicationRecordId(),
                medicationRecord.getMedicationSchedule().getMedicationScheduleId(),
                medicationRecord.getMedicationTime().getMedicationTimeId(),
                medicationRecord.getIsTakeMedication(),
                medicationRecord.getRealTakeTime()
        );
    }
}