package com.Hamalog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * AOP 관련 설정을 관리하는 구성 클래스
 */
@Configuration
@EnableAspectJAutoProxy
@ConfigurationProperties(prefix = "app.aop")
@Data
public class AopConfiguration {

    /**
     * API 로깅 관련 설정
     */
    private ApiLogging apiLogging = new ApiLogging();

    /**
     * 성능 모니터링 관련 설정
     */
    private Performance performance = new Performance();

    /**
     * 비즈니스 감사 관련 설정
     */
    private Audit audit = new Audit();

    /**
     * 재시도 관련 설정
     */
    private Retry retry = new Retry();

    /**
     * 캐시 관련 설정
     */
    private Cache cache = new Cache();

    @Data
    public static class ApiLogging {
        /**
         * API 로깅 활성화 여부
         */
        private boolean enabled = true;

        /**
         * 응답 데이터 최대 길이 (로그에 표시될)
         */
        private int maxResponseLength = 200;

        /**
         * 파라미터 데이터 최대 길이
         */
        private int maxParameterLength = 200;

        /**
         * 민감한 파라미터 키워드들
         */
        private String[] sensitiveKeywords = {"password", "token", "authorization", "secret", "key"};

        /**
         * 성능 임계값 (밀리초)
         */
        private Performance performance = new Performance();

        @Data
        public static class Performance {
            private long veryFastThreshold = 100;
            private long fastThreshold = 500;
            private long moderateThreshold = 1000;
            private long slowThreshold = 3000;
        }
    }

    @Data
    public static class Performance {
        /**
         * 성능 모니터링 활성화 여부
         */
        private boolean enabled = true;

        /**
         * 느린 실행 임계값 (밀리초)
         */
        private long slowThreshold = 1000;

        /**
         * 매우 느린 실행 임계값 (밀리초)
         */
        private long verySlowThreshold = 3000;

        /**
         * 통계 수집 활성화 여부
         */
        private boolean collectStats = true;

        /**
         * 통계 초기화 주기 (시간, 0이면 초기화하지 않음)
         */
        private long statsResetInterval = 24;

        /**
         * 상세 로그 레벨 (DEBUG, INFO, WARN, ERROR)
         */
        private String logLevel = "DEBUG";

        /**
         * 나노초 단위 측정 활성화
         */
        private boolean enableNanoPrecision = true;
    }

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
    public static class Retry {
        /**
         * 재시도 기능 활성화 여부
         */
        private boolean enabled = true;

        /**
         * 기본 최대 재시도 횟수
         */
        private int defaultMaxAttempts = 3;

        /**
         * 기본 재시도 지연 시간 (밀리초)
         */
        private long defaultDelay = 1000;

        /**
         * 최대 지연 시간 (밀리초)
         */
        private long maxDelay = 10000;

        /**
         * 기본 백오프 승수
         */
        private double defaultBackoffMultiplier = 2.0;

        /**
         * 기본 지연 시간 랜덤화 여부
         */
        private boolean defaultRandomizeDelay = true;

        /**
         * 재시도 통계 수집 활성화
         */
        private boolean collectStats = true;

        /**
         * 재시도 로그 레벨
         */
        private String logLevel = "WARN";
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
         * 로컬 캐시 정리 주기 (분)
         */
        private long localCacheCleanupInterval = 30;

        /**
         * null 값 캐시 기본 설정
         */
        private boolean defaultCacheNull = false;

        /**
         * 캐시 통계 수집 활성화
         */
        private boolean collectStats = true;

        /**
         * 캐시 키 최대 길이
         */
        private int maxKeyLength = 250;

        /**
         * 캐시 로그 레벨
         */
        private String logLevel = "DEBUG";

        /**
         * Redis 연결 실패 시 로컬 캐시 사용 여부
         */
        private boolean fallbackOnRedisFailure = true;
    }

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
}