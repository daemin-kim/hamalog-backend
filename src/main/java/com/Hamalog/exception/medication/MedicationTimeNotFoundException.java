package com.Hamalog.exception.medication;

import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;

public class MedicationTimeNotFoundException extends CustomException {

    public MedicationTimeNotFoundException() {
        super(ErrorCode.MEDICATION_TIME_NOT_FOUND);
    }
}
