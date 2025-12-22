package com.Hamalog.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // Business Logic Errors - Member
    MEMBER_NOT_FOUND("MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다."),
    DUPLICATE_MEMBER("DUPLICATE_MEMBER", "이미 존재하는 회원입니다."),
    INVALID_CURRENT_PASSWORD("INVALID_CURRENT_PASSWORD", "현재 비밀번호가 일치하지 않습니다."),
    PASSWORD_CONFIRM_MISMATCH("PASSWORD_CONFIRM_MISMATCH", "새 비밀번호와 확인 비밀번호가 일치하지 않습니다."),
    SAME_AS_CURRENT_PASSWORD("SAME_AS_CURRENT_PASSWORD", "새 비밀번호는 현재 비밀번호와 달라야 합니다."),
    NO_PROFILE_UPDATE_DATA("NO_PROFILE_UPDATE_DATA", "수정할 프로필 정보가 없습니다."),

    // Business Logic Errors - Medication
    MEDICATION_SCHEDULE_NOT_FOUND("SCHEDULE_NOT_FOUND", "복약 스케줄을 찾을 수 없습니다."),
    MEDICATION_RECORD_NOT_FOUND("RECORD_NOT_FOUND", "복약 기록을 찾을 수 없습니다."),
    MEDICATION_TIME_NOT_FOUND("TIME_NOT_FOUND", "복약 시간 정보를 찾을 수 없습니다."),
    INVALID_MEDICATION_SCHEDULE("INVALID_SCHEDULE", "유효하지 않은 복약 스케줄입니다."),
    INVALID_PRESCRIPTION_DAYS("INVALID_PRESCRIPTION_DAYS", "처방 일수는 1일 이상이어야 합니다."),
    INVALID_PER_DAY("INVALID_PER_DAY", "1일 복용 횟수는 1회 이상이어야 합니다."),
    INVALID_DATE_RANGE("INVALID_DATE_RANGE", "시작일은 처방일 이후여야 합니다."),

    // Business Logic Errors - Side Effect
    SIDE_EFFECT_NOT_FOUND("SIDE_EFFECT_NOT_FOUND", "부작용 정보를 찾을 수 없습니다."),
    INVALID_SIDE_EFFECT_DEGREE("INVALID_DEGREE", "부작용 정도는 1-5 사이여야 합니다."),
    EMPTY_SIDE_EFFECT_LIST("EMPTY_SIDE_EFFECT_LIST", "부작용 목록이 비어있습니다."),

    // Business Logic Errors - Mood Diary
    MOOD_DIARY_NOT_FOUND("MOOD_DIARY_NOT_FOUND", "마음 일기를 찾을 수 없습니다."),
    DIARY_ALREADY_EXISTS("DIARY_ALREADY_EXISTS", "해당 날짜에 이미 일기가 작성되어 있습니다."),
    INVALID_DIARY_TYPE("INVALID_DIARY_TYPE", "유효하지 않은 일기 형식입니다."),
    INVALID_MOOD_TYPE("INVALID_MOOD_TYPE", "유효하지 않은 기분 타입입니다."),
    TEMPLATE_ANSWER_REQUIRED("TEMPLATE_ANSWER_REQUIRED", "템플릿 형식에서는 모든 질문에 답변이 필요합니다."),
    FREE_CONTENT_REQUIRED("FREE_CONTENT_REQUIRED", "자유 형식에서는 내용이 필요합니다."),

    // Authorization and Security Errors
    UNAUTHORIZED("UNAUTHORIZED", "인증이 필요합니다."),
    FORBIDDEN("FORBIDDEN", "접근 권한이 없습니다."),
    INVALID_TOKEN("INVALID_TOKEN", "유효하지 않은 토큰입니다."),
    TOKEN_EXPIRED("TOKEN_EXPIRED", "토큰이 만료되었습니다."),
    TOKEN_BLACKLISTED("TOKEN_BLACKLISTED", "무효화된 토큰입니다."),
    INVALID_REFRESH_TOKEN("INVALID_REFRESH_TOKEN", "유효하지 않은 Refresh Token입니다."),
    REFRESH_TOKEN_EXPIRED("REFRESH_TOKEN_EXPIRED", "Refresh Token이 만료되었습니다."),
    REFRESH_TOKEN_REVOKED("REFRESH_TOKEN_REVOKED", "폐기된 Refresh Token입니다."),

    // OAuth2 Errors
    OAUTH2_CONFIG_ERROR("OAUTH2_CONFIG_ERROR", "OAuth2 설정 오류가 발생했습니다."),
    OAUTH2_INIT_ERROR("OAUTH2_INIT_ERROR", "OAuth2 초기화 중 오류가 발생했습니다."),
    OAUTH2_TOKEN_EXCHANGE_FAILED("TOKEN_EXCHANGE_FAILED", "토큰 교환에 실패했습니다."),
    OAUTH2_USER_INFO_FAILED("USER_INFO_FAILED", "사용자 정보 조회에 실패했습니다."),
    OAUTH2_INVALID_CODE("INVALID_AUTH_CODE", "유효하지 않은 인증 코드입니다."),
    OAUTH2_STATE_VALIDATION_FAILED("CSRF_VALIDATION_FAILED", "CSRF 검증에 실패했습니다."),
    OAUTH2_AUTHORIZATION_FAILED("AUTHORIZATION_FAILED", "OAuth2 인증에 실패했습니다."),

    // Validation Errors
    BAD_REQUEST("BAD_REQUEST", "잘못된 요청입니다."),
    INVALID_INPUT("INVALID_INPUT", "입력값이 유효하지 않습니다."),
    INVALID_PARAMETER("INVALID_PARAMETER", "파라미터가 유효하지 않습니다."),
    MISSING_REQUIRED_FIELD("MISSING_REQUIRED_FIELD", "필수 필드가 누락되었습니다."),
    INVALID_PAGE_SIZE("INVALID_PAGE_SIZE", "페이지 크기는 1-100 사이여야 합니다."),
    INVALID_PAGE_NUMBER("INVALID_PAGE_NUMBER", "페이지 번호는 0 이상이어야 합니다."),

    // File Errors
    FILE_SAVE_FAIL("FILE_SAVE_FAIL", "파일 저장에 실패했습니다."),
    FILE_SIZE_EXCEEDED("FILE_SIZE_EXCEEDED", "파일 크기가 제한을 초과했습니다."),
    INVALID_FILE_TYPE("INVALID_FILE_TYPE", "지원하지 않는 파일 형식입니다."),
    FILE_NOT_FOUND("FILE_NOT_FOUND", "파일을 찾을 수 없습니다."),

    // Concurrency Errors
    OPTIMISTIC_LOCK_FAILED("OPTIMISTIC_LOCK_FAILED", "다른 사용자가 데이터를 수정했습니다. 다시 시도해주세요."),
    RESOURCE_CONFLICT("RESOURCE_CONFLICT", "리소스 충돌이 발생했습니다."),

    // External API Errors
    EXTERNAL_API_ERROR("EXTERNAL_API_ERROR", "외부 API 호출 중 오류가 발생했습니다."),
    EXTERNAL_API_TIMEOUT("EXTERNAL_API_TIMEOUT", "외부 API 응답 시간이 초과되었습니다."),

    // System Errors
    INTERNAL_SERVER_ERROR("INTERNAL_ERROR", "서버 내부 오류가 발생했습니다."),
    DATABASE_ERROR("DATABASE_ERROR", "데이터베이스 오류가 발생했습니다."),
    CACHE_ERROR("CACHE_ERROR", "캐시 처리 중 오류가 발생했습니다.");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
