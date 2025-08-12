package com.Hamalog.dto.medication.response;

import com.Hamalog.domain.medication.AlarmType;
import com.Hamalog.domain.medication.MedicationSchedule;
import java.time.LocalDate;

public record MedicationScheduleResponse(
        Long medicationScheduleId,
        Long memberId,
        String name,
        String hospitalName,
        LocalDate prescriptionDate,
        String memo,
        LocalDate startOfAd,
        Integer prescriptionDays,
        Integer perDay,
        AlarmType alarmType
) {
    public static MedicationScheduleResponse from(MedicationSchedule medicationSchedule) {
        return new MedicationScheduleResponse(
                medicationSchedule.getMedicationScheduleId(),
                medicationSchedule.getMember().getMemberId(),
                medicationSchedule.getName(),
                medicationSchedule.getHospitalName(),
                medicationSchedule.getPrescriptionDate(),
                medicationSchedule.getMemo(),
                medicationSchedule.getStartOfAd(),
                medicationSchedule.getPrescriptionDays(),
                medicationSchedule.getPerDay(),
                medicationSchedule.getAlarmType()
        );
    }
}