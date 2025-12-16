package com.Hamalog.dto.sideEffect.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "부작용 기록 생성 요청")
public record SideEffectRecordRequest(
        @NotNull(message = "{sideEffect.memberId.notNull}")
        @Schema(description = "회원 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Long memberId,

        @NotNull(message = "{sideEffect.createdAt.notNull}")
        @Schema(description = "기록 시간", example = "2025-08-29T10:30:00", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime createdAt,

        @NotEmpty(message = "{sideEffect.sideEffects.notEmpty}")
        @Valid
        @Schema(description = "부작용 항목 목록", requiredMode = Schema.RequiredMode.REQUIRED)
        List<SideEffectItem> sideEffects
) {
    @Schema(description = "부작용 항목")
    public record SideEffectItem(
            @NotNull(message = "{sideEffect.sideEffectId.notNull}")
            @Schema(description = "부작용 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
            Long sideEffectId,

            @NotNull(message = "{sideEffect.degree.notNull}")
            @Min(value = 1, message = "{sideEffect.degree.min}")
            @Max(value = 5, message = "{sideEffect.degree.max}")
            @Schema(description = "부작용 정도 (1-5)", example = "2", minimum = "1", maximum = "5", requiredMode = Schema.RequiredMode.REQUIRED)
            Integer degree
    ) {}
}
