package com.Hamalog.dto.auth.request;

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

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SignupRequest DTO Tests")
class SignupRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Should create valid SignupRequest with valid data")
    void constructor_WithValidData_ShouldCreateValidRequest() {
        // given
        SignupRequest signupRequest = new SignupRequest(
                "user@example.com",
                "password123",
                "홍길동",
                "길동이",
                "01012345678",
                LocalDate.of(1990, 1, 1)
        );

        // when
        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(signupRequest);

        // then
        assertThat(violations).isEmpty();
        assertThat(signupRequest.loginId()).isEqualTo("user@example.com");
        assertThat(signupRequest.password()).isEqualTo("password123");
        assertThat(signupRequest.name()).isEqualTo("홍길동");
        assertThat(signupRequest.nickName()).isEqualTo("길동이");
        assertThat(signupRequest.phoneNumber()).isEqualTo("01012345678");
        assertThat(signupRequest.birth()).isEqualTo(LocalDate.of(1990, 1, 1));
    }

    // LoginId validation tests
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Should fail validation when loginId is null, empty, or blank")
    void validation_WithInvalidLoginId_ShouldFailValidation(String invalidLoginId) {
        // given
        SignupRequest signupRequest = createValidSignupRequestBuilder()
                .loginId(invalidLoginId)
                .build();

        // when
        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(signupRequest);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("loginId"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid-email", "user@", "@example.com", "user.example.com", "user@.com"})
    @DisplayName("Should fail validation when loginId is not valid email format")
    void validation_WithInvalidEmailFormat_ShouldFailValidation(String invalidEmail) {
        // given
        SignupRequest signupRequest = createValidSignupRequestBuilder()
                .loginId(invalidEmail)
                .build();

        // when
        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(signupRequest);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
        assertThat(violations).anyMatch(v -> 
                v.getPropertyPath().toString().equals("loginId") && 
                v.getMessage().contains("email") || v.getMessage().contains("이메일")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"user@gmail.com", "test.user@example.co.kr", "user123+tag@domain.org"})
    @DisplayName("Should pass validation with valid email formats")
    void validation_WithValidEmailFormats_ShouldPassValidation(String validEmail) {
        // given
        SignupRequest signupRequest = createValidSignupRequestBuilder()
                .loginId(validEmail)
                .build();

        // when
        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(signupRequest);

        // then
        assertThat(violations).isEmpty();
    }

    // Password validation tests
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("Should fail validation when password is null, empty, or blank")
    void validation_WithInvalidPassword_ShouldFailValidation(String invalidPassword) {
        // given
        SignupRequest signupRequest = createValidSignupRequestBuilder()
                .password(invalidPassword)
                .build();

        // when
        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(signupRequest);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("Should fail validation when password is too short")
    void validation_WithTooShortPassword_ShouldFailValidation() {
        // given
        SignupRequest signupRequest = createValidSignupRequestBuilder()
                .password("12345") // 5 characters
                .build();

        // when
        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(signupRequest);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("Should fail validation when password is too long")
    void validation_WithTooLongPassword_ShouldFailValidation() {
        // given
        String longPassword = "a".repeat(31); // 31 characters
        SignupRequest signupRequest = createValidSignupRequestBuilder()
                .password(longPassword)
                .build();

        // when
        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(signupRequest);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("Should pass validation with valid password lengths")
    void validation_WithValidPasswordLengths_ShouldPassValidation() {
        // given - minimum length (6 characters)
        SignupRequest minPasswordRequest = createValidSignupRequestBuilder()
                .password("123456")
                .build();
        
        // given - maximum length (30 characters)
        SignupRequest maxPasswordRequest = createValidSignupRequestBuilder()
                .password("a".repeat(30))
                .build();

        // when & then
        assertThat(validator.validate(minPasswordRequest)).isEmpty();
        assertThat(validator.validate(maxPasswordRequest)).isEmpty();
    }

    // Name validation tests
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("Should fail validation when name is null, empty, or blank")
    void validation_WithInvalidName_ShouldFailValidation(String invalidName) {
        // given
        SignupRequest signupRequest = createValidSignupRequestBuilder()
                .name(invalidName)
                .build();

        // when
        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(signupRequest);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
    }

    @Test
    @DisplayName("Should fail validation when name is too long")
    void validation_WithTooLongName_ShouldFailValidation() {
        // given
        String longName = "a".repeat(16); // 16 characters
        SignupRequest signupRequest = createValidSignupRequestBuilder()
                .name(longName)
                .build();

        // when
        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(signupRequest);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
    }

    // Nickname validation tests
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("Should fail validation when nickName is null, empty, or blank")
    void validation_WithInvalidNickName_ShouldFailValidation(String invalidNickName) {
        // given
        SignupRequest signupRequest = createValidSignupRequestBuilder()
                .nickName(invalidNickName)
                .build();

        // when
        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(signupRequest);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("nickName"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"nick123", "한글123", "nick@", "한글!"})
    @DisplayName("Should fail validation when nickName contains invalid characters or is too long")
    void validation_WithInvalidNickNamePattern_ShouldFailValidation(String invalidNickName) {
        // given
        SignupRequest signupRequest = createValidSignupRequestBuilder()
                .nickName(invalidNickName)
                .build();

        // when
        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(signupRequest);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("nickName"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"홍길동", "GilDong", "길동", "John", "김", "a", "김영수김영수", "너무긴닉네임이다"})
    @DisplayName("Should pass validation with valid nickName patterns")
    void validation_WithValidNickNamePatterns_ShouldPassValidation(String validNickName) {
        // given
        SignupRequest signupRequest = createValidSignupRequestBuilder()
                .nickName(validNickName)
                .build();

        // when
        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(signupRequest);

        // then
        assertThat(violations).isEmpty();
    }

    // Phone number validation tests
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("Should fail validation when phoneNumber is null, empty, or blank")
    void validation_WithInvalidPhoneNumber_ShouldFailValidation(String invalidPhoneNumber) {
        // given
        SignupRequest signupRequest = createValidSignupRequestBuilder()
                .phoneNumber(invalidPhoneNumber)
                .build();

        // when
        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(signupRequest);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("phoneNumber"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0101234567", "010123456789", "01112345678", "02012345678", "010-1234-5678", "010 1234 5678"})
    @DisplayName("Should fail validation with invalid phone number patterns")
    void validation_WithInvalidPhoneNumberPattern_ShouldFailValidation(String invalidPhoneNumber) {
        // given
        SignupRequest signupRequest = createValidSignupRequestBuilder()
                .phoneNumber(invalidPhoneNumber)
                .build();

        // when
        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(signupRequest);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("phoneNumber"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"01012345678", "01087654321", "01000000000", "01099999999"})
    @DisplayName("Should pass validation with valid phone number patterns")
    void validation_WithValidPhoneNumberPatterns_ShouldPassValidation(String validPhoneNumber) {
        // given
        SignupRequest signupRequest = createValidSignupRequestBuilder()
                .phoneNumber(validPhoneNumber)
                .build();

        // when
        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(signupRequest);

        // then
        assertThat(violations).isEmpty();
    }

    // Birth date validation tests
    @Test
    @DisplayName("Should fail validation when birth is null")
    void validation_WithNullBirth_ShouldFailValidation() {
        // given
        SignupRequest signupRequest = new SignupRequest(
                "user@example.com", "password123", "홍길동", "길동이", "01012345678", null
        );

        // when
        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(signupRequest);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("birth"));
    }

    @Test
    @DisplayName("Should fail validation when birth is in the future")
    void validation_WithFutureBirth_ShouldFailValidation() {
        // given
        LocalDate futureDate = LocalDate.now().plusDays(1);
        SignupRequest signupRequest = createValidSignupRequestBuilder()
                .birth(futureDate)
                .build();

        // when
        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(signupRequest);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("birth"));
    }

    @Test
    @DisplayName("Should pass validation with past birth date")
    void validation_WithPastBirth_ShouldPassValidation() {
        // given
        LocalDate pastDate = LocalDate.now().minusYears(20);
        SignupRequest signupRequest = createValidSignupRequestBuilder()
                .birth(pastDate)
                .build();

        // when
        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(signupRequest);

        // then
        assertThat(violations).isEmpty();
    }

    // Record functionality tests
    @Test
    @DisplayName("Should maintain record equality and hashCode behavior")
    void record_ShouldMaintainEqualityAndHashCode() {
        // given
        SignupRequest request1 = createValidSignupRequestBuilder().build();
        SignupRequest request2 = createValidSignupRequestBuilder().build();
        SignupRequest request3 = createValidSignupRequestBuilder()
                .loginId("different@example.com")
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
        SignupRequest signupRequest = createValidSignupRequestBuilder().build();

        // when
        String result = signupRequest.toString();

        // then
        assertThat(result).contains("SignupRequest");
        assertThat(result).contains("user@example.com");
        assertThat(result).contains("홍길동");
        assertThat(result).contains("길동이");
    }

    @Test
    @DisplayName("Should handle multiple validation errors")
    void validation_WithMultipleErrors_ShouldReturnAllViolations() {
        // given - all fields are invalid
        SignupRequest invalidRequest = new SignupRequest(
                "invalid-email", "123", "", "invalid123!", "123456789", LocalDate.now().plusDays(1)
        );

        // when
        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(invalidRequest);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(6); // Should have violations for all fields
    }

    // Helper method to create valid SignupRequest builder
    private SignupRequestBuilder createValidSignupRequestBuilder() {
        return new SignupRequestBuilder()
                .loginId("user@example.com")
                .password("password123")
                .name("홍길동")
                .nickName("길동이")
                .phoneNumber("01012345678")
                .birth(LocalDate.of(1990, 1, 1));
    }

    // Helper builder class for test data creation
    private static class SignupRequestBuilder {
        private String loginId;
        private String password;
        private String name;
        private String nickName;
        private String phoneNumber;
        private LocalDate birth;

        public SignupRequestBuilder loginId(String loginId) {
            this.loginId = loginId;
            return this;
        }

        public SignupRequestBuilder password(String password) {
            this.password = password;
            return this;
        }

        public SignupRequestBuilder name(String name) {
            this.name = name;
            return this;
        }

        public SignupRequestBuilder nickName(String nickName) {
            this.nickName = nickName;
            return this;
        }

        public SignupRequestBuilder phoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public SignupRequestBuilder birth(LocalDate birth) {
            this.birth = birth;
            return this;
        }

        public SignupRequest build() {
            return new SignupRequest(loginId, password, name, nickName, phoneNumber, birth);
        }
    }
}