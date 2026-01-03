package com.Hamalog.service.queue;

import com.Hamalog.config.MessageQueueProperties;
import com.Hamalog.service.queue.message.NotificationMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 메시지 큐 서비스 (Producer)
 * Redis Stream을 사용하여 비동기 메시지를 발행합니다.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "hamalog.queue.enabled", havingValue = "true")
public class MessageQueueService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final MessageQueueProperties queueProperties;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    private Counter publishedCounter;
    private Counter failedCounter;

    public MessageQueueService(
            RedisTemplate<String, Object> redisTemplate,
            MessageQueueProperties queueProperties,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.queueProperties = queueProperties;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    void initMetrics() {
        publishedCounter =
                Counter.builder("hamalog.queue.messages.published")
                        .description("Total messages published to queue")
                        .register(meterRegistry);
        failedCounter =
                Counter.builder("hamalog.queue.messages.publish_failed")
                        .description("Total messages failed to publish")
                        .register(meterRegistry);
    }

    /**
     * 알림 메시지를 큐에 발행
     *
     * @param message 알림 메시지
     * @return 발행된 메시지의 Record ID (실패 시 null)
     */
    public RecordId publish(NotificationMessage message) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(message);

            MapRecord<String, String, String> record =
                    StreamRecords.string(
                                    Map.of("messageId", message.messageId(), "payload", jsonPayload))
                            .withStreamKey(queueProperties.notificationStream());

            RecordId recordId = redisTemplate.opsForStream().add(record);
            publishedCounter.increment();

            log.debug(
                    "Message published to stream: {} with recordId: {}",
                    queueProperties.notificationStream(),
                    recordId);

            return recordId;

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize message: {}", e.getMessage());
            failedCounter.increment();
            return null;
        } catch (Exception e) {
            log.error("Failed to publish message to Redis Stream: {}", e.getMessage());
            failedCounter.increment();
            return null;
        }
    }

    /**
     * 메시지를 Dead Letter Queue로 이동
     *
     * @param message 실패한 메시지
     * @param errorMessage 에러 내용
     */
    public void publishToDeadLetterQueue(NotificationMessage message, String errorMessage) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(message);

            MapRecord<String, String, String> record =
                    StreamRecords.string(
                                    Map.of(
                                            "messageId",
                                            message.messageId(),
                                            "payload",
                                            jsonPayload,
                                            "error",
                                            errorMessage != null ? errorMessage : "Unknown error"))
                            .withStreamKey(queueProperties.deadLetterStream());

            redisTemplate.opsForStream().add(record);

            log.warn("Message moved to DLQ: messageId={}, error={}", message.messageId(), errorMessage);

        } catch (Exception e) {
            log.error("Failed to publish to DLQ: {}", e.getMessage());
        }
    }

    /**
     * Consumer Group 생성 (이미 존재하면 무시)
     */
    public void createConsumerGroupIfNotExists() {
        try {
            redisTemplate
                    .opsForStream()
                    .createGroup(queueProperties.notificationStream(), queueProperties.consumerGroup());
            log.info(
                    "Consumer group created: {} for stream: {}",
                    queueProperties.consumerGroup(),
                    queueProperties.notificationStream());
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("BUSYGROUP")) {
                log.debug("Consumer group already exists: {}", queueProperties.consumerGroup());
            } else {
                log.warn("Failed to create consumer group: {}", e.getMessage());
            }
        }
    }

    /**
     * 큐 길이 조회
     */
    public long getQueueLength() {
        try {
            Long size = redisTemplate.opsForStream().size(queueProperties.notificationStream());
            return size != null ? size : 0;
        } catch (Exception e) {
            log.error("Failed to get queue length: {}", e.getMessage());
            return -1;
        }
    }

    /**
     * DLQ 길이 조회
     */
    public long getDeadLetterQueueLength() {
        try {
            Long size = redisTemplate.opsForStream().size(queueProperties.deadLetterStream());
            return size != null ? size : 0;
        } catch (Exception e) {
            log.error("Failed to get DLQ length: {}", e.getMessage());
            return -1;
        }
    }
}
