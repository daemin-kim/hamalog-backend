package com.Hamalog.dto.sideEffect.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "최근 부작용 응답 데이터")
public record RecentSideEffectResponse(
        @Schema(description = "최근 부작용 이름 목록 (최대 5개)", example = "[\"두통\", \"메스꺼움\", \"발진\", \"현기증\", \"복통\"]")
        List<String> recentSideEffect
) {}
