package com.Hamalog.domain.sideEffect;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SideEffectDegree {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "side_effect_record_id", nullable = false)
    private SideEffectRecord sideEffectRecord;

    private Integer degree;

    @Version
    private Long version;
}
