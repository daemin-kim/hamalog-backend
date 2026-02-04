package com.Hamalog.controller.sideEffect;

import com.Hamalog.config.ApiVersion;
import com.Hamalog.dto.sideEffect.request.SideEffectRecordRequest;
import com.Hamalog.dto.sideEffect.response.RecentSideEffectResponse;
import com.Hamalog.dto.sideEffect.response.SideEffectRecordListResponse;
import com.Hamalog.dto.sideEffect.response.SideEffectRecordResponse;
import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.security.CustomUserDetails;
import com.Hamalog.security.annotation.RequireResourceOwnership;
import com.Hamalog.service.sideEffect.SideEffectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Side Effect API", description = "사용자 부작용 기록 관련 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping(ApiVersion.SIDE_EFFECT)
@RequiredArgsConstructor
public class SideEffectController {

    private final SideEffectService sideEffectService;

    @Operation(summary = "최근 부작용 목록 조회",
            description = "사용자 ID로 최근 5개의 부작용 이름을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "최근 부작용 조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/recent")
    @RequireResourceOwnership(
        resourceType = RequireResourceOwnership.ResourceType.MEMBER,
        paramName = "userId",
        source = RequireResourceOwnership.ParameterSource.REQUEST_PARAM,
        strategy = RequireResourceOwnership.OwnershipStrategy.DIRECT
    )
    public ResponseEntity<RecentSideEffectResponse> getRecentSideEffects(
            @Parameter(
                    in = ParameterIn.QUERY,
                    description = "사용자 ID",
                    required = true,
                    example = "1"
            )
            @RequestParam Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        return ResponseEntity.ok(sideEffectService.getRecentSideEffects(userId));
    }

    @Operation(summary = "부작용 기록 생성",
            description = "새로운 부작용 기록을 생성하고 Redis 캐시를 업데이트합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "부작용 기록 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @PostMapping("/record")
    @RequireResourceOwnership(
        resourceType = RequireResourceOwnership.ResourceType.MEMBER,
        paramName = "request",
        source = RequireResourceOwnership.ParameterSource.REQUEST_BODY,
        bodyField = "memberId",
        strategy = RequireResourceOwnership.OwnershipStrategy.DIRECT
    )
    public ResponseEntity<Void> createSideEffectRecord(
            @Valid @RequestBody SideEffectRecordRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        sideEffectService.createSideEffectRecord(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "부작용 기록 목록 조회",
            description = "회원의 부작용 기록 목록을 페이징하여 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "부작용 기록 목록 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SideEffectRecordListResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/list/{member-id}")
    @RequireResourceOwnership(
        resourceType = RequireResourceOwnership.ResourceType.MEMBER,
        paramName = "member-id",
        strategy = RequireResourceOwnership.OwnershipStrategy.DIRECT
    )
    public ResponseEntity<SideEffectRecordListResponse> getSideEffectRecords(
            @Parameter(description = "회원 ID", required = true, example = "1", in = ParameterIn.PATH)
            @PathVariable("member-id") Long memberId,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기 (최대 100)", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        validateMemberAccess(memberId, userDetails);
        SideEffectRecordListResponse response = sideEffectService.getSideEffectRecords(memberId, page, size);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "부작용 기록 상세 조회",
            description = "특정 부작용 기록의 상세 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "부작용 기록 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SideEffectRecordResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "부작용 기록을 찾을 수 없음")
    })
    @GetMapping("/{record-id}")
    public ResponseEntity<SideEffectRecordResponse> getSideEffectRecord(
            @Parameter(description = "부작용 기록 ID", required = true, example = "1", in = ParameterIn.PATH)
            @PathVariable("record-id") Long recordId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        SideEffectRecordResponse response = sideEffectService.getSideEffectRecord(recordId);
        // 소유권 검증
        validateMemberAccess(response.memberId(), userDetails);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "부작용 기록 삭제",
            description = "특정 부작용 기록을 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "부작용 기록 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "부작용 기록을 찾을 수 없음")
    })
    @DeleteMapping("/{record-id}")
    public ResponseEntity<Void> deleteSideEffectRecord(
            @Parameter(description = "부작용 기록 ID", required = true, example = "1", in = ParameterIn.PATH)
            @PathVariable("record-id") Long recordId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long memberId = getAuthenticatedMemberId(userDetails);
        sideEffectService.deleteSideEffectRecord(recordId, memberId);
        return ResponseEntity.noContent().build();
    }

    private void validateMemberAccess(Long memberId, CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getMember() == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        if (!userDetails.getMember().getMemberId().equals(memberId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }

    private Long getAuthenticatedMemberId(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getMember() == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return userDetails.getMember().getMemberId();
    }
}
