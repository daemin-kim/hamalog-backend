package com.Hamalog.controller.medication;

import com.Hamalog.config.ApiVersion;
import com.Hamalog.dto.medication.request.MedicationScheduleGroupCreateRequest;
import com.Hamalog.dto.medication.request.MedicationScheduleGroupUpdateRequest;
import com.Hamalog.dto.medication.response.MedicationScheduleGroupResponse;
import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.security.CustomUserDetails;
import com.Hamalog.service.medication.MedicationScheduleGroupService;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 복약 스케줄 그룹 Controller
 *
 * 복약 스케줄을 그룹화하여 관리하는 기능을 제공합니다.
 * 예: "아침약", "저녁약", "병원약" 등으로 분류
 */
@Tag(name = "Medication Group API", description = "복약 스케줄 그룹 관리 API")
@RestController
@RequestMapping(ApiVersion.MEDICATION_GROUP)
@RequiredArgsConstructor
@Slf4j
public class MedicationScheduleGroupController {

    private final MedicationScheduleGroupService groupService;

    @Operation(summary = "그룹 목록 조회",
            description = "현재 로그인한 사용자의 복약 스케줄 그룹 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = MedicationScheduleGroupResponse.class)))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
    })
    @GetMapping
    public ResponseEntity<List<MedicationScheduleGroupResponse>> getGroups(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = getAuthenticatedMemberId(userDetails);
        List<MedicationScheduleGroupResponse> groups = groupService.getGroups(memberId);
        return ResponseEntity.ok(groups);
    }

    @Operation(summary = "그룹 상세 조회",
            description = "특정 복약 스케줄 그룹의 상세 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MedicationScheduleGroupResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content),
            @ApiResponse(responseCode = "404", description = "그룹을 찾을 수 없음", content = @Content)
    })
    @GetMapping("/{group-id}")
    public ResponseEntity<MedicationScheduleGroupResponse> getGroup(
            @Parameter(description = "그룹 ID", required = true, example = "1", in = ParameterIn.PATH)
            @PathVariable("group-id") Long groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = getAuthenticatedMemberId(userDetails);
        MedicationScheduleGroupResponse group = groupService.getGroup(memberId, groupId);
        return ResponseEntity.ok(group);
    }

    @Operation(summary = "그룹 생성",
            description = "새로운 복약 스케줄 그룹을 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "그룹 생성 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MedicationScheduleGroupResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검증 실패)", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "409", description = "그룹명 중복", content = @Content)
    })
    @PostMapping
    public ResponseEntity<MedicationScheduleGroupResponse> createGroup(
            @Valid @RequestBody MedicationScheduleGroupCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = getAuthenticatedMemberId(userDetails);
        log.info("복약 그룹 생성 요청 - memberId: {}, name: {}", memberId, request.name());

        MedicationScheduleGroupResponse response = groupService.createGroup(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "그룹 수정",
            description = "복약 스케줄 그룹의 정보를 수정합니다. 수정하지 않을 필드는 null로 전송합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "그룹 수정 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MedicationScheduleGroupResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검증 실패)", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content),
            @ApiResponse(responseCode = "404", description = "그룹을 찾을 수 없음", content = @Content),
            @ApiResponse(responseCode = "409", description = "그룹명 중복", content = @Content)
    })
    @PutMapping("/{group-id}")
    public ResponseEntity<MedicationScheduleGroupResponse> updateGroup(
            @Parameter(description = "그룹 ID", required = true, example = "1", in = ParameterIn.PATH)
            @PathVariable("group-id") Long groupId,
            @Valid @RequestBody MedicationScheduleGroupUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = getAuthenticatedMemberId(userDetails);
        log.info("복약 그룹 수정 요청 - memberId: {}, groupId: {}", memberId, groupId);

        MedicationScheduleGroupResponse response = groupService.updateGroup(memberId, groupId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "그룹 삭제",
            description = "복약 스케줄 그룹을 삭제합니다. 그룹에 속한 스케줄은 그룹에서 제외되지만 삭제되지 않습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "그룹 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content),
            @ApiResponse(responseCode = "404", description = "그룹을 찾을 수 없음", content = @Content)
    })
    @DeleteMapping("/{group-id}")
    public ResponseEntity<Void> deleteGroup(
            @Parameter(description = "그룹 ID", required = true, example = "1", in = ParameterIn.PATH)
            @PathVariable("group-id") Long groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = getAuthenticatedMemberId(userDetails);
        log.info("복약 그룹 삭제 요청 - memberId: {}, groupId: {}", memberId, groupId);

        groupService.deleteGroup(memberId, groupId);
        return ResponseEntity.noContent().build();
    }

    private Long getAuthenticatedMemberId(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getMember() == null
                || userDetails.getMember().getMemberId() == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return userDetails.getMember().getMemberId();
    }
}
