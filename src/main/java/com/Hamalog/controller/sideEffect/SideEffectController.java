package com.Hamalog.controller.sideEffect;

import com.Hamalog.dto.sideEffect.request.SideEffectRecordRequest;
import com.Hamalog.dto.sideEffect.response.RecentSideEffectResponse;
import com.Hamalog.security.annotation.RequireResourceOwnership;
import com.Hamalog.service.sideEffect.SideEffectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Side Effect API", description = "사용자 부작용 기록 관련 API")
@RestController
@RequestMapping("/side-effect")
public class SideEffectController {

    private final SideEffectService sideEffectService;

    public SideEffectController(SideEffectService sideEffectService) {
        this.sideEffectService = sideEffectService;
    }

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
}
