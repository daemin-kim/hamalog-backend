package com.Hamalog.dto.medication.response;

import java.util.List;

public record MedicationRecordBatchResponse(
    int totalRequested,
    int successCount,
    int failedCount,
    List<MedicationRecordResponse> successRecords,
    List<BatchErrorItem> failedItems
) {
    public record BatchErrorItem(
        int index,
        String reason
    ) {}

    public static MedicationRecordBatchResponse success(List<MedicationRecordResponse> records) {
        return new MedicationRecordBatchResponse(
            records.size(),
            records.size(),
            0,
            records,
            List.of()
        );
    }

    public static MedicationRecordBatchResponse partial(
            int total,
            List<MedicationRecordResponse> successRecords,
            List<BatchErrorItem> failedItems
    ) {
        return new MedicationRecordBatchResponse(
            total,
            successRecords.size(),
            failedItems.size(),
            successRecords,
            failedItems
        );
    }
}

