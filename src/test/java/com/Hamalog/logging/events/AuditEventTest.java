package com.Hamalog.logging.events;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AuditEvent Tests")
class AuditEventTest {

    @Test
    @DisplayName("빌더 패턴을 사용하여 AuditEvent를 생성할 수 있어야 함")
    void builder_ShouldCreateAuditEventWithAllFields() {
        // given
        // when
        AuditEvent event = AuditEvent.builder()
                .operation("CREATE")
                .entityType("MedicationSchedule")
                .entityId("12345")
                .userId("user123")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .status("SUCCESS")
                .details("복약 스케줄이 성공적으로 생성되었습니다.")
                .build();

        // then
        assertThat(event.getOperation()).isEqualTo("CREATE");
        assertThat(event.getEntityType()).isEqualTo("MedicationSchedule");
        assertThat(event.getEntityId()).isEqualTo("12345");
        assertThat(event.getUserId()).isEqualTo("user123");
        assertThat(event.getIpAddress()).isEqualTo("192.168.1.1");
        assertThat(event.getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(event.getStatus()).isEqualTo("SUCCESS");
        assertThat(event.getDetails()).isEqualTo("복약 스케줄이 성공적으로 생성되었습니다.");
    }

    @Test
    @DisplayName("빈 AuditEvent를 생성할 수 있어야 함")
    void builder_ShouldCreateEmptyAuditEvent() {
        // given
        // when
        AuditEvent event = AuditEvent.builder().build();

        // then
        assertThat(event.getOperation()).isNull();
        assertThat(event.getEntityType()).isNull();
        assertThat(event.getEntityId()).isNull();
        assertThat(event.getUserId()).isNull();
        assertThat(event.getIpAddress()).isNull();
        assertThat(event.getUserAgent()).isNull();
        assertThat(event.getStatus()).isNull();
        assertThat(event.getDetails()).isNull();
    }

    @Test
    @DisplayName("빌더 패턴은 체이닝을 지원해야 함")
    void builder_ShouldSupportMethodChaining() {
        // given
        // when
        AuditEvent event = AuditEvent.builder()
                .operation("UPDATE")
                .entityType("Member")
                .status("SUCCESS")
                .build();

        // then
        assertThat(event.getOperation()).isEqualTo("UPDATE");
        assertThat(event.getEntityType()).isEqualTo("Member");
        assertThat(event.getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("CRUD 연산을 올바르게 처리해야 함")
    void builder_ShouldHandleCRUDOperations() {
        // given & when
        AuditEvent createEvent = AuditEvent.builder().operation("CREATE").build();
        AuditEvent readEvent = AuditEvent.builder().operation("READ").build();
        AuditEvent updateEvent = AuditEvent.builder().operation("UPDATE").build();
        AuditEvent deleteEvent = AuditEvent.builder().operation("DELETE").build();

        // then
        assertThat(createEvent.getOperation()).isEqualTo("CREATE");
        assertThat(readEvent.getOperation()).isEqualTo("READ");
        assertThat(updateEvent.getOperation()).isEqualTo("UPDATE");
        assertThat(deleteEvent.getOperation()).isEqualTo("DELETE");
    }

    @Test
    @DisplayName("다양한 엔티티 타입을 처리할 수 있어야 함")
    void builder_ShouldHandleVariousEntityTypes() {
        // given & when
        AuditEvent memberEvent = AuditEvent.builder()
                .entityType("Member")
                .build();
        
        AuditEvent medicationEvent = AuditEvent.builder()
                .entityType("MedicationSchedule")
                .build();
        
        AuditEvent recordEvent = AuditEvent.builder()
                .entityType("MedicationRecord")
                .build();
        
        AuditEvent sideEffectEvent = AuditEvent.builder()
                .entityType("SideEffect")
                .build();

        // then
        assertThat(memberEvent.getEntityType()).isEqualTo("Member");
        assertThat(medicationEvent.getEntityType()).isEqualTo("MedicationSchedule");
        assertThat(recordEvent.getEntityType()).isEqualTo("MedicationRecord");
        assertThat(sideEffectEvent.getEntityType()).isEqualTo("SideEffect");
    }

    @Test
    @DisplayName("다양한 상태를 처리할 수 있어야 함")
    void builder_ShouldHandleVariousStatuses() {
        // given & when
        AuditEvent successEvent = AuditEvent.builder().status("SUCCESS").build();
        AuditEvent failureEvent = AuditEvent.builder().status("FAILURE").build();
        AuditEvent errorEvent = AuditEvent.builder().status("ERROR").build();

        // then
        assertThat(successEvent.getStatus()).isEqualTo("SUCCESS");
        assertThat(failureEvent.getStatus()).isEqualTo("FAILURE");
        assertThat(errorEvent.getStatus()).isEqualTo("ERROR");
    }

    @Test
    @DisplayName("상세 정보를 한글로 처리할 수 있어야 함")
    void builder_ShouldHandleKoreanDetails() {
        // given
        String koreanDetails = "사용자가 복약 기록을 성공적으로 업데이트했습니다. 이전 값: 복용함, 새 값: 미복용";

        // when
        AuditEvent event = AuditEvent.builder()
                .details(koreanDetails)
                .build();

        // then
        assertThat(event.getDetails()).isEqualTo(koreanDetails);
    }

    @Test
    @DisplayName("null 값을 처리할 수 있어야 함")
    void builder_ShouldHandleNullValues() {
        // given
        // when
        AuditEvent event = AuditEvent.builder()
                .operation("DELETE")
                .entityId(null)
                .userId(null)
                .details(null)
                .build();

        // then
        assertThat(event.getOperation()).isEqualTo("DELETE");
        assertThat(event.getEntityId()).isNull();
        assertThat(event.getUserId()).isNull();
        assertThat(event.getDetails()).isNull();
    }

    @Test
    @DisplayName("긴 상세 정보를 처리할 수 있어야 함")
    void builder_ShouldHandleLongDetails() {
        // given
        String longDetails = "이것은 매우 긴 상세 정보입니다. ".repeat(100);

        // when
        AuditEvent event = AuditEvent.builder()
                .details(longDetails)
                .build();

        // then
        assertThat(event.getDetails()).isEqualTo(longDetails);
        assertThat(event.getDetails().length()).isGreaterThan(1000);
    }

    @Test
    @DisplayName("특수 문자를 포함한 엔티티 ID를 처리할 수 있어야 함")
    void builder_ShouldHandleSpecialCharactersInEntityId() {
        // given
        String specialEntityId = "entity-123_test@domain.com";

        // when
        AuditEvent event = AuditEvent.builder()
                .entityId(specialEntityId)
                .build();

        // then
        assertThat(event.getEntityId()).isEqualTo(specialEntityId);
    }

    @Test
    @DisplayName("실제 감사 로그 시나리오를 처리할 수 있어야 함")
    void builder_ShouldHandleRealAuditScenario() {
        // given
        // when
        AuditEvent event = AuditEvent.builder()
                .operation("UPDATE")
                .entityType("MedicationSchedule")
                .entityId("medication_schedule_456")
                .userId("user789@example.com")
                .ipAddress("203.0.113.42")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .status("SUCCESS")
                .details("복약 스케줄의 알람 시간이 09:00에서 10:30으로 변경되었습니다.")
                .build();

        // then
        assertThat(event.getOperation()).isEqualTo("UPDATE");
        assertThat(event.getEntityType()).isEqualTo("MedicationSchedule");
        assertThat(event.getEntityId()).isEqualTo("medication_schedule_456");
        assertThat(event.getUserId()).isEqualTo("user789@example.com");
        assertThat(event.getIpAddress()).isEqualTo("203.0.113.42");
        assertThat(event.getUserAgent()).contains("Mozilla/5.0");
        assertThat(event.getStatus()).isEqualTo("SUCCESS");
        assertThat(event.getDetails()).contains("복약 스케줄");
        assertThat(event.getDetails()).contains("09:00");
        assertThat(event.getDetails()).contains("10:30");
    }
}