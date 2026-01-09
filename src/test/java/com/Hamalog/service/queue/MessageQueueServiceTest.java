package com.Hamalog.service.queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.Hamalog.config.MessageQueueProperties;
import com.Hamalog.service.queue.message.NotificationMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageQueueService 테스트")
class MessageQueueServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private StreamOperations<String, Object, Object> streamOperations;

    @Mock
    private MessageQueueProperties queueProperties;

    private ObjectMapper objectMapper;
    private MeterRegistry meterRegistry;
    private MessageQueueService messageQueueService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // JavaTimeModule 등록
        meterRegistry = new SimpleMeterRegistry();

        lenient().when(redisTemplate.opsForStream()).thenReturn(streamOperations);
        lenient().when(queueProperties.notificationStream()).thenReturn("test-notification-stream");
        lenient().when(queueProperties.deadLetterStream()).thenReturn("test-dlq-stream");

        messageQueueService = new MessageQueueService(
                redisTemplate, queueProperties, objectMapper, meterRegistry
        );
        // 수동으로 metrics 초기화
        messageQueueService.initMetrics();
    }

    @Nested
    @DisplayName("메시지 발행")
    class Publish {

        @Test
        @DisplayName("성공: 메시지 발행")
        void success() {
            // given
            NotificationMessage message = NotificationMessage.of(
                    1L,
                    "복약 알림",
                    "약 복용 시간입니다.",
                    Map.of("scheduleId", "1"),
                    "MEDICATION_REMINDER"
            );

            RecordId mockRecordId = RecordId.of("1-0");
            when(streamOperations.add(any())).thenReturn(mockRecordId);

            // when
            RecordId result = messageQueueService.publish(message);

            // then
            assertThat(result).isEqualTo(mockRecordId);
            verify(streamOperations).add(any());
        }

        @Test
        @DisplayName("실패: Redis 오류 시 null 반환")
        void fail_redisError() {
            // given
            NotificationMessage message = NotificationMessage.of(
                    1L,
                    "복약 알림",
                    "약 복용 시간입니다.",
                    Map.of(),
                    "MEDICATION_REMINDER"
            );

            when(streamOperations.add(any())).thenThrow(new RuntimeException("Redis connection error"));

            // when
            RecordId result = messageQueueService.publish(message);

            // then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("Dead Letter Queue 발행")
    class PublishToDeadLetterQueue {

        @Test
        @DisplayName("성공: DLQ로 메시지 이동")
        void success() {
            // given
            NotificationMessage message = NotificationMessage.of(
                    1L,
                    "일기 알림",
                    "일기를 작성해주세요.",
                    Map.of(),
                    "DIARY_REMINDER"
            );

            RecordId mockRecordId = RecordId.of("2-0");
            when(streamOperations.add(any())).thenReturn(mockRecordId);

            // when
            messageQueueService.publishToDeadLetterQueue(message, "처리 실패");

            // then
            verify(streamOperations).add(any());
        }
    }
}
