package com.Hamalog.dto.sideEffect.request;

import java.time.LocalDateTime;

public record SideEffectRecordRequest(
        Long memberId,
        LocalDateTime createdAt
) {}
