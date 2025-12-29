package com.Hamalog.repository.sideEffect;

import com.Hamalog.domain.idClass.SideEffectSideEffectRecordId;
import com.Hamalog.domain.sideEffect.SideEffectSideEffectRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SideEffectSideEffectRecordRepository extends JpaRepository<SideEffectSideEffectRecord, SideEffectSideEffectRecordId> {

    // 부작용 기록 ID로 연관된 모든 부작용 조회
    @Query("SELECT ssr FROM SideEffectSideEffectRecord ssr " +
           "JOIN FETCH ssr.sideEffect " +
           "WHERE ssr.sideEffectRecord.sideEffectRecordId = :recordId")
    List<SideEffectSideEffectRecord> findBySideEffectRecordIdWithSideEffect(@Param("recordId") Long recordId);

    // 부작용 기록 ID로 연관 데이터 삭제
    @Modifying
    @Query("DELETE FROM SideEffectSideEffectRecord ssr WHERE ssr.sideEffectRecord.sideEffectRecordId = :recordId")
    void deleteBySideEffectRecordId(@Param("recordId") Long recordId);

    // 여러 부작용 기록 ID로 연관된 모든 부작용 배치 조회 (N+1 문제 해결)
    @Query("SELECT ssr FROM SideEffectSideEffectRecord ssr " +
           "JOIN FETCH ssr.sideEffect " +
           "WHERE ssr.sideEffectRecord.sideEffectRecordId IN :recordIds")
    List<SideEffectSideEffectRecord> findByRecordIdsWithSideEffect(@Param("recordIds") List<Long> recordIds);
}