package com.Hamalog.service.sideEffect;

import com.Hamalog.domain.events.sideEffect.SideEffectRecordCreated;
import com.Hamalog.logging.StructuredLogger;
import com.Hamalog.logging.events.BusinessEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 부작용 기록 이벤트 핸들러
 * 캐시 갱신(동기)과 심각도 분석/알림(비동기)를 분리하여 처리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SideEffectEventHandler {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StructuredLogger structuredLogger;

    /**
     * 부작용 기록 생성 시 캐시 갱신 (동기 처리)
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCacheUpdate(SideEffectRecordCreated event) {
        log.debug("Updating caches for SideEffectRecordCreated: {}", event.getSideEffectRecordId());

        try {
            // 최근 부작용 캐시 갱신
            String recentKey = "recent_side_effects:" + event.getMemberId();
            redisTemplate.delete(recentKey);

            // 부작용 목록 캐시 무효화
            String listKey = "side_effect_list:" + event.getMemberId();
            redisTemplate.delete(listKey);

            log.debug("Updated side effect caches for memberId: {}", event.getMemberId());

        } catch (Exception e) {
            log.error("Failed to update caches for SideEffectRecordCreated: {}",
                    event.getSideEffectRecordId(), e);
        }
    }

    /**
     * 부작용 기록 생성 시 비즈니스 로깅 및 심각도 분석 (비동기)
     */
    @Async("eventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAsyncProcessing(SideEffectRecordCreated event) {
        log.debug("Async processing for SideEffectRecordCreated: {}", event.getSideEffectRecordId());

        try {
            // 비즈니스 이벤트 로깅
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("maxDegree", event.getMaxDegree().toString());
            metadata.put("sideEffectCount", String.valueOf(event.getSideEffects().size()));
            metadata.put("sideEffects", event.getSideEffects().stream()
                    .map(s -> s.name() + "(" + s.degree() + ")")
                    .collect(Collectors.joining(", ")));

            if (event.isLinkedToMedication()) {
                metadata.put("linkedMedicationScheduleId", event.getLinkedMedicationScheduleId().toString());
            }

            BusinessEvent businessEvent = BusinessEvent.builder()
                    .eventType("SIDE_EFFECT_RECORD_CREATED")
                    .userId(event.getMemberLoginId())
                    .entity("SideEffectRecord")
                    .action("CREATED")
                    .result(event.hasSevereSideEffect() ? "SEVERE" : "NORMAL")
                    .metadata(metadata)
                    .build();

            structuredLogger.business(businessEvent);

            // 심각한 부작용(degree >= 3) 발생 시 알림
            if (event.hasSevereSideEffect()) {
                log.warn("Severe side effect recorded for memberId: {}, maxDegree: {}",
                        event.getMemberId(), event.getMaxDegree());
                // TODO: 심각한 부작용 알림 발송
                // TODO: 의료진 상담 권유 메시지 발송
            }

            // 복약 스케줄과 연계된 부작용인 경우 통계 집계
            if (event.isLinkedToMedication()) {
                log.info("Side effect linked to medication schedule: {} for memberId: {}",
                        event.getLinkedMedicationScheduleId(), event.getMemberId());
                // TODO: 특정 약물별 부작용 통계 업데이트
            }

            log.info("Completed async processing for SideEffectRecordCreated: recordId={}, memberId={}",
                    event.getSideEffectRecordId(), event.getMemberId());

        } catch (Exception e) {
            log.error("Failed async processing for SideEffectRecordCreated: {}",
                    event.getSideEffectRecordId(), e);
        }
    }
}
