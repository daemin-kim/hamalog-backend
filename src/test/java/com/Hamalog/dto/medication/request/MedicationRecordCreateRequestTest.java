package com.Hamalog.dto.medication.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MedicationRecordCreateRequest Tests")
class MedicationRecordCreateRequestTest {

    @Test
    @DisplayName("Should create MedicationRecordCreateRequest with all valid fields")
    void constructor_ValidFields_CreatesSuccessfully() {
        // given
        Long medicationScheduleId = 1L;
        Long medicationTimeId = 2L;
        Boolean isTakeMedication = true;
        LocalDateTime realTakeTime = LocalDateTime.of(2023, 10, 15, 10, 30);

        // when
        MedicationRecordCreateRequest request = new MedicationRecordCreateRequest(
                medicationScheduleId,
                medicationTimeId,
                isTakeMedication,
                realTakeTime
        );

        // then
        assertThat(request).isNotNull();
        assertThat(request.medicationScheduleId()).isEqualTo(medicationScheduleId);
        assertThat(request.medicationTimeId()).isEqualTo(medicationTimeId);
        assertThat(request.isTakeMedication()).isEqualTo(isTakeMedication);
        assertThat(request.realTakeTime()).isEqualTo(realTakeTime);
    }

    @Test
    @DisplayName("Should create MedicationRecordCreateRequest with null values")
    void constructor_NullValues_CreatesSuccessfully() {
        // when
        MedicationRecordCreateRequest request = new MedicationRecordCreateRequest(
                null,
                null,
                null,
                null
        );

        // then
        assertThat(request).isNotNull();
        assertThat(request.medicationScheduleId()).isNull();
        assertThat(request.medicationTimeId()).isNull();
        assertThat(request.isTakeMedication()).isNull();
        assertThat(request.realTakeTime()).isNull();
    }

    @Test
    @DisplayName("Should create MedicationRecordCreateRequest with false isTakeMedication")
    void constructor_FalseIsTakeMedication_CreatesSuccessfully() {
        // given
        Long medicationScheduleId = 1L;
        Long medicationTimeId = 2L;
        Boolean isTakeMedication = false;
        LocalDateTime realTakeTime = null; // null when not taken

        // when
        MedicationRecordCreateRequest request = new MedicationRecordCreateRequest(
                medicationScheduleId,
                medicationTimeId,
                isTakeMedication,
                realTakeTime
        );

        // then
        assertThat(request.medicationScheduleId()).isEqualTo(medicationScheduleId);
        assertThat(request.medicationTimeId()).isEqualTo(medicationTimeId);
        assertThat(request.isTakeMedication()).isFalse();
        assertThat(request.realTakeTime()).isNull();
    }

    @Test
    @DisplayName("Should handle equality correctly for records with same values")
    void equals_SameValues_ReturnsTrue() {
        // given
        Long medicationScheduleId = 1L;
        Long medicationTimeId = 2L;
        Boolean isTakeMedication = true;
        LocalDateTime realTakeTime = LocalDateTime.of(2023, 10, 15, 10, 30);

        MedicationRecordCreateRequest request1 = new MedicationRecordCreateRequest(
                medicationScheduleId, medicationTimeId, isTakeMedication, realTakeTime);
        MedicationRecordCreateRequest request2 = new MedicationRecordCreateRequest(
                medicationScheduleId, medicationTimeId, isTakeMedication, realTakeTime);

        // when/then
        assertThat(request1).isEqualTo(request2);
        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
    }

    @Test
    @DisplayName("Should handle inequality correctly for records with different values")
    void equals_DifferentValues_ReturnsFalse() {
        // given
        LocalDateTime realTakeTime = LocalDateTime.of(2023, 10, 15, 10, 30);

        MedicationRecordCreateRequest request1 = new MedicationRecordCreateRequest(
                1L, 2L, true, realTakeTime);
        MedicationRecordCreateRequest request2 = new MedicationRecordCreateRequest(
                1L, 2L, false, realTakeTime);

        // when/then
        assertThat(request1).isNotEqualTo(request2);
    }

    @Test
    @DisplayName("Should provide meaningful toString representation")
    void toString_Always_ContainsAllFields() {
        // given
        Long medicationScheduleId = 1L;
        Long medicationTimeId = 2L;
        Boolean isTakeMedication = true;
        LocalDateTime realTakeTime = LocalDateTime.of(2023, 10, 15, 10, 30);

        MedicationRecordCreateRequest request = new MedicationRecordCreateRequest(
                medicationScheduleId, medicationTimeId, isTakeMedication, realTakeTime);

        // when
        String toString = request.toString();

        // then
        assertThat(toString).contains("medicationScheduleId=1");
        assertThat(toString).contains("medicationTimeId=2");
        assertThat(toString).contains("isTakeMedication=true");
        assertThat(toString).contains("realTakeTime=2023-10-15T10:30");
    }

    @Test
    @DisplayName("Should handle large ID values")
    void constructor_LargeIdValues_CreatesSuccessfully() {
        // given
        Long medicationScheduleId = Long.MAX_VALUE;
        Long medicationTimeId = Long.MAX_VALUE - 1;
        Boolean isTakeMedication = true;
        LocalDateTime realTakeTime = LocalDateTime.of(2023, 10, 15, 10, 30);

        // when
        MedicationRecordCreateRequest request = new MedicationRecordCreateRequest(
                medicationScheduleId, medicationTimeId, isTakeMedication, realTakeTime);

        // then
        assertThat(request.medicationScheduleId()).isEqualTo(Long.MAX_VALUE);
        assertThat(request.medicationTimeId()).isEqualTo(Long.MAX_VALUE - 1);
    }

    @Test
    @DisplayName("Should handle edge case LocalDateTime values")
    void constructor_EdgeCaseDateTimes_CreatesSuccessfully() {
        // given
        LocalDateTime minDateTime = LocalDateTime.MIN;
        LocalDateTime maxDateTime = LocalDateTime.MAX;

        // when
        MedicationRecordCreateRequest request1 = new MedicationRecordCreateRequest(
                1L, 2L, true, minDateTime);
        MedicationRecordCreateRequest request2 = new MedicationRecordCreateRequest(
                1L, 2L, true, maxDateTime);

        // then
        assertThat(request1.realTakeTime()).isEqualTo(minDateTime);
        assertThat(request2.realTakeTime()).isEqualTo(maxDateTime);
    }

    @Test
    @DisplayName("Should handle current time accurately")
    void constructor_CurrentTime_CreatesSuccessfully() {
        // given
        LocalDateTime now = LocalDateTime.now();
        
        // when
        MedicationRecordCreateRequest request = new MedicationRecordCreateRequest(
                1L, 2L, true, now);

        // then
        assertThat(request.realTakeTime()).isEqualTo(now);
    }
}