package com.Hamalog.service.queue;

import com.Hamalog.config.MessageQueueProperties;
import com.Hamalog.service.queue.message.NotificationMessage;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Discord Webhook 알림 서비스
 * Dead Letter Queue에 메시지가 적재될 때 Discord로 알림을 발송합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "hamalog.queue.discord.enabled", havingValue = "true")
public class DiscordWebhookService {

    private final MessageQueueProperties queueProperties;
    private final RestTemplate restTemplate;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.of("Asia/Seoul"));

    /**
     * DLQ 적재 알림 발송
     *
     * @param message 실패한 알림 메시지
     * @param errorMessage 에러 메시지
     */
    @Async("eventExecutor")
    public void sendDeadLetterAlert(NotificationMessage message, String errorMessage) {
        if (!isWebhookConfigured()) {
            log.debug("Discord webhook not configured, skipping alert");
            return;
        }

        try {
            Map<String, Object> payload = buildPayload(message, errorMessage);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            restTemplate.postForEntity(
                    queueProperties.discord().webhookUrl(),
                    request,
                    String.class
            );

            log.info("Discord DLQ alert sent for messageId: {}", message.messageId());
        } catch (RestClientException e) {
            log.error("Failed to send Discord webhook alert: {}", e.getMessage());
        }
    }

    /**
     * Discord 메시지 Payload 생성
     */
    private Map<String, Object> buildPayload(NotificationMessage message, String errorMessage) {
        String timestamp = FORMATTER.format(Instant.now());
        String originalTimestamp = FORMATTER.format(message.createdAt());

        // Discord Embed 형식
        Map<String, Object> embed = Map.of(
                "title", "🚨 Dead Letter Queue 알림",
                "color", 15158332, // Red color
                "fields", List.of(
                        Map.of("name", "메시지 ID", "value", message.messageId(), "inline", true),
                        Map.of("name", "회원 ID", "value", String.valueOf(message.memberId()), "inline", true),
                        Map.of("name", "알림 유형", "value", message.notificationType(), "inline", true),
                        Map.of("name", "제목", "value", message.title(), "inline", false),
                        Map.of("name", "재시도 횟수", "value", String.valueOf(message.retryCount()), "inline", true),
                        Map.of("name", "최초 생성 시간", "value", originalTimestamp, "inline", true),
                        Map.of("name", "DLQ 적재 시간", "value", timestamp, "inline", true),
                        Map.of("name", "에러 메시지", "value", truncate(errorMessage, 500), "inline", false)
                ),
                "footer", Map.of(
                        "text", "Hamalog Message Queue"
                )
        );

        return Map.of(
                "username", "Hamalog DLQ Monitor",
                "embeds", List.of(embed)
        );
    }

    /**
     * Webhook 설정 여부 확인
     */
    private boolean isWebhookConfigured() {
        return queueProperties.discord() != null
                && queueProperties.discord().enabled()
                && queueProperties.discord().webhookUrl() != null
                && !queueProperties.discord().webhookUrl().isBlank();
    }

    /**
     * 문자열 길이 제한
     */
    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "N/A";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}
