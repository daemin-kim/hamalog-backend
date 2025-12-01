package com.Hamalog.exception.diary;
import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
public class MoodDiaryNotFoundException extends CustomException {
    public MoodDiaryNotFoundException() {
        super(ErrorCode.MOOD_DIARY_NOT_FOUND);
    }
    public MoodDiaryNotFoundException(String message) {
        super(ErrorCode.MOOD_DIARY_NOT_FOUND, message);
    }
}
