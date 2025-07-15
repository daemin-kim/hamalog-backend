package com.Hamalog.exception.medication;

import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;

public class MedicationScheduleNotFoundException extends CustomException {

    public MedicationScheduleNotFoundException() {
        super(ErrorCode.MEDICATION_SCHEDULE_NOT_FOUND);
    }
}
