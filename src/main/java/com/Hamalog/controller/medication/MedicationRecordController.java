package com.Hamalog.controller.medication;

import com.Hamalog.domain.medication.MedicationRecord;
import com.Hamalog.dto.medication.request.MedicationRecordCreateRequest;
import com.Hamalog.dto.medication.request.MedicationRecordUpdateRequest;
import com.Hamalog.dto.medication.response.MedicationRecordResponse;
import com.Hamalog.service.medication.MedicationRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Tag(name = "Medication Record API", description = "복약 기록(CRUD) 관련 API")
@RestController
@RequestMapping("/medication-record")
public class MedicationRecordController {

    private final MedicationRecordService medicationRecordService;

    public MedicationRecordController(MedicationRecordService medicationRecordService) {
        this.medicationRecordService = medicationRecordService;
    }

    @Operation(
            summary = "특정 복약 스케줄의 복약 기록 목록 조회",
            description = "주어진 복약 스케줄 ID에 대한 모든 복약 기록을 리스트로 반환합니다. 최신순 또는 필터링은 서비스 로직에 따라 처리 가능."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "복약 기록 목록 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MedicationRecordResponse.class))),
            @ApiResponse(responseCode = "404", description = "복약 스케줄을 찾을 수 없음",
                    content = @Content)
    })
    @GetMapping("/list/{medication-schedule-id}")
    public ResponseEntity<List<MedicationRecordResponse>> getMedicationRecords(
            @Parameter(in = ParameterIn.PATH, description = "복약 스케줄 ID", required = true, example = "1")
            @PathVariable("medication-schedule-id") Long medicationScheduleId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        // Authorization check: ensure user can only access their own medication records
        String currentLoginId = userDetails.getUsername();
        if (!medicationRecordService.isOwnerOfSchedule(medicationScheduleId, currentLoginId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        List<MedicationRecord> medicationRecords = medicationRecordService.getMedicationRecords(medicationScheduleId);
        List<MedicationRecordResponse> medicationRecordResponses = medicationRecords.stream()
                .map(MedicationRecordResponse::from)
                .toList();
        return ResponseEntity.ok(medicationRecordResponses);
    }

    @Operation(
            summary = "복약 기록 상세 조회",
            description = "복약 기록 ID를 이용해 특정 복약 기록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "복약 기록 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MedicationRecordResponse.class))),
            @ApiResponse(responseCode = "404", description = "복약 기록을 찾을 수 없음",
                    content = @Content)
    })
    @GetMapping("/{medication-record-id}")
    public ResponseEntity<MedicationRecordResponse> getMedicationRecordById(
            @Parameter(in = ParameterIn.PATH, description = "복약 기록 ID", required = true, example = "1")
            @PathVariable("medication-record-id") Long medicationRecordId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        // Authorization check: ensure user can only access their own medication records
        String currentLoginId = userDetails.getUsername();
        if (!medicationRecordService.isOwnerOfRecord(medicationRecordId, currentLoginId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        MedicationRecord medicationRecord = medicationRecordService.getMedicationRecord(medicationRecordId);
        return ResponseEntity.ok(MedicationRecordResponse.from(medicationRecord));
    }

    @Operation(
            summary = "복약 기록 생성",
            description = "새로운 복약 기록을 생성합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "복약 기록 생성 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MedicationRecordResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터",
                    content = @Content)
    })
    @PostMapping
    public ResponseEntity<MedicationRecordResponse> createMedicationRecord(
            @Parameter(description = "복약 기록 생성 요청 데이터", required = true)
            @Valid @RequestBody MedicationRecordCreateRequest medicationRecordCreateRequest,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        // Authorization check: ensure user can only create records for their own schedules
        String currentLoginId = userDetails.getUsername();
        if (!medicationRecordService.isOwnerOfSchedule(medicationRecordCreateRequest.medicationScheduleId(), currentLoginId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        MedicationRecord createdMedicationRecord = medicationRecordService.createMedicationRecord(medicationRecordCreateRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(MedicationRecordResponse.from(createdMedicationRecord));
    }

    @Operation(
            summary = "복약 기록 수정",
            description = "기존 복약 기록을 수정합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "복약 기록 수정 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MedicationRecordResponse.class))),
            @ApiResponse(responseCode = "404", description = "복약 기록을 찾을 수 없음",
                    content = @Content)
    })
    @PutMapping("/{medication-record-id}")
    public ResponseEntity<MedicationRecordResponse> updateMedicationRecord(
            @Parameter(in = ParameterIn.PATH, description = "수정할 복약 기록 ID", required = true, example = "1")
            @PathVariable("medication-record-id") Long medicationRecordId,

            @Parameter(description = "복약 기록 수정 요청 데이터", required = true)
            @Valid @RequestBody MedicationRecordUpdateRequest medicationRecordUpdateRequest,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        // Authorization check: ensure user can only update their own medication records
        String currentLoginId = userDetails.getUsername();
        if (!medicationRecordService.isOwnerOfRecord(medicationRecordId, currentLoginId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        MedicationRecord updatedMedicationRecord = medicationRecordService.updateMedicationRecord(medicationRecordId, medicationRecordUpdateRequest);
        return ResponseEntity.ok(MedicationRecordResponse.from(updatedMedicationRecord));
    }

    @Operation(
            summary = "복약 기록 삭제",
            description = "복약 기록 ID로 해당 기록을 삭제합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "복약 기록 삭제 성공", content = @Content),
            @ApiResponse(responseCode = "404", description = "복약 기록을 찾을 수 없음", content = @Content)
    })
    @DeleteMapping("/{medication-record-id}")
    public ResponseEntity<Void> deleteMedicationRecord(
            @Parameter(in = ParameterIn.PATH, description = "삭제할 복약 기록 ID", required = true, example = "1")
            @PathVariable("medication-record-id") Long medicationRecordId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        // Authorization check: ensure user can only delete their own medication records
        String currentLoginId = userDetails.getUsername();
        if (!medicationRecordService.isOwnerOfRecord(medicationRecordId, currentLoginId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        medicationRecordService.deleteMedicationRecord(medicationRecordId);
        return ResponseEntity.noContent().build();
    }
}
