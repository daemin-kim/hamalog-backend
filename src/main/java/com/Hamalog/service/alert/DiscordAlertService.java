package com.Hamalog.service.alert;

import com.Hamalog.config.AlertProperties;
import com.Hamalog.handler.ErrorSeverity;
import com.Hamalog.service.queue.message.NotificationMessage;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Discord 알림 서비스
 * 서버 에러 및 DLQ 적재 시 Discord로 알림을 발송합니다.
 * Rate Limiting을 적용하여 알림 폭탄을 방지합니다.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "hamalog.alert.discord.enabled", havingValue = "true")
public class DiscordAlertService {

    private final AlertProperties alertProperties;
    private final RestTemplate restTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.of("Asia/Seoul"));

    private static final String RATE_LIMIT_KEY_PREFIX = "hamalog:alert:ratelimit:";

    // Discord Embed 색상
    private static final int COLOR_CRITICAL = 15158332; // Red
    private static final int COLOR_HIGH = 15105570;     // Orange
    private static final int COLOR_MEDIUM = 16776960;   // Yellow
    private static final int COLOR_DLQ = 10181046;      // Purple

    public DiscordAlertService(
            AlertProperties alertProperties,
            RestTemplate restTemplate,
            RedisTemplate<String, Object> redisTemplate
    ) {
        this.alertProperties = alertProperties;
        this.restTemplate = restTemplate;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 서버 에러 알림 발송
     *
     * @param ex 발생한 예외
     * @param request HTTP 요청 정보
     * @param severity 에러 심각도
     */
    @Async("eventExecutor")
    public void sendServerErrorAlert(Exception ex, HttpServletRequest request, ErrorSeverity severity) {
        if (!shouldSendAlert(severity)) {
            return;
        }

        String errorKey = generateErrorKey(ex);
        if (!checkRateLimit(errorKey)) {
            log.debug("Rate limit exceeded for error: {}", errorKey);
            return;
        }

        try {
            Map<String, Object> payload = buildServerErrorPayload(ex, request, severity);
            sendWebhook(alertProperties.discord().errorWebhookUrl(), payload);
            log.info("Discord server error alert sent: {} - {}",
                    ex.getClass().getSimpleName(), severity);
        } catch (RestClientException e) {
            log.error("Failed to send Discord server error alert: {}", e.getMessage());
        }
    }

    /**
     * DLQ 적재 알림 발송
     *
     * @param message 실패한 알림 메시지
     * @param errorMessage 에러 메시지
     */
    @Async("eventExecutor")
    public void sendDeadLetterAlert(NotificationMessage message, String errorMessage) {
        if (!alertProperties.discord().isDlqWebhookConfigured()) {
            log.debug("Discord DLQ webhook not configured, skipping alert");
            return;
        }

        String dlqKey = "dlq:" + message.messageId();
        if (!checkRateLimit(dlqKey)) {
            log.debug("Rate limit exceeded for DLQ message: {}", message.messageId());
            return;
        }

        try {
            Map<String, Object> payload = buildDlqPayload(message, errorMessage);
            sendWebhook(alertProperties.discord().dlqWebhookUrl(), payload);
            log.info("Discord DLQ alert sent for messageId: {}", message.messageId());
        } catch (RestClientException e) {
            log.error("Failed to send Discord DLQ alert: {}", e.getMessage());
        }
    }

    /**
     * 알림 발송 여부 결정
     */
    private boolean shouldSendAlert(ErrorSeverity severity) {
        if (!alertProperties.discord().isErrorWebhookConfigured()) {
            log.debug("Discord error webhook not configured, skipping alert");
            return false;
        }

        if (!alertProperties.discord().shouldAlert(severity)) {
            log.debug("Severity {} below minimum alert level {}",
                    severity, alertProperties.discord().minSeverity());
            return false;
        }

        return true;
    }

    /**
     * Rate Limit 확인 및 카운터 증가
     *
     * @param key 에러 식별 키
     * @return 알림 발송 가능 여부
     */
    private boolean checkRateLimit(String key) {
        String redisKey = RATE_LIMIT_KEY_PREFIX + key;

        try {
            Long count = redisTemplate.opsForValue().increment(redisKey);

            if (count == null) {
                return true;
            }

            // 첫 번째 요청인 경우 TTL 설정
            if (count == 1L) {
                redisTemplate.expire(redisKey,
                        alertProperties.rateLimit().windowSeconds(),
                        TimeUnit.SECONDS);
            }

            return count <= alertProperties.rateLimit().maxAlertsPerHour();
        } catch (Exception e) {
            // Redis 연결 실패 시 알림 허용 (fail-open)
            log.warn("Rate limit check failed, allowing alert: {}", e.getMessage());
            return true;
        }
    }

    /**
     * 에러 식별 키 생성
     * 동일 예외 타입 + 메시지 조합으로 중복 알림 방지
     */
    private String generateErrorKey(Exception ex) {
        String exceptionName = ex.getClass().getSimpleName();
        String message = ex.getMessage();
        int messageHash = message != null ? message.hashCode() : 0;
        return exceptionName + ":" + messageHash;
    }

    /**
     * 서버 에러 Payload 생성
     */
    private Map<String, Object> buildServerErrorPayload(
            Exception ex, HttpServletRequest request, ErrorSeverity severity) {

        String timestamp = FORMATTER.format(Instant.now());
        int color = getColorBySeverity(severity);

        String stackTrace = getStackTraceSummary(ex, 5);

        Map<String, Object> embed = Map.of(
                "title", getAlertTitle(severity),
                "color", color,
                "fields", List.of(
                        Map.of("name", "예외 타입", "value", ex.getClass().getSimpleName(), "inline", true),
                        Map.of("name", "심각도", "value", severity.name(), "inline", true),
                        Map.of("name", "발생 시간", "value", timestamp, "inline", true),
                        Map.of("name", "요청 경로", "value", getRequestInfo(request), "inline", false),
                        Map.of("name", "에러 메시지", "value", truncate(ex.getMessage(), 500), "inline", false),
                        Map.of("name", "스택 트레이스", "value", "```\n" + stackTrace + "\n```", "inline", false)
                ),
                "footer", Map.of(
                        "text", "Hamalog Server Alert"
                )
        );

        return Map.of(
                "username", "Hamalog Alert Bot",
                "embeds", List.of(embed)
        );
    }

    /**
     * DLQ Payload 생성
     */
    private Map<String, Object> buildDlqPayload(NotificationMessage message, String errorMessage) {
        String timestamp = FORMATTER.format(Instant.now());
        String originalTimestamp = FORMATTER.format(message.createdAt());

        Map<String, Object> embed = Map.of(
                "title", "🚨 Dead Letter Queue 알림",
                "color", COLOR_DLQ,
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
     * Webhook 발송
     */
    private void sendWebhook(String webhookUrl, Map<String, Object> payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
        restTemplate.postForEntity(webhookUrl, request, String.class);
    }

    /**
     * 심각도에 따른 알림 제목
     */
    private String getAlertTitle(ErrorSeverity severity) {
        return switch (severity) {
            case CRITICAL -> "🔴 CRITICAL: 시스템 장애 발생";
            case HIGH -> "🟠 HIGH: 보안/인증 오류 발생";
            case MEDIUM -> "🟡 MEDIUM: 처리 오류 발생";
            case LOW -> "🔵 LOW: 일반 오류 발생";
        };
    }

    /**
     * 심각도에 따른 색상
     */
    private int getColorBySeverity(ErrorSeverity severity) {
        return switch (severity) {
            case CRITICAL -> COLOR_CRITICAL;
            case HIGH -> COLOR_HIGH;
            case MEDIUM -> COLOR_MEDIUM;
            case LOW -> 3447003; // Blue
        };
    }

    /**
     * 요청 정보 문자열 생성
     */
    private String getRequestInfo(HttpServletRequest request) {
        if (request == null) {
            return "N/A";
        }
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();

        if (query != null) {
            return String.format("%s %s?%s", method, uri, query);
        }
        return String.format("%s %s", method, uri);
    }

    /**
     * 스택 트레이스 요약
     */
    private String getStackTraceSummary(Exception ex, int lines) {
        StackTraceElement[] stackTrace = ex.getStackTrace();
        StringBuilder sb = new StringBuilder();

        int limit = Math.min(lines, stackTrace.length);
        for (int i = 0; i < limit; i++) {
            sb.append(stackTrace[i].toString());
            if (i < limit - 1) {
                sb.append("\n");
            }
        }

        if (stackTrace.length > lines) {
            sb.append("\n... ").append(stackTrace.length - lines).append(" more");
        }

        return sb.toString();
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
