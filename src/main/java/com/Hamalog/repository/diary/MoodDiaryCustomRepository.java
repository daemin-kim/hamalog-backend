package com.Hamalog.repository.diary;

import com.Hamalog.domain.diary.MoodType;
import com.Hamalog.dto.diary.projection.MoodDiaryProjection;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * MoodDiary QueryDSL Custom Repository Interface
 * 복잡한 동적 쿼리를 위한 QueryDSL 기반 메서드 정의
 */
public interface MoodDiaryCustomRepository {

    /**
     * 동적 검색 조건을 활용한 마음 일기 조회
     *
     * @param memberId   회원 ID
     * @param keyword    검색 키워드 (내용 검색) - nullable
     * @param moodType   기분 타입 필터 - nullable
     * @param startDate  시작 날짜 - nullable
     * @param endDate    종료 날짜 - nullable
     * @param pageable   페이징 정보
     * @return 검색 조건에 맞는 마음 일기 목록 (DTO Projection)
     */
    Page<MoodDiaryProjection> searchWithConditions(
            Long memberId,
            String keyword,
            MoodType moodType,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );
}
