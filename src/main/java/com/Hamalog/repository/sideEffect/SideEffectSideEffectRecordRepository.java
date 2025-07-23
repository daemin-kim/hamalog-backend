package com.Hamalog.repository.sideEffect;

import com.Hamalog.domain.sideEffect.SideEffectSideEffectRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SideEffectSideEffectRecordRepository extends JpaRepository<SideEffectSideEffectRecord, Long> {
    List<SideEffectSideEffectRecord> findBySideEffectRecordIdIn(List<Long> sideEffectRecordIds);
}
