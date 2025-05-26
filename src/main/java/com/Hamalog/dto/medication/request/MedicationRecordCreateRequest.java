package com.Hamalog.dto.medication.request;

import java.time.LocalDateTime;

public record MedicationRecordCreateRequest(
        Long medicationScheduleId,
        Long medicationTimeId,
        Boolean isTakeMedication,
        LocalDateTime realTakeTime
) {}
