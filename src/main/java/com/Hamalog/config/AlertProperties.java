package com.Hamalog.config;

import com.Hamalog.handler.ErrorSeverity;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 알림 설정 프로퍼티
 * 서버 에러 및 DLQ 알림의 Discord 웹훅 설정을 관리합니다.
 */
@ConfigurationProperties(prefix = "hamalog.alert")
public record AlertProperties(
        DiscordConfig discord,
        RateLimitConfig rateLimit
) {
    /**
     * 기본값 적용 생성자
     */
    public AlertProperties {
        if (discord == null) {
            discord = new DiscordConfig(false, "", "", ErrorSeverity.HIGH);
        }
        if (rateLimit == null) {
            rateLimit = new RateLimitConfig(10, 3600);
        }
    }

    /**
     * Discord 웹훅 설정
     */
    public record DiscordConfig(
            boolean enabled,
            String errorWebhookUrl,
            String dlqWebhookUrl,
            ErrorSeverity minSeverity
    ) {
        public DiscordConfig {
            if (errorWebhookUrl == null) {
                errorWebhookUrl = "";
            }
            if (dlqWebhookUrl == null) {
                dlqWebhookUrl = "";
            }
            if (minSeverity == null) {
                minSeverity = ErrorSeverity.HIGH;
            }
        }

        /**
         * 에러 웹훅 URL이 설정되었는지 확인
         */
        public boolean isErrorWebhookConfigured() {
            return enabled && errorWebhookUrl != null && !errorWebhookUrl.isBlank();
        }

        /**
         * DLQ 웹훅 URL이 설정되었는지 확인
         */
        public boolean isDlqWebhookConfigured() {
            return enabled && dlqWebhookUrl != null && !dlqWebhookUrl.isBlank();
        }

        /**
         * 지정된 심각도가 최소 알림 레벨 이상인지 확인
         */
        public boolean shouldAlert(ErrorSeverity severity) {
            return severity.ordinal() >= minSeverity.ordinal();
        }
    }

    /**
     * Rate Limiting 설정
     * 동일 에러에 대한 알림 폭탄 방지
     */
    public record RateLimitConfig(
            int maxAlertsPerHour,
            int windowSeconds
    ) {
        public RateLimitConfig {
            if (maxAlertsPerHour <= 0) {
                maxAlertsPerHour = 10;
            }
            if (windowSeconds <= 0) {
                windowSeconds = 3600;
            }
        }
    }
}
