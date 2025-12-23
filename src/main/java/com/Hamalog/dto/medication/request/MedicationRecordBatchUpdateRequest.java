package com.Hamalog.dto.medication.request;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
public record MedicationRecordBatchUpdateRequest(
    @NotEmpty(message = "수정할 기록 목록은 비어있을 수 없습니다")
    @Size(max = 100, message = "한 번에 최대 100개까지 수정 가능합니다")
    @Valid
    List<MedicationRecordBatchUpdateItem> records
) {}
