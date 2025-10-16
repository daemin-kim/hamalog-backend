package com.Hamalog.dto.medication.response;

import com.Hamalog.domain.medication.AlarmType;
import com.Hamalog.domain.medication.MedicationSchedule;
import com.Hamalog.domain.member.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MedicationScheduleResponse Tests")
class MedicationScheduleResponseTest {

    @Mock
    private MedicationSchedule mockMedicationSchedule;

    @Mock
    private Member mockMember;

    @Test
    @DisplayName("Should create valid MedicationScheduleResponse with constructor")
    void constructor_WithValidData_ShouldCreateValidResponse() {
        // given
        Long medicationScheduleId = 1L;
        Long memberId = 2L;
        String name = "Test Medication";
        String hospitalName = "Test Hospital";
        LocalDate prescriptionDate = LocalDate.of(2023, 10, 15);
        String memo = "Test memo";
        LocalDate startOfAd = LocalDate.of(2023, 10, 16);
        Integer prescriptionDays = 7;
        Integer perDay = 3;
        AlarmType alarmType = AlarmType.SOUND;

        // when
        MedicationScheduleResponse response = new MedicationScheduleResponse(
                medicationScheduleId, memberId, name, hospitalName, prescriptionDate,
                memo, startOfAd, prescriptionDays, perDay, alarmType
        );

        // then
        assertThat(response.medicationScheduleId()).isEqualTo(medicationScheduleId);
        assertThat(response.memberId()).isEqualTo(memberId);
        assertThat(response.name()).isEqualTo(name);
        assertThat(response.hospitalName()).isEqualTo(hospitalName);
        assertThat(response.prescriptionDate()).isEqualTo(prescriptionDate);
        assertThat(response.memo()).isEqualTo(memo);
        assertThat(response.startOfAd()).isEqualTo(startOfAd);
        assertThat(response.prescriptionDays()).isEqualTo(prescriptionDays);
        assertThat(response.perDay()).isEqualTo(perDay);
        assertThat(response.alarmType()).isEqualTo(alarmType);
    }

    @Test
    @DisplayName("Should create MedicationScheduleResponse with null fields")
    void constructor_WithNullFields_ShouldCreateResponse() {
        // given
        Long medicationScheduleId = null;
        Long memberId = null;
        String name = null;
        String hospitalName = null;
        LocalDate prescriptionDate = null;
        String memo = null;
        LocalDate startOfAd = null;
        Integer prescriptionDays = null;
        Integer perDay = null;
        AlarmType alarmType = null;

        // when
        MedicationScheduleResponse response = new MedicationScheduleResponse(
                medicationScheduleId, memberId, name, hospitalName, prescriptionDate,
                memo, startOfAd, prescriptionDays, perDay, alarmType
        );

        // then
        assertThat(response.medicationScheduleId()).isNull();
        assertThat(response.memberId()).isNull();
        assertThat(response.name()).isNull();
        assertThat(response.hospitalName()).isNull();
        assertThat(response.prescriptionDate()).isNull();
        assertThat(response.memo()).isNull();
        assertThat(response.startOfAd()).isNull();
        assertThat(response.prescriptionDays()).isNull();
        assertThat(response.perDay()).isNull();
        assertThat(response.alarmType()).isNull();
    }

    @Test
    @DisplayName("Should create valid response from MedicationSchedule using from() method")
    void from_WithValidMedicationSchedule_ShouldCreateValidResponse() {
        // given
        Long medicationScheduleId = 1L;
        Long memberId = 2L;
        String name = "Test Medication";
        String hospitalName = "Test Hospital";
        LocalDate prescriptionDate = LocalDate.of(2023, 10, 15);
        String memo = "Test memo";
        LocalDate startOfAd = LocalDate.of(2023, 10, 16);
        Integer prescriptionDays = 7;
        Integer perDay = 3;
        AlarmType alarmType = AlarmType.SOUND;

        when(mockMedicationSchedule.getMedicationScheduleId()).thenReturn(medicationScheduleId);
        when(mockMedicationSchedule.getMember()).thenReturn(mockMember);
        when(mockMember.getMemberId()).thenReturn(memberId);
        when(mockMedicationSchedule.getName()).thenReturn(name);
        when(mockMedicationSchedule.getHospitalName()).thenReturn(hospitalName);
        when(mockMedicationSchedule.getPrescriptionDate()).thenReturn(prescriptionDate);
        when(mockMedicationSchedule.getMemo()).thenReturn(memo);
        when(mockMedicationSchedule.getStartOfAd()).thenReturn(startOfAd);
        when(mockMedicationSchedule.getPrescriptionDays()).thenReturn(prescriptionDays);
        when(mockMedicationSchedule.getPerDay()).thenReturn(perDay);
        when(mockMedicationSchedule.getAlarmType()).thenReturn(alarmType);

        // when
        MedicationScheduleResponse response = MedicationScheduleResponse.from(mockMedicationSchedule);

        // then
        assertThat(response.medicationScheduleId()).isEqualTo(medicationScheduleId);
        assertThat(response.memberId()).isEqualTo(memberId);
        assertThat(response.name()).isEqualTo(name);
        assertThat(response.hospitalName()).isEqualTo(hospitalName);
        assertThat(response.prescriptionDate()).isEqualTo(prescriptionDate);
        assertThat(response.memo()).isEqualTo(memo);
        assertThat(response.startOfAd()).isEqualTo(startOfAd);
        assertThat(response.prescriptionDays()).isEqualTo(prescriptionDays);
        assertThat(response.perDay()).isEqualTo(perDay);
        assertThat(response.alarmType()).isEqualTo(alarmType);
    }

