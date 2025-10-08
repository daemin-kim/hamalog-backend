package com.Hamalog.dto.medication.request;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MedicationRecordUpdateRequest Tests")
class MedicationRecordUpdateRequestTest {

    @Test
    @DisplayName("Should create MedicationRecordUpdateRequest with valid fields")
    void constructor_ValidFields_CreatesSuccessfully() {
        // given
        Boolean isTakeMedication = true;
        LocalDateTime realTakeTime = LocalDateTime.of(2023, 10, 15, 10, 30);

        // when
        MedicationRecordUpdateRequest request = new MedicationRecordUpdateRequest(
                isTakeMedication,
                realTakeTime
        );

        // then
        assertThat(request).isNotNull();
        assertThat(request.isTakeMedication()).isEqualTo(isTakeMedication);
        assertThat(request.realTakeTime()).isEqualTo(realTakeTime);
    }

    @Test
    @DisplayName("Should create MedicationRecordUpdateRequest with null values")
    void constructor_NullValues_CreatesSuccessfully() {
        // when
        MedicationRecordUpdateRequest request = new MedicationRecordUpdateRequest(
                null,
                null
        );

        // then
        assertThat(request).isNotNull();
        assertThat(request.isTakeMedication()).isNull();
        assertThat(request.realTakeTime()).isNull();
    }

    @Test
    @DisplayName("Should create MedicationRecordUpdateRequest with false isTakeMedication")
    void constructor_FalseIsTakeMedication_CreatesSuccessfully() {
        // given
        Boolean isTakeMedication = false;
        LocalDateTime realTakeTime = null; // null when not taken

        // when
        MedicationRecordUpdateRequest request = new MedicationRecordUpdateRequest(
                isTakeMedication,
                realTakeTime
        );

        // then
        assertThat(request.isTakeMedication()).isFalse();
        assertThat(request.realTakeTime()).isNull();
    }

    @Test
    @DisplayName("Should create MedicationRecordUpdateRequest with medication taken but no time")
    void constructor_TakenWithoutTime_CreatesSuccessfully() {
        // given
        Boolean isTakeMedication = true;
        LocalDateTime realTakeTime = null;

        // when
        MedicationRecordUpdateRequest request = new MedicationRecordUpdateRequest(
                isTakeMedication,
                realTakeTime
        );

        // then
        assertThat(request.isTakeMedication()).isTrue();
        assertThat(request.realTakeTime()).isNull();
    }

    @Test
    @DisplayName("Should handle equality correctly for records with same values")
    void equals_SameValues_ReturnsTrue() {
        // given
        Boolean isTakeMedication = true;
        LocalDateTime realTakeTime = LocalDateTime.of(2023, 10, 15, 10, 30);

        MedicationRecordUpdateRequest request1 = new MedicationRecordUpdateRequest(
                isTakeMedication, realTakeTime);
        MedicationRecordUpdateRequest request2 = new MedicationRecordUpdateRequest(
                isTakeMedication, realTakeTime);

        // when/then
        assertThat(request1).isEqualTo(request2);
        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
    }

    @Test
    @DisplayName("Should handle inequality correctly for records with different values")
    void equals_DifferentValues_ReturnsFalse() {
        // given
        LocalDateTime realTakeTime = LocalDateTime.of(2023, 10, 15, 10, 30);

        MedicationRecordUpdateRequest request1 = new MedicationRecordUpdateRequest(
                true, realTakeTime);
        MedicationRecordUpdateRequest request2 = new MedicationRecordUpdateRequest(
                false, realTakeTime);

        // when/then
        assertThat(request1).isNotEqualTo(request2);
    }

    @Test
    @DisplayName("Should handle inequality for different time values")
    void equals_DifferentTimes_ReturnsFalse() {
        // given
        LocalDateTime time1 = LocalDateTime.of(2023, 10, 15, 10, 30);
        LocalDateTime time2 = LocalDateTime.of(2023, 10, 15, 11, 30);

        MedicationRecordUpdateRequest request1 = new MedicationRecordUpdateRequest(
                true, time1);
        MedicationRecordUpdateRequest request2 = new MedicationRecordUpdateRequest(
                true, time2);

        // when/then
        assertThat(request1).isNotEqualTo(request2);
    }

    @Test
    @DisplayName("Should provide meaningful toString representation")
    void toString_Always_ContainsAllFields() {
        // given
        Boolean isTakeMedication = true;
        LocalDateTime realTakeTime = LocalDateTime.of(2023, 10, 15, 10, 30);

        MedicationRecordUpdateRequest request = new MedicationRecordUpdateRequest(
                isTakeMedication, realTakeTime);

        // when
        String toString = request.toString();

        // then
        assertThat(toString).contains("isTakeMedication=true");
        assertThat(toString).contains("realTakeTime=2023-10-15T10:30");
    }

    @Test
    @DisplayName("Should handle edge case LocalDateTime values")
    void constructor_EdgeCaseDateTimes_CreatesSuccessfully() {
        // given
        LocalDateTime minDateTime = LocalDateTime.MIN;
        LocalDateTime maxDateTime = LocalDateTime.MAX;

        // when
        MedicationRecordUpdateRequest request1 = new MedicationRecordUpdateRequest(
                true, minDateTime);
        MedicationRecordUpdateRequest request2 = new MedicationRecordUpdateRequest(
                true, maxDateTime);

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
        MedicationRecordUpdateRequest request = new MedicationRecordUpdateRequest(
                true, now);

        // then
        assertThat(request.realTakeTime()).isEqualTo(now);
    }

    @Test
    @DisplayName("Should handle all null values in toString")
    void toString_NullValues_HandlesGracefully() {
        // given
        MedicationRecordUpdateRequest request = new MedicationRecordUpdateRequest(
                null, null);

        // when
        String toString = request.toString();

        // then
        assertThat(toString).contains("isTakeMedication=null");
        assertThat(toString).contains("realTakeTime=null");
    }

    @Test
    @DisplayName("Should handle mixed null values")
    void constructor_MixedNullValues_CreatesSuccessfully() {
        // given/when
        MedicationRecordUpdateRequest request1 = new MedicationRecordUpdateRequest(
                true, null);
        MedicationRecordUpdateRequest request2 = new MedicationRecordUpdateRequest(
                null, LocalDateTime.now());

        // then
        assertThat(request1.isTakeMedication()).isTrue();
        assertThat(request1.realTakeTime()).isNull();
        
        assertThat(request2.isTakeMedication()).isNull();
        assertThat(request2.realTakeTime()).isNotNull();
    }

    @Test
    @DisplayName("Should handle equality with null values")
    void equals_NullValues_HandlesCorrectly() {
        // given
        MedicationRecordUpdateRequest request1 = new MedicationRecordUpdateRequest(
                null, null);
        MedicationRecordUpdateRequest request2 = new MedicationRecordUpdateRequest(
                null, null);
        MedicationRecordUpdateRequest request3 = new MedicationRecordUpdateRequest(
                true, null);

        // when/then
        assertThat(request1).isEqualTo(request2);
        assertThat(request1).isNotEqualTo(request3);
        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
    }
}