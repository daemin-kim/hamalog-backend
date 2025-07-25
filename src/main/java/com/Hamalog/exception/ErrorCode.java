package com.Hamalog.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    MEMBER_NOT_FOUND("MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다."),
    MEDICATION_SCHEDULE_NOT_FOUND("SCHEDULE_NOT_FOUND", "복약 스케줄을 찾을 수 없습니다."),
    MEDICATION_RECORD_NOT_FOUND("RECORD_NOT_FOUND", "복약 기록을 찾을 수 없습니다."),
    MEDICATION_TIME_NOT_FOUND("TIME_NOT_FOUND", "복약 시간 정보를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR("INTERNAL_ERROR", "서버 내부 오류가 발생했습니다."),
    FILE_SAVE_FAIL("FILE_SAVE_FAIL", "파일 저장에 실패했습니다.");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
