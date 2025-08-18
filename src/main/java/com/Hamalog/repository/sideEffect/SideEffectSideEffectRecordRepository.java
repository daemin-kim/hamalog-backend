package com.Hamalog.repository.sideEffect;

import com.Hamalog.domain.idClass.SideEffectSideEffectRecordId;
import com.Hamalog.domain.sideEffect.SideEffectSideEffectRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SideEffectSideEffectRecordRepository extends JpaRepository<SideEffectSideEffectRecord, SideEffectSideEffectRecordId> {
}