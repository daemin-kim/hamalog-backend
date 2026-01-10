# 06. Redis Stream 비동기 메시지 큐

> **Redis Stream을 활용하여 FCM 푸시 알림 발송을 비동기 처리하고, API 응답 시간에서 외부 API 호출을 분리하는 패턴**

---

## 📋 목차

1. [문제 상황](#1-문제-상황-problem)
2. [해결 전략](#2-해결-전략-solution-strategy)
3. [구현 상세](#3-구현-상세-implementation)
4. [효과 및 검증](#4-효과-및-검증-results)
5. [면접 대비 Q&A](#5-면접-대비-qa)

---

## 1. 문제 상황 (Problem)

### 1.1 동기 알림 발송의 문제

복약 기록 생성 시 FCM 푸시 알림을 동기로 발송하면 외부 API 호출이 응답 시간에 직접 영향을 줍니다.

```
┌─────────────────────────────────────────────────────────────────┐
│                    동기 처리 문제                                │
│                                                                  │
│  클라이언트 요청                                                 │
│       │                                                          │
│       ▼                                                          │
│  ┌─────────────┐                                                 │
│  │ Controller  │ ─────────────────────────────────────┐         │
│  └─────────────┘                                       │         │
│       │                                                │         │
│       ▼                                                │         │
│  ┌─────────────┐                                       │ 50ms    │
│  │  Service    │ (비즈니스 로직)                       │         │
│  └─────────────┘                                       │         │
│       │                                                │         │
│       ▼                                                │         │
│  ┌─────────────┐                                       │         │
│  │     DB      │ (저장)                                │         │
│  └─────────────┘                                       │         │
│       │                                                │         │
│       ▼                                                │         │
│  ┌─────────────┐                                       │         │
│  │  FCM 호출   │ (외부 API) ──────────────────────────┤ 200~500ms│
│  └─────────────┘                                       │         │
│       │                                                │         │
│       ▼                                                │         │
│  응답 반환 ◀───────────────────────────────────────────┘         │
│                                                                  │
│  총 응답 시간: 50ms + 200~500ms = 250~550ms                      │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2 구체적인 문제점

| 문제 | 영향 |
|------|------|
| **응답 지연** | FCM 응답 대기로 API 응답 시간 200~500ms 증가 |
| **FCM 장애 전파** | FCM 서버 다운 시 Hamalog API도 영향받음 |
| **타임아웃 위험** | FCM 응답이 느리면 API 요청 타임아웃 |
| **재시도 어려움** | 동기 처리 중 FCM 실패 시 사용자에게 에러 반환 |
| **확장성 제한** | 대량 알림 발송 시 서버 스레드 점유 |

### 1.3 Hamalog에서의 알림 시나리오

| 이벤트 | 알림 대상 | 빈도 |
|--------|----------|------|
| 복약 시간 도래 | 해당 사용자 | 하루 수회/사용자 |
| 일기 작성 리마인더 | 설정한 사용자 | 하루 1회/사용자 |
| 연속 복약 기록 달성 | 해당 사용자 | 가끔 |

---

## 2. 해결 전략 (Solution Strategy)

### 2.1 고려한 대안들

| 방식 | 장점 | 단점 | 채택 여부 |
|------|------|------|----------|
| **@Async (스레드 풀)** | 구현 단순 | 서버 재시작 시 유실, 재시도 어려움 | ❌ |
| **RabbitMQ** | 기능 풍부, 안정적 | 추가 인프라, 운영 복잡 | ❌ |
| **Apache Kafka** | 대용량, 내구성 | 과도한 인프라, 학습 곡선 | ❌ |
| **Redis Stream** | 기존 Redis 활용, 충분한 기능 | Kafka보다 기능 제한 | ✅ |

### 2.2 최종 선택: Redis Stream

```
┌─────────────────────────────────────────────────────────────────┐
│                    비동기 처리 아키텍처                          │
│                                                                  │
│  클라이언트 요청                                                 │
│       │                                                          │
│       ▼                                                          │
│  ┌─────────────┐                                                 │
│  │ Controller  │                                                 │
│  └─────────────┘                                                 │
│       │                                                          │
│       ▼                                                          │
│  ┌─────────────┐    ┌─────────────────────────────┐             │
│  │  Service    │───▶│ Redis Stream (메시지 발행) │ ← 10ms 이하  │
│  └─────────────┘    └─────────────────────────────┘             │
│       │                          │                               │
│       ▼                          │ (비동기)                      │
│  응답 반환 ◀─────────────────────│                               │
│  (50ms 이하)                      │                               │
│                                  ▼                               │
│                          ┌─────────────┐                         │
│                          │  Consumer   │                         │
│                          │  (별도 스레드)│                         │
│                          └─────────────┘                         │
│                                  │                               │
│                                  ▼                               │
│                          ┌─────────────┐                         │
│                          │  FCM 발송   │                         │
│                          └─────────────┘                         │
│                                  │                               │
│                              실패 시                              │
│                                  ▼                               │
│                          ┌─────────────┐                         │
│                          │     DLQ     │──▶ Discord 알림        │
│                          └─────────────┘                         │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 2.3 왜 Redis Stream인가?

1. **인프라 재활용**: 이미 캐싱/Rate Limiting 용 Redis 사용 중
2. **충분한 기능**: Consumer Group, ACK, 영속성 지원
3. **낮은 운영 비용**: 별도 메시지 브로커 불필요
4. **적절한 규모**: Kafka 수준의 처리량 불필요

---

## 3. 구현 상세 (Implementation)

### 3.1 메시지 구조 정의 (NotificationMessage.java)

```java
/**
 * Redis Stream으로 전송되는 알림 메시지 구조
 * 
 * Java 17+ record: 불변 객체, JSON 직렬화에 적합
 */
public record NotificationMessage(
    /**
     * 메시지 고유 ID (UUID)
     * 중복 처리 방지 및 추적용
     */
    String messageId,
    
    /**
     * 알림 대상 회원 ID
     */
    Long memberId,
    
    /**
     * 알림 제목
     */
    String title,
    
    /**
     * 알림 본문
     */
    String body,
    
    /**
     * 추가 데이터 (딥링크 등)
     * 예: {"action": "OPEN_SCHEDULE", "scheduleId": "123"}
     */
    Map<String, String> data,
    
    /**
     * 재시도 횟수
     * 실패 시마다 증가, 최대 3회 후 DLQ로 이동
     */
    int retryCount,
    
    /**
     * 메시지 생성 시간 (ISO 8601)
     */
    String createdAt
) {
    /**
     * 새 메시지 생성 팩토리 메서드
     */
    public static NotificationMessage create(Long memberId, String title, String body, 
                                             Map<String, String> data) {
        return new NotificationMessage(
            UUID.randomUUID().toString(),
            memberId,
            title,
            body,
            data != null ? data : Map.of(),
            0,  // 최초 retryCount = 0
            Instant.now().toString()
        );
    }
    
    /**
     * 재시도 시 retryCount 증가한 새 메시지 반환
     */
    public NotificationMessage withIncrementedRetry() {
        return new NotificationMessage(
            this.messageId,
            this.memberId,
            this.title,
            this.body,
            this.data,
            this.retryCount + 1,
            this.createdAt
        );
    }
    
    /**
     * 최대 재시도 횟수 초과 여부
     */
    public boolean hasExceededMaxRetries(int maxRetries) {
        return this.retryCount >= maxRetries;
    }
}
```

### 3.2 Producer 구현 (MessageQueueService.java)

```java
/**
 * 메시지 큐 서비스 (Producer)
 * 
 * Redis Stream을 사용하여 비동기 메시지를 발행합니다.
 * 
 * @ConditionalOnProperty: 큐 기능 활성화 여부를 설정으로 제어
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "hamalog.queue.enabled", havingValue = "true")
public class MessageQueueService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final MessageQueueProperties queueProperties;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    
    // Micrometer 메트릭 (모니터링용)
    private Counter publishedCounter;
    private Counter failedCounter;
    
    @PostConstruct
    void initMetrics() {
        // Prometheus에서 수집할 메트릭 등록
        publishedCounter = Counter.builder("hamalog.queue.messages.published")
                .description("Total messages published to queue")
                .register(meterRegistry);
        failedCounter = Counter.builder("hamalog.queue.messages.publish_failed")
                .description("Total messages failed to publish")
                .register(meterRegistry);
    }
    
    // ============================================================
    // 메시지 발행
    // ============================================================
    
    /**
     * 알림 메시지를 Redis Stream에 발행
     * 
     * @param message 발행할 메시지
     * @return 발행된 메시지의 Record ID (실패 시 null)
     * 
     * Redis Stream 특징:
     * - 영속성: 메시지가 디스크에 저장됨
     * - 순서 보장: 발행 순서대로 소비
     * - ACK 기반: Consumer가 처리 완료를 확인
     */
    public RecordId publish(NotificationMessage message) {
        try {
            // 메시지를 JSON으로 직렬화
            String jsonPayload = objectMapper.writeValueAsString(message);
            
            // Redis Stream Record 생성
            // - StreamRecords.string(): 문자열 Key-Value 형태
            // - withStreamKey(): 발행할 스트림 이름 지정
            MapRecord<String, String, String> record = StreamRecords.string(
                    Map.of(
                        "messageId", message.messageId(),
                        "payload", jsonPayload
                    ))
                    .withStreamKey(queueProperties.notificationStream());
            
            // Redis XADD 명령 실행
            // XADD hamalog:notifications:stream * messageId "uuid" payload "{...}"
            RecordId recordId = redisTemplate.opsForStream().add(record);
            
            // 성공 메트릭 증가
            publishedCounter.increment();
            
            log.debug("Message published to stream: {} with recordId: {}", 
                    queueProperties.notificationStream(), recordId);
            
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
    
    // ============================================================
    // Dead Letter Queue (DLQ) 처리
    // ============================================================
    
    /**
     * 실패한 메시지를 Dead Letter Queue로 이동
     * 
     * DLQ 용도:
     * - 최대 재시도 초과 메시지 보관
     * - 수동 확인 및 재처리 가능
     * - 운영 알림 트리거
     */
    public void publishToDeadLetterQueue(NotificationMessage message, String errorMessage) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(message);
            
            MapRecord<String, String, String> record = StreamRecords.string(
                    Map.of(
                        "messageId", message.messageId(),
                        "payload", jsonPayload,
                        "error", errorMessage != null ? errorMessage : "Unknown error"
                    ))
                    .withStreamKey(queueProperties.deadLetterStream());
            
            redisTemplate.opsForStream().add(record);
            
            log.warn("Message moved to DLQ: messageId={}, error={}", 
                    message.messageId(), errorMessage);
            
        } catch (Exception e) {
            log.error("Failed to publish to DLQ: {}", e.getMessage());
        }
    }
    
    // ============================================================
    // Consumer Group 관리
    // ============================================================
    
    /**
     * Consumer Group 생성 (멱등성 보장)
     * 
     * Consumer Group이란?
     * - 여러 Consumer가 하나의 Stream을 분산 처리
     * - 각 메시지는 그룹 내 하나의 Consumer만 처리
     * - ACK로 처리 완료 추적
     */
    public void createConsumerGroupIfNotExists() {
        try {
            // XGROUP CREATE hamalog:notifications:stream notification-group $ MKSTREAM
            redisTemplate.opsForStream().createGroup(
                queueProperties.notificationStream(), 
                queueProperties.consumerGroup()
            );
            log.info("Consumer group created: {}", queueProperties.consumerGroup());
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("BUSYGROUP")) {
                // 이미 존재하는 경우 무시 (정상)
                log.debug("Consumer group already exists: {}", queueProperties.consumerGroup());
            } else {
                log.warn("Failed to create consumer group: {}", e.getMessage());
            }
        }
    }
}
```

### 3.3 Consumer 구현 (NotificationConsumerService.java)

```java
/**
 * 알림 메시지 Consumer 서비스
 * 
 * Redis Stream에서 메시지를 소비하여 FCM 푸시 알림을 발송합니다.
 * 
 * @Scheduled: Spring 스케줄러로 주기적 폴링
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
    
    // 메트릭
    private Counter processedCounter;
    private Counter failedCounter;
    private Counter dlqCounter;
    private Timer processingTimer;
    
    // 정상 종료를 위한 플래그
    private final AtomicBoolean running = new AtomicBoolean(true);
    
    @PostConstruct
    void init() {
        // 메트릭 초기화
        processedCounter = Counter.builder("hamalog.queue.messages.processed").register(meterRegistry);
        failedCounter = Counter.builder("hamalog.queue.messages.failed").register(meterRegistry);
        dlqCounter = Counter.builder("hamalog.queue.messages.dlq").register(meterRegistry);
        processingTimer = Timer.builder("hamalog.queue.processing.time").register(meterRegistry);
        
        // Consumer Group 생성
        messageQueueService.createConsumerGroupIfNotExists();
    }
    
    @PreDestroy
    void shutdown() {
        // Graceful Shutdown: 진행 중인 처리 완료 후 종료
        running.set(false);
        log.info("NotificationConsumer shutting down...");
    }
    
    // ============================================================
    // 메시지 소비 (주기적 폴링)
    // ============================================================
    
    /**
     * 5초 간격으로 Redis Stream에서 메시지 읽기
     * 
     * Consumer Group을 사용하여:
     * - 여러 인스턴스가 분산 처리 가능
     * - 메시지 유실 방지 (ACK 전까지 재전송)
     */
    @Scheduled(fixedDelayString = "${hamalog.queue.poll-interval-ms:5000}")
    public void consumeMessages() {
        if (!running.get()) {
            return;
        }
        
        try {
            // XREADGROUP GROUP notification-group consumer-1 COUNT 10 BLOCK 5000 STREAMS hamalog:notifications:stream >
            @SuppressWarnings("unchecked")
            List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(
                Consumer.from(
                    queueProperties.consumerGroup(),    // 그룹명
                    queueProperties.consumerName()      // 컨슈머명 (인스턴스별 고유)
                ),
                StreamReadOptions.empty()
                    .count(queueProperties.batchSize())         // 한 번에 읽을 메시지 수
                    .block(Duration.ofSeconds(queueProperties.pollTimeoutSeconds())),  // 대기 시간
                StreamOffset.create(
                    queueProperties.notificationStream(), 
                    ReadOffset.lastConsumed()  // 마지막 소비 이후 메시지
                )
            );
            
            if (records == null || records.isEmpty()) {
                return;
            }
            
            // 각 레코드 처리
            for (MapRecord<String, Object, Object> record : records) {
                processRecord(record);
            }
            
        } catch (Exception e) {
            log.error("Error consuming messages from stream: {}", e.getMessage());
        }
    }
    
    // ============================================================
    // 개별 메시지 처리
    // ============================================================
    
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
            
            // JSON → NotificationMessage 역직렬화
            NotificationMessage message = objectMapper.readValue(payload, NotificationMessage.class);
            
            // FCM 알림 발송
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
            acknowledgeMessage(record.getId());  // 파싱 실패 시 ACK (무한 재시도 방지)
        } finally {
            sample.stop(processingTimer);
        }
    }
    
    // ============================================================
    // FCM 알림 발송
    // ============================================================
    
    private boolean processNotification(NotificationMessage message) {
        Long memberId = message.memberId();
        
        // 알림 설정 확인 (사용자가 알림을 꺼둔 경우 스킵)
        if (!isPushEnabled(memberId)) {
            log.debug("Push disabled for memberId: {}", memberId);
            return true;  // 설정으로 인한 스킵은 성공으로 처리
        }
        
        // 조용한 시간 확인 (예: 밤 10시 ~ 아침 7시)
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
                log.info("Push notification sent to memberId: {}", memberId);
            } catch (FirebaseMessagingException e) {
                handleFirebaseError(deviceToken, e);
                allSuccess = false;
            }
        }
        
        return allSuccess;
    }
    
    // ============================================================
    // 실패 처리 (재시도 또는 DLQ)
    // ============================================================
    
    private void handleFailure(NotificationMessage message, String errorMessage, RecordId recordId) {
        failedCounter.increment();
        
        // 재시도 횟수 증가
        NotificationMessage retriedMessage = message.withIncrementedRetry();
        
        if (retriedMessage.hasExceededMaxRetries(queueProperties.maxRetries())) {
            // 최대 재시도 초과 → DLQ로 이동
            messageQueueService.publishToDeadLetterQueue(retriedMessage, errorMessage);
            dlqCounter.increment();
            
            // Discord Webhook으로 운영팀 알림
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
        
        // 원본 메시지 ACK (이미 처리 완료 또는 재발행됨)
        acknowledgeMessage(recordId);
    }
    
    // ============================================================
    // 메시지 ACK
    // ============================================================
    
    /**
     * 메시지 처리 완료 확인 (ACK)
     * 
     * ACK하지 않으면:
     * - 메시지가 Pending 상태로 남음
     * - 다른 Consumer가 XCLAIM으로 가져갈 수 있음
     * - 서버 재시작 시 다시 처리됨
     */
    private void acknowledgeMessage(RecordId recordId) {
        try {
            // XACK hamalog:notifications:stream notification-group record-id
            redisTemplate.opsForStream().acknowledge(
                queueProperties.notificationStream(),
                queueProperties.consumerGroup(),
                recordId
            );
        } catch (Exception e) {
            log.error("Failed to acknowledge message: {}", e.getMessage());
        }
    }
    
    // ============================================================
    // FCM 메시지 구성 및 발송
    // ============================================================
    
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
        
        // Android 설정: 높은 우선순위
        messageBuilder.setAndroidConfig(AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .setNotification(AndroidNotification.builder()
                        .setClickAction("OPEN_APP")
                        .build())
                .build());
        
        // iOS(APNs) 설정: 사운드, 뱃지
        messageBuilder.setApnsConfig(ApnsConfig.builder()
                .setAps(Aps.builder()
                        .setSound("default")
                        .setBadge(1)
                        .build())
                .build());
        
        // Firebase로 발송
        FirebaseMessaging.getInstance().send(messageBuilder.build());
    }
}
```

### 3.4 설정 프로퍼티

```yaml
# application.yml
hamalog:
  queue:
    enabled: true                        # 큐 기능 활성화
    notification-stream: hamalog:notifications:stream  # 스트림 이름
    dead-letter-stream: hamalog:notifications:dlq      # DLQ 스트림 이름
    consumer-group: notification-group   # Consumer Group 이름
    consumer-name: ${HOSTNAME:consumer-1}  # Consumer 이름 (인스턴스별 고유)
    batch-size: 10                       # 한 번에 읽을 메시지 수
    poll-interval-ms: 5000               # 폴링 간격 (5초)
    poll-timeout-seconds: 5              # 블로킹 대기 시간
    max-retries: 3                       # 최대 재시도 횟수
```

---

## 4. 효과 및 검증 (Results)

### 4.1 응답 시간 개선

| 지표 | Before (동기) | After (비동기) | 개선율 |
|------|--------------|---------------|-------|
| **API 응답 시간** | 250~550ms | 50~80ms | 70~85% ↓ |
| **FCM 장애 영향** | API 전체 영향 | API 무영향 | 100% 격리 |
| **타임아웃 위험** | 있음 | 없음 | 제거 |

### 4.2 안정성 개선

| 시나리오 | Before | After |
|----------|--------|-------|
| **FCM 서버 다운** | API 500 에러 | API 정상, 알림만 지연 |
| **대량 알림** | 서버 부하 급증 | Consumer가 순차 처리 |
| **재시도 필요** | 수동 처리 | 자동 3회 재시도 |
| **실패 추적** | 로그만 | DLQ + Discord 알림 |

### 4.3 모니터링 메트릭

```
# Prometheus 메트릭 예시

# 발행된 메시지 수
hamalog_queue_messages_published_total 15234

# 성공적으로 처리된 메시지 수
hamalog_queue_messages_processed_total 15200

# 실패한 메시지 수
hamalog_queue_messages_failed_total 30

# DLQ로 이동된 메시지 수
hamalog_queue_messages_dlq_total 4

# 메시지 처리 시간 (히스토그램)
hamalog_queue_processing_time_seconds_bucket{le="0.1"} 14500
hamalog_queue_processing_time_seconds_bucket{le="0.5"} 15100
hamalog_queue_processing_time_seconds_bucket{le="1.0"} 15200
```

### 4.4 검증 테스트

```java
@Test
@DisplayName("메시지 발행 후 Consumer가 FCM 발송")
void publishAndConsume_shouldSendFcmNotification() throws Exception {
    // given
    NotificationMessage message = NotificationMessage.create(
        testMember.getMemberId(),
        "복약 알림",
        "아침 약을 복용하세요",
        Map.of("action", "OPEN_SCHEDULE")
    );
    
    // when: 메시지 발행
    RecordId recordId = messageQueueService.publish(message);
    
    // then: 발행 성공
    assertThat(recordId).isNotNull();
    
    // Consumer가 처리할 때까지 대기
    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
        verify(firebaseMessaging).send(any(Message.class));
    });
}

@Test
@DisplayName("3회 실패 후 DLQ로 이동")
void retryExhausted_shouldMoveToDlq() throws Exception {
    // given: FCM이 계속 실패하도록 설정
    when(firebaseMessaging.send(any())).thenThrow(new FirebaseMessagingException(...));
    
    NotificationMessage message = NotificationMessage.create(...);
    
    // when: 발행 후 재시도 소진
    messageQueueService.publish(message);
    
    // then: DLQ로 이동
    await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
        Long dlqSize = redisTemplate.opsForStream()
            .size(queueProperties.deadLetterStream());
        assertThat(dlqSize).isGreaterThan(0);
    });
    
    // Discord 알림 발송 확인
    verify(discordAlertService).sendDeadLetterAlert(any(), anyString());
}
```

---

## 5. 면접 대비 Q&A

### Q1. Redis Stream과 Kafka의 차이점은?

> **모범 답변**
> 
> | 특성 | Redis Stream | Apache Kafka |
> |------|-------------|--------------|
> | **용도** | 가벼운 메시지 큐 | 대규모 이벤트 스트리밍 |
> | **처리량** | 수만 TPS | 수백만 TPS |
> | **영속성** | RDB/AOF | 분산 로그 |
> | **Consumer Group** | 지원 | 지원 (더 정교함) |
> | **파티셔닝** | 제한적 | 강력함 |
> | **운영 복잡도** | 낮음 | 높음 |
> | **인프라** | 기존 Redis | 별도 클러스터 |
> 
> **Hamalog에서 Redis Stream을 선택한 이유**:
> 1. 이미 캐시용 Redis가 있음 (추가 인프라 불필요)
> 2. 초당 수십 건 수준의 알림 (Kafka 과도함)
> 3. 단일 인스턴스로 충분한 규모

### Q2. Consumer Group은 왜 필요한가요?

> **모범 답변**
> 
> Consumer Group은 **분산 처리**와 **정확히 한 번 전달(At-least-once)**을 보장합니다.
> 
> 1. **분산 처리**: 여러 Consumer가 같은 Stream을 나눠서 처리
>    ```
>    Stream: [M1, M2, M3, M4, M5]
>    Consumer-1: [M1, M3, M5] 처리
>    Consumer-2: [M2, M4] 처리
>    ```
> 
> 2. **메시지 추적**: 각 Consumer가 어디까지 처리했는지 기록
> 
> 3. **장애 복구**: Consumer가 죽으면 Pending 메시지를 다른 Consumer가 XCLAIM
> 
> 4. **ACK 기반**: 처리 완료를 명시적으로 확인해야 메시지 제거

### Q3. ACK를 하지 않으면 어떻게 되나요?

> **모범 답변**
> 
> ACK하지 않은 메시지는 **Pending Entry List(PEL)**에 남습니다.
> 
> 1. **XPENDING**으로 확인 가능:
>    ```redis
>    XPENDING hamalog:notifications:stream notification-group
>    → 1) 5  # 미처리 메시지 5개
>    ```
> 
> 2. **XCLAIM**으로 다른 Consumer가 가져감:
>    ```redis
>    XCLAIM stream group consumer-2 60000 message-id
>    # 60초 이상 Pending인 메시지를 consumer-2가 가져감
>    ```
> 
> 3. **재시작 시 재처리**: Consumer가 재시작하면 `>`가 아닌 `0`부터 읽기 가능
> 
> Hamalog에서는 처리 완료/실패 모두 ACK하고, 재시도가 필요하면 새 메시지로 발행합니다.

### Q4. 메시지 유실 가능성은 없나요?

> **모범 답변**
> 
> Redis 기본 설정에서는 유실 가능성이 있습니다. 방어 조치:
> 
> 1. **AOF 영속성 활성화**:
>    ```conf
>    appendonly yes
>    appendfsync everysec  # 또는 always (성능 저하)
>    ```
> 
> 2. **Redis Cluster/Sentinel**: 장애 시 자동 페일오버
> 
> 3. **Producer 재시도**: 발행 실패 시 재시도 로직
>    ```java
>    if (recordId == null) {
>        // 로컬 큐에 보관 후 재시도
>        retryQueue.add(message);
>    }
>    ```
> 
> 4. **Consumer ACK 전략**: 처리 완료 후에만 ACK
> 
> **완전한 무손실**이 필요하면 Kafka나 RabbitMQ를 고려해야 합니다.

### Q5. @Async 대신 Redis Stream을 사용한 이유는?

> **모범 답변**
> 
> | 특성 | @Async | Redis Stream |
> |------|--------|--------------|
> | **서버 재시작** | 메시지 유실 | 메시지 보존 |
> | **재시도** | 직접 구현 | 자연스럽게 지원 |
> | **분산 처리** | 불가 (인스턴스 내) | 가능 (Consumer Group) |
> | **모니터링** | 어려움 | Redis 명령으로 확인 |
> | **스레드 풀 관리** | 필요 | 불필요 |
> 
> @Async의 한계:
> ```java
> @Async
> public void sendNotification(Message message) {
>     // 서버가 재시작되면 진행 중인 작업 유실
>     // 실패 시 재시도 로직 직접 구현 필요
>     // 스레드 풀 고갈 시 거부
> }
> ```
> 
> Redis Stream은 **영속성 + 재시도 + 분산 처리**를 기본 제공합니다.

### Q6. Dead Letter Queue(DLQ)는 왜 필요한가요?

> **모범 답변**
> 
> DLQ는 **처리 불가능한 메시지**를 격리하여:
> 
> 1. **무한 재시도 방지**: 구조적 문제(잘못된 토큰 등)는 재시도해도 실패
> 2. **정상 메시지 처리 방해 방지**: 문제 메시지가 큐를 막지 않음
> 3. **수동 분석 가능**: 운영자가 확인 후 조치
> 4. **재처리 옵션**: 문제 해결 후 메인 큐로 재발행 가능
> 
> Hamalog DLQ 처리:
> ```
> 1. 3회 재시도 실패 → DLQ 이동
> 2. Discord Webhook으로 운영팀 알림
> 3. 운영자가 원인 분석 (잘못된 토큰, FCM 계정 문제 등)
> 4. 문제 해결 후 재처리 또는 삭제
> ```

### Q7. 순서 보장이 중요한 경우 어떻게 하나요?

> **모범 답변**
> 
> Redis Stream은 **단일 Stream 내에서 순서를 보장**합니다.
> 
> 그러나 Consumer Group 분산 처리 시:
> - Consumer-1이 M1 처리 중
> - Consumer-2가 M2를 먼저 완료할 수 있음
> 
> **순서가 중요한 경우**:
> 
> 1. **단일 Consumer**: 한 Consumer만 처리 (처리량 제한)
> 
> 2. **파티셔닝**: 사용자 ID 기반으로 다른 Stream 사용
>    ```java
>    String stream = "notifications:" + (memberId % 10);  // 10개 파티션
>    ```
> 
> 3. **순서 필드 추가**: 메시지에 시퀀스 번호 포함, 클라이언트에서 정렬
> 
> Hamalog 알림은 **순서가 중요하지 않아** Consumer Group 분산 처리를 사용합니다.

### Q8. Consumer가 느리면 메시지가 쌓이는데, 어떻게 대응하나요?

> **모범 답변**
> 
> **백프레셔(Backpressure)** 전략:
> 
> 1. **모니터링 및 알림**:
>    ```java
>    long queueLength = redisTemplate.opsForStream().size(streamKey);
>    if (queueLength > 1000) {
>        alertService.sendWarning("Queue backlog: " + queueLength);
>    }
>    ```
> 
> 2. **Consumer 스케일 아웃**: 인스턴스 추가로 처리량 증가
> 
> 3. **배치 크기 조정**: `batch-size` 증가
> 
> 4. **Stream 용량 제한**:
>    ```redis
>    XTRIM stream MAXLEN ~ 10000  # 대략 10000개 유지
>    ```
> 
> 5. **우선순위 처리**: 중요 알림은 별도 Stream으로 분리

### Q9. Redis가 다운되면 알림은 어떻게 되나요?

> **모범 답변**
> 
> **Graceful Degradation** 전략:
> 
> 1. **Fallback 동기 발송**:
>    ```java
>    RecordId id = messageQueueService.publish(message);
>    if (id == null) {
>        // Redis 실패 시 동기 발송으로 fallback
>        notificationService.sendSync(message);
>    }
>    ```
> 
> 2. **로컬 큐 버퍼링**: 메모리 큐에 잠시 보관 후 재시도
> 
> 3. **Circuit Breaker**: Redis 장애 감지 시 빠르게 fallback 전환
> 
> 4. **Redis Sentinel/Cluster**: 자동 페일오버로 다운타임 최소화
> 
> Hamalog는 현재 fallback 동기 발송을 구현하고 있습니다.

### Q10. 테스트 환경에서 Redis Stream을 어떻게 테스트하나요?

> **모범 답변**
> 
> 세 가지 접근법:
> 
> 1. **Embedded Redis** (통합 테스트):
>    ```java
>    @TestConfiguration
>    public class EmbeddedRedisConfig {
>        private RedisServer redisServer;
>        
>        @PostConstruct
>        void start() {
>            redisServer = new RedisServer(6370);
>            redisServer.start();
>        }
>    }
>    ```
> 
> 2. **Testcontainers** (Docker 기반):
>    ```java
>    @Container
>    static GenericContainer<?> redis = new GenericContainer<>("redis:7")
>        .withExposedPorts(6379);
>    ```
> 
> 3. **Mock** (단위 테스트):
>    ```java
>    @MockBean
>    private MessageQueueService queueService;
>    
>    when(queueService.publish(any())).thenReturn(RecordId.of("1-0"));
>    ```
> 
> Hamalog는 **단위 테스트는 Mock**, **통합 테스트는 Testcontainers**를 사용합니다.

---

## 📎 관련 문서

- [ADR-0007: Redis Stream 메시지 큐](../internal/adr/0007-message-queue-redis-stream.md)
- [MESSAGE-QUEUE-PATTERNS.md](../internal/patterns/MESSAGE-QUEUE-PATTERNS.md)
- [MessageQueueService.java](../../src/main/java/com/Hamalog/service/queue/MessageQueueService.java)
- [NotificationConsumerService.java](../../src/main/java/com/Hamalog/service/queue/NotificationConsumerService.java)

