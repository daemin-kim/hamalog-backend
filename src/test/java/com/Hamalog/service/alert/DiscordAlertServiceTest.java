package com.Hamalog.service.alert;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.Hamalog.config.AlertProperties;
import com.Hamalog.handler.ErrorSeverity;
import com.Hamalog.service.queue.message.NotificationMessage;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@DisplayName("DiscordAlertService 테스트")
class DiscordAlertServiceTest {

    private DiscordAlertService discordAlertService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private AlertProperties alertProperties;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.increment(anyString())).thenReturn(1L);
        lenient().when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
    }

    @Nested
    @DisplayName("서버 에러 알림")
    class ServerErrorAlert {

        @Test
        @DisplayName("성공: CRITICAL 에러 발생 시 Discord 알림 발송")
        void sendCriticalErrorAlert() {
            // given
            alertProperties = createAlertProperties(true, "https://error.webhook", "", ErrorSeverity.HIGH);
            discordAlertService = new DiscordAlertService(alertProperties, restTemplate, redisTemplate);

            Exception exception = new RuntimeException("Critical system error");
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setMethod("GET");
            request.setRequestURI("/api/test");

            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(""));

            // when
            discordAlertService.sendServerErrorAlert(exception, request, ErrorSeverity.CRITICAL);

            // then
            verify(restTemplate).postForEntity(
                    eq("https://error.webhook"),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }

        @Test
        @DisplayName("성공: HIGH 에러 발생 시 Discord 알림 발송")
        void sendHighErrorAlert() {
            // given
            alertProperties = createAlertProperties(true, "https://error.webhook", "", ErrorSeverity.HIGH);
            discordAlertService = new DiscordAlertService(alertProperties, restTemplate, redisTemplate);

            Exception exception = new SecurityException("Security violation");
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setMethod("POST");
            request.setRequestURI("/api/auth/login");

            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(""));

            // when
            discordAlertService.sendServerErrorAlert(exception, request, ErrorSeverity.HIGH);

            // then
            verify(restTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
        }

        @Test
        @DisplayName("스킵: minSeverity보다 낮은 심각도는 알림 발송하지 않음")
        void skipLowSeverityAlert() {
            // given
            alertProperties = createAlertProperties(true, "https://error.webhook", "", ErrorSeverity.HIGH);
            discordAlertService = new DiscordAlertService(alertProperties, restTemplate, redisTemplate);

            Exception exception = new IllegalArgumentException("Validation error");
            MockHttpServletRequest request = new MockHttpServletRequest();

            // when
            discordAlertService.sendServerErrorAlert(exception, request, ErrorSeverity.MEDIUM);

            // then
            verifyNoInteractions(restTemplate);
        }

        @Test
        @DisplayName("스킵: 웹훅 URL이 설정되지 않으면 알림 발송하지 않음")
        void skipWhenWebhookNotConfigured() {
            // given
            alertProperties = createAlertProperties(true, "", "", ErrorSeverity.HIGH);
            discordAlertService = new DiscordAlertService(alertProperties, restTemplate, redisTemplate);

            Exception exception = new RuntimeException("Error");
            MockHttpServletRequest request = new MockHttpServletRequest();

            // when
            discordAlertService.sendServerErrorAlert(exception, request, ErrorSeverity.CRITICAL);

            // then
            verifyNoInteractions(restTemplate);
        }
    }

    @Nested
    @DisplayName("Rate Limiting")
    class RateLimiting {

        @Test
        @DisplayName("Rate limit 초과 시 알림 발송하지 않음")
        void rateLimitExceeded() {
            // given
            alertProperties = createAlertProperties(true, "https://error.webhook", "", ErrorSeverity.HIGH, 5);
            discordAlertService = new DiscordAlertService(alertProperties, restTemplate, redisTemplate);

            // Rate limit 초과 상황 시뮬레이션
            when(valueOperations.increment(anyString())).thenReturn(6L);

            Exception exception = new RuntimeException("Error");
            MockHttpServletRequest request = new MockHttpServletRequest();

            // when
            discordAlertService.sendServerErrorAlert(exception, request, ErrorSeverity.CRITICAL);

            // then
            verifyNoInteractions(restTemplate);
        }

        @Test
        @DisplayName("Rate limit 이내 시 알림 정상 발송")
        void withinRateLimit() {
            // given
            alertProperties = createAlertProperties(true, "https://error.webhook", "", ErrorSeverity.HIGH, 10);
            discordAlertService = new DiscordAlertService(alertProperties, restTemplate, redisTemplate);

            when(valueOperations.increment(anyString())).thenReturn(5L);
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(""));

            Exception exception = new RuntimeException("Error");
            MockHttpServletRequest request = new MockHttpServletRequest();

            // when
            discordAlertService.sendServerErrorAlert(exception, request, ErrorSeverity.CRITICAL);

            // then
            verify(restTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
        }
    }

    @Nested
    @DisplayName("DLQ 알림")
    class DlqAlert {

        @Test
        @DisplayName("성공: DLQ 메시지 적재 시 Discord 알림 발송")
        void sendDlqAlert() {
            // given
            alertProperties = createAlertProperties(true, "", "https://dlq.webhook", ErrorSeverity.HIGH);
            discordAlertService = new DiscordAlertService(alertProperties, restTemplate, redisTemplate);

            NotificationMessage message = new NotificationMessage(
                    "msg-001",
                    1L,
                    "복약 알림",
                    "약 복용 시간입니다",
                    Map.of(),
                    "MEDICATION_REMINDER",
                    Instant.now(),
                    3
            );

            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(""));

            // when
            discordAlertService.sendDeadLetterAlert(message, "Max retries exceeded");

            // then
            verify(restTemplate).postForEntity(
                    eq("https://dlq.webhook"),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }

        @Test
        @DisplayName("스킵: DLQ 웹훅 URL이 없으면 알림 발송하지 않음")
        void skipDlqAlertWhenNotConfigured() {
            // given
            alertProperties = createAlertProperties(true, "https://error.webhook", "", ErrorSeverity.HIGH);
            discordAlertService = new DiscordAlertService(alertProperties, restTemplate, redisTemplate);

            NotificationMessage message = new NotificationMessage(
                    "msg-001", 1L, "Title", "Body", Map.of(), "TEST", Instant.now(), 3
            );

            // when
            discordAlertService.sendDeadLetterAlert(message, "Error");

            // then
            verifyNoInteractions(restTemplate);
        }
    }

    // ==================== Helper Methods ====================

    private AlertProperties createAlertProperties(
            boolean enabled, String errorUrl, String dlqUrl, ErrorSeverity minSeverity) {
        return createAlertProperties(enabled, errorUrl, dlqUrl, minSeverity, 10);
    }

    private AlertProperties createAlertProperties(
            boolean enabled, String errorUrl, String dlqUrl, ErrorSeverity minSeverity, int maxAlertsPerHour) {
        return new AlertProperties(
                new AlertProperties.DiscordConfig(enabled, errorUrl, dlqUrl, minSeverity),
                new AlertProperties.RateLimitConfig(maxAlertsPerHour, 3600)
        );
    }
}
