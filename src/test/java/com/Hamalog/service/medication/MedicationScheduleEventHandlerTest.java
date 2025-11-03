package com.Hamalog.service.medication;

import com.Hamalog.domain.events.medication.MedicationScheduleCreated;
import com.Hamalog.domain.events.medication.MedicationScheduleDeleted;
import com.Hamalog.domain.events.medication.MedicationScheduleUpdated;
import com.Hamalog.logging.StructuredLogger;
import com.Hamalog.logging.events.AuditEvent;
import com.Hamalog.logging.events.BusinessEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import com.Hamalog.domain.medication.AlarmType;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MedicationScheduleEventHandler 테스트")
class MedicationScheduleEventHandlerTest {

    @Mock
    private StructuredLogger structuredLogger;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Captor
    private ArgumentCaptor<AuditEvent> auditEventCaptor;

    @Captor
    private ArgumentCaptor<BusinessEvent> businessEventCaptor;

    private MedicationScheduleEventHandler eventHandler;

    @BeforeEach
    void setUp() {
        eventHandler = new MedicationScheduleEventHandler(structuredLogger, redisTemplate);
    }

    @Test
    @DisplayName("MedicationScheduleCreated 이벤트 처리 - 정상 케이스")
    void handleMedicationScheduleCreated_Success() {
        // Given
        Long scheduleId = 1L;
        Long memberId = 1L;
        MedicationScheduleCreated event = new MedicationScheduleCreated(
                scheduleId,
                memberId,
                "testUser",
                "Morning Medicine",
                "Test Hospital",
                LocalDate.now(),
                "Test memo",
                LocalDate.now(),
                30,
                3,
                AlarmType.SOUND);

        // When
        eventHandler.handleMedicationScheduleCreated(event);

        // Then
        verify(structuredLogger).audit(auditEventCaptor.capture());
        verify(structuredLogger).business(businessEventCaptor.capture());
        verify(redisTemplate).delete("medication_schedules:" + memberId);

        AuditEvent capturedAuditEvent = auditEventCaptor.getValue();
        assertThat(capturedAuditEvent.getUserId()).isEqualTo("testUser");
        assertThat(capturedAuditEvent.getOperation()).isEqualTo("MEDICATION_SCHEDULE_CREATED");
        assertThat(capturedAuditEvent.getEntityId()).isEqualTo(scheduleId.toString());

        BusinessEvent capturedBusinessEvent = businessEventCaptor.getValue();
        assertThat(capturedBusinessEvent.getUserId()).isEqualTo("testUser");
        assertThat(capturedBusinessEvent.getEventType()).isEqualTo("MEDICATION_SCHEDULE_CREATED");
        assertThat(capturedBusinessEvent.getMetadata())
                .containsEntry("hospitalName", "Test Hospital")
                .containsEntry("prescriptionDays", "30")
                .containsEntry("perDay", "3")
                .containsEntry("alarmType", "SOUND");
    }

    @Test
    @DisplayName("MedicationScheduleUpdated 이벤트 처리 - 정상 케이스")
    void handleMedicationScheduleUpdated_Success() {
        // Given
        Long scheduleId = 2L;
        Long memberId = 1L;
        MedicationScheduleUpdated event = new MedicationScheduleUpdated(
                scheduleId,
                memberId,
                "testUser",
                "Updated Medicine",
                "Test Hospital",
                LocalDate.now(),
                "Updated memo",
                LocalDate.now(),
                30,
                3,
                AlarmType.SOUND);

        // When
        eventHandler.handleMedicationScheduleUpdated(event);

        // Then
        verify(structuredLogger).audit(auditEventCaptor.capture());
        verify(structuredLogger).business(businessEventCaptor.capture());
        verify(redisTemplate).delete("medication_schedules:" + memberId);
        verify(redisTemplate).delete("medication_schedule:" + scheduleId);

        AuditEvent capturedAuditEvent = auditEventCaptor.getValue();
        assertThat(capturedAuditEvent.getUserId()).isEqualTo("testUser");
        assertThat(capturedAuditEvent.getOperation()).isEqualTo("MEDICATION_SCHEDULE_UPDATED");
        assertThat(capturedAuditEvent.getEntityId()).isEqualTo(scheduleId.toString());

        BusinessEvent capturedBusinessEvent = businessEventCaptor.getValue();
        assertThat(capturedBusinessEvent.getUserId()).isEqualTo("testUser");
        assertThat(capturedBusinessEvent.getEventType()).isEqualTo("MEDICATION_SCHEDULE_UPDATED");
        assertThat(capturedBusinessEvent.getMetadata())
                .containsEntry("scheduleId", scheduleId.toString())
                .containsEntry("scheduleName", "Updated Medicine");
    }

    @Test
    @DisplayName("MedicationScheduleDeleted 이벤트 처리 - 정상 케이스")
    void handleMedicationScheduleDeleted_Success() {
        // Given
        Long scheduleId = 3L;
        Long memberId = 1L;
        MedicationScheduleDeleted event = new MedicationScheduleDeleted(
                scheduleId,
                memberId,
                "testUser",
                "Deleted Medicine");

        // When
        eventHandler.handleMedicationScheduleDeleted(event);

        // Then
        verify(structuredLogger).audit(auditEventCaptor.capture());
        verify(structuredLogger).business(businessEventCaptor.capture());
        verify(redisTemplate).delete("medication_schedules:" + memberId);
        verify(redisTemplate).delete("medication_schedule:" + scheduleId);

        AuditEvent capturedAuditEvent = auditEventCaptor.getValue();
        assertThat(capturedAuditEvent.getUserId()).isEqualTo("testUser");
        assertThat(capturedAuditEvent.getOperation()).isEqualTo("MEDICATION_SCHEDULE_DELETED");
        assertThat(capturedAuditEvent.getEntityId()).isEqualTo(scheduleId.toString());

        BusinessEvent capturedBusinessEvent = businessEventCaptor.getValue();
        assertThat(capturedBusinessEvent.getUserId()).isEqualTo("testUser");
        assertThat(capturedBusinessEvent.getEventType()).isEqualTo("MEDICATION_SCHEDULE_DELETED");
        assertThat(capturedBusinessEvent.getMetadata())
                .containsEntry("scheduleId", scheduleId.toString())
                .containsEntry("scheduleName", "Deleted Medicine");
    }

    @Test
    @DisplayName("이벤트 처리 중 예외 발생 시 로깅")
    void handleEvents_ExceptionHandling() {
        // Given
        Long scheduleId = 4L;
        MedicationScheduleCreated event = new MedicationScheduleCreated(
                scheduleId,
                1L,
                "testUser",
                "Test Medicine",
                "Test Hospital",
                LocalDate.now(),
                "Test memo",
                LocalDate.now(),
                30,
                3,
                AlarmType.SOUND);

        doThrow(new RuntimeException("Test exception"))
                .when(structuredLogger)
                .audit(any(AuditEvent.class));

        // When
        eventHandler.handleMedicationScheduleCreated(event);

        // Then
        verify(structuredLogger).audit(any(AuditEvent.class));
        // Exception should be caught and logged, test passes if no exception is thrown
    }
}
