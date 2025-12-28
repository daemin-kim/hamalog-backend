package com.Hamalog.repository.diary;

import com.Hamalog.domain.diary.MoodDiary;
import com.Hamalog.domain.member.Member;
import com.Hamalog.dto.diary.projection.MoodDiaryProjection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MoodDiaryRepository extends JpaRepository<MoodDiary, Long> {

    @EntityGraph(attributePaths = {"member"})
    Optional<MoodDiary> findByMemberAndDiaryDate(Member member, LocalDate diaryDate);

    boolean existsByMemberAndDiaryDate(Member member, LocalDate diaryDate);

    @EntityGraph(attributePaths = {"member"})
    Page<MoodDiary> findByMemberOrderByDiaryDateDesc(Member member, Pageable pageable);

    // DTO Projection: 목록 조회 시 필요한 필드만 조회하여 성능 최적화
    @Query("SELECT new com.Hamalog.dto.diary.projection.MoodDiaryProjection(" +
           "m.moodDiaryId, m.member.memberId, m.diaryDate, m.moodType, m.diaryType, m.createdAt) " +
           "FROM MoodDiary m WHERE m.member.memberId = :memberId ORDER BY m.diaryDate DESC")
    Page<MoodDiaryProjection> findProjectionsByMemberId(@Param("memberId") Long memberId, Pageable pageable);

    @Query("SELECT m FROM MoodDiary m WHERE m.moodDiaryId = :moodDiaryId AND m.member.memberId = :memberId")
    Optional<MoodDiary> findByIdAndMemberId(@Param("moodDiaryId") Long moodDiaryId, @Param("memberId") Long memberId);

    @Modifying
    @Query("DELETE FROM MoodDiary m WHERE m.member.memberId = :memberId")
    void deleteByMember_MemberId(@Param("memberId") Long memberId);

    // 기간별 일기 조회 (통계용)
    @Query("SELECT m FROM MoodDiary m WHERE m.member.memberId = :memberId " +
           "AND m.diaryDate BETWEEN :startDate AND :endDate ORDER BY m.diaryDate ASC")
    List<MoodDiary> findByMemberIdAndDateRange(
            @Param("memberId") Long memberId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // DTO Projection: 기간별 일기 조회 (캘린더용 - 필요한 필드만)
    @Query("SELECT new com.Hamalog.dto.diary.projection.MoodDiaryProjection(" +
           "m.moodDiaryId, m.member.memberId, m.diaryDate, m.moodType, m.diaryType, m.createdAt) " +
           "FROM MoodDiary m WHERE m.member.memberId = :memberId " +
           "AND m.diaryDate BETWEEN :startDate AND :endDate ORDER BY m.diaryDate ASC")
    List<MoodDiaryProjection> findProjectionsByMemberIdAndDateRange(
            @Param("memberId") Long memberId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // 월별 일기 조회 (캘린더용)
    @Query("SELECT m FROM MoodDiary m WHERE m.member.memberId = :memberId " +
           "AND YEAR(m.diaryDate) = :year AND MONTH(m.diaryDate) = :month ORDER BY m.diaryDate ASC")
    List<MoodDiary> findByMemberIdAndYearMonth(
            @Param("memberId") Long memberId,
            @Param("year") int year,
            @Param("month") int month
    );

    // DTO Projection: 월별 일기 조회 (캘린더용 - 필요한 필드만)
    @Query("SELECT new com.Hamalog.dto.diary.projection.MoodDiaryProjection(" +
           "m.moodDiaryId, m.member.memberId, m.diaryDate, m.moodType, m.diaryType, m.createdAt) " +
           "FROM MoodDiary m WHERE m.member.memberId = :memberId " +
           "AND YEAR(m.diaryDate) = :year AND MONTH(m.diaryDate) = :month ORDER BY m.diaryDate ASC")
    List<MoodDiaryProjection> findProjectionsByMemberIdAndYearMonth(
            @Param("memberId") Long memberId,
            @Param("year") int year,
            @Param("month") int month
    );

    // 회원별 일기 작성 일수 조회
    @Query("SELECT COUNT(m) FROM MoodDiary m WHERE m.member.memberId = :memberId " +
           "AND m.diaryDate BETWEEN :startDate AND :endDate")
    long countByMemberIdAndDateRange(
            @Param("memberId") Long memberId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // 일기 내용 검색 (자유형식 내용 + 템플릿 답변 검색)
    @Query("SELECT m FROM MoodDiary m WHERE m.member.memberId = :memberId " +
           "AND (LOWER(m.freeContent) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(m.templateAnswer1) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(m.templateAnswer2) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(m.templateAnswer3) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(m.templateAnswer4) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY m.diaryDate DESC")
    Page<MoodDiary> searchByKeyword(
            @Param("memberId") Long memberId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // 기분 타입별 필터링
    @Query("SELECT m FROM MoodDiary m WHERE m.member.memberId = :memberId " +
           "AND m.moodType = :moodType ORDER BY m.diaryDate DESC")
    Page<MoodDiary> findByMemberIdAndMoodType(
            @Param("memberId") Long memberId,
            @Param("moodType") com.Hamalog.domain.diary.MoodType moodType,
            Pageable pageable
    );
}
