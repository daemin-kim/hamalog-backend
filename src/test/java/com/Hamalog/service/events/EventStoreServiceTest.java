package com.Hamalog.service.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.Hamalog.domain.events.DomainEvent;
import com.Hamalog.domain.events.StoredDomainEvent;
import com.Hamalog.repository.events.StoredDomainEventRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("이벤��� 저장 서비스 테스트")
class EventStoreServiceTest {

    @Mock
    private StoredDomainEventRepository repository;

    private EventStoreService eventStoreService;

    @BeforeEach
    void setUp() {
        eventStoreService = new EventStoreService(repository);
    }

    @Nested
    @DisplayName("store")
    class Store {

        @Test
        @DisplayName("성공: 도메인 이벤트 저장")
        void success() {
            // given
            TestDomainEvent event = new TestDomainEvent(1L);
            StoredDomainEvent storedEvent = mock(StoredDomainEvent.class);
            when(storedEvent.getId()).thenReturn(100L);
            when(repository.save(any(StoredDomainEvent.class))).thenReturn(storedEvent);

            // when
            StoredDomainEvent result = eventStoreService.store(event);

            // then
            assertThat(result.getId()).isEqualTo(100L);

            ArgumentCaptor<StoredDomainEvent> captor = ArgumentCaptor.forClass(StoredDomainEvent.class);
            verify(repository).save(captor.capture());

            StoredDomainEvent captured = captor.getValue();
            assertThat(captured.getEventType()).isEqualTo("TestDomainEvent");
            assertThat(captured.getAggregateId()).isEqualTo("1");
        }

        @Test
        @DisplayName("성공: Created 이벤트 aggregate type 추출")
        void success_extractAggregateType_created() {
            // given
            MemberCreated event = new MemberCreated(1L);
            StoredDomainEvent storedEvent = mock(StoredDomainEvent.class);
            when(repository.save(any(StoredDomainEvent.class))).thenReturn(storedEvent);

            // when
            eventStoreService.store(event);

            // then
            ArgumentCaptor<StoredDomainEvent> captor = ArgumentCaptor.forClass(StoredDomainEvent.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getAggregateType()).isEqualTo("Member");
        }

        @Test
        @DisplayName("성공: Event 접미사 제거 후 aggregate type 추출")
        void success_extractAggregateType_event() {
            // given
            MemberDeletedEvent event = new MemberDeletedEvent(1L);
            StoredDomainEvent storedEvent = mock(StoredDomainEvent.class);
            when(repository.save(any(StoredDomainEvent.class))).thenReturn(storedEvent);

            // when
            eventStoreService.store(event);

            // then
            ArgumentCaptor<StoredDomainEvent> captor = ArgumentCaptor.forClass(StoredDomainEvent.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getAggregateType()).isEqualTo("Member");
        }
    }

    @Nested
    @DisplayName("markAsProcessed")
    class MarkAsProcessed {

        @Test
        @DisplayName("성공: 이벤트 처리 완료 표시")
        void success() {
            // given
            Long eventId = 1L;
            StoredDomainEvent storedEvent = mock(StoredDomainEvent.class);
            when(repository.findById(eventId)).thenReturn(Optional.of(storedEvent));

            // when
            eventStoreService.markAsProcessed(eventId);

            // then
            verify(storedEvent).markAsProcessed();
            verify(repository).save(storedEvent);
        }

        @Test
        @DisplayName("성공: 존재하지 않는 이벤트는 무시")
        void success_eventNotFound() {
            // given
            Long eventId = 999L;
            when(repository.findById(eventId)).thenReturn(Optional.empty());

            // when
            eventStoreService.markAsProcessed(eventId);

            // then
            verify(repository).findById(eventId);
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("markAsFailed")
    class MarkAsFailed {

        @Test
        @DisplayName("성공: 이벤트 처리 실패 표시")
        void success() {
            // given
            Long eventId = 1L;
            String errorMessage = "Processing error";
            StoredDomainEvent storedEvent = mock(StoredDomainEvent.class);
            when(repository.findById(eventId)).thenReturn(Optional.of(storedEvent));

            // when
            eventStoreService.markAsFailed(eventId, errorMessage);

            // then
            verify(storedEvent).markAsFailed(errorMessage);
            verify(repository).save(storedEvent);
        }

        @Test
        @DisplayName("성공: 존재하지 않는 이벤트는 무시")
        void success_eventNotFound() {
            // given
            Long eventId = 999L;
            when(repository.findById(eventId)).thenReturn(Optional.empty());

            // when
            eventStoreService.markAsFailed(eventId, "Error");

            // then
            verify(repository).findById(eventId);
            verify(repository, never()).save(any());
        }
    }

    // 테스트용 도메인 이벤트 클래스
    private static class TestDomainEvent extends DomainEvent {
        private final Long aggregateId;

        TestDomainEvent(Long aggregateId) {
            this.aggregateId = aggregateId;
        }

        @Override
        public String getAggregateId() {
            return String.valueOf(aggregateId);
        }
    }

    private static class MemberCreated extends DomainEvent {
        private final Long memberId;

        MemberCreated(Long memberId) {
            this.memberId = memberId;
        }

        @Override
        public String getAggregateId() {
            return String.valueOf(memberId);
        }
    }

    private static class MemberDeletedEvent extends DomainEvent {
        private final Long memberId;

        MemberDeletedEvent(Long memberId) {
            this.memberId = memberId;
        }

        @Override
        public String getAggregateId() {
            return String.valueOf(memberId);
        }
    }
}
