package com.Hamalog.service.events;

import com.Hamalog.domain.events.DomainEvent;
import com.Hamalog.domain.events.StoredDomainEvent;
import com.Hamalog.logging.StructuredLogger;
import com.Hamalog.logging.events.AuditEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 모든 도메인 이벤트를 저장소에 영속화하는 핸들러
 * 가장 먼저 실행되어 이벤트를 저장한 후 다른 핸들러들이 처리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventPersistenceHandler {

    private final EventStoreService eventStoreService;
    private final StructuredLogger structuredLogger;

    /**
     * 모든 도메인 이벤트를 저장소에 영속화
     * Order(-100)으로 다른 핸들러들보다 먼저 실행
     */
    @EventListener
    @Order(-100)
    public void persistEvent(DomainEvent event) {
        try {
            StoredDomainEvent storedEvent = eventStoreService.store(event);
            log.debug("Persisted domain event: {} -> stored ID: {}",
                    event.getEventType(), storedEvent.getId());

            // 감사 로그 기록
            AuditEvent auditEvent = AuditEvent.builder()
                    .userId(extractUserId(event))
                    .operation("DOMAIN_EVENT_PUBLISHED")
                    .entityType(event.getEventType())
                    .entityId(event.getAggregateId())
                    .details("Event ID: " + event.getEventId())
                    .build();
            structuredLogger.audit(auditEvent);

        } catch (Exception e) {
            // 이벤트 저장 실패해도 메인 로직은 계속 진행
            log.error("Failed to persist domain event: {} - {}",
                    event.getEventType(), e.getMessage(), e);
        }
    }

    private String extractUserId(DomainEvent event) {
        // 이벤트에서 userId 추출 시도
        try {
            var method = event.getClass().getMethod("getMemberLoginId");
            Object result = method.invoke(event);
            return result != null ? result.toString() : "system";
        } catch (Exception e) {
            return "system";
        }
    }
}
