package com.Hamalog.domain.sideEffect;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class SideEffectDegree {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "side_effect_record_id", nullable = false)
    private SideEffectRecord sideEffectRecord;

    private Integer degree;
}
