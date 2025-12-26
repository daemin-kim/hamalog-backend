package com.Hamalog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * AOP 관련 설정을 관리하는 구성 클래스
 *
 * 현재 활성화된 AOP:
 * - BusinessAuditAspect: 인증 관련 감사 로깅
 * - CachingAspect: 커스텀 캐싱 처리
 */
@Configuration
@EnableAspectJAutoProxy
@ConfigurationProperties(prefix = "app.aop")
@Data
public class AopConfiguration {

    /**
     * 비즈니스 감사 관련 설정
     */
    private Audit audit = new Audit();

    /**
     * 캐시 관련 설정
     */
    private Cache cache = new Cache();

    /**
     * 전체 AOP 기능 활성화 여부
     */
    private boolean globalEnabled = true;

    /**
     * 개발 모드 활성화 (더 상세한 로깅)
     */
    private boolean developmentMode = false;

    /**
     * 스택 트레이스 포함 여부
     */
    private boolean includeStackTrace = false;

    /**
     * MDC 컨텍스트 자동 정리 활성화
     */
    private boolean autoCleanMdcContext = true;

    /**
     * 비동기 처리 지원 활성화
     */
    private boolean asyncSupport = false;

    /**
     * 메트릭 수집 간격 (초)
     */
    private long metricsCollectionInterval = 60;

    /**
     * 로그 압축 활성화 (긴 데이터의 경우)
     */
    private boolean enableLogCompression = false;

    /**
     * 스레드 로컬 정리 활성화
     */
    private boolean cleanupThreadLocals = true;

    /**
     * JVM 메트릭 수집 활성화
     */
    private boolean collectJvmMetrics = true;

    @Data
    public static class Audit {
        /**
         * 감사 로깅 활성화 여부
         */
        private boolean enabled = true;

        /**
         * 로그인 실패 감사 활성화
         */
        private boolean auditLoginFailures = true;

        /**
         * 데이터 변경 감사 활성화
         */
        private boolean auditDataChanges = true;

        /**
         * IP 주소 수집 활성화
         */
        private boolean collectIpAddress = true;

        /**
         * User-Agent 수집 활성화
         */
        private boolean collectUserAgent = true;

        /**
         * 파라미터 로깅 최대 길이
         */
        private int maxParameterLength = 100;

        /**
         * 결과 로깅 최대 길이
         */
        private int maxResultLength = 100;

        /**
         * 민감한 파라미터 마스킹 활성화
         */
        private boolean maskSensitiveParams = true;

        /**
         * 감사 로그 레벨
         */
        private String logLevel = "INFO";
    }

    @Data
    public static class Cache {
        /**
         * 캐시 기능 활성화 여부
         */
        private boolean enabled = true;

        /**
         * 기본 TTL (초)
         */
        private long defaultTtl = 300;

        /**
         * 최대 TTL (초)
         */
        private long maxTtl = 86400; // 24시간

        /**
         * 로컬 캐시 fallback 기본 활성화 여부
         */
        private boolean defaultUseLocalFallback = true;

        /**
         * 로컬 캐시 최대 크기
         */
        private int localCacheMaxSize = 1000;

        /**
         * 캐시 통계 수집 활성화
         */
        private boolean collectStats = true;

        /**
         * Redis 연결 실패 시 로컬 캐시 사용 여부
         */
        private boolean fallbackOnRedisFailure = true;
    }
}