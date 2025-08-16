package com.Hamalog.dto.medication.response;

import java.util.List;

/**
 * 복약 스케줄 목록 응답 DTO
 * 페이지네이션 대신 사용되는 커스텀 응답 구조
 */
public record MedicationScheduleListResponse(
        List<MedicationScheduleResponse> schedules,
        long totalCount,
        int currentPage,
        int pageSize,
        boolean hasNext,
        boolean hasPrevious
) {
    
    /**
     * Spring Data Page 객체로부터 MedicationScheduleListResponse 생성
     */
    public static MedicationScheduleListResponse from(org.springframework.data.domain.Page<MedicationScheduleResponse> page) {
        return new MedicationScheduleListResponse(
                page.getContent(),
                page.getTotalElements(),
                page.getNumber(),
                page.getSize(),
                page.hasNext(),
                page.hasPrevious()
        );
    }
    
    /**
     * 전체 목록으로부터 MedicationScheduleListResponse 생성 (페이지네이션 없음)
     */
    public static MedicationScheduleListResponse fromList(List<MedicationScheduleResponse> schedules) {
        return new MedicationScheduleListResponse(
                schedules,
                schedules.size(),
                0,
                schedules.size(),
                false,
                false
        );
    }
}