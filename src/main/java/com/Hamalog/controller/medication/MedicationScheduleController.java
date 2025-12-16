package com.Hamalog.controller.medication;

import com.Hamalog.domain.medication.MedicationSchedule;
import com.Hamalog.dto.medication.request.MedicationScheduleCreateRequest;
import com.Hamalog.dto.medication.request.MedicationScheduleUpdateRequest;
import com.Hamalog.dto.medication.response.MedicationScheduleListResponse;
import com.Hamalog.dto.medication.response.MedicationScheduleResponse;
import com.Hamalog.security.annotation.RequireResourceOwnership;
import com.Hamalog.service.medication.MedicationScheduleService;
import com.Hamalog.validation.ValidImage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Medication Schedule API", description = "복약 스케줄 관련 CRUD API")
@RestController
@RequestMapping("/medication-schedule")
public class MedicationScheduleController {

    private final MedicationScheduleService medicationScheduleService;

    public MedicationScheduleController(
            MedicationScheduleService medicationScheduleService
    ) {
        this.medicationScheduleService = medicationScheduleService;
    }

    @Operation(summary = "회원의 복약 스케줄 목록 조회",
            description = "회원 ID를 통해 해당 사용자의 모든 복약 스케줄을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "복약 스케줄 목록 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MedicationScheduleListResponse.class))),
            @ApiResponse(responseCode = "404", description = "회원이 존재하지 않음", content = @Content)
    })
    @GetMapping("/list/{member-id}")
    @RequireResourceOwnership(
        resourceType = RequireResourceOwnership.ResourceType.MEDICATION_SCHEDULE_BY_MEMBER,
        paramName = "member-id",
        strategy = RequireResourceOwnership.OwnershipStrategy.DIRECT
    )
    public ResponseEntity<MedicationScheduleListResponse> getMedicationSchedules(
            @Parameter(description = "회원 ID", required = true, example = "1", in = ParameterIn.PATH)
            @PathVariable("member-id") Long memberId,
            @Parameter(description = "페이지네이션 정보 (최대 100개)", required = false)
            @PageableDefault(size = 20, page = 0) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails
    ){
        // 페이지 크기 제한 (DoS 방지)
        if (pageable.getPageSize() > 100) {
            pageable = PageRequest.of(pageable.getPageNumber(), 100, pageable.getSort());
        }

        Page<MedicationSchedule> medicationSchedules = medicationScheduleService.getMedicationSchedules(memberId, pageable);
        Page<MedicationScheduleResponse> medicationScheduleResponses = medicationSchedules.map(MedicationScheduleResponse::from);
        MedicationScheduleListResponse response = MedicationScheduleListResponse.from(medicationScheduleResponses);
        return ResponseEntity.ok(response);
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
    @RequireResourceOwnership(
        resourceType = RequireResourceOwnership.ResourceType.MEDICATION_SCHEDULE,
        paramName = "medication-schedule-id",
        strategy = RequireResourceOwnership.OwnershipStrategy.DIRECT
    )
    public ResponseEntity<MedicationScheduleResponse> getMedicationScheduleById(
            @Parameter(description = "복약 스케줄 ID", required = true, example = "1", in = ParameterIn.PATH)
            @PathVariable("medication-schedule-id") Long medicationScheduleId,
            @AuthenticationPrincipal UserDetails userDetails
    ){
        MedicationSchedule medicationSchedule = medicationScheduleService.getMedicationSchedule(medicationScheduleId);
        return ResponseEntity.ok(MedicationScheduleResponse.from(medicationSchedule));
    }

    @Operation(summary = "복약 스케줄 생성",
            description = "복약 스케줄 정보를 입력받아 새 스케줄을 생성합니다. 이미지 파일은 multipart로 업로드 가능하며 선택 사항입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "복약 스케줄 생성 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MedicationScheduleResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터", content = @Content),
            @ApiResponse(responseCode = "404", description = "회원이 존재하지 않음", content = @Content)
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequireResourceOwnership(
        resourceType = RequireResourceOwnership.ResourceType.MEDICATION_SCHEDULE_BY_MEMBER,
        paramName = "data",
        source = RequireResourceOwnership.ParameterSource.REQUEST_BODY,
        bodyField = "memberId",
        strategy = RequireResourceOwnership.OwnershipStrategy.DIRECT
    )
    public ResponseEntity<MedicationScheduleResponse> createMedicationSchedule(
            @Parameter(description = "복약 스케줄 생성 요청 데이터", required = true)
            @RequestPart("data") @Valid MedicationScheduleCreateRequest medicationScheduleCreateRequest,

            @Parameter(description = "복약 스케줄에 첨부할 이미지 파일 (선택 사항, 최대 5MB)", required = false, content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE))
            @RequestPart(value = "image", required = false)
            @ValidImage(maxSize = 5 * 1024 * 1024)
            MultipartFile image,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        MedicationSchedule createdMedicationSchedule = medicationScheduleService.createMedicationSchedule(medicationScheduleCreateRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(MedicationScheduleResponse.from(createdMedicationSchedule));
    }

    @Operation(summary = "복약 스케줄 수정",
            description = "기존 복약 스케줄을 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "복약 스케줄 수정 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MedicationScheduleResponse.class))),
            @ApiResponse(responseCode = "404", description = "복약 스케줄이 존재하지 않음", content = @Content)
    })
    @PutMapping("/{medication-schedule-id}")
    @RequireResourceOwnership(
        resourceType = RequireResourceOwnership.ResourceType.MEDICATION_SCHEDULE,
        paramName = "medication-schedule-id",
        strategy = RequireResourceOwnership.OwnershipStrategy.DIRECT
    )
    public ResponseEntity<MedicationScheduleResponse> updateMedicationSchedule(
            @Parameter(description = "수정할 복약 스케줄 ID", required = true, example = "1", in = ParameterIn.PATH)
            @PathVariable("medication-schedule-id") Long medicationScheduleId,

            @Parameter(description = "복약 스케줄 수정 요청 데이터", required = true)
            @RequestBody @Valid MedicationScheduleUpdateRequest medicationScheduleUpdateRequest,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        MedicationSchedule updatedMedicationSchedule = medicationScheduleService.updateMedicationSchedule(medicationScheduleId, medicationScheduleUpdateRequest);
        return ResponseEntity.ok(MedicationScheduleResponse.from(updatedMedicationSchedule));
    }

    @Operation(summary = "복약 스케줄 삭제",
            description = "복약 스케줄 ID를 통해 해당 스케줄을 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "복약 스케줄 삭제 성공", content = @Content),
            @ApiResponse(responseCode = "404", description = "복약 스케줄이 존재하지 않음", content = @Content)
    })
    @DeleteMapping("/{medication-schedule-id}")
    @RequireResourceOwnership(
        resourceType = RequireResourceOwnership.ResourceType.MEDICATION_SCHEDULE,
        paramName = "medication-schedule-id",
        strategy = RequireResourceOwnership.OwnershipStrategy.DIRECT
    )
    public ResponseEntity<Void> deleteMedicationSchedule(
            @Parameter(description = "삭제할 복약 스케줄 ID", required = true, example = "1", in = ParameterIn.PATH)
            @PathVariable("medication-schedule-id") Long medicationScheduleId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        medicationScheduleService.deleteMedicationSchedule(medicationScheduleId);
        return ResponseEntity.noContent().build();
    }

}
