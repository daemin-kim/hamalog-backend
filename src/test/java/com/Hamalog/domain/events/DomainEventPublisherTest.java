package com.Hamalog.domain.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("DomainEventPublisher Tests")
class DomainEventPublisherTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private DomainEventPublisher domainEventPublisher;

    private TestDomainEvent testEvent;

    @BeforeEach
    void setUp() {
        testEvent = new TestDomainEvent("test-aggregate-123");
    }

    @Test
    @DisplayName("Should successfully publish domain event when valid event provided")
    void publish_WithValidEvent_ShouldPublishEvent() {
        // given
        doNothing().when(applicationEventPublisher).publishEvent(any(DomainEvent.class));

        // when
        domainEventPublisher.publish(testEvent);

        // then
        verify(applicationEventPublisher, times(1)).publishEvent(testEvent);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when null event provided")
    void publish_WithNullEvent_ShouldThrowIllegalArgumentException() {
        // given
        DomainEvent nullEvent = null;

        // when & then
        assertThatThrownBy(() -> domainEventPublisher.publish(nullEvent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Domain event cannot be null");

        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Should throw DomainEventPublishingException when ApplicationEventPublisher throws exception")
    void publish_WhenApplicationEventPublisherThrowsException_ShouldThrowDomainEventPublishingException() {
        // given
        RuntimeException cause = new RuntimeException("Publishing failed");
        doThrow(cause).when(applicationEventPublisher).publishEvent(any(DomainEvent.class));

        // when & then
        assertThatThrownBy(() -> domainEventPublisher.publish(testEvent))
                .isInstanceOf(DomainEventPublisher.DomainEventPublishingException.class)
                .hasMessage("Failed to publish domain event: " + testEvent.getEventType())
                .hasCause(cause);

        verify(applicationEventPublisher, times(1)).publishEvent(testEvent);
    }

    @Test
    @DisplayName("Should handle multiple event publications correctly")
    void publish_WithMultipleEvents_ShouldPublishAllEvents() {
        // given
        TestDomainEvent event1 = new TestDomainEvent("aggregate-1");
        TestDomainEvent event2 = new TestDomainEvent("aggregate-2");
        doNothing().when(applicationEventPublisher).publishEvent(any(DomainEvent.class));

        // when
        domainEventPublisher.publish(event1);
        domainEventPublisher.publish(event2);

        // then
        verify(applicationEventPublisher).publishEvent(event1);
        verify(applicationEventPublisher).publishEvent(event2);
        verify(applicationEventPublisher, times(2)).publishEvent(any(DomainEvent.class));
    }

    @Test
    @DisplayName("DomainEventPublishingException should maintain message and cause")
    void domainEventPublishingException_ShouldMaintainMessageAndCause() {
        // given
        String message = "Test error message";
        RuntimeException cause = new RuntimeException("Root cause");

        // when
        DomainEventPublisher.DomainEventPublishingException exception = 
                new DomainEventPublisher.DomainEventPublishingException(message, cause);

        // then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
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