package com.Hamalog.dto.medication.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MedicationScheduleCreateRequest DTO Tests")
class MedicationScheduleCreateRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Should create valid MedicationScheduleCreateRequest with valid data")
    void constructor_WithValidData_ShouldCreateValidRequest() {
        // given
        MedicationScheduleCreateRequest request = new MedicationScheduleCreateRequest(
                1L,
                "아스피린",
                "서울병원",
                "2024-01-15",
                "식후 복용",
                "2024-01-16",
                30,
                2,
                "SOUND"
        );

        // when
        Set<ConstraintViolation<MedicationScheduleCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
        assertThat(request.memberId()).isEqualTo(1L);
        assertThat(request.name()).isEqualTo("아스피린");
        assertThat(request.hospitalName()).isEqualTo("서울병원");
        assertThat(request.prescriptionDate()).isEqualTo("2024-01-15");
        assertThat(request.memo()).isEqualTo("식후 복용");
        assertThat(request.startOfAd()).isEqualTo("2024-01-16");
        assertThat(request.prescriptionDays()).isEqualTo(30);
        assertThat(request.perDay()).isEqualTo(2);
        assertThat(request.alarmType()).isEqualTo("SOUND");
    }

    // MemberId validation tests
    @Test
    @DisplayName("Should fail validation when memberId is null")
    void validation_WithNullMemberId_ShouldFailValidation() {
        // given
        MedicationScheduleCreateRequest request = createValidRequestBuilder()
                .memberId(null)
                .build();

        // when
        Set<ConstraintViolation<MedicationScheduleCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("memberId"));
    }

    // Name validation tests
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Should fail validation when name is null, empty, or blank")
    void validation_WithInvalidName_ShouldFailValidation(String invalidName) {
        // given
        MedicationScheduleCreateRequest request = createValidRequestBuilder()
                .name(invalidName)
                .build();

        // when
        Set<ConstraintViolation<MedicationScheduleCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
    }

    @Test
    @DisplayName("Should fail validation when name exceeds maximum length")
    void validation_WithTooLongName_ShouldFailValidation() {
        // given
        String tooLongName = "a".repeat(21); // 21 characters
        MedicationScheduleCreateRequest request = createValidRequestBuilder()
                .name(tooLongName)
                .build();

        // when
        Set<ConstraintViolation<MedicationScheduleCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
    }

    @Test
    @DisplayName("Should pass validation with maximum valid name length")
    void validation_WithMaxValidNameLength_ShouldPassValidation() {
        // given
        String maxLengthName = "a".repeat(20); // 20 characters
        MedicationScheduleCreateRequest request = createValidRequestBuilder()
                .name(maxLengthName)
                .build();

        // when
        Set<ConstraintViolation<MedicationScheduleCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    // Hospital name validation tests
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("Should fail validation when hospitalName is null, empty, or blank")
    void validation_WithInvalidHospitalName_ShouldFailValidation(String invalidHospitalName) {
        // given
        MedicationScheduleCreateRequest request = createValidRequestBuilder()
                .hospitalName(invalidHospitalName)
                .build();

        // when
        Set<ConstraintViolation<MedicationScheduleCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("hospitalName"));
    }

    @Test
    @DisplayName("Should fail validation when hospitalName exceeds maximum length")
    void validation_WithTooLongHospitalName_ShouldFailValidation() {
        // given
        String tooLongHospitalName = "서울중앙병원내과전문의원센터부설".repeat(2); // Over 20 characters
        MedicationScheduleCreateRequest request = createValidRequestBuilder()
                .hospitalName(tooLongHospitalName)
                .build();

        // when
        Set<ConstraintViolation<MedicationScheduleCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("hospitalName"));
    }

    // Prescription date validation tests
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("Should fail validation when prescriptionDate is null, empty, or blank")
    void validation_WithInvalidPrescriptionDate_ShouldFailValidation(String invalidDate) {
        // given
        MedicationScheduleCreateRequest request = createValidRequestBuilder()
                .prescriptionDate(invalidDate)
                .build();

        // when
        Set<ConstraintViolation<MedicationScheduleCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("prescriptionDate"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2024-1-15", "24-01-15", "2024/01/15", "2024.01.15", "invalid-date"})
    @DisplayName("Should fail validation when prescriptionDate has invalid format")
    void validation_WithInvalidPrescriptionDateFormat_ShouldFailValidation(String invalidDateFormat) {
        // given
        MedicationScheduleCreateRequest request = createValidRequestBuilder()
                .prescriptionDate(invalidDateFormat)
                .build();

        // when
        Set<ConstraintViolation<MedicationScheduleCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
        assertThat(violations).anyMatch(v -> 
                v.getPropertyPath().toString().equals("prescriptionDate")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"2024-01-01", "2024-12-31", "2023-06-15", "2025-02-28"})
    @DisplayName("Should pass validation with valid prescriptionDate formats")
    void validation_WithValidPrescriptionDateFormats_ShouldPassValidation(String validDate) {
        // given
        MedicationScheduleCreateRequest request = createValidRequestBuilder()
                .prescriptionDate(validDate)
                .build();

        // when
        Set<ConstraintViolation<MedicationScheduleCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    // Memo validation tests
    @Test
    @DisplayName("Should pass validation when memo is null")
    void validation_WithNullMemo_ShouldPassValidation() {
        // given
        MedicationScheduleCreateRequest request = createValidRequestBuilder()
                .memo(null)
                .build();

        // when
        Set<ConstraintViolation<MedicationScheduleCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation when memo exceeds maximum length")
    void validation_WithTooLongMemo_ShouldFailValidation() {
        // given
        String tooLongMemo = "a".repeat(501); // 501 characters
        MedicationScheduleCreateRequest request = createValidRequestBuilder()
                .memo(tooLongMemo)
                .build();

        // when
        Set<ConstraintViolation<MedicationScheduleCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("memo"));
    }

    @Test
    @DisplayName("Should pass validation with maximum valid memo length")
    void validation_WithMaxValidMemoLength_ShouldPassValidation() {
        // given
        String maxLengthMemo = "a".repeat(500); // 500 characters
        MedicationScheduleCreateRequest request = createValidRequestBuilder()
                .memo(maxLengthMemo)
                .build();

        // when
        Set<ConstraintViolation<MedicationScheduleCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    // Start of administration date validation tests
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("Should fail validation when startOfAd is null, empty, or blank")
    void validation_WithInvalidStartOfAd_ShouldFailValidation(String invalidDate) {
        // given
        MedicationScheduleCreateRequest request = createValidRequestBuilder()
                .startOfAd(invalidDate)
                .build();

        // when
        Set<ConstraintViolation<MedicationScheduleCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("startOfAd"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2024-1-16", "24-01-16", "2024/01/16", "invalid-start-date"})
    @DisplayName("Should fail validation when startOfAd has invalid format")
    void validation_WithInvalidStartOfAdFormat_ShouldFailValidation(String invalidDateFormat) {
        // given
        MedicationScheduleCreateRequest request = createValidRequestBuilder()
                .startOfAd(invalidDateFormat)
                .build();

        // when
        Set<ConstraintViolation<MedicationScheduleCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
        assertThat(violations).anyMatch(v -> 
                v.getPropertyPath().toString().equals("startOfAd")
        );
    }

    // Prescription days validation tests
    @Test
    @DisplayName("Should fail validation when prescriptionDays is null")
    void validation_WithNullPrescriptionDays_ShouldFailValidation() {
        // given
        MedicationScheduleCreateRequest request = createValidRequestBuilder()
                .prescriptionDays(null)
                .build();

        // when
        Set<ConstraintViolation<MedicationScheduleCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("prescriptionDays"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -10})
    @DisplayName("Should fail validation when prescriptionDays is zero or negative")
    void validation_WithInvalidPrescriptionDays_ShouldFailValidation(int invalidDays) {
        // given
        MedicationScheduleCreateRequest request = createValidRequestBuilder()
                .prescriptionDays(invalidDays)
                .build();

        // when
        Set<ConstraintViolation<MedicationScheduleCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("prescriptionDays"));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 7, 30, 90, 365})
    @DisplayName("Should pass validation with valid prescriptionDays")
    void validation_WithValidPrescriptionDays_ShouldPassValidation(int validDays) {
        // given
        MedicationScheduleCreateRequest request = createValidRequestBuilder()
                .prescriptionDays(validDays)
                .build();

        // when
        Set<ConstraintViolation<MedicationScheduleCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    // Per day validation tests
    @Test
    @DisplayName("Should fail validation when perDay is null")
    void validation_WithNullPerDay_ShouldFailValidation() {
        // given
        MedicationScheduleCreateRequest request = createValidRequestBuilder()
                .perDay(null)
                .build();

        // when
        Set<ConstraintViolation<MedicationScheduleCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("perDay"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -5})
    @DisplayName("Should fail validation when perDay is zero or negative")
    void validation_WithInvalidPerDay_ShouldFailValidation(int invalidPerDay) {
        // given
        MedicationScheduleCreateRequest request = createValidRequestBuilder()
                .perDay(invalidPerDay)
                .build();

        // when
        Set<ConstraintViolation<MedicationScheduleCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("perDay"));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 6, 8})
    @DisplayName("Should pass validation with valid perDay values")
    void validation_WithValidPerDay_ShouldPassValidation(int validPerDay) {
        // given
        MedicationScheduleCreateRequest request = createValidRequestBuilder()
                .perDay(validPerDay)
                .build();

        // when
        Set<ConstraintViolation<MedicationScheduleCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    // Alarm type validation tests
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("Should fail validation when alarmType is null, empty, or blank")
    void validation_WithInvalidAlarmType_ShouldFailValidation(String invalidAlarmType) {
        // given
        MedicationScheduleCreateRequest request = createValidRequestBuilder()
                .alarmType(invalidAlarmType)
                .build();

        // when
        Set<ConstraintViolation<MedicationScheduleCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("alarmType"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"SOUND", "VIBE", "BOTH", "NONE"})
    @DisplayName("Should pass validation with valid alarmType values")
    void validation_WithValidAlarmTypes_ShouldPassValidation(String validAlarmType) {
        // given
        MedicationScheduleCreateRequest request = createValidRequestBuilder()
                .alarmType(validAlarmType)
                .build();

        // when
        Set<ConstraintViolation<MedicationScheduleCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    // Record functionality tests
    @Test
    @DisplayName("Should maintain record equality and hashCode behavior")
    void record_ShouldMaintainEqualityAndHashCode() {
        // given
        MedicationScheduleCreateRequest request1 = createValidRequestBuilder().build();
        MedicationScheduleCreateRequest request2 = createValidRequestBuilder().build();
        MedicationScheduleCreateRequest request3 = createValidRequestBuilder()
                .name("다른약품")
                .build();

        // then
        assertThat(request1).isEqualTo(request2);
        assertThat(request1).isNotEqualTo(request3);
        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
    }

    @Test
    @DisplayName("Should have proper toString representation")
    void toString_ShouldContainFieldInformation() {
        // given
        MedicationScheduleCreateRequest request = createValidRequestBuilder().build();

        // when
        String result = request.toString();

        // then
        assertThat(result).contains("MedicationScheduleCreateRequest");
        assertThat(result).contains("아스피린");
        assertThat(result).contains("서울병원");
        assertThat(result).contains("2024-01-15");
    }

    @Test
    @DisplayName("Should handle multiple validation errors")
    void validation_WithMultipleErrors_ShouldReturnAllViolations() {
        // given - multiple invalid fields
        MedicationScheduleCreateRequest invalidRequest = new MedicationScheduleCreateRequest(
                null, "", "", "invalid-date", "a".repeat(501), "invalid-start", 0, -1, ""
        );

        // when
        Set<ConstraintViolation<MedicationScheduleCreateRequest>> violations = validator.validate(invalidRequest);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(8); // Should have violations for most fields
    }

    // Helper method to create valid request builder
    private MedicationScheduleCreateRequestBuilder createValidRequestBuilder() {
        return new MedicationScheduleCreateRequestBuilder()
                .memberId(1L)
                .name("아스피린")
                .hospitalName("서울병원")
                .prescriptionDate("2024-01-15")
                .memo("식후 복용")
                .startOfAd("2024-01-16")
                .prescriptionDays(30)
                .perDay(2)
                .alarmType("SOUND");
    }

    // Helper builder class for test data creation
    private static class MedicationScheduleCreateRequestBuilder {
        private Long memberId;
        private String name;
        private String hospitalName;
        private String prescriptionDate;
        private String memo;
        private String startOfAd;
        private Integer prescriptionDays;
        private Integer perDay;
        private String alarmType;

        public MedicationScheduleCreateRequestBuilder memberId(Long memberId) {
            this.memberId = memberId;
            return this;
        }

        public MedicationScheduleCreateRequestBuilder name(String name) {
            this.name = name;
            return this;
        }

        public MedicationScheduleCreateRequestBuilder hospitalName(String hospitalName) {
            this.hospitalName = hospitalName;
            return this;
        }

        public MedicationScheduleCreateRequestBuilder prescriptionDate(String prescriptionDate) {
            this.prescriptionDate = prescriptionDate;
            return this;
        }

        public MedicationScheduleCreateRequestBuilder memo(String memo) {
            this.memo = memo;
            return this;
        }

        public MedicationScheduleCreateRequestBuilder startOfAd(String startOfAd) {
            this.startOfAd = startOfAd;
            return this;
        }

        public MedicationScheduleCreateRequestBuilder prescriptionDays(Integer prescriptionDays) {
            this.prescriptionDays = prescriptionDays;
            return this;
        }

        public MedicationScheduleCreateRequestBuilder perDay(Integer perDay) {
            this.perDay = perDay;
            return this;
        }

        public MedicationScheduleCreateRequestBuilder alarmType(String alarmType) {
            this.alarmType = alarmType;
            return this;
        }

        public MedicationScheduleCreateRequest build() {
            return new MedicationScheduleCreateRequest(
                    memberId, name, hospitalName, prescriptionDate, memo,
                    startOfAd, prescriptionDays, perDay, alarmType
            );
        }
    }
}