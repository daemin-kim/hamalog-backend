package com.Hamalog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 메시지 큐 설정 프로퍼티
 * Redis Stream 기반 메시지 큐의 동작을 제어합니다.
 */
@ConfigurationProperties(prefix = "hamalog.queue")
public record MessageQueueProperties(
        boolean enabled,
        String notificationStream,
        String deadLetterStream,
        String consumerGroup,
        String consumerName,
        int maxRetries,
        int pollTimeoutSeconds,
        int batchSize,
        DiscordWebhook discord
) {
    /**
     * 기본값 적용 생성자
     */
    public MessageQueueProperties {
        if (notificationStream == null || notificationStream.isBlank()) {
            notificationStream = "hamalog:notifications";
        }
        if (deadLetterStream == null || deadLetterStream.isBlank()) {
            deadLetterStream = "hamalog:notifications:dlq";
        }
        if (consumerGroup == null || consumerGroup.isBlank()) {
            consumerGroup = "notification-consumers";
        }
        if (consumerName == null || consumerName.isBlank()) {
            consumerName = "consumer-1";
        }
        if (maxRetries <= 0) {
            maxRetries = 3;
        }
        if (pollTimeoutSeconds <= 0) {
            pollTimeoutSeconds = 5;
        }
        if (batchSize <= 0) {
            batchSize = 10;
        }
    }

    /**
     * Discord Webhook 설정
     */
    public record DiscordWebhook(
            boolean enabled,
            String webhookUrl
    ) {
        public DiscordWebhook {
            if (webhookUrl == null) {
                webhookUrl = "";
            }
        }
    }
}
