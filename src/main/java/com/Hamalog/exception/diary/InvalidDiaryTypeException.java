package com.Hamalog.exception.diary;
import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
public class InvalidDiaryTypeException extends CustomException {
    public InvalidDiaryTypeException() {
        super(ErrorCode.INVALID_DIARY_TYPE);
    }
}
