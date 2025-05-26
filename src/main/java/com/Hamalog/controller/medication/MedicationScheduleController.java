package com.Hamalog.controller.medication;

import com.Hamalog.domain.medication.MedicationSchedule;
import com.Hamalog.dto.medication.request.MedicationScheduleCreateRequest;
import com.Hamalog.dto.medication.request.MedicationScheduleUpdateRequest;
import com.Hamalog.service.medication.MedicationScheduleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/medication-schedule")
public class MedicationScheduleController {

    private final MedicationScheduleService medicationScheduleService;

    public MedicationScheduleController(MedicationScheduleService medicationScheduleService) {
        this.medicationScheduleService = medicationScheduleService;
    }

    @GetMapping("/list/{member-id}")
    public ResponseEntity<List<MedicationSchedule>> getMedicationSchedules(
            @PathVariable("member-id") Long memberId
    ){
        List<MedicationSchedule> medicationSchedules = medicationScheduleService
                .getMedicationSchedules(memberId);
        return ResponseEntity.ok(medicationSchedules);
    }

    @GetMapping("/{medication-schedule-id}")
    public ResponseEntity<MedicationSchedule> getMedicationScheduleById(
            @PathVariable("medication-schedule-id") Long medicationScheduleId
    ){
        MedicationSchedule medicationSchedule = medicationScheduleService
                .getMedicationSchedule(medicationScheduleId);
        return ResponseEntity.ok(medicationSchedule);
    }

    @PostMapping
    public ResponseEntity<MedicationSchedule> createMedicationSchedule(
            @RequestBody MedicationScheduleCreateRequest medicationScheduleCreateRequest
            ) {
        MedicationSchedule createdMedicationSchedule = medicationScheduleService
                .createMedicationSchedule(medicationScheduleCreateRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdMedicationSchedule);
    }

    @PutMapping("/{medication-schedule-id}")
    public ResponseEntity<MedicationSchedule> updateMedicationSchedule(
            @PathVariable("medication-schedule-id") Long medicationScheduleId,
            @RequestBody MedicationScheduleUpdateRequest medicationScheduleUpdateRequest
    ) {
        MedicationSchedule updatedMedicationSchedule = medicationScheduleService
                .updateMedicationSchedule(medicationScheduleId, medicationScheduleUpdateRequest);
        return ResponseEntity.ok(updatedMedicationSchedule);
    }

    @DeleteMapping("/{medication-schedule-id}")
    public ResponseEntity<Void> deleteMedicationSchedule(
            @PathVariable("medication-schedule-id") Long medicationScheduleId
    ) {
        medicationScheduleService.deleteMedicationSchedule(medicationScheduleId);
        return ResponseEntity.noContent().build();
    }

}
