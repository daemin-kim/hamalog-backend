package com.Hamalog.domain.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Domain event publisher that wraps Spring's ApplicationEventPublisher.
 * Provides a domain-specific interface for publishing domain events.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Publishes a domain event to all registered event handlers.
     * 
     * @param event the domain event to publish
     * @throws IllegalArgumentException if event is null
     */
    public void publish(DomainEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Domain event cannot be null");
        }

        log.debug("Publishing domain event: {}", event);
        
        try {
            applicationEventPublisher.publishEvent(event);
            log.debug("Successfully published domain event: {} with ID: {}", 
                    event.getEventType(), event.getEventId());
        } catch (Exception e) {
            log.error("Failed to publish domain event: {} with ID: {}. Error: {}", 
                    event.getEventType(), event.getEventId(), e.getMessage(), e);
            throw new DomainEventPublishingException(
                    "Failed to publish domain event: " + event.getEventType(), e);
        }
    }

    /**
     * Exception thrown when domain event publishing fails.
     */
    public static class DomainEventPublishingException extends RuntimeException {
        public DomainEventPublishingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}