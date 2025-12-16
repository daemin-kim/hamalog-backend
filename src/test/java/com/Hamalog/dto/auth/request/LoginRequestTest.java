package com.Hamalog.dto.auth.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("LoginRequest DTO Tests")
class LoginRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Should create valid LoginRequest with valid data")
    void constructor_WithValidData_ShouldCreateValidRequest() {
        // given
        String loginId = "user@example.com";
        String password = "validpassword";

        // when
        LoginRequest loginRequest = new LoginRequest(loginId, password);

        // then
        assertThat(loginRequest.loginId()).isEqualTo(loginId);
        assertThat(loginRequest.password()).isEqualTo(password);

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(loginRequest);
        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Should fail validation when loginId is null, empty, or blank")
    void validation_WithInvalidLoginId_ShouldFailValidation(String invalidLoginId) {
        // given
        LoginRequest loginRequest = new LoginRequest(invalidLoginId, "validpassword");

        // when
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(loginRequest);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("로그인 ID는 필수입니다");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("loginId");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Should fail validation when password is null, empty, or blank")
    void validation_WithInvalidPassword_ShouldFailValidation(String invalidPassword) {
        // given
        LoginRequest loginRequest = new LoginRequest("user@example.com", invalidPassword);

        // when
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(loginRequest);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
        assertThat(violations).anyMatch(v -> 
                v.getPropertyPath().toString().equals("password") && 
                (v.getMessage().contains("필수") || v.getMessage().contains("6자 이상"))
        );
    }

    @Test
    @DisplayName("Should fail validation when loginId exceeds maximum length")
    void validation_WithTooLongLoginId_ShouldFailValidation() {
        // given
        String tooLongLoginId = "a".repeat(101); // 101 characters
        LoginRequest loginRequest = new LoginRequest(tooLongLoginId, "validpassword");

        // when
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(loginRequest);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("로그인 ID는 100자를 초과할 수 없습니다");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("loginId");
    }

    @Test
    @DisplayName("Should fail validation when password is too short")
    void validation_WithTooShortPassword_ShouldFailValidation() {
        // given
        String tooShortPassword = "12345"; // 5 characters
        LoginRequest loginRequest = new LoginRequest("user@example.com", tooShortPassword);

        // when
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(loginRequest);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("비밀번호는 6자 이상 100자 이하여야 합니다");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("password");
    }

    @Test
    @DisplayName("Should fail validation when password is too long")
    void validation_WithTooLongPassword_ShouldFailValidation() {
        // given
        String tooLongPassword = "a".repeat(101); // 101 characters
        LoginRequest loginRequest = new LoginRequest("user@example.com", tooLongPassword);

        // when
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(loginRequest);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("비밀번호는 6자 이상 100자 이하여야 합니다");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("password");
    }

    @Test
    @DisplayName("Should pass validation with boundary values")
    void validation_WithBoundaryValues_ShouldPassValidation() {
        // given - minimum valid password length (6 characters)
        LoginRequest minPasswordRequest = new LoginRequest("user@example.com", "123456");
        
        // given - maximum valid loginId length (100 characters)
        String maxLoginId = "a".repeat(100);
        LoginRequest maxLoginIdRequest = new LoginRequest(maxLoginId, "validpassword");
        
        // given - maximum valid password length (100 characters)
        String maxPassword = "a".repeat(100);
        LoginRequest maxPasswordRequest = new LoginRequest("user@example.com", maxPassword);

        // when & then
        assertThat(validator.validate(minPasswordRequest)).isEmpty();
        assertThat(validator.validate(maxLoginIdRequest)).isEmpty();
        assertThat(validator.validate(maxPasswordRequest)).isEmpty();
    }

    @Test
    @DisplayName("Should handle multiple validation errors")
    void validation_WithMultipleErrors_ShouldReturnAllViolations() {
        // given
        LoginRequest invalidRequest = new LoginRequest("", "123"); // both invalid

        // when
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(invalidRequest);

        // then
        assertThat(violations).hasSize(2);
        assertThat(violations).extracting(ConstraintViolation::getPropertyPath)
                .extracting(Object::toString)
                .containsExactlyInAnyOrder("loginId", "password");
    }

    @Test
    @DisplayName("Should maintain record equality and hashCode behavior")
    void record_ShouldMaintainEqualityAndHashCode() {
        // given
        LoginRequest request1 = new LoginRequest("user@example.com", "password123");
        LoginRequest request2 = new LoginRequest("user@example.com", "password123");
        LoginRequest request3 = new LoginRequest("other@example.com", "password123");

        // then
        assertThat(request1).isEqualTo(request2);
        assertThat(request1).isNotEqualTo(request3);
        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
        assertThat(request1.hashCode()).isNotEqualTo(request3.hashCode());
    }

    @Test
    @DisplayName("Should have proper toString representation")
    void toString_ShouldContainFieldInformation() {
        // given
        LoginRequest loginRequest = new LoginRequest("user@example.com", "password123");

        // when
        String result = loginRequest.toString();

        // then
        assertThat(result).contains("LoginRequest");
        assertThat(result).contains("loginId=user@example.com");
        assertThat(result).contains("password=password123");
    }

    @Test
    @DisplayName("Should handle special characters in loginId and password")
    void validation_WithSpecialCharacters_ShouldPassValidation() {
        // given
        LoginRequest loginRequest = new LoginRequest("user+test@example-domain.com", "p@ssw0rd!#$");

        // when
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(loginRequest);

        // then
        assertThat(violations).isEmpty();
        assertThat(loginRequest.loginId()).isEqualTo("user+test@example-domain.com");
        assertThat(loginRequest.password()).isEqualTo("p@ssw0rd!#$");
    }

    @Test
    @DisplayName("Should handle Unicode characters correctly")
    void validation_WithUnicodeCharacters_ShouldHandleCorrectly() {
        // given
        LoginRequest loginRequest = new LoginRequest("사용자@예시.com", "한글비밀번호123");

        // when
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(loginRequest);

        // then
        assertThat(violations).isEmpty();
        assertThat(loginRequest.loginId()).isEqualTo("사용자@예시.com");
        assertThat(loginRequest.password()).isEqualTo("한글비밀번호123");
    }
}