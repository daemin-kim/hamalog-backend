package com.Hamalog.dto.medication;

public record MedicationRecordUpdateRequest(
        Boolean isTake

) {
    // This record can be used to encapsulate the data needed for updating a medication record.
    // It includes fields for medication name, dosage, frequency, and any additional notes.
}
