package com.Hamalog.dto.diary.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "마음 일기 목록 응답 데이터")
public record MoodDiaryListResponse(
        @Schema(description = "마음 일기 목록")
        List<MoodDiaryResponse> diaries,

        @Schema(description = "전체 개수", example = "15")
        long totalCount,

        @Schema(description = "현재 페이지", example = "0")
        int currentPage,

        @Schema(description = "페이지 크기", example = "20")
        int pageSize,

        @Schema(description = "다음 페이지 존재 여부", example = "false")
        boolean hasNext,

        @Schema(description = "이전 페이지 존재 여부", example = "false")
        boolean hasPrevious
) {}
