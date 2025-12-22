package com.Hamalog.dto.sideEffect.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "부작용 기록 목록 응답")
public record SideEffectRecordListResponse(
        @Schema(description = "부작용 기록 목록")
        List<SideEffectRecordResponse> records,

        @Schema(description = "전체 기록 수", example = "50")
        long totalElements,

        @Schema(description = "현재 페이지 번호", example = "0")
        int pageNumber,

        @Schema(description = "페이지 크기", example = "20")
        int pageSize,

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext,

        @Schema(description = "이전 페이지 존재 여부", example = "false")
        boolean hasPrevious
) {}
