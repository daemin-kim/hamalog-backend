package com.Hamalog.controller.medication;

import com.Hamalog.domain.medication.MedicationSchedule;
import com.Hamalog.dto.medication.request.MedicationScheduleCreateRequest;
import com.Hamalog.dto.medication.request.MedicationScheduleUpdateRequest;
import com.Hamalog.dto.medication.response.MedicationScheduleResponse;
import com.Hamalog.service.medication.MedicationScheduleService;
import com.Hamalog.service.medication.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

@Tag(name = "Medication Schedule API", description = "복약 스케줄 관련 CRUD API")
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

    @Operation(summary = "회원의 복약 스케줄 목록 조회",
            description = "회원 ID를 통해 해당 사용자의 모든 복약 스케줄을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "복약 스케줄 목록 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MedicationSchedule.class))),
            @ApiResponse(responseCode = "404", description = "회원이 존재하지 않음", content = @Content)
    })
    @GetMapping("/list/{member-id}")
    public ResponseEntity<org.springframework.data.domain.Page<MedicationSchedule>> getMedicationSchedules(
            @Parameter(description = "회원 ID", required = true, example = "1", in = ParameterIn.PATH)
            @PathVariable("member-id") Long memberId,
            @Parameter(description = "페이지네이션 정보", required = false)
            org.springframework.data.domain.Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails
    ){
        // Authorization check: ensure user can only access their own data
        String currentLoginId = userDetails.getUsername();
        if (!medicationScheduleService.isOwner(memberId, currentLoginId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        org.springframework.data.domain.Page<MedicationSchedule> medicationSchedules = medicationScheduleService.getMedicationSchedules(memberId, pageable);
        return ResponseEntity.ok(medicationSchedules);
    }

    @Operation(summary = "특정 복약 스케줄 상세 조회",
            description = "복약 스케줄 ID로 복약 스케줄 상세 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "복약 스케줄 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MedicationSchedule.class))),
            @ApiResponse(responseCode = "404", description = "복약 스케줄이 존재하지 않음", content = @Content)
    })
    @GetMapping("/{medication-schedule-id}")
    public ResponseEntity<MedicationScheduleResponse> getMedicationScheduleById(
            @Parameter(description = "복약 스케줄 ID", required = true, example = "1", in = ParameterIn.PATH)
            @PathVariable("medication-schedule-id") Long medicationScheduleId,
            @AuthenticationPrincipal UserDetails userDetails
    ){
        MedicationSchedule medicationSchedule = medicationScheduleService.getMedicationSchedule(medicationScheduleId);
        
        // Authorization check: ensure user can only access their own data
        String currentLoginId = userDetails.getUsername();
        if (!medicationScheduleService.isOwner(medicationSchedule.getMember().getMemberId(), currentLoginId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(MedicationScheduleResponse.from(medicationSchedule));
    }

    @Operation(summary = "복약 스케줄 생성",
            description = "복약 스케줄 정보를 입력받아 새 스케줄을 생성합니다. 이미지 파일은 multipart로 업로드 가능하며 선택 사항입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "복약 스케줄 생성 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MedicationSchedule.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터", content = @Content),
            @ApiResponse(responseCode = "404", description = "회원이 존재하지 않음", content = @Content)
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MedicationSchedule> createMedicationSchedule(
            @Parameter(description = "복약 스케줄 생성 요청 데이터", required = true)
            @RequestPart("data") @jakarta.validation.Valid MedicationScheduleCreateRequest medicationScheduleCreateRequest,

            @Parameter(description = "복약 스케줄에 첨부할 이미지 파일 (선택 사항)", required = false, content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE))
            @RequestPart(value = "image", required = false) MultipartFile image,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        // Authorization check: ensure user can only create schedules for themselves
        String currentLoginId = userDetails.getUsername();
        if (!medicationScheduleService.isOwner(medicationScheduleCreateRequest.memberId(), currentLoginId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        // Note: Image handling removed as imagePath field doesn't exist in database schema
        MedicationSchedule createdMedicationSchedule = medicationScheduleService.createMedicationSchedule(medicationScheduleCreateRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdMedicationSchedule);
    }

    @Operation(summary = "복약 스케줄 수정",
            description = "기존 복약 스케줄을 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "복약 스케줄 수정 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MedicationSchedule.class))),
            @ApiResponse(responseCode = "404", description = "복약 스케줄이 존재하지 않음", content = @Content)
    })
    @PutMapping("/{medication-schedule-id}")
    public ResponseEntity<MedicationSchedule> updateMedicationSchedule(
            @Parameter(description = "수정할 복약 스케줄 ID", required = true, example = "1", in = ParameterIn.PATH)
            @PathVariable("medication-schedule-id") Long medicationScheduleId,

            @Parameter(description = "복약 스케줄 수정 요청 데이터", required = true)
            @RequestBody @jakarta.validation.Valid MedicationScheduleUpdateRequest medicationScheduleUpdateRequest,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        MedicationSchedule medicationSchedule = medicationScheduleService.getMedicationSchedule(medicationScheduleId);
        
        // Authorization check: ensure user can only update their own data
        String currentLoginId = userDetails.getUsername();
        if (!medicationScheduleService.isOwner(medicationSchedule.getMember().getMemberId(), currentLoginId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        MedicationSchedule updatedMedicationSchedule = medicationScheduleService.updateMedicationSchedule(medicationScheduleId, medicationScheduleUpdateRequest);
        return ResponseEntity.ok(updatedMedicationSchedule);
    }

    @Operation(summary = "복약 스케줄 삭제",
            description = "복약 스케줄 ID를 통해 해당 스케줄을 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "복약 스케줄 삭제 성공", content = @Content),
            @ApiResponse(responseCode = "404", description = "복약 스케줄이 존재하지 않음", content = @Content)
    })
    @DeleteMapping("/{medication-schedule-id}")
    public ResponseEntity<Void> deleteMedicationSchedule(
            @Parameter(description = "삭제할 복약 스케줄 ID", required = true, example = "1", in = ParameterIn.PATH)
            @PathVariable("medication-schedule-id") Long medicationScheduleId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        MedicationSchedule medicationSchedule = medicationScheduleService.getMedicationSchedule(medicationScheduleId);
        
        // Authorization check: ensure user can only delete their own data
        String currentLoginId = userDetails.getUsername();
        if (!medicationScheduleService.isOwner(medicationSchedule.getMember().getMemberId(), currentLoginId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        medicationScheduleService.deleteMedicationSchedule(medicationScheduleId);
        return ResponseEntity.noContent().build();
    }

}
