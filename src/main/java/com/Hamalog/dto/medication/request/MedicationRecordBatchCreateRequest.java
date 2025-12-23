package com.Hamalog.dto.medication.request;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
public record MedicationRecordBatchCreateRequest(
    @NotEmpty(message = "복약 기록 목록은 비어있을 수 없습니다")
    @Size(max = 100, message = "한 번에 최대 100개까지 생성 가능합니다")
    @Valid
    List<MedicationRecordCreateRequest> records
) {}
