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
@EqualsAndHashCode(of = {"sideEffectRecordId", "sideEffectId"})
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

    // ========== 도메인 로직 ==========

    /**
     * 증상 정도 업데이트
     * @param degree 새로운 증상 정도 (1-5)
     */
    public void updateDegree(Integer degree) {
        if (degree == null || degree < 1 || degree > 5) {
            throw new IllegalArgumentException("Degree must be between 1 and 5");
        }
        this.degree = degree;
    }

    /**
     * 증상 정도가 심각한지 확인 (4 이상)
     * @return 심각하면 true
     */
    public boolean isSevere() {
        return this.degree != null && this.degree >= 4;
    }
}
