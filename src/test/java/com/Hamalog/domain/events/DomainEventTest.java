package com.Hamalog.domain.events;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DomainEvent Tests")
class DomainEventTest {

    @Test
    @DisplayName("Should generate unique event ID when event is created")
    void constructor_ShouldGenerateUniqueEventId() {
        // given & when
        TestDomainEvent event1 = new TestDomainEvent("aggregate-1");
        TestDomainEvent event2 = new TestDomainEvent("aggregate-2");

        // then
        assertThat(event1.getEventId()).isNotNull();
        assertThat(event2.getEventId()).isNotNull();
        assertThat(event1.getEventId()).isNotEqualTo(event2.getEventId());
    }

    @Test
    @DisplayName("Should set occurredOn timestamp when event is created")
    void constructor_ShouldSetOccurredOnTimestamp() {
        // given
        LocalDateTime beforeCreation = LocalDateTime.now().minusSeconds(1);
        
        // when
        TestDomainEvent event = new TestDomainEvent("test-aggregate");
        
        // then
        LocalDateTime afterCreation = LocalDateTime.now().plusSeconds(1);
        assertThat(event.getOccurredOn()).isNotNull();
        assertThat(event.getOccurredOn()).isAfter(beforeCreation);
        assertThat(event.getOccurredOn()).isBefore(afterCreation);
    }

    @Test
    @DisplayName("Should set event type based on class name")
    void constructor_ShouldSetEventTypeBasedOnClassName() {
        // given & when
        TestDomainEvent event = new TestDomainEvent("test-aggregate");

        // then
        assertThat(event.getEventType()).isEqualTo("TestDomainEvent");
    }

    @Test
    @DisplayName("Should return aggregate ID from concrete implementation")
    void getAggregateId_ShouldReturnAggregateIdFromConcreteImplementation() {
        // given
        String expectedAggregateId = "test-aggregate-123";
        
        // when
        TestDomainEvent event = new TestDomainEvent(expectedAggregateId);

        // then
        assertThat(event.getAggregateId()).isEqualTo(expectedAggregateId);
    }

    @Test
    @DisplayName("Should generate proper toString representation")
    void toString_ShouldGenerateProperStringRepresentation() {
        // given
        String aggregateId = "test-aggregate";
        TestDomainEvent event = new TestDomainEvent(aggregateId);

        // when
        String result = event.toString();

        // then
        assertThat(result).contains("TestDomainEvent");
        assertThat(result).contains("eventId='" + event.getEventId() + "'");
        assertThat(result).contains("occurredOn=" + event.getOccurredOn());
        assertThat(result).contains("aggregateId='" + aggregateId + "'");
    }

    @Test
    @DisplayName("Should maintain immutable event metadata")
    void eventMetadata_ShouldBeImmutable() {
        // given
        TestDomainEvent event = new TestDomainEvent("test-aggregate");
        String originalEventId = event.getEventId();
        LocalDateTime originalOccurredOn = event.getOccurredOn();
        String originalEventType = event.getEventType();

        // when - simulate time passing
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // then - metadata should remain unchanged
        assertThat(event.getEventId()).isEqualTo(originalEventId);
        assertThat(event.getOccurredOn()).isEqualTo(originalOccurredOn);
        assertThat(event.getEventType()).isEqualTo(originalEventType);
    }

    @Test
    @DisplayName("Should handle different aggregate ID types correctly")
    void getAggregateId_WithDifferentIdTypes_ShouldHandleCorrectly() {
        // given & when
        TestDomainEvent eventWithNumericId = new TestDomainEvent("12345");
        TestDomainEvent eventWithUuid = new TestDomainEvent("550e8400-e29b-41d4-a716-446655440000");
        TestDomainEvent eventWithStringId = new TestDomainEvent("user-profile-id");

        // then
        assertThat(eventWithNumericId.getAggregateId()).isEqualTo("12345");
        assertThat(eventWithUuid.getAggregateId()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
        assertThat(eventWithStringId.getAggregateId()).isEqualTo("user-profile-id");
    }

    // Test implementation of DomainEvent for testing purposes
    private static class TestDomainEvent extends DomainEvent {
        private final String aggregateId;

        public TestDomainEvent(String aggregateId) {
            super();
            this.aggregateId = aggregateId;
        }

        @Override
        public String getAggregateId() {
            return aggregateId;
        }
    }
}