package com.Hamalog.domain.events.medication;

import com.Hamalog.domain.events.DomainEvent;
import java.time.LocalDateTime;
import lombok.Getter;

/**
 * 복약 기록 생성 이벤트
 * 복약 기록이 생성될 때 발행되어 통계 캐시 무효화 및 연속 복약 추적에 사용
 */
@Getter
public class MedicationRecordCreated extends DomainEvent {

    private final Long medicationRecordId;
    private final Long medicationScheduleId;
    private final Long medicationTimeId;
    private final Long memberId;
    private final String memberLoginId;
    private final Boolean isTakeMedication;
    private final LocalDateTime realTakeTime;

    public MedicationRecordCreated(
            Long medicationRecordId,
            Long medicationScheduleId,
            Long medicationTimeId,
            Long memberId,
            String memberLoginId,
            Boolean isTakeMedication,
            LocalDateTime realTakeTime
    ) {
        super();
        this.medicationRecordId = medicationRecordId;
        this.medicationScheduleId = medicationScheduleId;
        this.medicationTimeId = medicationTimeId;
        this.memberId = memberId;
        this.memberLoginId = memberLoginId;
        this.isTakeMedication = isTakeMedication;
        this.realTakeTime = realTakeTime;
    }

    @Override
    public String getAggregateId() {
        return medicationRecordId.toString();
    }

    @Override
    public String toString() {
        return String.format(
                "MedicationRecordCreated{recordId=%d, scheduleId=%d, memberId=%d, taken=%s, occurredOn=%s}",
                medicationRecordId, medicationScheduleId, memberId, isTakeMedication, getOccurredOn()
        );
    }
}
