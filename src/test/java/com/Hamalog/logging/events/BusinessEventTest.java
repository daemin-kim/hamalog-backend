package com.Hamalog.logging.events;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BusinessEvent Tests")
class BusinessEventTest {

    @Test
    @DisplayName("빌더 패턴을 사용하여 BusinessEvent를 생성할 수 있어야 함")
    void builder_ShouldCreateBusinessEventWithAllFields() {
        // given
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("medicationName", "타이레놀");
        metadata.put("dosage", "500mg");
        metadata.put("frequency", 3);

        // when
        BusinessEvent event = BusinessEvent.builder()
                .eventType("MEDICATION_TAKEN")
                .entity("MedicationRecord")
                .action("복약 기록 생성")
                .userId("user123")
                .result("SUCCESS")
                .metadata(metadata)
                .build();

        // then
        assertThat(event.getEventType()).isEqualTo("MEDICATION_TAKEN");
        assertThat(event.getEntity()).isEqualTo("MedicationRecord");
        assertThat(event.getAction()).isEqualTo("복약 기록 생성");
        assertThat(event.getUserId()).isEqualTo("user123");
        assertThat(event.getResult()).isEqualTo("SUCCESS");
        assertThat(event.getMetadata()).containsExactlyInAnyOrderEntriesOf(metadata);
    }

    @Test
    @DisplayName("빈 BusinessEvent를 생성할 수 있어야 함")
    void builder_ShouldCreateEmptyBusinessEvent() {
        // given
        // when
        BusinessEvent event = BusinessEvent.builder().build();

        // then
        assertThat(event.getEventType()).isNull();
        assertThat(event.getEntity()).isNull();
        assertThat(event.getAction()).isNull();
        assertThat(event.getUserId()).isNull();
        assertThat(event.getResult()).isNull();
        assertThat(event.getMetadata()).isNull();
    }

