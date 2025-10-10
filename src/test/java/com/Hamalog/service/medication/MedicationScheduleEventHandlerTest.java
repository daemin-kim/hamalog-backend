package com.Hamalog.service.medication;

import com.Hamalog.domain.events.medication.MedicationScheduleCreated;
import com.Hamalog.domain.events.medication.MedicationScheduleDeleted;
import com.Hamalog.domain.events.medication.MedicationScheduleUpdated;
import com.Hamalog.domain.medication.AlarmType;
import com.Hamalog.logging.StructuredLogger;
import com.Hamalog.logging.events.AuditEvent;
import com.Hamalog.logging.events.BusinessEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MedicationScheduleEventHandler Tests")
class MedicationScheduleEventHandlerTest {

    @Mock
    private StructuredLogger structuredLogger;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private MedicationScheduleEventHandler eventHandler;

    private MedicationScheduleCreated createdEvent;
    private MedicationScheduleUpdated updatedEvent;
    private MedicationScheduleDeleted deletedEvent;

    @BeforeEach
    void setUp() {
        // Setup MedicationScheduleCreated event
        createdEvent = new MedicationScheduleCreated(
                1L,                          // medicationScheduleId
                100L,                        // memberId
                "testuser",                  // memberLoginId
                "Morning Pills",             // name
                "Seoul Hospital",            // hospitalName
                LocalDate.now(),             // prescriptionDate
                "Test memo",                 // memo
                LocalDate.now().plusDays(1), // startOfAd
                30,                          // prescriptionDays
                2,                           // perDay
                AlarmType.SOUND              // alarmType
        );

        // Setup MedicationScheduleUpdated event
        updatedEvent = new MedicationScheduleUpdated(
                1L,                          // medicationScheduleId
                100L,                        // memberId
                "testuser",                  // memberLoginId
                "Updated Morning Pills",     // name
                "Seoul Hospital",            // hospitalName
                LocalDate.now(),             // prescriptionDate
                "Updated memo",              // memo
                LocalDate.now().plusDays(1), // startOfAd
                30,                          // prescriptionDays
                2,                           // perDay
                AlarmType.SOUND              // alarmType
        );

        // Setup MedicationScheduleDeleted event
        deletedEvent = new MedicationScheduleDeleted(
                1L,                          // medicationScheduleId
                100L,                        // memberId
                "testuser",                  // memberLoginId
                "Deleted Pills"              // name
        );
    }

    @Test
    @DisplayName("Should handle MedicationScheduleCreated event successfully")
    void handleMedicationScheduleCreated_WithValidEvent_ShouldProcessSuccessfully() {
        // given
        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        ArgumentCaptor<BusinessEvent> businessCaptor = ArgumentCaptor.forClass(BusinessEvent.class);

        // when
        eventHandler.handleMedicationScheduleCreated(createdEvent);

        // then
        // Verify audit logging
        verify(structuredLogger).audit(auditCaptor.capture());
        AuditEvent capturedAuditEvent = auditCaptor.getValue();
        assertThat(capturedAuditEvent.getUserId()).isEqualTo("testuser");
        assertThat(capturedAuditEvent.getOperation()).isEqualTo("MEDICATION_SCHEDULE_CREATED");
        assertThat(capturedAuditEvent.getEntityType()).isEqualTo("MedicationSchedule");
        assertThat(capturedAuditEvent.getEntityId()).isEqualTo("1");
        assertThat(capturedAuditEvent.getDetails()).contains("Morning Pills");

        // Verify business logging
        verify(structuredLogger).business(businessCaptor.capture());
        BusinessEvent capturedBusinessEvent = businessCaptor.getValue();
        assertThat(capturedBusinessEvent.getEventType()).isEqualTo("MEDICATION_SCHEDULE_CREATED");
        assertThat(capturedBusinessEvent.getUserId()).isEqualTo("testuser");
        assertThat(capturedBusinessEvent.getEntity()).isEqualTo("MedicationSchedule");
        assertThat(capturedBusinessEvent.getAction()).isEqualTo("CREATED");
        assertThat(capturedBusinessEvent.getResult()).isEqualTo("SUCCESS");

        // Verify business event metadata
        Map<String, Object> metadata = capturedBusinessEvent.getMetadata();
        assertThat(metadata).containsEntry("hospitalName", "Seoul Hospital");
        assertThat(metadata).containsEntry("prescriptionDays", "30");
        assertThat(metadata).containsEntry("perDay", "2");
        assertThat(metadata).containsEntry("alarmType", "SOUND");

        // Verify cache invalidation
        verify(redisTemplate).delete("medication_schedules:100");
    }

