package com.Hamalog.repository.diary;

import com.Hamalog.domain.diary.MoodDiary;
import com.Hamalog.domain.member.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface MoodDiaryRepository extends JpaRepository<MoodDiary, Long> {

    Optional<MoodDiary> findByMemberAndDiaryDate(Member member, LocalDate diaryDate);

    boolean existsByMemberAndDiaryDate(Member member, LocalDate diaryDate);

    Page<MoodDiary> findByMemberOrderByDiaryDateDesc(Member member, Pageable pageable);

    @Query("SELECT m FROM MoodDiary m WHERE m.moodDiaryId = :moodDiaryId AND m.member.memberId = :memberId")
    Optional<MoodDiary> findByIdAndMemberId(@Param("moodDiaryId") Long moodDiaryId, @Param("memberId") Long memberId);
}

