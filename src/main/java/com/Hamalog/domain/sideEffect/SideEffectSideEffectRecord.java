package com.Hamalog.domain.sideEffect;

import com.Hamalog.domain.idClass.SideEffectSideEffectRecordId;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@IdClass(SideEffectSideEffectRecordId.class)
@EqualsAndHashCode
public class SideEffectSideEffectRecord {
    @Id
    @Column(name = "side_effect_record_id")
    private Long sideEffectRecordId;

    @Id
    @Column(name = "side_effect_id")
    private Long sideEffectId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "side_effect_record_id", insertable = false, updatable = false)
    private SideEffectRecord sideEffectRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "side_effect_id", insertable = false, updatable = false)
    private SideEffect sideEffect;

    @Column(nullable = false)
    private Integer degree;
}
