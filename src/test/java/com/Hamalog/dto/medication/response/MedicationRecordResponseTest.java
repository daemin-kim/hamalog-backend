package com.Hamalog.dto.medication.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.Hamalog.domain.medication.MedicationRecord;
import com.Hamalog.domain.medication.MedicationSchedule;
import com.Hamalog.domain.medication.MedicationTime;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("MedicationRecordResponse Tests")
class MedicationRecordResponseTest {

    @Mock
    private MedicationRecord medicationRecord;

    @Mock
    private MedicationSchedule medicationSchedule;

    @Mock
    private MedicationTime medicationTime;

    @Test
    @DisplayName("Should create MedicationRecordResponse with all valid fields")
    void constructor_ValidFields_CreatesSuccessfully() {
        // given
        Long medicationRecordId = 1L;
        Long medicationScheduleId = 2L;
        Long medicationTimeId = 3L;
        Boolean isTakeMedication = true;
        LocalDateTime realTakeTime = LocalDateTime.of(2023, 10, 15, 10, 30);

        // when
        MedicationRecordResponse response = new MedicationRecordResponse(
                medicationRecordId,
                medicationScheduleId,
                medicationTimeId,
                isTakeMedication,
                realTakeTime
        );

        // then
        assertThat(response).isNotNull();
        assertThat(response.medicationRecordId()).isEqualTo(medicationRecordId);
        assertThat(response.medicationScheduleId()).isEqualTo(medicationScheduleId);
        assertThat(response.medicationTimeId()).isEqualTo(medicationTimeId);
        assertThat(response.isTakeMedication()).isEqualTo(isTakeMedication);
        assertThat(response.realTakeTime()).isEqualTo(realTakeTime);
    }

    @Test
    @DisplayName("Should create MedicationRecordResponse with null values")
    void constructor_NullValues_CreatesSuccessfully() {
        // when
        MedicationRecordResponse response = new MedicationRecordResponse(
                null, null, null, null, null
        );

        // then
        assertThat(response).isNotNull();
        assertThat(response.medicationRecordId()).isNull();
        assertThat(response.medicationScheduleId()).isNull();
        assertThat(response.medicationTimeId()).isNull();
        assertThat(response.isTakeMedication()).isNull();
        assertThat(response.realTakeTime()).isNull();
    }

    @Test
    @DisplayName("Should create MedicationRecordResponse from MedicationRecord entity")
    void from_ValidMedicationRecord_CreatesResponseSuccessfully() {
        // given
        Long medicationRecordId = 1L;
        Long medicationScheduleId = 2L;
        Long medicationTimeId = 3L;
        Boolean isTakeMedication = true;
        LocalDateTime realTakeTime = LocalDateTime.of(2023, 10, 15, 10, 30);

        when(medicationRecord.getMedicationRecordId()).thenReturn(medicationRecordId);
        when(medicationRecord.getMedicationSchedule()).thenReturn(medicationSchedule);
        when(medicationSchedule.getMedicationScheduleId()).thenReturn(medicationScheduleId);
        when(medicationRecord.getMedicationTime()).thenReturn(medicationTime);
        when(medicationTime.getMedicationTimeId()).thenReturn(medicationTimeId);
        when(medicationRecord.getIsTakeMedication()).thenReturn(isTakeMedication);
        when(medicationRecord.getRealTakeTime()).thenReturn(realTakeTime);

        // when
        MedicationRecordResponse response = MedicationRecordResponse.from(medicationRecord);

        // then
        assertThat(response).isNotNull();
        assertThat(response.medicationRecordId()).isEqualTo(medicationRecordId);
        assertThat(response.medicationScheduleId()).isEqualTo(medicationScheduleId);
        assertThat(response.medicationTimeId()).isEqualTo(medicationTimeId);
        assertThat(response.isTakeMedication()).isEqualTo(isTakeMedication);
        assertThat(response.realTakeTime()).isEqualTo(realTakeTime);
    }

