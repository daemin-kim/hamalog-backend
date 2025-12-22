package com.Hamalog.config;

/**
 * API 경로 관리를 위한 상수 클래스
 */
public final class ApiVersion {

    private ApiVersion() {
        // 유틸리티 클래스 - 인스턴스 생성 방지
    }

    /**
     * API 기본 경로 (버전 프리픽스 없음)
     */
    public static final String CURRENT = "";

    /**
     * 인증 관련 API 경로
     */
    public static final String AUTH = "/auth";

    /**
     * 복약 스케줄 API 경로
     */
    public static final String MEDICATION_SCHEDULE = "/medication-schedule";

    /**
     * 복약 기록 API 경로
     */
    public static final String MEDICATION_RECORD = "/medication-record";

    /**
     * 마음 일기 API 경로
     */
    public static final String MOOD_DIARY = "/mood-diary";

    /**
     * 부작용 API 경로
     */
    public static final String SIDE_EFFECT = "/side-effect";

    /**
     * OAuth2 API 경로
     */
    public static final String OAUTH2 = "/oauth2";

    /**
     * 회원 프로필 API 경로
     */
    public static final String MEMBER = "/member";
}