    @ParameterizedTest
    @EnumSource(AlarmType.class)
    @DisplayName("Should handle all AlarmType values in from() method")
    void from_WithAllAlarmTypes_ShouldCreateValidResponse(AlarmType alarmType) {
        // given
        when(mockMedicationSchedule.getMedicationScheduleId()).thenReturn(1L);
        when(mockMedicationSchedule.getMember()).thenReturn(mockMember);
        when(mockMember.getMemberId()).thenReturn(2L);
        when(mockMedicationSchedule.getName()).thenReturn("Test Med");
        when(mockMedicationSchedule.getHospitalName()).thenReturn("Test Hospital");
        when(mockMedicationSchedule.getPrescriptionDate()).thenReturn(LocalDate.now());
        when(mockMedicationSchedule.getMemo()).thenReturn("Test memo");
        when(mockMedicationSchedule.getStartOfAd()).thenReturn(LocalDate.now());
        when(mockMedicationSchedule.getPrescriptionDays()).thenReturn(7);
        when(mockMedicationSchedule.getPerDay()).thenReturn(3);
        when(mockMedicationSchedule.getAlarmType()).thenReturn(alarmType);

        // when
        MedicationScheduleResponse response = MedicationScheduleResponse.from(mockMedicationSchedule);

        // then
        assertThat(response.alarmType()).isEqualTo(alarmType);
        assertThat(response.medicationScheduleId()).isEqualTo(1L);
        assertThat(response.memberId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Should handle null values in from() method")
    void from_WithNullValues_ShouldCreateResponseWithNulls() {
        // given
        when(mockMedicationSchedule.getMedicationScheduleId()).thenReturn(null);
        when(mockMedicationSchedule.getMember()).thenReturn(mockMember);
        when(mockMember.getMemberId()).thenReturn(null);
        when(mockMedicationSchedule.getName()).thenReturn(null);
        when(mockMedicationSchedule.getHospitalName()).thenReturn(null);
        when(mockMedicationSchedule.getPrescriptionDate()).thenReturn(null);
        when(mockMedicationSchedule.getMemo()).thenReturn(null);
        when(mockMedicationSchedule.getStartOfAd()).thenReturn(null);
        when(mockMedicationSchedule.getPrescriptionDays()).thenReturn(null);
        when(mockMedicationSchedule.getPerDay()).thenReturn(null);
        when(mockMedicationSchedule.getAlarmType()).thenReturn(null);

        // when
        MedicationScheduleResponse response = MedicationScheduleResponse.from(mockMedicationSchedule);

        // then
        assertThat(response.medicationScheduleId()).isNull();
        assertThat(response.memberId()).isNull();
        assertThat(response.name()).isNull();
        assertThat(response.hospitalName()).isNull();
        assertThat(response.prescriptionDate()).isNull();
        assertThat(response.memo()).isNull();
        assertThat(response.startOfAd()).isNull();
        assertThat(response.prescriptionDays()).isNull();
        assertThat(response.perDay()).isNull();
        assertThat(response.alarmType()).isNull();
    }

    @Test
    @DisplayName("Should throw exception when from() method receives null MedicationSchedule")
    void from_WithNullMedicationSchedule_ShouldThrowException() {
        // when & then
        assertThatThrownBy(() -> MedicationScheduleResponse.from(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should throw exception when MedicationSchedule has null member")
    void from_WithNullMember_ShouldThrowException() {
        // given
        when(mockMedicationSchedule.getMember()).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> MedicationScheduleResponse.from(mockMedicationSchedule))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should maintain equality and hashCode contract")
    void record_ShouldMaintainEqualityAndHashCode() {
        // given
        Long medicationScheduleId = 1L;
        Long memberId = 2L;
        String name = "Test Medication";
        String hospitalName = "Test Hospital";
        LocalDate prescriptionDate = LocalDate.of(2023, 10, 15);
        String memo = "Test memo";
        LocalDate startOfAd = LocalDate.of(2023, 10, 16);
        Integer prescriptionDays = 7;
        Integer perDay = 3;
        AlarmType alarmType = AlarmType.SOUND;

        MedicationScheduleResponse response1 = new MedicationScheduleResponse(
                medicationScheduleId, memberId, name, hospitalName, prescriptionDate,
                memo, startOfAd, prescriptionDays, perDay, alarmType
        );
        MedicationScheduleResponse response2 = new MedicationScheduleResponse(
                medicationScheduleId, memberId, name, hospitalName, prescriptionDate,
                memo, startOfAd, prescriptionDays, perDay, alarmType
        );

        // then
        assertThat(response1).isEqualTo(response2);
        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
        assertThat(response1).isEqualTo(response1); // reflexive
    }

    @Test
    @DisplayName("Should not be equal when fields differ")
    void record_WithDifferentFields_ShouldNotBeEqual() {
        // given
        LocalDate prescriptionDate = LocalDate.of(2023, 10, 15);
        LocalDate startOfAd = LocalDate.of(2023, 10, 16);

        MedicationScheduleResponse response1 = new MedicationScheduleResponse(
                1L, 2L, "Test Med", "Hospital A", prescriptionDate,
                "memo", startOfAd, 7, 3, AlarmType.SOUND
        );
        MedicationScheduleResponse response2 = new MedicationScheduleResponse(
                1L, 2L, "Test Med", "Hospital B", prescriptionDate,  // different hospital name
                "memo", startOfAd, 7, 3, AlarmType.SOUND
        );

        // then
        assertThat(response1).isNotEqualTo(response2);
        assertThat(response1.hashCode()).isNotEqualTo(response2.hashCode());
    }

    @Test
    @DisplayName("Should contain field information in toString")
    void toString_ShouldContainFieldInformation() {
        // given
        Long medicationScheduleId = 1L;
        Long memberId = 2L;
        String name = "Test Medication";
        String hospitalName = "Test Hospital";
        LocalDate prescriptionDate = LocalDate.of(2023, 10, 15);
        String memo = "Test memo";
        LocalDate startOfAd = LocalDate.of(2023, 10, 16);
        Integer prescriptionDays = 7;
        Integer perDay = 3;
        AlarmType alarmType = AlarmType.SOUND;

        MedicationScheduleResponse response = new MedicationScheduleResponse(
                medicationScheduleId, memberId, name, hospitalName, prescriptionDate,
                memo, startOfAd, prescriptionDays, perDay, alarmType
        );

        // when
        String toStringResult = response.toString();

        // then
        assertThat(toStringResult).contains("MedicationScheduleResponse");
        assertThat(toStringResult).contains("medicationScheduleId=1");
        assertThat(toStringResult).contains("memberId=2");
        assertThat(toStringResult).contains("name=Test Medication");
        assertThat(toStringResult).contains("hospitalName=Test Hospital");
        assertThat(toStringResult).contains("prescriptionDate=2023-10-15");
        assertThat(toStringResult).contains("memo=Test memo");
        assertThat(toStringResult).contains("startOfAd=2023-10-16");
        assertThat(toStringResult).contains("prescriptionDays=7");
        assertThat(toStringResult).contains("perDay=3");
        assertThat(toStringResult).contains("alarmType=SOUND");
    }

    @Test
    @DisplayName("Should handle edge case values in fields")
    void constructor_WithEdgeCaseValues_ShouldCreateResponse() {
        // given
        Long medicationScheduleId = Long.MAX_VALUE;
        Long memberId = Long.MIN_VALUE;
        String name = "";  // empty string
        String hospitalName = "A".repeat(255);  // very long string
        LocalDate prescriptionDate = LocalDate.MIN;
        String memo = "";
        LocalDate startOfAd = LocalDate.MAX;
        Integer prescriptionDays = Integer.MAX_VALUE;
        Integer perDay = Integer.MIN_VALUE;
        AlarmType alarmType = AlarmType.VIBE;

        // when
        MedicationScheduleResponse response = new MedicationScheduleResponse(
                medicationScheduleId, memberId, name, hospitalName, prescriptionDate,
                memo, startOfAd, prescriptionDays, perDay, alarmType
        );

        // then
        assertThat(response.medicationScheduleId()).isEqualTo(medicationScheduleId);
        assertThat(response.memberId()).isEqualTo(memberId);
        assertThat(response.name()).isEqualTo(name);
        assertThat(response.hospitalName()).isEqualTo(hospitalName);
        assertThat(response.prescriptionDate()).isEqualTo(prescriptionDate);
        assertThat(response.memo()).isEqualTo(memo);
        assertThat(response.startOfAd()).isEqualTo(startOfAd);
        assertThat(response.prescriptionDays()).isEqualTo(prescriptionDays);
        assertThat(response.perDay()).isEqualTo(perDay);
        assertThat(response.alarmType()).isEqualTo(alarmType);
    }

    @Test
    @DisplayName("Should handle zero values in numeric fields")
    void constructor_WithZeroValues_ShouldCreateResponse() {
        // given
        Long medicationScheduleId = 0L;
        Long memberId = 0L;
        Integer prescriptionDays = 0;
        Integer perDay = 0;

        // when
        MedicationScheduleResponse response = new MedicationScheduleResponse(
                medicationScheduleId, memberId, "Test", "Hospital", LocalDate.now(),
                "memo", LocalDate.now(), prescriptionDays, perDay, AlarmType.SOUND
        );

        // then
        assertThat(response.medicationScheduleId()).isZero();
        assertThat(response.memberId()).isZero();
        assertThat(response.prescriptionDays()).isZero();
        assertThat(response.perDay()).isZero();
    }
}