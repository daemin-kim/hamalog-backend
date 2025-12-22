package com.Hamalog.dto.sideEffect.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "부작용 기록 상세 응답")
public record SideEffectRecordResponse(
        @Schema(description = "부작용 기록 ID", example = "1")
        Long sideEffectRecordId,

        @Schema(description = "회원 ID", example = "1")
        Long memberId,

        @Schema(description = "기록 생성 시각")
        LocalDateTime createdAt,

        @Schema(description = "해당 기록의 부작용 목록")
        List<SideEffectDetailResponse> sideEffects
) {
    @Schema(description = "부작용 상세 정보")
    public record SideEffectDetailResponse(
            @Schema(description = "부작용 ID", example = "1")
            Long sideEffectId,

            @Schema(description = "부작용 이름", example = "두통")
            String name,

            @Schema(description = "부작용 정도 (1-5)", example = "3")
            Integer degree
    ) {}
}
