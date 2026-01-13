package com.Hamalog.service.medication;

import com.Hamalog.domain.events.medication.MedicationScheduleCreated;
import com.Hamalog.domain.events.medication.MedicationScheduleDeleted;
import com.Hamalog.domain.events.medication.MedicationScheduleUpdated;
import com.Hamalog.logging.StructuredLogger;
import com.Hamalog.logging.events.AuditEvent;
import com.Hamalog.logging.events.BusinessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Event handler for medication schedule domain events.
 * Handles cross-cutting concerns like audit logging, cache invalidation,
 * and business intelligence tracking in a decoupled manner.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MedicationScheduleEventHandler {

    private final StructuredLogger structuredLogger;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Handles medication schedule creation events.
     * Performs audit logging, cache warming, and business intelligence tracking.
     */
    @EventListener
    public void handleMedicationScheduleCreated(MedicationScheduleCreated event) {
        log.debug("Processing MedicationScheduleCreated event: {}", event.getEventId());

        try {
            // Audit logging for compliance and security
            AuditEvent auditEvent = AuditEvent.builder()
                    .userId(event.getMemberLoginId())
                    .operation("MEDICATION_SCHEDULE_CREATED")
                    .entityType("MedicationSchedule")
                    .entityId(event.getMedicationScheduleId().toString())
                    .details("Created medication schedule: " + event.getName())
                    .build();
            
            structuredLogger.audit(auditEvent);

            // Business intelligence tracking
            java.util.Map<String, Object> metadata = new java.util.HashMap<>();
            metadata.put("hospitalName", event.getHospitalName());
            metadata.put("prescriptionDays", event.getPrescriptionDays().toString());
            metadata.put("perDay", event.getPerDay().toString());
            metadata.put("alarmType", event.getAlarmType().toString());
            
            BusinessEvent businessEvent = BusinessEvent.builder()
                    .eventType("MEDICATION_SCHEDULE_CREATED")
                    .userId(event.getMemberLoginId())
                    .entity("MedicationSchedule")
                    .action("CREATED")
                    .result("SUCCESS")
                    .metadata(metadata)
                    .build();
            
            structuredLogger.business(businessEvent);

            // Cache invalidation - clear user's medication schedules cache
            String cacheKey = "medication_schedules:" + event.getMemberId();
            redisTemplate.delete(cacheKey);
            log.debug("Invalidated cache for user medication schedules: {}", event.getMemberId());

            log.info("Successfully processed MedicationScheduleCreated event for schedule ID: {}", 
                    event.getMedicationScheduleId());

        } catch (Exception e) {
            log.error("Failed to process MedicationScheduleCreated event: {} - Error: {}", 
                    event.getEventId(), e.getMessage(), e);
            // In production, you might want to publish a failure event or retry
        }
    }

    /**
     * Handles medication schedule update events.
     * Performs audit logging, cache invalidation, and change tracking.
     */
    @EventListener
    public void handleMedicationScheduleUpdated(MedicationScheduleUpdated event) {
        log.debug("Processing MedicationScheduleUpdated event: {}", event.getEventId());

        try {
            // Audit logging for compliance and security
            AuditEvent auditEvent = AuditEvent.builder()
                    .userId(event.getMemberLoginId())
                    .operation("MEDICATION_SCHEDULE_UPDATED")
                    .entityType("MedicationSchedule")
                    .entityId(event.getMedicationScheduleId().toString())
                    .details("Updated medication schedule: " + event.getName())
                    .build();
            
            structuredLogger.audit(auditEvent);

            // Business intelligence tracking for schedule modifications
            java.util.Map<String, Object> metadata = new java.util.HashMap<>();
            metadata.put("scheduleId", event.getMedicationScheduleId().toString());
            metadata.put("scheduleName", event.getName());
            
            BusinessEvent businessEvent = BusinessEvent.builder()
                    .eventType("MEDICATION_SCHEDULE_UPDATED")
                    .userId(event.getMemberLoginId())
                    .entity("MedicationSchedule")
                    .action("UPDATED")
                    .result("SUCCESS")
                    .metadata(metadata)
                    .build();
            
            structuredLogger.business(businessEvent);

            // Cache invalidation - clear related caches
            String userCacheKey = "medication_schedules:" + event.getMemberId();
            String scheduleCacheKey = "medication_schedule:" + event.getMedicationScheduleId();
            redisTemplate.delete(userCacheKey);
            redisTemplate.delete(scheduleCacheKey);
            log.debug("Invalidated caches for medication schedule update: {}", event.getMedicationScheduleId());

            log.info("Successfully processed MedicationScheduleUpdated event for schedule ID: {}", 
                    event.getMedicationScheduleId());

        } catch (Exception e) {
            log.error("Failed to process MedicationScheduleUpdated event: {} - Error: {}", 
                    event.getEventId(), e.getMessage(), e);
        }
    }

    /**
     * Handles medication schedule deletion events.
     * Performs audit logging, cache cleanup, and retention policy enforcement.
     */
    @EventListener
    public void handleMedicationScheduleDeleted(MedicationScheduleDeleted event) {
        log.debug("Processing MedicationScheduleDeleted event: {}", event.getEventId());

        try {
            // Audit logging for compliance and security (important for deletions)
            AuditEvent auditEvent = AuditEvent.builder()
                    .userId(event.getMemberLoginId())
                    .operation("MEDICATION_SCHEDULE_DELETED")
                    .entityType("MedicationSchedule")
                    .entityId(event.getMedicationScheduleId().toString())
                    .details("Deleted medication schedule: " + event.getName())
                    .build();
            
            structuredLogger.audit(auditEvent);

            // Business intelligence tracking for deletion patterns
            java.util.Map<String, Object> metadata = new java.util.HashMap<>();
            metadata.put("scheduleId", event.getMedicationScheduleId().toString());
            metadata.put("scheduleName", event.getName());
            
            BusinessEvent businessEvent = BusinessEvent.builder()
                    .eventType("MEDICATION_SCHEDULE_DELETED")
                    .userId(event.getMemberLoginId())
                    .entity("MedicationSchedule")
                    .action("DELETED")
                    .result("SUCCESS")
                    .metadata(metadata)
                    .build();
            
            structuredLogger.business(businessEvent);

            // Cache cleanup - remove all related cached data
            String userCacheKey = "medication_schedules:" + event.getMemberId();
            String scheduleCacheKey = "medication_schedule:" + event.getMedicationScheduleId();
            redisTemplate.delete(userCacheKey);
            redisTemplate.delete(scheduleCacheKey);
            log.debug("Cleaned up caches for deleted medication schedule: {}", event.getMedicationScheduleId());

            // NOTE: 향후 개선사항 (FUTURE-IMPROVEMENTS.md 참조)
            // - Soft Delete 도입 시 삭제된 데이터 아카이빙
            // - 데이터 보존 정책 적용
            // - 관련 통계/카운터 업데이트

            log.info("Successfully processed MedicationScheduleDeleted event for schedule ID: {}", 
                    event.getMedicationScheduleId());

        } catch (Exception e) {
            log.error("Failed to process MedicationScheduleDeleted event: {} - Error: {}", 
                    event.getEventId(), e.getMessage(), e);
        }
    }
}