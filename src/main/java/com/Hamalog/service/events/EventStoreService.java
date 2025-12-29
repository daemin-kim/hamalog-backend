package com.Hamalog.service.events;

import com.Hamalog.domain.events.DomainEvent;
import com.Hamalog.domain.events.StoredDomainEvent;
import com.Hamalog.repository.events.StoredDomainEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 도메인 이벤트 저장 서비스
 * 모든 도메인 이벤트를 영속화하여 감사 로그 및 장애 복구에 활용
 */
@Service
@Slf4j
public class EventStoreService {

    private final StoredDomainEventRepository repository;
    private final ObjectMapper objectMapper;

    public EventStoreService(StoredDomainEventRepository repository) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * 도메인 이벤트를 저장소에 영속화
     * 별도 트랜잭션으로 처리하여 메인 트랜잭션에 영향을 주지 않음
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public StoredDomainEvent store(DomainEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            String aggregateType = extractAggregateType(event);

            StoredDomainEvent storedEvent = new StoredDomainEvent(
                    event.getEventId(),
                    event.getEventType(),
                    aggregateType,
                    event.getAggregateId(),
                    payload,
                    event.getOccurredOn()
            );

            StoredDomainEvent saved = repository.save(storedEvent);
            log.debug("Stored domain event: {} with ID: {}", event.getEventType(), saved.getId());
            return saved;

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize domain event: {}", event.getEventType(), e);
            throw new EventStoreException("Failed to serialize event", e);
        }
    }

    /**
     * 이벤트 처리 완료 표시
     */
    @Transactional
    public void markAsProcessed(Long eventId) {
        repository.findById(eventId).ifPresent(event -> {
            event.markAsProcessed();
            repository.save(event);
            log.debug("Marked event as processed: {}", eventId);
        });
    }

    /**
     * 이벤트 처리 실패 표시
     */
    @Transactional
    public void markAsFailed(Long eventId, String errorMessage) {
        repository.findById(eventId).ifPresent(event -> {
            event.markAsFailed(errorMessage);
            repository.save(event);
            log.warn("Marked event as failed: {} - {}", eventId, errorMessage);
        });
    }

    private String extractAggregateType(DomainEvent event) {
        String eventType = event.getEventType();
        // MedicationScheduleCreated -> MedicationSchedule
        // MemberDeletedEvent -> Member
        if (eventType.endsWith("Event")) {
            eventType = eventType.substring(0, eventType.length() - 5);
        }
        // 마지막 동사 제거 (Created, Updated, Deleted 등)
        String[] suffixes = {"Created", "Updated", "Deleted", "Succeeded", "Failed"};
        for (String suffix : suffixes) {
            if (eventType.endsWith(suffix)) {
                return eventType.substring(0, eventType.length() - suffix.length());
            }
        }
        return eventType;
    }

    public static class EventStoreException extends RuntimeException {
        public EventStoreException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
