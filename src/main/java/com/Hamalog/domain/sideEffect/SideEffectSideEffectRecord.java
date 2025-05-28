package com.Hamalog.domain.sideEffect;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

@Entity
@IdClass(SideEffectSideEffectRecord.class)
public class SideEffectSideEffectRecord {
    @Id
    private Long sideEffectRecordId;

    @Id
    private Long sideEffectId;
}
