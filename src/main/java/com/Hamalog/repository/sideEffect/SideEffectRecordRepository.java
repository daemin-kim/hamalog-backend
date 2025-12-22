package com.Hamalog.repository.sideEffect;

import com.Hamalog.domain.sideEffect.SideEffectRecord;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SideEffectRecordRepository extends JpaRepository<SideEffectRecord, Long> {
    
    // Efficient batch delete for member deletion
    @Modifying
    @Query("DELETE FROM SideEffectRecord ser WHERE ser.member.memberId = :memberId")
    void deleteByMemberId(@Param("memberId") Long memberId);

    // 회원별 부작용 기록 목록 조회 (페이징, 최신순)
    @EntityGraph(attributePaths = {"member"})
    Page<SideEffectRecord> findByMember_MemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    // 부작용 기록 단건 조회 (회원 검증용)
    @Query("SELECT ser FROM SideEffectRecord ser " +
           "JOIN FETCH ser.member " +
           "WHERE ser.sideEffectRecordId = :recordId")
    Optional<SideEffectRecord> findByIdWithMember(@Param("recordId") Long recordId);

    // 부작용 기록이 특정 회원의 것인지 확인
    @Query("SELECT CASE WHEN COUNT(ser) > 0 THEN true ELSE false END " +
           "FROM SideEffectRecord ser " +
           "WHERE ser.sideEffectRecordId = :recordId AND ser.member.memberId = :memberId")
    boolean existsByIdAndMemberId(@Param("recordId") Long recordId, @Param("memberId") Long memberId);
}