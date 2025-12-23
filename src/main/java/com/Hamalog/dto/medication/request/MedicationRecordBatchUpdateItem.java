package com.Hamalog.dto.medication.request;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
public record MedicationRecordBatchUpdateItem(
    @NotNull(message = "복약 기록 ID는 필수입니다")
    Long medicationRecordId,
    Boolean isTakeMedication,
    LocalDateTime realTakeTime
) {}
