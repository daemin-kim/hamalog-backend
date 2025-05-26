package com.Hamalog.dto.medication.request;

import java.time.LocalDateTime;

public record MedicationRecordUpdateRequest(
        Boolean isTakeMedication,
        LocalDateTime realTakeTime
) {}
