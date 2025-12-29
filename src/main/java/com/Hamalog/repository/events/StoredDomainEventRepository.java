package com.Hamalog.repository.events;

import com.Hamalog.domain.events.StoredDomainEvent;
import com.Hamalog.domain.events.StoredDomainEvent.EventStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 도메인 이벤트 저장소 Repository
 */
@Repository
public interface StoredDomainEventRepository extends JpaRepository<StoredDomainEvent, Long> {

    /**
     * 특정 상태의 이벤트 조회
     */
    List<StoredDomainEvent> findByStatus(EventStatus status);

    /**
     * 재시도 대상 실패 이벤트 조회 (최대 재시도 횟수 이하)
     */
    @Query("SELECT e FROM StoredDomainEvent e WHERE e.status = 'FAILED' AND e.retryCount < :maxRetries ORDER BY e.occurredOn ASC")
    List<StoredDomainEvent> findFailedEventsForRetry(@Param("maxRetries") int maxRetries);

    /**
     * 특정 Aggregate의 이벤트 이력 조회
     */
    List<StoredDomainEvent> findByAggregateTypeAndAggregateIdOrderByOccurredOnAsc(
            String aggregateType, String aggregateId);

    /**
     * 특정 기간의 이벤트 조회
     */
    List<StoredDomainEvent> findByOccurredOnBetweenOrderByOccurredOnDesc(
            LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 이벤트 타입별 조회
     */
    List<StoredDomainEvent> findByEventTypeOrderByOccurredOnDesc(String eventType);

    /**
     * 오래된 처리 완료 이벤트 삭제 (데이터 정리용)
     */
    @Modifying
    @Query("DELETE FROM StoredDomainEvent e WHERE e.status = 'PROCESSED' AND e.processedAt < :cutoffDate")
    int deleteProcessedEventsBefore(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * 미처리 이벤트 수 조회
     */
    long countByStatus(EventStatus status);
}
