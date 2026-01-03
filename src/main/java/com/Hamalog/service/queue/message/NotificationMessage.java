package com.Hamalog.service.queue.message;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * 푸시 알림 메시지 DTO
 * Redis Stream으로 전송되는 알림 메시지 구조
 */
public record NotificationMessage(
        String messageId,
        Long memberId,
        String title,
        String body,
        Map<String, String> data,
        String notificationType,
        Instant createdAt,
        int retryCount
) {
    /**
     * 새 알림 메시지 생성
     */
    public static NotificationMessage of(
            Long memberId,
            String title,
            String body,
            Map<String, String> data,
            String notificationType
    ) {
        return new NotificationMessage(
                UUID.randomUUID().toString(),
                memberId,
                title,
                body,
                data,
                notificationType,
                Instant.now(),
                0
        );
    }

    /**
     * 재시도 횟수 증가
     */
    public NotificationMessage withIncrementedRetry() {
        return new NotificationMessage(
                this.messageId,
                this.memberId,
                this.title,
                this.body,
                this.data,
                this.notificationType,
                this.createdAt,
                this.retryCount + 1
        );
    }

    /**
     * 최대 재시도 횟수 초과 여부
     */
    public boolean hasExceededMaxRetries(int maxRetries) {
        return this.retryCount >= maxRetries;
    }
}
