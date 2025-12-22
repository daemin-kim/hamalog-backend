package com.Hamalog.controller.medication;

import com.Hamalog.config.ApiVersion;
import com.Hamalog.dto.medication.request.MedicationTimeCreateRequest;
import com.Hamalog.dto.medication.request.MedicationTimeUpdateRequest;
import com.Hamalog.dto.medication.response.MedicationTimeResponse;
import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.security.CustomUserDetails;
import com.Hamalog.security.annotation.RequireResourceOwnership;
import com.Hamalog.service.medication.MedicationTimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Medication Time API", description = "복약 알림 시간 관련 CRUD API")
@RestController
@RequiredArgsConstructor
public class MedicationTimeController {

    private final MedicationTimeService medicationTimeService;

    @Operation(summary = "스케줄별 알림 시간 목록 조회",
            description = "특정 복약 스케줄의 모든 알림 시간을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "알림 시간 목록 조회 성공",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = MedicationTimeResponse.class)))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content),
            @ApiResponse(responseCode = "404", description = "복약 스케줄을 찾을 수 없음", content = @Content)
    })
    @GetMapping(ApiVersion.MEDICATION_SCHEDULE + "/{schedule-id}/times")
    @RequireResourceOwnership(
            resourceType = RequireResourceOwnership.ResourceType.MEDICATION_SCHEDULE,
            paramName = "schedule-id",
            strategy = RequireResourceOwnership.OwnershipStrategy.DIRECT
    )
    public ResponseEntity<List<MedicationTimeResponse>> getMedicationTimes(
            @Parameter(description = "복약 스케줄 ID", required = true, example = "1", in = ParameterIn.PATH)
            @PathVariable("schedule-id") Long scheduleId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<MedicationTimeResponse> times = medicationTimeService.getMedicationTimes(scheduleId);
        return ResponseEntity.ok(times);
    }

    @Operation(summary = "알림 시간 추가",
            description = "복약 스케줄에 새로운 알림 시간을 추가합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "알림 시간 추가 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MedicationTimeResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content),
            @ApiResponse(responseCode = "404", description = "복약 스케줄을 찾을 수 없음", content = @Content)
    })
    @PostMapping(ApiVersion.MEDICATION_SCHEDULE + "/{schedule-id}/times")
    @RequireResourceOwnership(
            resourceType = RequireResourceOwnership.ResourceType.MEDICATION_SCHEDULE,
            paramName = "schedule-id",
            strategy = RequireResourceOwnership.OwnershipStrategy.DIRECT
    )
    public ResponseEntity<MedicationTimeResponse> createMedicationTime(
            @Parameter(description = "복약 스케줄 ID", required = true, example = "1", in = ParameterIn.PATH)
            @PathVariable("schedule-id") Long scheduleId,
            @Valid @RequestBody MedicationTimeCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        MedicationTimeResponse response = medicationTimeService.createMedicationTime(scheduleId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "알림 시간 수정",
            description = "복약 알림 시간을 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "알림 시간 수정 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MedicationTimeResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content),
            @ApiResponse(responseCode = "404", description = "알림 시간을 찾을 수 없음", content = @Content)
    })
    @PutMapping("/medication-time/{time-id}")
    public ResponseEntity<MedicationTimeResponse> updateMedicationTime(
            @Parameter(description = "알림 시간 ID", required = true, example = "1", in = ParameterIn.PATH)
            @PathVariable("time-id") Long timeId,
            @Valid @RequestBody MedicationTimeUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        validateTimeOwnership(timeId, userDetails);
        MedicationTimeResponse response = medicationTimeService.updateMedicationTime(timeId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "알림 시간 삭제",
            description = "복약 알림 시간을 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "알림 시간 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content),
            @ApiResponse(responseCode = "404", description = "알림 시간을 찾을 수 없음", content = @Content)
    })
    @DeleteMapping("/medication-time/{time-id}")
    public ResponseEntity<Void> deleteMedicationTime(
            @Parameter(description = "알림 시간 ID", required = true, example = "1", in = ParameterIn.PATH)
            @PathVariable("time-id") Long timeId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        validateTimeOwnership(timeId, userDetails);
        medicationTimeService.deleteMedicationTime(timeId);
        return ResponseEntity.noContent().build();
    }

    private void validateTimeOwnership(Long timeId, CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getMember() == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        Long memberId = userDetails.getMember().getMemberId();
        if (!medicationTimeService.isOwnedByMember(timeId, memberId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }
}
