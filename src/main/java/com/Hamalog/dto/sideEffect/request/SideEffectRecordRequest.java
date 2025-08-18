package com.Hamalog.dto.sideEffect.request;

import java.time.LocalDateTime;
import java.util.List;

public record SideEffectRecordRequest(
        Long memberId,
        LocalDateTime createdAt,
        List<SideEffectItem> sideEffects
) {
    public record SideEffectItem(
            Long sideEffectId,
            Integer degree
    ) {}
}
