package com.Hamalog.dto.medication.request;

import static org.assertj.core.api.Assertions.assertThat;

import com.Hamalog.domain.medication.AlarmType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MedicationScheduleUpdateRequest Tests")
class MedicationScheduleUpdateRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Should create MedicationScheduleUpdateRequest with all valid fields")
    void constructor_ValidFields_CreatesSuccessfully() {
        // given
        String name = "TestMedicine";
        String hospitalName = "TestHospital";
        LocalDate prescriptionDate = LocalDate.of(2023, 10, 1);
        String memo = "Test memo";
        LocalDate startOfAd = LocalDate.of(2023, 10, 15);
        Integer prescriptionDays = 30;
        Integer perDay = 2;
        AlarmType alarmType = AlarmType.SOUND;

        // when
        MedicationScheduleUpdateRequest request = new MedicationScheduleUpdateRequest(
                name, hospitalName, prescriptionDate, memo, startOfAd,
                prescriptionDays, perDay, alarmType
        );

        // then
        assertThat(request).isNotNull();
        assertThat(request.name()).isEqualTo(name);
        assertThat(request.hospitalName()).isEqualTo(hospitalName);
        assertThat(request.prescriptionDate()).isEqualTo(prescriptionDate);
        assertThat(request.memo()).isEqualTo(memo);
        assertThat(request.startOfAd()).isEqualTo(startOfAd);
        assertThat(request.prescriptionDays()).isEqualTo(prescriptionDays);
        assertThat(request.perDay()).isEqualTo(perDay);
        assertThat(request.alarmType()).isEqualTo(alarmType);
    }

    @Test
    @DisplayName("Should pass validation with valid fields")
    void validation_ValidFields_PassesValidation() {
        // given
        MedicationScheduleUpdateRequest request = new MedicationScheduleUpdateRequest(
                "TestMedicine", "TestHospital", LocalDate.of(2023, 10, 1),
                "Test memo", LocalDate.of(2023, 10, 15), 30, 2, AlarmType.SOUND
        );

        // when
        Set<ConstraintViolation<MedicationScheduleUpdateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation with null name")
    void validation_NullName_FailsValidation() {
        // given
        MedicationScheduleUpdateRequest request = new MedicationScheduleUpdateRequest(
                null, "TestHospital", LocalDate.of(2023, 10, 1),
                "Test memo", LocalDate.of(2023, 10, 15), 30, 2, AlarmType.SOUND
        );

        // when
        Set<ConstraintViolation<MedicationScheduleUpdateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("name");
    }

    @Test
    @DisplayName("Should fail validation with blank name")
    void validation_BlankName_FailsValidation() {
        // given
        MedicationScheduleUpdateRequest request = new MedicationScheduleUpdateRequest(
                "   ", "TestHospital", LocalDate.of(2023, 10, 1),
                "Test memo", LocalDate.of(2023, 10, 15), 30, 2, AlarmType.SOUND
        );

        // when
        Set<ConstraintViolation<MedicationScheduleUpdateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("name");
    }

    @Test
    @DisplayName("Should fail validation with name exceeding max length")
    void validation_NameTooLong_FailsValidation() {
        // given
        String longName = "a".repeat(21); // 21 characters, exceeds max 20
        MedicationScheduleUpdateRequest request = new MedicationScheduleUpdateRequest(
                longName, "TestHospital", LocalDate.of(2023, 10, 1),
                "Test memo", LocalDate.of(2023, 10, 15), 30, 2, AlarmType.SOUND
        );

        // when
        Set<ConstraintViolation<MedicationScheduleUpdateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("name");
    }

    @Test
    @DisplayName("Should pass validation with name at max length")
    void validation_NameAtMaxLength_PassesValidation() {
        // given
        String maxLengthName = "a".repeat(20); // Exactly 20 characters
        MedicationScheduleUpdateRequest request = new MedicationScheduleUpdateRequest(
                maxLengthName, "TestHospital", LocalDate.of(2023, 10, 1),
                "Test memo", LocalDate.of(2023, 10, 15), 30, 2, AlarmType.SOUND
        );

        // when
        Set<ConstraintViolation<MedicationScheduleUpdateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation with null hospital name")
    void validation_NullHospitalName_FailsValidation() {
        // given
        MedicationScheduleUpdateRequest request = new MedicationScheduleUpdateRequest(
                "TestMedicine", null, LocalDate.of(2023, 10, 1),
                "Test memo", LocalDate.of(2023, 10, 15), 30, 2, AlarmType.SOUND
        );

        // when
        Set<ConstraintViolation<MedicationScheduleUpdateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("hospitalName");
    }

    @Test
    @DisplayName("Should fail validation with hospital name exceeding max length")
    void validation_HospitalNameTooLong_FailsValidation() {
        // given
        String longHospitalName = "a".repeat(21); // 21 characters
        MedicationScheduleUpdateRequest request = new MedicationScheduleUpdateRequest(
                "TestMedicine", longHospitalName, LocalDate.of(2023, 10, 1),
                "Test memo", LocalDate.of(2023, 10, 15), 30, 2, AlarmType.SOUND
        );

        // when
        Set<ConstraintViolation<MedicationScheduleUpdateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("hospitalName");
    }

    @Test
    @DisplayName("Should fail validation with null prescription date")
    void validation_NullPrescriptionDate_FailsValidation() {
        // given
        MedicationScheduleUpdateRequest request = new MedicationScheduleUpdateRequest(
                "TestMedicine", "TestHospital", null,
                "Test memo", LocalDate.of(2023, 10, 15), 30, 2, AlarmType.SOUND
        );

        // when
        Set<ConstraintViolation<MedicationScheduleUpdateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("prescriptionDate");
    }

    @Test
    @DisplayName("Should pass validation with null memo")
    void validation_NullMemo_PassesValidation() {
        // given
        MedicationScheduleUpdateRequest request = new MedicationScheduleUpdateRequest(
                "TestMedicine", "TestHospital", LocalDate.of(2023, 10, 1),
                null, LocalDate.of(2023, 10, 15), 30, 2, AlarmType.SOUND
        );

        // when
        Set<ConstraintViolation<MedicationScheduleUpdateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation with memo exceeding max length")
    void validation_MemoTooLong_FailsValidation() {
        // given
        String longMemo = "a".repeat(501); // 501 characters
        MedicationScheduleUpdateRequest request = new MedicationScheduleUpdateRequest(
                "TestMedicine", "TestHospital", LocalDate.of(2023, 10, 1),
                longMemo, LocalDate.of(2023, 10, 15), 30, 2, AlarmType.SOUND
        );

        // when
        Set<ConstraintViolation<MedicationScheduleUpdateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("memo");
    }

    @Test
    @DisplayName("Should pass validation with memo at max length")
    void validation_MemoAtMaxLength_PassesValidation() {
        // given
        String maxLengthMemo = "a".repeat(500); // Exactly 500 characters
        MedicationScheduleUpdateRequest request = new MedicationScheduleUpdateRequest(
                "TestMedicine", "TestHospital", LocalDate.of(2023, 10, 1),
                maxLengthMemo, LocalDate.of(2023, 10, 15), 30, 2, AlarmType.SOUND
        );

        // when
        Set<ConstraintViolation<MedicationScheduleUpdateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation with null start of ad")
    void validation_NullStartOfAd_FailsValidation() {
        // given
        MedicationScheduleUpdateRequest request = new MedicationScheduleUpdateRequest(
                "TestMedicine", "TestHospital", LocalDate.of(2023, 10, 1),
                "Test memo", null, 30, 2, AlarmType.SOUND
        );

        // when
        Set<ConstraintViolation<MedicationScheduleUpdateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("startOfAd");
    }

    @Test
    @DisplayName("Should fail validation with prescription days less than 1")
    void validation_PrescriptionDaysLessThanOne_FailsValidation() {
        // given
        MedicationScheduleUpdateRequest request = new MedicationScheduleUpdateRequest(
                "TestMedicine", "TestHospital", LocalDate.of(2023, 10, 1),
                "Test memo", LocalDate.of(2023, 10, 15), 0, 2, AlarmType.SOUND
        );

        // when
        Set<ConstraintViolation<MedicationScheduleUpdateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("prescriptionDays");
    }

    @Test
    @DisplayName("Should fail validation with per day less than 1")
    void validation_PerDayLessThanOne_FailsValidation() {
        // given
        MedicationScheduleUpdateRequest request = new MedicationScheduleUpdateRequest(
                "TestMedicine", "TestHospital", LocalDate.of(2023, 10, 1),
                "Test memo", LocalDate.of(2023, 10, 15), 30, 0, AlarmType.SOUND
        );

        // when
        Set<ConstraintViolation<MedicationScheduleUpdateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("perDay");
    }

    @Test
    @DisplayName("Should fail validation with null alarm type")
    void validation_NullAlarmType_FailsValidation() {
        // given
        MedicationScheduleUpdateRequest request = new MedicationScheduleUpdateRequest(
                "TestMedicine", "TestHospital", LocalDate.of(2023, 10, 1),
                "Test memo", LocalDate.of(2023, 10, 15), 30, 2, null
        );

        // when
        Set<ConstraintViolation<MedicationScheduleUpdateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("alarmType");
    }

    @Test
    @DisplayName("Should handle both AlarmType values")
    void constructor_BothAlarmTypes_CreatesSuccessfully() {
        // given/when
        MedicationScheduleUpdateRequest soundRequest = new MedicationScheduleUpdateRequest(
                "TestMedicine", "TestHospital", LocalDate.of(2023, 10, 1),
                "Test memo", LocalDate.of(2023, 10, 15), 30, 2, AlarmType.SOUND
        );
        MedicationScheduleUpdateRequest vibeRequest = new MedicationScheduleUpdateRequest(
                "TestMedicine", "TestHospital", LocalDate.of(2023, 10, 1),
                "Test memo", LocalDate.of(2023, 10, 15), 30, 2, AlarmType.VIBE
        );

        // then
        assertThat(soundRequest.alarmType()).isEqualTo(AlarmType.SOUND);
        assertThat(vibeRequest.alarmType()).isEqualTo(AlarmType.VIBE);
    }

    @Test
    @DisplayName("Should handle equality correctly for records with same values")
    void equals_SameValues_ReturnsTrue() {
        // given
        MedicationScheduleUpdateRequest request1 = new MedicationScheduleUpdateRequest(
                "TestMedicine", "TestHospital", LocalDate.of(2023, 10, 1),
                "Test memo", LocalDate.of(2023, 10, 15), 30, 2, AlarmType.SOUND
        );
        MedicationScheduleUpdateRequest request2 = new MedicationScheduleUpdateRequest(
                "TestMedicine", "TestHospital", LocalDate.of(2023, 10, 1),
                "Test memo", LocalDate.of(2023, 10, 15), 30, 2, AlarmType.SOUND
        );

        // when/then
        assertThat(request1).isEqualTo(request2);
        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
    }

    @Test
    @DisplayName("Should handle inequality correctly for records with different values")
    void equals_DifferentValues_ReturnsFalse() {
        // given
        MedicationScheduleUpdateRequest request1 = new MedicationScheduleUpdateRequest(
                "TestMedicine", "TestHospital", LocalDate.of(2023, 10, 1),
                "Test memo", LocalDate.of(2023, 10, 15), 30, 2, AlarmType.SOUND
        );
        MedicationScheduleUpdateRequest request2 = new MedicationScheduleUpdateRequest(
                "DifferentMedicine", "TestHospital", LocalDate.of(2023, 10, 1),
                "Test memo", LocalDate.of(2023, 10, 15), 30, 2, AlarmType.SOUND
        );

        // when/then
        assertThat(request1).isNotEqualTo(request2);
    }

    @Test
    @DisplayName("Should provide meaningful toString representation")
    void toString_Always_ContainsAllFields() {
        // given
        MedicationScheduleUpdateRequest request = new MedicationScheduleUpdateRequest(
                "TestMedicine", "TestHospital", LocalDate.of(2023, 10, 1),
                "Test memo", LocalDate.of(2023, 10, 15), 30, 2, AlarmType.SOUND
        );

        // when
        String toString = request.toString();

        // then
        assertThat(toString).contains("name=TestMedicine");
        assertThat(toString).contains("hospitalName=TestHospital");
        assertThat(toString).contains("prescriptionDate=2023-10-01");
        assertThat(toString).contains("memo=Test memo");
        assertThat(toString).contains("startOfAd=2023-10-15");
        assertThat(toString).contains("prescriptionDays=30");
        assertThat(toString).contains("perDay=2");
        assertThat(toString).contains("alarmType=SOUND");
    }

    @Test
    @DisplayName("Should handle edge case dates")
    void constructor_EdgeCaseDates_CreatesSuccessfully() {
        // given
        LocalDate minDate = LocalDate.MIN;
        LocalDate maxDate = LocalDate.MAX;

        // when
        MedicationScheduleUpdateRequest request = new MedicationScheduleUpdateRequest(
                "TestMedicine", "TestHospital", minDate,
                "Test memo", maxDate, 1, 1, AlarmType.SOUND
        );

        // then
        assertThat(request.prescriptionDate()).isEqualTo(minDate);
        assertThat(request.startOfAd()).isEqualTo(maxDate);
    }

    @Test
    @DisplayName("Should handle large integer values")
    void constructor_LargeIntegers_CreatesSuccessfully() {
        // given
        Integer maxPrescriptionDays = Integer.MAX_VALUE;
        Integer maxPerDay = Integer.MAX_VALUE;

        // when
        MedicationScheduleUpdateRequest request = new MedicationScheduleUpdateRequest(
                "TestMedicine", "TestHospital", LocalDate.of(2023, 10, 1),
                "Test memo", LocalDate.of(2023, 10, 15), 
                maxPrescriptionDays, maxPerDay, AlarmType.SOUND
        );

        // then
        assertThat(request.prescriptionDays()).isEqualTo(maxPrescriptionDays);
        assertThat(request.perDay()).isEqualTo(maxPerDay);
    }
}