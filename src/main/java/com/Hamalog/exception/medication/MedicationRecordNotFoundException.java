package com.Hamalog.exception.medication;

import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;

public class MedicationRecordNotFoundException extends CustomException {

    public MedicationRecordNotFoundException() {
        super(ErrorCode.MEDICATION_RECORD_NOT_FOUND);
    }
}