    @Test
    @DisplayName("Should handle MedicationScheduleUpdated event successfully")
    void handleMedicationScheduleUpdated_WithValidEvent_ShouldProcessSuccessfully() {
        // given
        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        ArgumentCaptor<BusinessEvent> businessCaptor = ArgumentCaptor.forClass(BusinessEvent.class);

        // when
        eventHandler.handleMedicationScheduleUpdated(updatedEvent);

        // then
        // Verify audit logging
        verify(structuredLogger).audit(auditCaptor.capture());
        AuditEvent capturedAuditEvent = auditCaptor.getValue();
        assertThat(capturedAuditEvent.getUserId()).isEqualTo("testuser");
        assertThat(capturedAuditEvent.getOperation()).isEqualTo("MEDICATION_SCHEDULE_UPDATED");
        assertThat(capturedAuditEvent.getEntityType()).isEqualTo("MedicationSchedule");
        assertThat(capturedAuditEvent.getEntityId()).isEqualTo("1");
        assertThat(capturedAuditEvent.getDetails()).contains("Updated Morning Pills");

        // Verify business logging
        verify(structuredLogger).business(businessCaptor.capture());
        BusinessEvent capturedBusinessEvent = businessCaptor.getValue();
        assertThat(capturedBusinessEvent.getEventType()).isEqualTo("MEDICATION_SCHEDULE_UPDATED");
        assertThat(capturedBusinessEvent.getUserId()).isEqualTo("testuser");
        assertThat(capturedBusinessEvent.getEntity()).isEqualTo("MedicationSchedule");
        assertThat(capturedBusinessEvent.getAction()).isEqualTo("UPDATED");
        assertThat(capturedBusinessEvent.getResult()).isEqualTo("SUCCESS");

        // Verify business event metadata
        Map<String, Object> metadata = capturedBusinessEvent.getMetadata();
        assertThat(metadata).containsEntry("scheduleId", "1");
        assertThat(metadata).containsEntry("scheduleName", "Updated Morning Pills");

        // Verify cache invalidation for both user and specific schedule
        verify(redisTemplate).delete("medication_schedules:100");
        verify(redisTemplate).delete("medication_schedule:1");
    }

    @Test
    @DisplayName("Should handle MedicationScheduleDeleted event successfully")
    void handleMedicationScheduleDeleted_WithValidEvent_ShouldProcessSuccessfully() {
        // given
        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        ArgumentCaptor<BusinessEvent> businessCaptor = ArgumentCaptor.forClass(BusinessEvent.class);

        // when
        eventHandler.handleMedicationScheduleDeleted(deletedEvent);

        // then
        // Verify audit logging
        verify(structuredLogger).audit(auditCaptor.capture());
        AuditEvent capturedAuditEvent = auditCaptor.getValue();
        assertThat(capturedAuditEvent.getUserId()).isEqualTo("testuser");
        assertThat(capturedAuditEvent.getOperation()).isEqualTo("MEDICATION_SCHEDULE_DELETED");
        assertThat(capturedAuditEvent.getEntityType()).isEqualTo("MedicationSchedule");
        assertThat(capturedAuditEvent.getEntityId()).isEqualTo("1");
        assertThat(capturedAuditEvent.getDetails()).contains("Deleted Pills");

        // Verify business logging
        verify(structuredLogger).business(businessCaptor.capture());
        BusinessEvent capturedBusinessEvent = businessCaptor.getValue();
        assertThat(capturedBusinessEvent.getEventType()).isEqualTo("MEDICATION_SCHEDULE_DELETED");
        assertThat(capturedBusinessEvent.getUserId()).isEqualTo("testuser");
        assertThat(capturedBusinessEvent.getEntity()).isEqualTo("MedicationSchedule");
        assertThat(capturedBusinessEvent.getAction()).isEqualTo("DELETED");
        assertThat(capturedBusinessEvent.getResult()).isEqualTo("SUCCESS");

        // Verify business event metadata
        Map<String, Object> metadata = capturedBusinessEvent.getMetadata();
        assertThat(metadata).containsEntry("scheduleId", "1");
        assertThat(metadata).containsEntry("scheduleName", "Deleted Pills");

        // Verify cache cleanup for both user and specific schedule
        verify(redisTemplate).delete("medication_schedules:100");
        verify(redisTemplate).delete("medication_schedule:1");
    }

