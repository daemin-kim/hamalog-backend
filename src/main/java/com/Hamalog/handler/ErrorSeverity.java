package com.Hamalog.handler;

/**
 * 에러 심각도 레벨
 * 알림 및 모니터링 시스템에서 사용
 */
public enum ErrorSeverity {
    /** 낮은 우선순위 - 일반적인 비즈니스 에러 */
    LOW,
    /** 중간 우선순위 - 검증 에러, 데이터 충돌 */
    MEDIUM,
    /** 높은 우선순위 - 보안 관련 에러 */
    HIGH,
    /** 치명적 - 시스템 장애, 즉시 조치 필요 */
    CRITICAL
}
