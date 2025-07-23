package com.Hamalog.repository.sideEffect;

import com.Hamalog.domain.sideEffect.SideEffectRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SideEffectRecordRepository extends JpaRepository<SideEffectRecord, Long> {
    List<SideEffectRecord> findTop5ByMember_MemberIdOrderByCreatedAtDesc(Long memberId);
}
