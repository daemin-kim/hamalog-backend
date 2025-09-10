package com.Hamalog.domain.events;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for all domain events in the Hamalog system.
 * Provides common event metadata and structure.
 */
public abstract class DomainEvent {

    private final String eventId;
    private final LocalDateTime occurredOn;
    private final String eventType;

    protected DomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = LocalDateTime.now();
        this.eventType = this.getClass().getSimpleName();
    }

    /**
     * Gets the unique identifier for this event.
     * @return event ID
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Gets the timestamp when this event occurred.
     * @return occurrence timestamp
     */
    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }

    /**
     * Gets the type of this event.
     * @return event type (class name)
     */
    public String getEventType() {
        return eventType;
    }

    /**
     * Gets the aggregate root ID that this event is related to.
     * Must be implemented by concrete event classes.
     * @return aggregate root identifier
     */
    public abstract String getAggregateId();

    @Override
    public String toString() {
        return String.format("%s{eventId='%s', occurredOn=%s, aggregateId='%s'}", 
                eventType, eventId, occurredOn, getAggregateId());
    }
}