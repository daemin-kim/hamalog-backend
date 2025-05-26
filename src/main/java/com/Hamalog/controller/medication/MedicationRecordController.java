package com.Hamalog.controller.medication;

import com.Hamalog.domain.medication.MedicationRecord;
import com.Hamalog.dto.medication.request.MedicationRecordUpdateRequest;
import com.Hamalog.dto.medication.request.MedicationRecordCreateRequest;
import com.Hamalog.service.medication.MedicationRecordService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/medication-record")
public class MedicationRecordController {

    private final MedicationRecordService medicationRecordService;

    public MedicationRecordController(MedicationRecordService medicationRecordService) {
        this.medicationRecordService = medicationRecordService;
    }

    @GetMapping("/list/{medication-schedule-id}")
    public ResponseEntity<List<MedicationRecord>> getMedicationRecords(
            @PathVariable("medication-schedule-id") Long medicationScheduleId
    ) {
        List<MedicationRecord> medicationRecords = medicationRecordService
                .getMedicationRecords(medicationScheduleId);
        return ResponseEntity.ok(medicationRecords);
    }

    @GetMapping("/{medication-record-id}")
    public ResponseEntity<MedicationRecord> getMedicationRecordById(
            @PathVariable("medication-record-id") Long medicationRecordId
    ) {
        MedicationRecord medicationRecord = medicationRecordService
                .getMedicationRecord(medicationRecordId);
        return ResponseEntity.ok(medicationRecord);
    }

    @PostMapping
    public ResponseEntity<MedicationRecord> createMedicationRecord(
            @RequestBody MedicationRecordCreateRequest medicationRecordCreateRequest
    ) {
        MedicationRecord createdMedicationRecord = medicationRecordService
                .createMedicationRecord(medicationRecordCreateRequest);
        return ResponseEntity.status(201).body(createdMedicationRecord);
    }

    @PutMapping("/{medication-record-id}")
    public ResponseEntity<MedicationRecord> updateMedicationRecord(
            @PathVariable("medication-record-id") Long medicationRecordId,
            @RequestBody MedicationRecordUpdateRequest medicationRecordUpdateRequest
    ) {
        MedicationRecord updatedMedicationRecord = medicationRecordService
                .updateMedicationRecord(medicationRecordId, medicationRecordUpdateRequest);
        return ResponseEntity.ok(updatedMedicationRecord);
    }

    @DeleteMapping("/{medication-record-id}")
    public ResponseEntity<Void> deleteMedicationRecord(
            @PathVariable("medication-record-id") Long medicationRecordId
    ) {
        medicationRecordService.deleteMedicationRecord(medicationRecordId);
        return ResponseEntity.noContent().build();
    }
}
