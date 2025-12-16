package com.Hamalog.domain.events.medication;

import com.Hamalog.domain.events.DomainEvent;
import com.Hamalog.domain.medication.AlarmType;
import java.time.LocalDate;
import lombok.Getter;

/**
 * Domain event published when a new medication schedule is created.
 * Contains the essential information about the created medication schedule.
 */
@Getter
public class MedicationScheduleCreated extends DomainEvent {

    private final Long medicationScheduleId;
    private final Long memberId;
    private final String memberLoginId;
    private final String name;
    private final String hospitalName;
    private final LocalDate prescriptionDate;
    private final String memo;
    private final LocalDate startOfAd;
    private final Integer prescriptionDays;
    private final Integer perDay;
    private final AlarmType alarmType;

    public MedicationScheduleCreated(
            Long medicationScheduleId,
            Long memberId,
            String memberLoginId,
            String name,
            String hospitalName,
            LocalDate prescriptionDate,
            String memo,
            LocalDate startOfAd,
            Integer prescriptionDays,
            Integer perDay,
            AlarmType alarmType
    ) {
        super();
        this.medicationScheduleId = medicationScheduleId;
        this.memberId = memberId;
        this.memberLoginId = memberLoginId;
        this.name = name;
        this.hospitalName = hospitalName;
        this.prescriptionDate = prescriptionDate;
        this.memo = memo;
        this.startOfAd = startOfAd;
        this.prescriptionDays = prescriptionDays;
        this.perDay = perDay;
        this.alarmType = alarmType;
    }

    @Override
    public String getAggregateId() {
        return medicationScheduleId != null ? medicationScheduleId.toString() : "unknown";
    }

    @Override
    public String toString() {
        return String.format(
                "MedicationScheduleCreated{medicationScheduleId=%d, memberId=%d, memberLoginId='%s', " +
                "name='%s', hospitalName='%s', prescriptionDate=%s, startOfAd=%s, prescriptionDays=%d, " +
                "perDay=%d, alarmType=%s, eventId='%s', occurredOn=%s}",
                medicationScheduleId, memberId, memberLoginId, name, hospitalName,
                prescriptionDate, startOfAd, prescriptionDays, perDay, alarmType,
                getEventId(), getOccurredOn()
        );
    }
}