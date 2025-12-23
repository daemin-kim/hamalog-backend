package com.Hamalog.dto.auth.response;

import java.util.List;
import org.springframework.data.domain.Page;

public record LoginHistoryListResponse(
    List<LoginHistoryResponse> histories,
    int totalPages,
    long totalElements,
    int currentPage,
    int pageSize,
    boolean hasNext,
    boolean hasPrevious
) {
    public static LoginHistoryListResponse from(Page<LoginHistoryResponse> page) {
        return new LoginHistoryListResponse(
            page.getContent(),
            page.getTotalPages(),
            page.getTotalElements(),
            page.getNumber(),
            page.getSize(),
            page.hasNext(),
            page.hasPrevious()
        );
    }
}
