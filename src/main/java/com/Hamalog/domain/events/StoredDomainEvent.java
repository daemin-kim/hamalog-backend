package com.Hamalog.domain.events;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 도메인 이벤트 영속화 엔티티
 * 감사 로그, 이벤트 소싱, 장애 복구를 위한 이벤트 저장소
 */
@Entity
@Table(name = "domain_event", indexes = {
    @Index(name = "idx_domain_event_aggregate", columnList = "aggregateType, aggregateId"),
    @Index(name = "idx_domain_event_occurred", columnList = "occurredOn"),
    @Index(name = "idx_domain_event_type", columnList = "eventType"),
    @Index(name = "idx_domain_event_status", columnList = "status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoredDomainEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String eventId;

    @Column(nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, length = 100)
    private String aggregateType;

    @Column(nullable = false, length = 100)
    private String aggregateId;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private LocalDateTime occurredOn;

    @Column(nullable = false)
    private LocalDateTime storedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status;

    private LocalDateTime processedAt;

    @Column(length = 500)
    private String errorMessage;

    private Integer retryCount;

    public StoredDomainEvent(
            String eventId,
            String eventType,
            String aggregateType,
            String aggregateId,
            String payload,
            LocalDateTime occurredOn
    ) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.occurredOn = occurredOn;
        this.storedAt = LocalDateTime.now();
        this.status = EventStatus.PENDING;
        this.retryCount = 0;
    }

    public void markAsProcessed() {
        this.status = EventStatus.PROCESSED;
        this.processedAt = LocalDateTime.now();
    }

    public void markAsFailed(String errorMessage) {
        this.status = EventStatus.FAILED;
        this.errorMessage = errorMessage;
        this.retryCount = (this.retryCount == null) ? 1 : this.retryCount + 1;
    }

    public void markForRetry() {
        this.status = EventStatus.PENDING;
    }

    public enum EventStatus {
        PENDING,    // 처리 대기
        PROCESSED,  // 처리 완료
        FAILED      // 처리 실패
    }
}
