# 📬 Hamalog 메시지 큐 패턴

> 이 문서는 Hamalog 프로젝트의 Redis Stream 기반 메시지 큐 패턴을 설명합니다.
> 비동기 알림 처리, 재시도 로직, Dead Letter Queue 구현 방법을 안내합니다.

---

## 📋 목차

1. [아키텍처 개요](#1-아키텍처-개요)
2. [Producer 패턴](#2-producer-패턴)
3. [Consumer 패턴](#3-consumer-패턴)
4. [재시도 및 DLQ](#4-재시도-및-dlq)
5. [Discord 알림](#5-discord-알림)
6. [설정 옵션](#6-설정-옵션)
7. [모니터링](#7-모니터링)

---

## 1. 아키텍처 개요

### 1.1 전체 흐름

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│    Service      │────▶│  Redis Stream   │────▶│   Consumer      │
│   (Producer)    │     │  (Message Queue)│     │   (FCM 발송)    │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                               │
                               ▼ (실패 시)
                        ┌─────────────────┐     ┌─────────────────┐
                        │  Dead Letter    │────▶│  Discord Alert  │
                        │  Queue (DLQ)    │     │  (Webhook)      │
                        └─────────────────┘     └─────────────────┘
```

### 1.2 컴포넌트 역할

| 컴포넌트 | 클래스 | 역할 |
|----------|--------|------|
| **Producer** | `MessageQueueService` | 메시지를 Redis Stream에 발행 |
| **Consumer** | `NotificationConsumerService` | Stream에서 메시지 소비, FCM 발송 |
| **Facade** | `QueuedNotificationService` | 큐 활성화 여부에 따른 분기 |
| **Alert** | `DiscordWebhookService` | DLQ 적재 시 Discord 알림 |

### 1.3 왜 Redis Stream인가?

| 기준 | Redis Stream | Kafka |
|------|--------------|-------|
| **기존 인프라** | ✅ 이미 사용 중 | ❌ 별도 클러스터 필요 |
| **처리량** | 헬스케어 앱 충분 | 대규모 서비스용 |
| **운영 복잡도** | 낮음 | 높음 |
| **비용** | 추가 비용 없음 | 높음 |

> 📄 상세 비교: [ADR-0007](../adr/0007-message-queue-redis-stream.md)

---

## 2. Producer 패턴

### 2.1 메시지 발행

```java
@Service
@RequiredArgsConstructor
public class SomeBusinessService {

    private final QueuedNotificationService notificationService;

    @Transactional
    public void processBusinessLogic() {
        // 비즈니스 로직 처리...

        // 비동기 알림 발송 (큐 활성화 시 즉시 반환)
        notificationService.sendSevereSideEffectAlert(
            memberId, 
            sideEffectName, 
            degree
        );
    }
}
```

### 2.2 QueuedNotificationService 사용

```java
// 큐가 활성화되면 → Redis Stream으로 발행 (비동기)
// 큐가 비활성화되면 → FcmPushService 직접 호출 (동기)

notificationService.sendPushNotification(
    memberId,           // 대상 회원 ID
    "알림 제목",        // 제목
    "알림 내용",        // 본문
    Map.of("key", "value"),  // 추가 데이터
    NotificationType.GENERAL // 알림 유형
);
```

### 2.3 실제 사용 예시: 부작용 리마인더

복약 후 1시간 뒤에 부작용 기록을 권유하는 알림 발송:

```java
// MedicationReminderService.java
@Async("eventExecutor")
public void scheduleSideEffectRecordReminder(Long memberId, Long scheduleId, LocalDateTime takeTime) {
    try {
        LocalDateTime reminderTime = takeTime.plusHours(1);

        if (reminderTime.isAfter(LocalDateTime.now())) {
            log.info("Scheduling side effect reminder for memberId: {} at {}", memberId, reminderTime);

            // 메시지 큐를 통해 부작용 기록 권유 알림 발송
            queuedNotificationService.sendSideEffectRecordReminder(
                    memberId,
                    "약 복용 1시간이 지났습니다. 혹시 부작용이 있다면 기록해주세요."
            );
        }
    } catch (Exception e) {
        log.error("Failed to schedule side effect reminder for memberId: {}", memberId, e);
    }
}
```

### 2.4 직접 MessageQueueService 사용

저수준 제어가 필요한 경우:

```java
@Autowired(required = false)
private MessageQueueService messageQueueService;

public void publishCustomMessage() {
    if (messageQueueService == null) {
        log.warn("Message queue is disabled");
        return;
    }

    NotificationMessage message = NotificationMessage.of(
        memberId,
        "제목",
        "내용",
        Map.of("customKey", "customValue"),
        NotificationType.GENERAL
    );

    RecordId recordId = messageQueueService.publish(message);
    log.info("Message published with recordId: {}", recordId);
}
```

---

## 3. Consumer 패턴

### 3.1 Consumer 동작 방식

`NotificationConsumerService`는 `@Scheduled`로 주기적으로 메시지를 폴링합니다:

```java
@Scheduled(fixedDelayString = "${hamalog.queue.poll-interval-ms:5000}")
public void consumeMessages() {
    // 1. Consumer Group에서 배치로 메시지 읽기
    // 2. 각 메시지 처리 (FCM 발송)
    // 3. 성공 시 ACK
    // 4. 실패 시 재시도 또는 DLQ
}
```

### 3.2 Consumer Group 확장

부하가 증가하면 Consumer를 여러 인스턴스로 확장 가능:

```properties
# 인스턴스별로 다른 consumer-name 지정
hamalog.queue.consumer-name=${HOSTNAME:consumer-1}
```

---

## 4. 재시도 및 DLQ

### 4.1 재시도 전략

```
메시지 발행 → Consumer 처리 시도
                  ↓ 실패
            재시도 1회차 (retryCount=1)
                  ↓ 실패
            재시도 2회차 (retryCount=2)
                  ↓ 실패
            재시도 3회차 (retryCount=3)
                  ↓ 실패
            DLQ 이동 + Discord 알림
```

### 4.2 재시도 로직

```java
private void handleFailure(NotificationMessage message, String errorMessage) {
    NotificationMessage retriedMessage = message.withIncrementedRetry();

    if (retriedMessage.hasExceededMaxRetries(queueProperties.maxRetries())) {
        // 최대 재시도 초과 → DLQ로 이동
        messageQueueService.publishToDeadLetterQueue(retriedMessage, errorMessage);
        discordWebhookService.sendDeadLetterAlert(retriedMessage, errorMessage);
    } else {
        // 재시도를 위해 다시 큐에 발행
        messageQueueService.publish(retriedMessage);
    }
}
```

### 4.3 DLQ 조회

Redis CLI로 DLQ 확인:

```bash
redis-cli XRANGE hamalog:notifications:dlq - + COUNT 10
```

---

## 5. Discord 알림

### 5.1 설정

```properties
hamalog.queue.discord.enabled=true
hamalog.queue.discord.webhook-url=https://discord.com/api/webhooks/YOUR_WEBHOOK_ID/YOUR_TOKEN
```

### 5.2 알림 형식

DLQ 적재 시 Discord로 다음 정보가 발송됩니다:

| 필드 | 설명 |
|------|------|
| 메시지 ID | 고유 식별자 |
| 회원 ID | 알림 대상 |
| 알림 유형 | SEVERE_SIDE_EFFECT, DIARY_REMINDER 등 |
| 재시도 횟수 | 실패 전 재시도 횟수 |
| 에러 메시지 | 마지막 실패 원인 |
| DLQ 적재 시간 | 타임스탬프 |

---

## 6. 설정 옵션

### 6.1 application.properties

```properties
# 메시지 큐 활성화/비활성화
hamalog.queue.enabled=true

# Redis Stream 키
hamalog.queue.notification-stream=hamalog:notifications
hamalog.queue.dead-letter-stream=hamalog:notifications:dlq

# Consumer Group 설정
hamalog.queue.consumer-group=notification-consumers
hamalog.queue.consumer-name=${HOSTNAME:consumer-1}

# 재시도 및 배치 설정
hamalog.queue.max-retries=3
hamalog.queue.poll-timeout-seconds=5
hamalog.queue.batch-size=10
hamalog.queue.poll-interval-ms=5000

# Discord Webhook 알림
hamalog.queue.discord.enabled=true
hamalog.queue.discord.webhook-url=${DISCORD_WEBHOOK_URL:}
```

### 6.2 환경변수

```bash
# 프로덕션 배포 시
export MESSAGE_QUEUE_ENABLED=true
export DISCORD_WEBHOOK_ENABLED=true
export DISCORD_WEBHOOK_URL=https://discord.com/api/webhooks/...
```

---

## 7. 모니터링

### 7.1 Prometheus 메트릭

| 메트릭 | 설명 |
|--------|------|
| `hamalog.queue.messages.published` | 발행된 메시지 수 |
| `hamalog.queue.messages.processed` | 처리 성공 메시지 수 |
| `hamalog.queue.messages.failed` | 처리 실패 메시지 수 |
| `hamalog.queue.messages.dlq` | DLQ로 이동된 메시지 수 |
| `hamalog.queue.processing.time` | 메시지 처리 시간 |

### 7.2 Grafana 쿼리 예시

```promql
# 초당 발행량
rate(hamalog_queue_messages_published_total[5m])

# DLQ 적재율
rate(hamalog_queue_messages_dlq_total[5m]) / rate(hamalog_queue_messages_published_total[5m])

# 평균 처리 시간
rate(hamalog_queue_processing_time_sum[5m]) / rate(hamalog_queue_processing_time_count[5m])
```

### 7.3 큐 상태 확인 API

```java
// MessageQueueService 메서드
long queueLength = messageQueueService.getQueueLength();
long dlqLength = messageQueueService.getDeadLetterQueueLength();
```

---

## 관련 문서

- [ADR-0007: 메시지 큐 도입 - Redis Stream 선택](../adr/0007-message-queue-redis-stream.md)
- [캐싱 패턴](./CACHING-PATTERNS.md) - Redis 사용 패턴
- [에러 처리 패턴](./ERROR-HANDLING.md) - 예외 처리

---

> 📝 최종 업데이트: 2026년 1월 13일

