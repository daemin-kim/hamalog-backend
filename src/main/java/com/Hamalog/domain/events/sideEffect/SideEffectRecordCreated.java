package com.Hamalog.domain.events.sideEffect;

import com.Hamalog.domain.events.DomainEvent;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;

/**
 * 부작용 기록 생성 이벤트
 * 부작용 기록이 생성될 때 발행되어 캐시 갱신 및 심각도 분석에 사용
 */
@Getter
public class SideEffectRecordCreated extends DomainEvent {

    private final Long sideEffectRecordId;
    private final Long memberId;
    private final String memberLoginId;
    private final Long linkedMedicationScheduleId;  // 연계된 복약 스케줄 (선택)
    private final LocalDateTime createdAt;
    private final List<SideEffectItem> sideEffects;
    private final Integer maxDegree;

    public SideEffectRecordCreated(
            Long sideEffectRecordId,
            Long memberId,
            String memberLoginId,
            Long linkedMedicationScheduleId,
            LocalDateTime createdAt,
            List<SideEffectItem> sideEffects
    ) {
        super();
        this.sideEffectRecordId = sideEffectRecordId;
        this.memberId = memberId;
        this.memberLoginId = memberLoginId;
        this.linkedMedicationScheduleId = linkedMedicationScheduleId;
        this.createdAt = createdAt;
        this.sideEffects = sideEffects;
        this.maxDegree = sideEffects.stream()
                .mapToInt(SideEffectItem::degree)
                .max()
                .orElse(0);
    }

    @Override
    public String getAggregateId() {
        return sideEffectRecordId.toString();
    }

    /**
     * 심각한 부작용인지 확인 (degree >= 3)
     */
    public boolean hasSevereSideEffect() {
        return maxDegree >= 3;
    }

    /**
     * 복약 스케줄과 연계되었는지 확인
     */
    public boolean isLinkedToMedication() {
        return linkedMedicationScheduleId != null;
    }

    @Override
    public String toString() {
        return String.format(
                "SideEffectRecordCreated{recordId=%d, memberId=%d, maxDegree=%d, linkedSchedule=%s, occurredOn=%s}",
                sideEffectRecordId, memberId, maxDegree, linkedMedicationScheduleId, getOccurredOn()
        );
    }

    /**
     * 부작용 항목 정보
     */
    public record SideEffectItem(
            Long sideEffectId,
            String name,
            Integer degree
    ) {}
}