    @Test
    @DisplayName("Should handle exception gracefully during created event processing")
    void handleMedicationScheduleCreated_WithException_ShouldLogErrorAndContinue() {
        // given
        doThrow(new RuntimeException("Redis connection failed"))
                .when(redisTemplate).delete(any(String.class));

        // when
        eventHandler.handleMedicationScheduleCreated(createdEvent);

        // then
        // Verify that audit and business logging still occurred
        verify(structuredLogger).audit(any(AuditEvent.class));
        verify(structuredLogger).business(any(BusinessEvent.class));
        
        // Verify that cache deletion was attempted despite the exception
        verify(redisTemplate).delete("medication_schedules:100");
    }

    @Test
    @DisplayName("Should handle exception gracefully during updated event processing")
    void handleMedicationScheduleUpdated_WithException_ShouldLogErrorAndContinue() {
        // given
        doThrow(new RuntimeException("Logging service unavailable"))
                .when(structuredLogger).audit(any(AuditEvent.class));

        // when
        eventHandler.handleMedicationScheduleUpdated(updatedEvent);

        // then
        // Verify that audit logging was attempted
        verify(structuredLogger).audit(any(AuditEvent.class));
        
        // Business logging and cache operations should not occur due to early exception
        verify(structuredLogger, never()).business(any(BusinessEvent.class));
        verify(redisTemplate, never()).delete(any(String.class));
    }

    @Test
    @DisplayName("Should handle exception gracefully during deleted event processing")
    void handleMedicationScheduleDeleted_WithException_ShouldLogErrorAndContinue() {
        // given
        doThrow(new RuntimeException("Business logging failed"))
                .when(structuredLogger).business(any(BusinessEvent.class));

        // when
        eventHandler.handleMedicationScheduleDeleted(deletedEvent);

        // then
        // Verify that audit logging occurred before the exception
        verify(structuredLogger).audit(any(AuditEvent.class));
        
        // Verify that business logging was attempted
        verify(structuredLogger).business(any(BusinessEvent.class));
        
        // Cache operations should not occur due to earlier exception
        verify(redisTemplate, never()).delete(any(String.class));
    }

    @Test
    @DisplayName("Should properly construct cache keys with member ID")
    void eventHandlers_ShouldConstructProperCacheKeys() {
        // when
        eventHandler.handleMedicationScheduleCreated(createdEvent);
        eventHandler.handleMedicationScheduleUpdated(updatedEvent);
        eventHandler.handleMedicationScheduleDeleted(deletedEvent);

        // then
        // Verify correct cache key construction for user schedules
        verify(redisTemplate, times(3)).delete("medication_schedules:100");
        
        // Verify specific schedule cache keys for update and delete operations
        verify(redisTemplate, times(2)).delete("medication_schedule:1");
    }

    @Test
    @DisplayName("Should handle events with minimal required data")
    void eventHandlers_WithMinimalData_ShouldProcessSuccessfully() {
        // given
        MedicationScheduleCreated minimalEvent = new MedicationScheduleCreated(
                999L,                        // medicationScheduleId
                888L,                        // memberId
                "minimaluser",               // memberLoginId
                "Minimal Schedule",          // name
                "",                          // hospitalName
                LocalDate.now(),             // prescriptionDate
                "Minimal memo",              // memo
                LocalDate.now(),             // startOfAd
                1,                           // prescriptionDays
                1,                           // perDay
                AlarmType.SOUND              // alarmType
        );

        // when
        eventHandler.handleMedicationScheduleCreated(minimalEvent);

        // then
        verify(structuredLogger).audit(any(AuditEvent.class));
        verify(structuredLogger).business(any(BusinessEvent.class));
        verify(redisTemplate).delete("medication_schedules:888");
    }
}