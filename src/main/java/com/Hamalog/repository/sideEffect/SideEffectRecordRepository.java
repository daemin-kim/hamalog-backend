package com.Hamalog.repository.sideEffect;

import com.Hamalog.domain.sideEffect.SideEffectRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SideEffectRecordRepository extends JpaRepository<SideEffectRecord, Long> {
    
    // Efficient batch delete for member deletion
    @Modifying
    @Query("DELETE FROM SideEffectRecord ser WHERE ser.member.memberId = :memberId")
    void deleteByMemberId(@Param("memberId") Long memberId);
}