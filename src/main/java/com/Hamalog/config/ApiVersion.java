package com.Hamalog.config;

/**
 * API 버전 관리를 위한 상수 클래스
 */
public final class ApiVersion {

    private ApiVersion() {
        // 유틸리티 클래스 - 인스턴스 생성 방지
    }

    /**
     * 현재 API 버전
     */
    public static final String CURRENT = "/api/v1";

    /**
     * 인증 관련 API 경로
     */
    public static final String AUTH = CURRENT + "/auth";

    /**
     * 복약 스케줄 API 경로
     */
    public static final String MEDICATION_SCHEDULE = CURRENT + "/medication-schedule";

    /**
     * 복약 기록 API 경로
     */
    public static final String MEDICATION_RECORD = CURRENT + "/medication-record";

    /**
     * 마음 일기 API 경로
     */
    public static final String MOOD_DIARY = CURRENT + "/mood-diary";

    /**
     * 부작용 API 경로
     */
    public static final String SIDE_EFFECT = CURRENT + "/side-effect";

    /**
     * OAuth2 API 경로
     */
    public static final String OAUTH2 = CURRENT + "/oauth2";
}
