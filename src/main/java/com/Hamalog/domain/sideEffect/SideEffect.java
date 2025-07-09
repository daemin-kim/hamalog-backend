package com.Hamalog.domain.sideEffect;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class SideEffect {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sideEffectId;

    @Column(length = 20, nullable = false)
    private String type;

    @Column(length = 20, nullable = false)
    private String name;

}
