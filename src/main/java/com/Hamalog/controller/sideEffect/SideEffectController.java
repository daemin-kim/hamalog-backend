package com.Hamalog.controller.sideEffect;

import com.Hamalog.dto.sideEffect.response.RecentSideEffectResponse;
import com.Hamalog.service.sideEffect.SideEffectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<RecentSideEffectResponse> getRecentSideEffects(
            @Parameter(
                    in = ParameterIn.QUERY,
                    description = "사용자 ID",
                    required = true,
                    example = "1"
            )
            @RequestParam Long userId) {
        return ResponseEntity.ok(sideEffectService.getRecentSideEffects(userId));
    }
}
