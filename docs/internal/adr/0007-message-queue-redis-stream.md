# ADR-0007: 메시지 큐 도입 - Redis Stream 선택

## 상태
Accepted

## 날짜
2026-01-03

## 컨텍스트

### 문제점
현재 Hamalog의 FCM 푸시 알림, 이메일 발송 등 I/O 작업이 동기적으로 처리되고 있습니다:

```java
// 기존: 동기 처리 - 사용자 응답 대기 발생
fcmService.sendNotification(token, message);
return ResponseEntity.ok("완료"); // FCM 응답까지 대기
```

이로 인한 문제:
1. **응답 지연**: 외부 API 호출 시간만큼 사용자 응답 지연
2. **장애 전파**: FCM/이메일 서버 장애 시 메인 서비스 영향
3. **재시도 불가**: 일시적 실패 시 재시도 로직 부재
4. **확장성 제한**: 대량 푸시 발송 시 처리량 한계

### 고려한 대안

| 솔루션 | 장점 | 단점 |
|--------|------|------|
| **Redis Stream** | 기존 Redis 활용, 낮은 복잡도, 즉시 도입 가능 | 제한된 메시지 보장 수준 |
| **Apache Kafka** | 강력한 내구성, 대용량 처리, 이벤트 리플레이 | 높은 운영 복잡도, 별도 인프라 필요 |
| **RabbitMQ** | 성숙한 AMQP, 유연한 라우팅 | 별도 서버 필요, 운영 부담 |
| **AWS SQS** | 완전 관리형, 높은 가용성 | 클라우드 종속, 비용 발생 |
| **@Async만** | 구현 간단 | 재시도/DLQ 없음, 서버 재시작 시 유실 |

## 결정

**Redis Stream**을 선택합니다.

### 선택 이유

1. **기존 인프라 활용**: Redis 7이 이미 캐싱, CSRF 토큰, Rate Limiting에 사용 중
2. **적절한 규모**: 헬스케어 앱 예상 트래픽(수백~수천 DAU)에 충분
3. **Consumer Group 지원**: 다중 Consumer 확장 가능
4. **At-least-once 보장**: XACK 메커니즘으로 메시지 손실 방지
5. **낮은 학습 곡선**: Spring Data Redis로 즉시 통합
6. **비용 효율**: 추가 인프라 비용 없음

### Kafka가 필요한 시점 (현재 해당 안 됨)

다음 조건 중 하나라도 해당하면 Kafka 도입 검토:
- 일일 10만 건 이상 메시지 처리
- 이벤트 소싱 + CQRS 아키텍처 도입
- 7일 이상 메시지 리플레이 필요
- 다중 서비스 간 이벤트 버스 필요

## 구현

### 아키텍처

```
┌─────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  Service    │────▶│  Redis Stream   │────▶│   Consumer      │
│  (Producer) │     │  (Message Queue)│     │  (FCM 발송)     │
└─────────────┘     └─────────────────┘     └─────────────────┘
                            │
                            ▼ (실패 시)
                    ┌─────────────────┐     ┌─────────────────┐
                    │  Dead Letter    │────▶│  Discord Alert  │
                    │  Queue (DLQ)    │     │  (Webhook)      │
                    └─────────────────┘     └─────────────────┘
```

### 핵심 컴포넌트

| 컴포넌트 | 역할 |
|----------|------|
| `MessageQueueService` | 메시지 발행 (Producer) |
| `NotificationConsumer` | 메시지 소비 및 FCM 발송 |
| `QueuedNotificationService` | 큐 활성화 여부에 따른 Facade |
| `DiscordWebhookService` | DLQ 알림 발송 |

### 재시도 전략

```
메시지 발행 → Consumer 처리 시도
                  ↓ 실패
            재시도 (최대 3회)
                  ↓ 모두 실패
            DLQ 이동 + Discord 알림
```

### 설정 옵션

```properties
# 활성화/비활성화
hamalog.queue.enabled=true

# 재시도 설정
hamalog.queue.max-retries=3

# Discord 알림
hamalog.queue.discord.enabled=true
hamalog.queue.discord.webhook-url=https://discord.com/api/webhooks/...
```

## 결과

### 장점
- ✅ API 응답 시간 단축 (FCM 호출 대기 제거)
- ✅ 장애 격리 (FCM 장애가 메인 서비스에 영향 안 함)
- ✅ 재시도 로직 내장 (일시적 실패 자동 복구)
- ✅ DLQ로 실패 메시지 추적 가능
- ✅ Discord 알림으로 실시간 모니터링
- ✅ 메트릭 수집 (Prometheus 연동)

### 단점
- ⚠️ Redis 장애 시 메시지 큐도 영향 (기존 캐시와 동일한 위험)
- ⚠️ Exactly-once 보장 안 됨 (At-least-once, 중복 가능)
- ⚠️ 대용량 트래픽 시 Kafka 마이그레이션 필요

### 마이그레이션 경로

현재 설계는 나중에 Kafka로 쉽게 전환할 수 있도록 구성되었습니다:
1. `MessageQueueService` 인터페이스 추출
2. `KafkaMessageQueueService` 구현체 추가
3. 설정으로 구현체 전환

## 모니터링

### Prometheus 메트릭

| 메트릭 | 설명 |
|--------|------|
| `hamalog.queue.messages.published` | 발행된 메시지 수 |
| `hamalog.queue.messages.processed` | 처리 성공 메시지 수 |
| `hamalog.queue.messages.failed` | 처리 실패 메시지 수 |
| `hamalog.queue.messages.dlq` | DLQ로 이동된 메시지 수 |
| `hamalog.queue.processing.time` | 메시지 처리 시간 |

### Discord 알림 예시

DLQ 적재 시 다음 정보가 Discord로 발송됩니다:
- 메시지 ID
- 회원 ID
- 알림 유형
- 재시도 횟수
- 에러 메시지

## 참고

- [Redis Streams 공식 문서](https://redis.io/docs/data-types/streams/)
- [Spring Data Redis Stream 가이드](https://docs.spring.io/spring-data/redis/docs/current/reference/html/#redis.streams)
- [ADR-0005: Redis 캐시 전략](./0005-redis-cache-strategy.md)

