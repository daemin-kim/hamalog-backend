package com.Hamalog.service.queue;

import com.Hamalog.config.MessageQueueProperties;
import com.Hamalog.domain.notification.FcmDeviceToken;
import com.Hamalog.domain.notification.NotificationSettings;
import com.Hamalog.repository.notification.FcmDeviceTokenRepository;
import com.Hamalog.repository.notification.NotificationSettingsRepository;
import com.Hamalog.service.alert.DiscordAlertService;
import com.Hamalog.service.queue.message.NotificationMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 알림 메시지 Consumer 서비스
 * Redis Stream에서 메시지를 소비하여 FCM 푸시 알림을 발송합니다.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "hamalog.queue.enabled", havingValue = "true")
public class NotificationConsumerService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final MessageQueueProperties queueProperties;
    private final MessageQueueService messageQueueService;
    private final DiscordAlertService discordAlertService;
    private final FcmDeviceTokenRepository fcmDeviceTokenRepository;
    private final NotificationSettingsRepository notificationSettingsRepository;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    private final AtomicBoolean running = new AtomicBoolean(true);

    private Counter processedCounter;
    private Counter failedCounter;
    private Counter dlqCounter;
    private Timer processingTimer;

    public NotificationConsumerService(
            RedisTemplate<String, Object> redisTemplate,
            MessageQueueProperties queueProperties,
            MessageQueueService messageQueueService,
            @Autowired(required = false) DiscordAlertService discordAlertService,
            FcmDeviceTokenRepository fcmDeviceTokenRepository,
            NotificationSettingsRepository notificationSettingsRepository,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry
    ) {
        this.redisTemplate = redisTemplate;
        this.queueProperties = queueProperties;
        this.messageQueueService = messageQueueService;
        this.discordAlertService = discordAlertService;
        this.fcmDeviceTokenRepository = fcmDeviceTokenRepository;
        this.notificationSettingsRepository = notificationSettingsRepository;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    void init() {
        // 메트릭 초기화
        processedCounter = Counter.builder("hamalog.queue.messages.processed")
                .description("Total messages processed successfully")
                .register(meterRegistry);
        failedCounter = Counter.builder("hamalog.queue.messages.failed")
                .description("Total messages failed to process")
                .register(meterRegistry);
        dlqCounter = Counter.builder("hamalog.queue.messages.dlq")
                .description("Total messages moved to DLQ")
                .register(meterRegistry);
        processingTimer = Timer.builder("hamalog.queue.processing.time")
                .description("Message processing time")
                .register(meterRegistry);

        // Consumer Group 생성
        messageQueueService.createConsumerGroupIfNotExists();
        log.info("NotificationConsumer initialized for stream: {}",
                queueProperties.notificationStream());
    }

    @PreDestroy
    void shutdown() {
        running.set(false);
        log.info("NotificationConsumer shutting down...");
    }

    /**
     * 주기적으로 메시지 소비 (5초 간격)
     * Consumer Group을 사용하여 메시지를 읽고 처리합니다.
     */
    @Scheduled(fixedDelayString = "${hamalog.queue.poll-interval-ms:5000}")
    public void consumeMessages() {
        if (!running.get()) {
            return;
        }

        try {
            // Consumer Group에서 메시지 읽기
            @SuppressWarnings("unchecked")
            List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(
                    Consumer.from(queueProperties.consumerGroup(), queueProperties.consumerName()),
                    StreamReadOptions.empty()
                            .count(queueProperties.batchSize())
                            .block(Duration.ofSeconds(queueProperties.pollTimeoutSeconds())),
                    StreamOffset.create(queueProperties.notificationStream(), ReadOffset.lastConsumed())
            );

            if (records == null || records.isEmpty()) {
                return;
            }

            for (MapRecord<String, Object, Object> record : records) {
                processRecord(record);
            }

        } catch (Exception e) {
            log.error("Error consuming messages from stream: {}", e.getMessage());
        }
    }

    /**
     * 개별 레코드 처리
     */
    private void processRecord(MapRecord<String, Object, Object> record) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String recordId = record.getId().getValue();

        try {
            Map<Object, Object> values = record.getValue();
            String payload = (String) values.get("payload");

            if (payload == null) {
                log.warn("Empty payload for record: {}", recordId);
                acknowledgeMessage(record.getId());
                return;
            }

            NotificationMessage message = objectMapper.readValue(payload, NotificationMessage.class);

            // 메시지 처리
            boolean success = processNotification(message);

            if (success) {
                processedCounter.increment();
                acknowledgeMessage(record.getId());
                log.debug("Successfully processed message: {}", message.messageId());
            } else {
                handleFailure(message, "Notification processing returned false", record.getId());
            }

        } catch (Exception e) {
            log.error("Error processing record {}: {}", recordId, e.getMessage());
            failedCounter.increment();
            // 파싱 실패 시 ACK하여 무한 재시도 방지
            acknowledgeMessage(record.getId());
        } finally {
            sample.stop(processingTimer);
        }
    }

    /**
     * 알림 처리 (FCM 발송)
     *
     * @param message 알림 메시지
     * @return 처리 성공 여부
     */
    private boolean processNotification(NotificationMessage message) {
        Long memberId = message.memberId();

        // 알림 설정 확인
        if (!isPushEnabled(memberId)) {
            log.debug("Push disabled for memberId: {}", memberId);
            return true; // 설정으로 인한 스킵은 성공으로 처리
        }

        // 조용한 시간 확인
        if (isQuietHours(memberId)) {
            log.debug("Quiet hours active for memberId: {}", memberId);
            return true;
        }

        // 활성화된 디바이스 토큰 조회
        List<FcmDeviceToken> tokens = fcmDeviceTokenRepository
                .findByMember_MemberIdAndIsActiveTrue(memberId);

        if (tokens.isEmpty()) {
            log.debug("No active device tokens for memberId: {}", memberId);
            return true;
        }

        // 각 디바이스에 알림 발송
        boolean allSuccess = true;
        for (FcmDeviceToken deviceToken : tokens) {
            try {
                sendToDevice(deviceToken.getToken(), message.title(), message.body(), message.data());
                deviceToken.markAsUsed();
                log.info("Push notification sent to memberId: {}, device: {}",
                        memberId, deviceToken.getDeviceName());
            } catch (FirebaseMessagingException e) {
                handleFirebaseError(deviceToken, e);
                allSuccess = false;
            }
        }

        return allSuccess;
    }

    /**
     * 실패 처리 (재시도 또는 DLQ)
     */
    private void handleFailure(NotificationMessage message, String errorMessage, RecordId recordId) {
        failedCounter.increment();
        NotificationMessage retriedMessage = message.withIncrementedRetry();

        if (retriedMessage.hasExceededMaxRetries(queueProperties.maxRetries())) {
            // 최대 재시도 초과 → DLQ로 이동
            messageQueueService.publishToDeadLetterQueue(retriedMessage, errorMessage);
            dlqCounter.increment();

            // Discord 알림 발송
            if (discordAlertService != null) {
                discordAlertService.sendDeadLetterAlert(retriedMessage, errorMessage);
            }

            log.warn("Message moved to DLQ after {} retries: {}",
                    retriedMessage.retryCount(), message.messageId());
        } else {
            // 재시도를 위해 다시 큐에 발행
            messageQueueService.publish(retriedMessage);
            log.info("Message re-queued for retry (attempt {}): {}",
                    retriedMessage.retryCount(), message.messageId());
        }

        // 원본 메시지 ACK
        acknowledgeMessage(recordId);
    }

    /**
     * 메시지 ACK 처리
     */
    private void acknowledgeMessage(RecordId recordId) {
        try {
            redisTemplate.opsForStream().acknowledge(
                    queueProperties.notificationStream(),
                    queueProperties.consumerGroup(),
                    recordId
            );
        } catch (Exception e) {
            log.error("Failed to acknowledge message: {}", e.getMessage());
        }
    }

    /**
     * FCM 메시지 전송
     */
    private void sendToDevice(String token, String title, String body, Map<String, String> data)
            throws FirebaseMessagingException {

        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        Message.Builder messageBuilder = Message.builder()
                .setToken(token)
                .setNotification(notification);

        if (data != null && !data.isEmpty()) {
            messageBuilder.putAllData(data);
        }

        // Android 설정
        messageBuilder.setAndroidConfig(AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .setNotification(AndroidNotification.builder()
                        .setClickAction("OPEN_APP")
                        .build())
                .build());

        // iOS(APNs) 설정
        messageBuilder.setApnsConfig(ApnsConfig.builder()
                .setAps(Aps.builder()
                        .setSound("default")
                        .setBadge(1)
                        .build())
                .build());

        FirebaseMessaging.getInstance().send(messageBuilder.build());
    }

    /**
     * Firebase 오류 처리
     */
    private void handleFirebaseError(FcmDeviceToken deviceToken, FirebaseMessagingException e) {
        MessagingErrorCode errorCode = e.getMessagingErrorCode();

        if (errorCode == MessagingErrorCode.UNREGISTERED ||
                errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
            deviceToken.deactivate();
            fcmDeviceTokenRepository.save(deviceToken);
            log.warn("Deactivated invalid FCM token: {}", deviceToken.getToken());
        } else {
            log.error("Failed to send FCM message: {}", e.getMessage());
        }
    }

    /**
     * 푸시 알림 활성화 여부 확인
     */
    private boolean isPushEnabled(Long memberId) {
        return notificationSettingsRepository.findByMember_MemberId(memberId)
                .map(NotificationSettings::isPushEnabled)
                .orElse(true);
    }

    /**
     * 조용한 시간 여부 확인
     */
    private boolean isQuietHours(Long memberId) {
        return notificationSettingsRepository.findByMember_MemberId(memberId)
                .filter(NotificationSettings::isQuietHoursEnabled)
                .map(settings -> {
                    LocalTime now = LocalTime.now();
                    LocalTime start = settings.getQuietHoursStart();
                    LocalTime end = settings.getQuietHoursEnd();

                    if (start == null || end == null) {
                        return false;
                    }

                    if (start.isAfter(end)) {
                        return now.isAfter(start) || now.isBefore(end);
                    }
                    return now.isAfter(start) && now.isBefore(end);
                })
                .orElse(false);
    }
}
