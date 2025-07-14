package com.Hamalog.controller.medication;

import com.Hamalog.domain.medication.MedicationSchedule;
import com.Hamalog.dto.medication.request.MedicationScheduleCreateRequest;
import com.Hamalog.dto.medication.request.MedicationScheduleUpdateRequest;
import com.Hamalog.service.medication.MedicationScheduleService;
import com.Hamalog.service.medication.FileStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/medication-schedule")
public class MedicationScheduleController {

    private final MedicationScheduleService medicationScheduleService;
    private final FileStorageService fileStorageService;

    public MedicationScheduleController(
            MedicationScheduleService medicationScheduleService,
            FileStorageService fileStorageService
    ) {
        this.medicationScheduleService = medicationScheduleService;
        this.fileStorageService = fileStorageService;
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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MedicationSchedule> createMedicationSchedule(
            @RequestPart("data") MedicationScheduleCreateRequest medicationScheduleCreateRequest,
            @RequestPart(value = "image", required = false) MultipartFile image
            ) {
        String imagePath = null;
        if (image != null && !image.isEmpty()) {
            imagePath = fileStorageService.save(image);
        }
        MedicationSchedule createdMedicationSchedule = medicationScheduleService
                .createMedicationSchedule(medicationScheduleCreateRequest, imagePath);
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
