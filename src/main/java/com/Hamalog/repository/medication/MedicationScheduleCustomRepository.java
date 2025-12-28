package com.Hamalog.repository.medication;

import com.Hamalog.dto.medication.projection.MedicationScheduleProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * MedicationSchedule QueryDSL Custom Repository Interface
 * 복잡한 동적 쿼리를 위한 QueryDSL 기반 메서드 정의
 */
public interface MedicationScheduleCustomRepository {

    /**
     * 동적 검색 조건을 활용한 복약 스케줄 조회
     *
     * @param memberId   회원 ID
     * @param keyword    검색 키워드 (약 이름, 병원 이름) - nullable
     * @param isActive   활성 상태 필터 - nullable
     * @param pageable   페이징 정보
     * @return 검색 조건에 맞는 복약 스케줄 목록 (DTO Projection)
     */
    Page<MedicationScheduleProjection> searchWithConditions(
            Long memberId,
            String keyword,
            Boolean isActive,
            Pageable pageable
    );
}
