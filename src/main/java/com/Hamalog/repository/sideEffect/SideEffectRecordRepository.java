package com.Hamalog.repository.sideEffect;

import com.Hamalog.domain.sideEffect.SideEffectRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SideEffectRecordRepository extends JpaRepository<SideEffectRecord, Long> {
}