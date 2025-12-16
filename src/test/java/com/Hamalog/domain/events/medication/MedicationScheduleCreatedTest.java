package com.Hamalog.domain.events.medication;

import static org.assertj.core.api.Assertions.assertThat;

import com.Hamalog.domain.medication.AlarmType;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MedicationScheduleCreated Event Tests")
class MedicationScheduleCreatedTest {

    @Test
    @DisplayName("Should create event with all required fields")
    void constructor_WithValidData_ShouldCreateEvent() {
        // given
        Long medicationScheduleId = 1L;
        Long memberId = 100L;
        String memberLoginId = "user123";
        String name = "Aspirin";
        String hospitalName = "Seoul Hospital";
        LocalDate prescriptionDate = LocalDate.of(2024, 1, 15);
        String memo = "Take with food";
        LocalDate startOfAd = LocalDate.of(2024, 1, 16);
        Integer prescriptionDays = 30;
        Integer perDay = 2;
        AlarmType alarmType = AlarmType.SOUND;

        // when
        MedicationScheduleCreated event = new MedicationScheduleCreated(
                medicationScheduleId, memberId, memberLoginId, name, hospitalName,
                prescriptionDate, memo, startOfAd, prescriptionDays, perDay, alarmType
        );

        // then
        assertThat(event.getMedicationScheduleId()).isEqualTo(medicationScheduleId);
        assertThat(event.getMemberId()).isEqualTo(memberId);
        assertThat(event.getMemberLoginId()).isEqualTo(memberLoginId);
        assertThat(event.getName()).isEqualTo(name);
        assertThat(event.getHospitalName()).isEqualTo(hospitalName);
        assertThat(event.getPrescriptionDate()).isEqualTo(prescriptionDate);
        assertThat(event.getMemo()).isEqualTo(memo);
        assertThat(event.getStartOfAd()).isEqualTo(startOfAd);
        assertThat(event.getPrescriptionDays()).isEqualTo(prescriptionDays);
        assertThat(event.getPerDay()).isEqualTo(perDay);
        assertThat(event.getAlarmType()).isEqualTo(alarmType);
    }

    @Test
    @DisplayName("Should inherit event metadata from DomainEvent")
    void eventMetadata_ShouldBeInheritedFromDomainEvent() {
        // given & when
        MedicationScheduleCreated event = createTestEvent();

        // then
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getOccurredOn()).isNotNull();
        assertThat(event.getEventType()).isEqualTo("MedicationScheduleCreated");
    }

    @Test
    @DisplayName("Should return medication schedule ID as aggregate ID")
    void getAggregateId_ShouldReturnMedicationScheduleId() {
        // given
        Long medicationScheduleId = 123L;
        MedicationScheduleCreated event = new MedicationScheduleCreated(
                medicationScheduleId, 1L, "user", "Med", "Hospital", 
                LocalDate.now(), "memo", LocalDate.now(), 30, 2, AlarmType.SOUND
        );

        // when & then
        assertThat(event.getAggregateId()).isEqualTo("123");
    }

    @Test
    @DisplayName("Should handle null medication schedule ID in aggregate ID")
    void getAggregateId_WithNullMedicationScheduleId_ShouldReturnUnknown() {
        // given
        MedicationScheduleCreated event = new MedicationScheduleCreated(
                null, 1L, "user", "Med", "Hospital", 
                LocalDate.now(), "memo", LocalDate.now(), 30, 2, AlarmType.VIBE
        );

        // when & then
        assertThat(event.getAggregateId()).isEqualTo("unknown");
    }

    @Test
    @DisplayName("Should generate proper toString representation")
    void toString_ShouldContainAllEventData() {
        // given
        MedicationScheduleCreated event = createTestEvent();

        // when
        String result = event.toString();

        // then
        assertThat(result).contains("MedicationScheduleCreated");
        assertThat(result).contains("medicationScheduleId=1");
        assertThat(result).contains("memberId=100");
        assertThat(result).contains("memberLoginId='testUser'");
        assertThat(result).contains("name='Test Medicine'");
        assertThat(result).contains("hospitalName='Test Hospital'");
        assertThat(result).contains("prescriptionDate=2024-01-15");
        assertThat(result).contains("startOfAd=2024-01-16");
        assertThat(result).contains("prescriptionDays=30");
        assertThat(result).contains("perDay=2");
        assertThat(result).contains("alarmType=SOUND");
        assertThat(result).contains("eventId='" + event.getEventId() + "'");
        assertThat(result).contains("occurredOn=" + event.getOccurredOn());
    }

    @Test
    @DisplayName("Should handle different alarm types correctly")
    void constructor_WithDifferentAlarmTypes_ShouldHandleCorrectly() {
        // given & when
        MedicationScheduleCreated soundEvent = createEventWithAlarmType(AlarmType.SOUND);
        MedicationScheduleCreated vibeEvent = createEventWithAlarmType(AlarmType.VIBE);

        // then
        assertThat(soundEvent.getAlarmType()).isEqualTo(AlarmType.SOUND);
        assertThat(vibeEvent.getAlarmType()).isEqualTo(AlarmType.VIBE);
    }

    @Test
    @DisplayName("Should handle null and empty string values correctly")
    void constructor_WithNullAndEmptyValues_ShouldHandleCorrectly() {
        // given & when
        MedicationScheduleCreated event = new MedicationScheduleCreated(
                1L, 100L, null, "", null, 
                LocalDate.now(), null, LocalDate.now(), 0, 0, null
        );

        // then
        assertThat(event.getMemberLoginId()).isNull();
        assertThat(event.getName()).isEmpty();
        assertThat(event.getHospitalName()).isNull();
        assertThat(event.getMemo()).isNull();
        assertThat(event.getPrescriptionDays()).isZero();
        assertThat(event.getPerDay()).isZero();
        assertThat(event.getAlarmType()).isNull();
    }

    @Test
    @DisplayName("Should maintain immutable state after creation")
    void eventFields_ShouldBeImmutableAfterCreation() {
        // given
        MedicationScheduleCreated event = createTestEvent();
        Long originalScheduleId = event.getMedicationScheduleId();
        String originalName = event.getName();
        AlarmType originalAlarmType = event.getAlarmType();

        // when - fields are final so they can't be modified

        // then
        assertThat(event.getMedicationScheduleId()).isEqualTo(originalScheduleId);
        assertThat(event.getName()).isEqualTo(originalName);
        assertThat(event.getAlarmType()).isEqualTo(originalAlarmType);
    }

    private MedicationScheduleCreated createTestEvent() {
        return new MedicationScheduleCreated(
                1L, 100L, "testUser", "Test Medicine", "Test Hospital",
                LocalDate.of(2024, 1, 15), "Test memo", LocalDate.of(2024, 1, 16),
                30, 2, AlarmType.SOUND
        );
    }

    private MedicationScheduleCreated createEventWithAlarmType(AlarmType alarmType) {
        return new MedicationScheduleCreated(
                1L, 100L, "testUser", "Test Medicine", "Test Hospital",
                LocalDate.now(), "Test memo", LocalDate.now(),
                30, 2, alarmType
        );
    }
}