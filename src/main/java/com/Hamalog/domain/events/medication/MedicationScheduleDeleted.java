package com.Hamalog.domain.events.medication;

import com.Hamalog.domain.events.DomainEvent;
import lombok.Getter;

/**
 * Domain event published when a medication schedule is deleted.
 * Contains the essential information about the deleted medication schedule.
 */
@Getter
public class MedicationScheduleDeleted extends DomainEvent {

    private final Long medicationScheduleId;
    private final Long memberId;
    private final String memberLoginId;
    private final String name;

    public MedicationScheduleDeleted(
            Long medicationScheduleId,
            Long memberId,
            String memberLoginId,
            String name
    ) {
        super();
        this.medicationScheduleId = medicationScheduleId;
        this.memberId = memberId;
        this.memberLoginId = memberLoginId;
        this.name = name;
    }

    @Override
    public String getAggregateId() {
        return medicationScheduleId != null ? medicationScheduleId.toString() : "unknown";
    }

    @Override
    public String toString() {
        return String.format(
                "MedicationScheduleDeleted{medicationScheduleId=%d, memberId=%d, memberLoginId='%s', " +
                "name='%s', eventId='%s', occurredOn=%s}",
                medicationScheduleId, memberId, memberLoginId, name,
                getEventId(), getOccurredOn()
        );
    }
}