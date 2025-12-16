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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

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
                LocalDate.of(2024, 1, 15),
                "식후 복용",
                LocalDate.of(2024, 1, 16),
                30,
                2,
                AlarmType.SOUND
        );

        // when
        Set<ConstraintViolation<MedicationScheduleCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
        assertThat(request.memberId()).isEqualTo(1L);
        assertThat(request.name()).isEqualTo("아스피린");
        assertThat(request.hospitalName()).isEqualTo("서울병원");
        assertThat(request.prescriptionDate()).isEqualTo(LocalDate.of(2024, 1, 15));
        assertThat(request.memo()).isEqualTo("식후 복용");
        assertThat(request.startOfAd()).isEqualTo(LocalDate.of(2024, 1, 16));
        assertThat(request.prescriptionDays()).isEqualTo(30);
        assertThat(request.perDay()).isEqualTo(2);
        assertThat(request.alarmType()).isEqualTo(AlarmType.SOUND);
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
    void validation_WithInvalidName_ShouldFailValidation(String name) {
        // given
        MedicationScheduleCreateRequest request = createValidRequestBuilder()
                .name(name)
                .build();

        // when
        Set<ConstraintViolation<MedicationScheduleCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
    }

    @Test
    @DisplayName("Should fail validation when name exceeds max length")
    void validation_WithTooLongName_ShouldFailValidation() {
        // given
        String longName = "a".repeat(21);
        MedicationScheduleCreateRequest request = createValidRequestBuilder()
                .name(longName)
                .build();

        // when
        Set<ConstraintViolation<MedicationScheduleCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
    }

    // HospitalName validation tests
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Should fail validation when hospitalName is null, empty, or blank")
    void validation_WithInvalidHospitalName_ShouldFailValidation(String hospitalName) {
        // given
        MedicationScheduleCreateRequest request = createValidRequestBuilder()
                .hospitalName(hospitalName)
                .build();

        // when
        Set<ConstraintViolation<MedicationScheduleCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("hospitalName"));
    }

    // PrescriptionDate validation tests
    @Test
    @DisplayName("Should fail validation when prescriptionDate is null")
    void validation_WithNullPrescriptionDate_ShouldFailValidation() {
        // given
        MedicationScheduleCreateRequest request = createValidRequestBuilder()
                .prescriptionDate(null)
                .build();

        // when
        Set<ConstraintViolation<MedicationScheduleCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("prescriptionDate"));
    }

    // StartOfAd validation tests
    @Test
    @DisplayName("Should fail validation when startOfAd is null")
    void validation_WithNullStartOfAd_ShouldFailValidation() {
        // given
        MedicationScheduleCreateRequest request = createValidRequestBuilder()
                .startOfAd(null)
                .build();

        // when
        Set<ConstraintViolation<MedicationScheduleCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("startOfAd"));
    }

    // PrescriptionDays validation tests
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

    @Test
    @DisplayName("Should fail validation when prescriptionDays is less than 1")
    void validation_WithPrescriptionDaysLessThan1_ShouldFailValidation() {
        // given
        MedicationScheduleCreateRequest request = createValidRequestBuilder()
                .prescriptionDays(0)
                .build();

        // when
        Set<ConstraintViolation<MedicationScheduleCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("prescriptionDays"));
    }

    // PerDay validation tests
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

    @Test
    @DisplayName("Should fail validation when perDay is less than 1")
    void validation_WithPerDayLessThan1_ShouldFailValidation() {
        // given
        MedicationScheduleCreateRequest request = createValidRequestBuilder()
                .perDay(0)
                .build();

        // when
        Set<ConstraintViolation<MedicationScheduleCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("perDay"));
    }

    // AlarmType validation tests
    @Test
    @DisplayName("Should fail validation when alarmType is null")
    void validation_WithNullAlarmType_ShouldFailValidation() {
        // given
        MedicationScheduleCreateRequest request = createValidRequestBuilder()
                .alarmType(null)
                .build();

        // when
        Set<ConstraintViolation<MedicationScheduleCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("alarmType"));
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
    @DisplayName("Should fail validation when memo exceeds max length")
    void validation_WithTooLongMemo_ShouldFailValidation() {
        // given
        String longMemo = "a".repeat(501);
        MedicationScheduleCreateRequest request = createValidRequestBuilder()
                .memo(longMemo)
                .build();

        // when
        Set<ConstraintViolation<MedicationScheduleCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("memo"));
    }

    // Helper method to create valid request builder
    private MedicationScheduleCreateRequestBuilder createValidRequestBuilder() {
        return new MedicationScheduleCreateRequestBuilder()
                .memberId(1L)
                .name("아스피린")
                .hospitalName("서울병원")
                .prescriptionDate(LocalDate.of(2024, 1, 15))
                .memo("식후 복용")
                .startOfAd(LocalDate.of(2024, 1, 16))
                .prescriptionDays(30)
                .perDay(2)
                .alarmType(AlarmType.SOUND);
    }

    // Helper builder class for test data creation
    private static class MedicationScheduleCreateRequestBuilder {
        private Long memberId;
        private String name;
        private String hospitalName;
        private LocalDate prescriptionDate;
        private String memo;
        private LocalDate startOfAd;
        private Integer prescriptionDays;
        private Integer perDay;
        private AlarmType alarmType;

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

        public MedicationScheduleCreateRequestBuilder prescriptionDate(LocalDate prescriptionDate) {
            this.prescriptionDate = prescriptionDate;
            return this;
        }

        public MedicationScheduleCreateRequestBuilder memo(String memo) {
            this.memo = memo;
            return this;
        }

        public MedicationScheduleCreateRequestBuilder startOfAd(LocalDate startOfAd) {
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

        public MedicationScheduleCreateRequestBuilder alarmType(AlarmType alarmType) {
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
