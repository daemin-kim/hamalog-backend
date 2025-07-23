package com.Hamalog.domain.sideEffect;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Entity
@Getter
@IdClass(SideEffectSideEffectRecord.class)
@EqualsAndHashCode
public class SideEffectSideEffectRecord {
    @Id
    private Long sideEffectRecordId;

    @Id
    private Long sideEffectId;
}
