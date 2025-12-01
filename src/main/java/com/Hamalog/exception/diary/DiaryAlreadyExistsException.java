package com.Hamalog.exception.diary;
import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
public class DiaryAlreadyExistsException extends CustomException {
    public DiaryAlreadyExistsException() {
        super(ErrorCode.DIARY_ALREADY_EXISTS);
    }
}