    @Test
    @DisplayName("Should handle MedicationRecord with false isTakeMedication")
    void from_MedicationRecordWithFalseFlag_CreatesResponseCorrectly() {
        // given
        when(medicationRecord.getMedicationRecordId()).thenReturn(1L);
        when(medicationRecord.getMedicationSchedule()).thenReturn(medicationSchedule);
        when(medicationSchedule.getMedicationScheduleId()).thenReturn(2L);
        when(medicationRecord.getMedicationTime()).thenReturn(medicationTime);
        when(medicationTime.getMedicationTimeId()).thenReturn(3L);
        when(medicationRecord.getIsTakeMedication()).thenReturn(false);
        when(medicationRecord.getRealTakeTime()).thenReturn(null);

        // when
        MedicationRecordResponse response = MedicationRecordResponse.from(medicationRecord);

        // then
        assertThat(response.isTakeMedication()).isFalse();
        assertThat(response.realTakeTime()).isNull();
    }

    @Test
    @DisplayName("Should handle null MedicationRecord")
    void from_NullMedicationRecord_ThrowsNullPointerException() {
        // when/then
        assertThatThrownBy(() -> MedicationRecordResponse.from(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should handle MedicationRecord with null schedule")
    void from_MedicationRecordWithNullSchedule_ThrowsNullPointerException() {
        // given
        when(medicationRecord.getMedicationRecordId()).thenReturn(1L);
        when(medicationRecord.getMedicationSchedule()).thenReturn(null);

        // when/then
        assertThatThrownBy(() -> MedicationRecordResponse.from(medicationRecord))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should handle MedicationRecord with null time")
    void from_MedicationRecordWithNullTime_ThrowsNullPointerException() {
        // given
        when(medicationRecord.getMedicationRecordId()).thenReturn(1L);
        when(medicationRecord.getMedicationSchedule()).thenReturn(medicationSchedule);
        when(medicationSchedule.getMedicationScheduleId()).thenReturn(2L);
        when(medicationRecord.getMedicationTime()).thenReturn(null);

        // when/then
        assertThatThrownBy(() -> MedicationRecordResponse.from(medicationRecord))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should handle equality correctly for responses with same values")
    void equals_SameValues_ReturnsTrue() {
        // given
        LocalDateTime realTakeTime = LocalDateTime.of(2023, 10, 15, 10, 30);

        MedicationRecordResponse response1 = new MedicationRecordResponse(
                1L, 2L, 3L, true, realTakeTime);
        MedicationRecordResponse response2 = new MedicationRecordResponse(
                1L, 2L, 3L, true, realTakeTime);

        // when/then
        assertThat(response1).isEqualTo(response2);
        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
    }

    @Test
    @DisplayName("Should handle inequality correctly for responses with different values")
    void equals_DifferentValues_ReturnsFalse() {
        // given
        LocalDateTime realTakeTime = LocalDateTime.of(2023, 10, 15, 10, 30);

        MedicationRecordResponse response1 = new MedicationRecordResponse(
                1L, 2L, 3L, true, realTakeTime);
        MedicationRecordResponse response2 = new MedicationRecordResponse(
                1L, 2L, 3L, false, realTakeTime);

        // when/then
        assertThat(response1).isNotEqualTo(response2);
    }

    @Test
    @DisplayName("Should provide meaningful toString representation")
    void toString_Always_ContainsAllFields() {
        // given
        LocalDateTime realTakeTime = LocalDateTime.of(2023, 10, 15, 10, 30);
        MedicationRecordResponse response = new MedicationRecordResponse(
                1L, 2L, 3L, true, realTakeTime);

        // when
        String toString = response.toString();

        // then
        assertThat(toString).contains("medicationRecordId=1");
        assertThat(toString).contains("medicationScheduleId=2");
        assertThat(toString).contains("medicationTimeId=3");
        assertThat(toString).contains("isTakeMedication=true");
        assertThat(toString).contains("realTakeTime=2023-10-15T10:30");
    }

    @Test
    @DisplayName("Should handle large ID values")
    void constructor_LargeIdValues_CreatesSuccessfully() {
        // given
        Long maxId = Long.MAX_VALUE;
        LocalDateTime realTakeTime = LocalDateTime.of(2023, 10, 15, 10, 30);

        // when
        MedicationRecordResponse response = new MedicationRecordResponse(
                maxId, maxId - 1, maxId - 2, true, realTakeTime);

        // then
        assertThat(response.medicationRecordId()).isEqualTo(maxId);
        assertThat(response.medicationScheduleId()).isEqualTo(maxId - 1);
        assertThat(response.medicationTimeId()).isEqualTo(maxId - 2);
    }

    @Test
    @DisplayName("Should handle edge case LocalDateTime values")
    void constructor_EdgeCaseDateTimes_CreatesSuccessfully() {
        // given
        LocalDateTime minDateTime = LocalDateTime.MIN;
        LocalDateTime maxDateTime = LocalDateTime.MAX;

        // when
        MedicationRecordResponse response1 = new MedicationRecordResponse(
                1L, 2L, 3L, true, minDateTime);
        MedicationRecordResponse response2 = new MedicationRecordResponse(
                1L, 2L, 3L, true, maxDateTime);

        // then
        assertThat(response1.realTakeTime()).isEqualTo(minDateTime);
        assertThat(response2.realTakeTime()).isEqualTo(maxDateTime);
    }

    @Test
    @DisplayName("Should handle from method with null real take time")
    void from_MedicationRecordWithNullRealTakeTime_CreatesResponseSuccessfully() {
        // given
        when(medicationRecord.getMedicationRecordId()).thenReturn(1L);
        when(medicationRecord.getMedicationSchedule()).thenReturn(medicationSchedule);
        when(medicationSchedule.getMedicationScheduleId()).thenReturn(2L);
        when(medicationRecord.getMedicationTime()).thenReturn(medicationTime);
        when(medicationTime.getMedicationTimeId()).thenReturn(3L);
        when(medicationRecord.getIsTakeMedication()).thenReturn(true);
        when(medicationRecord.getRealTakeTime()).thenReturn(null);

        // when
        MedicationRecordResponse response = MedicationRecordResponse.from(medicationRecord);

        // then
        assertThat(response.realTakeTime()).isNull();
        assertThat(response.isTakeMedication()).isTrue();
    }

    @Test
    @DisplayName("Should handle from method with null isTakeMedication")
    void from_MedicationRecordWithNullIsTakeMedication_CreatesResponseSuccessfully() {
        // given
        LocalDateTime realTakeTime = LocalDateTime.of(2023, 10, 15, 10, 30);

        when(medicationRecord.getMedicationRecordId()).thenReturn(1L);
        when(medicationRecord.getMedicationSchedule()).thenReturn(medicationSchedule);
        when(medicationSchedule.getMedicationScheduleId()).thenReturn(2L);
        when(medicationRecord.getMedicationTime()).thenReturn(medicationTime);
        when(medicationTime.getMedicationTimeId()).thenReturn(3L);
        when(medicationRecord.getIsTakeMedication()).thenReturn(null);
        when(medicationRecord.getRealTakeTime()).thenReturn(realTakeTime);

        // when
        MedicationRecordResponse response = MedicationRecordResponse.from(medicationRecord);

        // then
        assertThat(response.isTakeMedication()).isNull();
        assertThat(response.realTakeTime()).isEqualTo(realTakeTime);
    }

    @Test
    @DisplayName("Should maintain consistent behavior across multiple from calls")
    void from_MultipleCalls_ConsistentBehavior() {
        // given
        when(medicationRecord.getMedicationRecordId()).thenReturn(1L);
        when(medicationRecord.getMedicationSchedule()).thenReturn(medicationSchedule);
        when(medicationSchedule.getMedicationScheduleId()).thenReturn(2L);
        when(medicationRecord.getMedicationTime()).thenReturn(medicationTime);
        when(medicationTime.getMedicationTimeId()).thenReturn(3L);
        when(medicationRecord.getIsTakeMedication()).thenReturn(true);
        when(medicationRecord.getRealTakeTime()).thenReturn(LocalDateTime.of(2023, 10, 15, 10, 30));

        // when
        MedicationRecordResponse response1 = MedicationRecordResponse.from(medicationRecord);
        MedicationRecordResponse response2 = MedicationRecordResponse.from(medicationRecord);

        // then
        assertThat(response1).isEqualTo(response2);
        assertThat(response1.medicationRecordId()).isEqualTo(response2.medicationRecordId());
        assertThat(response1.medicationScheduleId()).isEqualTo(response2.medicationScheduleId());
        assertThat(response1.medicationTimeId()).isEqualTo(response2.medicationTimeId());
        assertThat(response1.isTakeMedication()).isEqualTo(response2.isTakeMedication());
        assertThat(response1.realTakeTime()).isEqualTo(response2.realTakeTime());
    }
}