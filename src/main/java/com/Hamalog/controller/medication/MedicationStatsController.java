package com.Hamalog.controller.medication;

import com.Hamalog.config.ApiVersion;
import com.Hamalog.dto.medication.response.MedicationAdherenceResponse;
import com.Hamalog.dto.medication.response.MedicationSummaryResponse;
import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.security.CustomUserDetails;
import com.Hamalog.security.annotation.RequireResourceOwnership;
import com.Hamalog.service.medication.MedicationStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Medication Stats API", description = "복약 통계 관련 API")
@RestController
@RequestMapping(ApiVersion.CURRENT + "/medication-stats")
@RequiredArgsConstructor
public class MedicationStatsController {

    private final MedicationStatsService medicationStatsService;

    @Operation(summary = "복약 이행률 조회",
            description = "기간별 복약 이행률을 조회합니다. 일별 통계와 미복용 날짜 목록을 포함합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "복약 이행률 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MedicationAdherenceResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content),
            @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음", content = @Content)
    })
    @GetMapping("/{member-id}/adherence")
    @RequireResourceOwnership(
            resourceType = RequireResourceOwnership.ResourceType.MEMBER,
            paramName = "member-id",
            strategy = RequireResourceOwnership.OwnershipStrategy.DIRECT
    )
    public ResponseEntity<MedicationAdherenceResponse> getAdherence(
            @Parameter(description = "회원 ID", required = true, example = "1", in = ParameterIn.PATH)
            @PathVariable("member-id") Long memberId,
            @Parameter(description = "시작 날짜 (yyyy-MM-dd)", required = true, example = "2025-12-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "종료 날짜 (yyyy-MM-dd)", required = true, example = "2025-12-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        validateMemberAccess(memberId, userDetails);
        MedicationAdherenceResponse response = medicationStatsService.getAdherence(memberId, startDate, endDate);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "복약 현황 요약 조회",
            description = "오늘/이번 주/이번 달 복약 이행률과 스케줄별 통계를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "복약 현황 요약 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MedicationSummaryResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content),
            @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음", content = @Content)
    })
    @GetMapping("/{member-id}/summary")
    @RequireResourceOwnership(
            resourceType = RequireResourceOwnership.ResourceType.MEMBER,
            paramName = "member-id",
            strategy = RequireResourceOwnership.OwnershipStrategy.DIRECT
    )
    public ResponseEntity<MedicationSummaryResponse> getSummary(
            @Parameter(description = "회원 ID", required = true, example = "1", in = ParameterIn.PATH)
            @PathVariable("member-id") Long memberId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        validateMemberAccess(memberId, userDetails);
        MedicationSummaryResponse response = medicationStatsService.getSummary(memberId);
        return ResponseEntity.ok(response);
    }

    private void validateMemberAccess(Long memberId, CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getMember() == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        if (!userDetails.getMember().getMemberId().equals(memberId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }
}
