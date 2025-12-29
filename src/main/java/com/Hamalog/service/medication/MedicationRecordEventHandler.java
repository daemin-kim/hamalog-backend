package com.Hamalog.service.medication;

import com.Hamalog.domain.events.medication.MedicationRecordCreated;
import com.Hamalog.logging.StructuredLogger;
import com.Hamalog.logging.events.BusinessEvent;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 복약 기록 이벤트 핸들러
 * 캐시 무효화(동기)와 알림 전송(비동기)를 분리하여 처리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MedicationRecordEventHandler {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StructuredLogger structuredLogger;

    /**
     * 복약 기록 생성 시 캐시 무효화 (동기 처리)
     * 트랜잭션 커밋 후 실행되어 데이터 정합성 보장
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCacheInvalidation(MedicationRecordCreated event) {
        log.debug("Invalidating caches for MedicationRecordCreated: {}", event.getMedicationRecordId());

        try {
            // 복약 통계 캐시 무효화
            String statsKey = "medication_stats:" + event.getMemberId();
            String adherenceKey = "medication_adherence:" + event.getMemberId();
            String summaryKey = "medication_summary:" + event.getMemberId();

            redisTemplate.delete(statsKey);
            redisTemplate.delete(adherenceKey);
            redisTemplate.delete(summaryKey);

            log.debug("Invalidated medication stats caches for memberId: {}", event.getMemberId());

        } catch (Exception e) {
            log.error("Failed to invalidate caches for MedicationRecordCreated: {}",
                    event.getMedicationRecordId(), e);
        }
    }

    /**
     * 복약 기록 생성 시 비즈니스 로깅 및 알림 처리 (비동기)
     */
    @Async("eventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAsyncProcessing(MedicationRecordCreated event) {
        log.debug("Async processing for MedicationRecordCreated: {}", event.getMedicationRecordId());

        try {
            // 비즈니스 이벤트 로깅
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("scheduleId", event.getMedicationScheduleId().toString());
            metadata.put("taken", event.getIsTakeMedication().toString());
            if (event.getRealTakeTime() != null) {
                metadata.put("realTakeTime", event.getRealTakeTime().toString());
            }

            BusinessEvent businessEvent = BusinessEvent.builder()
                    .eventType("MEDICATION_RECORD_CREATED")
                    .userId(event.getMemberLoginId())
                    .entity("MedicationRecord")
                    .action("CREATED")
                    .result(event.getIsTakeMedication() ? "TAKEN" : "MISSED")
                    .metadata(metadata)
                    .build();

            structuredLogger.business(businessEvent);

            // TODO: 연속 복약 달성 시 업적 알림
            // TODO: 복용 1시간 후 부작용 기록 권유 알림 스케줄링

            log.info("Completed async processing for MedicationRecordCreated: recordId={}, memberId={}",
                    event.getMedicationRecordId(), event.getMemberId());

        } catch (Exception e) {
            log.error("Failed async processing for MedicationRecordCreated: {}",
                    event.getMedicationRecordId(), e);
        }
    }
}