    @Test
    @DisplayName("빌더 패턴은 체이닝을 지원해야 함")
    void builder_ShouldSupportMethodChaining() {
        // given
        // when
        BusinessEvent event = BusinessEvent.builder()
                .eventType("USER_REGISTRATION")
                .entity("Member")
                .result("SUCCESS")
                .build();

        // then
        assertThat(event.getEventType()).isEqualTo("USER_REGISTRATION");
        assertThat(event.getEntity()).isEqualTo("Member");
        assertThat(event.getResult()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("다양한 비즈니스 이벤트 타입을 처리할 수 있어야 함")
    void builder_ShouldHandleVariousEventTypes() {
        // given & when
        BusinessEvent medicationEvent = BusinessEvent.builder()
                .eventType("MEDICATION_SCHEDULED")
                .build();
        
        BusinessEvent sideEffectEvent = BusinessEvent.builder()
                .eventType("SIDE_EFFECT_REPORTED")
                .build();
        
        BusinessEvent reminderEvent = BusinessEvent.builder()
                .eventType("MEDICATION_REMINDER_SENT")
                .build();

        // then
        assertThat(medicationEvent.getEventType()).isEqualTo("MEDICATION_SCHEDULED");
        assertThat(sideEffectEvent.getEventType()).isEqualTo("SIDE_EFFECT_REPORTED");
        assertThat(reminderEvent.getEventType()).isEqualTo("MEDICATION_REMINDER_SENT");
    }

    @Test
    @DisplayName("다양한 결과 상태를 처리할 수 있어야 함")
    void builder_ShouldHandleVariousResults() {
        // given & when
        BusinessEvent successEvent = BusinessEvent.builder().result("SUCCESS").build();
        BusinessEvent failureEvent = BusinessEvent.builder().result("FAILURE").build();
        BusinessEvent partialEvent = BusinessEvent.builder().result("PARTIAL_SUCCESS").build();
        BusinessEvent errorEvent = BusinessEvent.builder().result("ERROR").build();

        // then
        assertThat(successEvent.getResult()).isEqualTo("SUCCESS");
        assertThat(failureEvent.getResult()).isEqualTo("FAILURE");
        assertThat(partialEvent.getResult()).isEqualTo("PARTIAL_SUCCESS");
        assertThat(errorEvent.getResult()).isEqualTo("ERROR");
    }

    @Test
    @DisplayName("메타데이터를 올바르게 처리해야 함")
    void builder_ShouldHandleMetadata() {
        // given
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("hospitalName", "서울대학교병원");
        metadata.put("doctorName", "김의사");
        metadata.put("prescriptionId", 12345);
        metadata.put("isEmergency", false);

        // when
        BusinessEvent event = BusinessEvent.builder()
                .metadata(metadata)
                .build();

        // then
        assertThat(event.getMetadata()).isNotNull();
        assertThat(event.getMetadata()).hasSize(4);
        assertThat(event.getMetadata().get("hospitalName")).isEqualTo("서울대학교병원");
        assertThat(event.getMetadata().get("doctorName")).isEqualTo("김의사");
        assertThat(event.getMetadata().get("prescriptionId")).isEqualTo(12345);
        assertThat(event.getMetadata().get("isEmergency")).isEqualTo(false);
    }

    @Test
    @DisplayName("null 메타데이터를 처리할 수 있어야 함")
    void builder_ShouldHandleNullMetadata() {
        // given
        // when
        BusinessEvent event = BusinessEvent.builder()
                .metadata(null)
                .userId(null)
                .build();

        // then
        assertThat(event.getMetadata()).isNull();
        assertThat(event.getUserId()).isNull();
    }

    @Test
    @DisplayName("한글 액션을 처리할 수 있어야 함")
    void builder_ShouldHandleKoreanAction() {
        // given
        String koreanAction = "사용자가 복약 알림을 확인하고 복약을 완료했습니다";

        // when
        BusinessEvent event = BusinessEvent.builder()
                .action(koreanAction)
                .build();

        // then
        assertThat(event.getAction()).isEqualTo(koreanAction);
    }

    @Test
    @DisplayName("복잡한 메타데이터 구조를 처리할 수 있어야 함")
    void builder_ShouldHandleComplexMetadata() {
        // given
        Map<String, Object> nestedMap = new HashMap<>();
        nestedMap.put("morningDose", "09:00");
        nestedMap.put("eveningDose", "21:00");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("schedule", nestedMap);
        metadata.put("totalDays", 14);
        metadata.put("notes", "식후 30분에 복용");

        // when
        BusinessEvent event = BusinessEvent.builder()
                .metadata(metadata)
                .build();

        // then
        assertThat(event.getMetadata()).containsKey("schedule");
        assertThat(event.getMetadata()).containsKey("totalDays");
        assertThat(event.getMetadata()).containsKey("notes");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> scheduleData = (Map<String, Object>) event.getMetadata().get("schedule");
        assertThat(scheduleData.get("morningDose")).isEqualTo("09:00");
        assertThat(scheduleData.get("eveningDose")).isEqualTo("21:00");
    }

    @Test
    @DisplayName("실제 비즈니스 시나리오를 처리할 수 있어야 함")
    void builder_ShouldHandleRealBusinessScenario() {
        // given
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("medicationId", "MED_001");
        metadata.put("scheduledTime", "2024-10-11T08:00:00");
        metadata.put("actualTime", "2024-10-11T08:15:00");
        metadata.put("delayMinutes", 15);
        metadata.put("remindersSent", 2);

        // when
        BusinessEvent event = BusinessEvent.builder()
                .eventType("MEDICATION_TAKEN_LATE")
                .entity("MedicationRecord")
                .action("지연된 복약 기록")
                .userId("patient_456@example.com")
                .result("SUCCESS_WITH_WARNING")
                .metadata(metadata)
                .build();

        // then
        assertThat(event.getEventType()).isEqualTo("MEDICATION_TAKEN_LATE");
        assertThat(event.getEntity()).isEqualTo("MedicationRecord");
        assertThat(event.getAction()).isEqualTo("지연된 복약 기록");
        assertThat(event.getUserId()).isEqualTo("patient_456@example.com");
        assertThat(event.getResult()).isEqualTo("SUCCESS_WITH_WARNING");
        assertThat(event.getMetadata().get("delayMinutes")).isEqualTo(15);
        assertThat(event.getMetadata().get("remindersSent")).isEqualTo(2);
    }

    @Test
    @DisplayName("다양한 엔티티를 처리할 수 있어야 함")
    void builder_ShouldHandleVariousEntities() {
        // given & when
        BusinessEvent memberEvent = BusinessEvent.builder().entity("Member").build();
        BusinessEvent scheduleEvent = BusinessEvent.builder().entity("MedicationSchedule").build();
        BusinessEvent recordEvent = BusinessEvent.builder().entity("MedicationRecord").build();
        BusinessEvent sideEffectEvent = BusinessEvent.builder().entity("SideEffectRecord").build();

        // then
        assertThat(memberEvent.getEntity()).isEqualTo("Member");
        assertThat(scheduleEvent.getEntity()).isEqualTo("MedicationSchedule");
        assertThat(recordEvent.getEntity()).isEqualTo("MedicationRecord");
        assertThat(sideEffectEvent.getEntity()).isEqualTo("SideEffectRecord");
    }
}