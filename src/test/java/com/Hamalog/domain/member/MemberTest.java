package com.Hamalog.domain.member;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Member 엔티티 테스트")
class MemberTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("유효한 nickName으로 Member 생성 성공")
    void createMember_ValidNickName_Success() {
        // Given
        Member member = Member.builder()
                .loginId("test@example.com")
                .password("password123")
                .name("홍길동")
                .nickName("길동이")
                .phoneNumber("0101234567")
                .birth(LocalDate.of(1990, 1, 1))
                .createdAt(LocalDateTime.now())
                .build();

        // When
        Set<ConstraintViolation<Member>> violations = validator.validate(member);

        // Then
        assertThat(violations).isEmpty();
        assertThat(member.getNickName()).isEqualTo("길동이");
    }

    @Test
    @DisplayName("nickName이 영어인 경우 검증 성공")
    void createMember_EnglishNickName_ValidationSuccess() {
        // Given
        Member member = Member.builder()
                .loginId("test@example.com")
                .password("password123")
                .name("홍길동")
                .nickName("nickname")
                .phoneNumber("0101234567")
                .birth(LocalDate.of(1990, 1, 1))
                .createdAt(LocalDateTime.now())
                .build();

        // When
        Set<ConstraintViolation<Member>> violations = validator.validate(member);

        // Then
        assertThat(violations).isEmpty();
        assertThat(member.getNickName()).isEqualTo("nickname");
    }

    @Test
    @DisplayName("nickName이 10자를 초과하는 경우 검증 실패")
    void createMember_TooLongNickName_ValidationFails() {
        // Given
        Member member = Member.builder()
                .loginId("test@example.com")
                .password("password123")
                .name("홍길동")
                .nickName("아주아주아주긴닉네임입니다")
                .phoneNumber("0101234567")
                .birth(LocalDate.of(1990, 1, 1))
                .createdAt(LocalDateTime.now())
                .build();

        // When
        Set<ConstraintViolation<Member>> violations = validator.validate(member);

        // Then
        assertThat(violations).hasSize(2); // Pattern and Size violations
    }

    @Test
    @DisplayName("nickName이 한글 10자인 경우 검증 성공")
    void createMember_MaxLengthKoreanNickName_Success() {
        // Given
        Member member = Member.builder()
                .loginId("test@example.com")
                .password("password123")
                .name("홍길동")
                .nickName("한글닉네임최대열자")
                .phoneNumber("0101234567")
                .birth(LocalDate.of(1990, 1, 1))
                .createdAt(LocalDateTime.now())
                .build();

        // When
        Set<ConstraintViolation<Member>> violations = validator.validate(member);

        // Then
        assertThat(violations).isEmpty();
        assertThat(member.getNickName()).isEqualTo("한글닉네임최대열자");
    }

    @Test
    @DisplayName("nickName이 한글과 영어 혼합인 경우 검증 성공")
    void createMember_MixedKoreanEnglishNickName_Success() {
        // Given
        Member member = Member.builder()
                .loginId("test@example.com")
                .password("password123")
                .name("홍길동")
                .nickName("홍길동Kim")
                .phoneNumber("0101234567")
                .birth(LocalDate.of(1990, 1, 1))
                .createdAt(LocalDateTime.now())
                .build();

        // When
        Set<ConstraintViolation<Member>> violations = validator.validate(member);

        // Then
        assertThat(violations).isEmpty();
        assertThat(member.getNickName()).isEqualTo("홍길동Kim");
    }

    @Test
    @DisplayName("nickName에 숫자가 포함된 경우 검증 실패")
    void createMember_NumberInNickName_ValidationFails() {
        // Given
        Member member = Member.builder()
                .loginId("test@example.com")
                .password("password123")
                .name("홍길동")
                .nickName("홍길동123")
                .phoneNumber("0101234567")
                .birth(LocalDate.of(1990, 1, 1))
                .createdAt(LocalDateTime.now())
                .build();

        // When
        Set<ConstraintViolation<Member>> violations = validator.validate(member);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("닉네임은 한글과 영어만 사용할 수 있습니다.");
    }

    @Test
    @DisplayName("nickName에 특수문자가 포함된 경우 검증 실패")
    void createMember_SpecialCharInNickName_ValidationFails() {
        // Given
        Member member = Member.builder()
                .loginId("test@example.com")
                .password("password123")
                .name("홍길동")
                .nickName("홍길동@")
                .phoneNumber("0101234567")
                .birth(LocalDate.of(1990, 1, 1))
                .createdAt(LocalDateTime.now())
                .build();

        // When
        Set<ConstraintViolation<Member>> violations = validator.validate(member);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("닉네임은 한글과 영어만 사용할 수 있습니다.");
    }

    @Test
    @DisplayName("유효한 이메일 형식의 loginId로 Member 생성 성공")
    void createMember_ValidEmailLoginId_Success() {
        // Given
        Member member = Member.builder()
                .loginId("user@example.com")
                .password("password123")
                .name("홍길동")
                .nickName("길동이")
                .phoneNumber("0101234567")
                .birth(LocalDate.of(1990, 1, 1))
                .createdAt(LocalDateTime.now())
                .build();

        // When
        Set<ConstraintViolation<Member>> violations = validator.validate(member);

        // Then
        assertThat(violations).isEmpty();
        assertThat(member.getLoginId()).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("잘못된 이메일 형식의 loginId로 Member 생성 실패")
    void createMember_InvalidEmailLoginId_ValidationFails() {
        // Given
        Member member = Member.builder()
                .loginId("invalidemailformat")
                .password("password123")
                .name("홍길동")
                .nickName("길동이")
                .phoneNumber("0101234567")
                .birth(LocalDate.of(1990, 1, 1))
                .createdAt(LocalDateTime.now())
                .build();

        // When
        Set<ConstraintViolation<Member>> violations = validator.validate(member);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("올바른 이메일 형식을 입력해주세요.");
    }

    @Test
    @DisplayName("유효한 010 휴대폰 번호로 Member 생성 성공")
    void createMember_ValidPhoneNumber_Success() {
        // Given
        Member member = Member.builder()
                .loginId("user@example.com")
                .password("password123")
                .name("홍길동")
                .nickName("길동이")
                .phoneNumber("0101234567")
                .birth(LocalDate.of(1990, 1, 1))
                .createdAt(LocalDateTime.now())
                .build();

        // When
        Set<ConstraintViolation<Member>> violations = validator.validate(member);

        // Then
        assertThat(violations).isEmpty();
        assertThat(member.getPhoneNumber()).isEqualTo("0101234567");
    }

    @Test
    @DisplayName("010으로 시작하지 않는 휴대폰 번호로 Member 생성 실패")
    void createMember_InvalidPhoneNumberNotStartWith010_ValidationFails() {
        // Given
        Member member = Member.builder()
                .loginId("user@example.com")
                .password("password123")
                .name("홍길동")
                .nickName("길동이")
                .phoneNumber("0201234567")
                .birth(LocalDate.of(1990, 1, 1))
                .createdAt(LocalDateTime.now())
                .build();

        // When
        Set<ConstraintViolation<Member>> violations = validator.validate(member);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("전화번호는 010으로 시작하는 10자리 숫자를 입력해주세요.");
    }

    @Test
    @DisplayName("10자리가 아닌 휴대폰 번호로 Member 생성 실패")
    void createMember_InvalidPhoneNumberLength_ValidationFails() {
        // Given
        Member member = Member.builder()
                .loginId("user@example.com")
                .password("password123")
                .name("홍길동")
                .nickName("길동이")
                .phoneNumber("010123456789")
                .birth(LocalDate.of(1990, 1, 1))
                .createdAt(LocalDateTime.now())
                .build();

        // When
        Set<ConstraintViolation<Member>> violations = validator.validate(member);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("전화번호는 010으로 시작하는 10자리 숫자를 입력해주세요.");
    }

    @Test
    @DisplayName("하이픈이 포함된 휴대폰 번호로 Member 생성 실패")
    void createMember_PhoneNumberWithHyphen_ValidationFails() {
        // Given
        Member member = Member.builder()
                .loginId("user@example.com")
                .password("password123")
                .name("홍길동")
                .nickName("길동이")
                .phoneNumber("010-123-4567")
                .birth(LocalDate.of(1990, 1, 1))
                .createdAt(LocalDateTime.now())
                .build();

        // When
        Set<ConstraintViolation<Member>> violations = validator.validate(member);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("전화번호는 010으로 시작하는 10자리 숫자를 입력해주세요.");
    }

    @Test
    @DisplayName("이름이 15자인 경우 검증 성공")
    void createMember_ValidMaxLengthName_Success() {
        // Given
        Member member = Member.builder()
                .loginId("user@example.com")
                .password("password123")
                .name("이름최대길이테스트열다섯자")
                .nickName("길동이")
                .phoneNumber("0101234567")
                .birth(LocalDate.of(1990, 1, 1))
                .createdAt(LocalDateTime.now())
                .build();

        // When
        Set<ConstraintViolation<Member>> violations = validator.validate(member);

        // Then
        assertThat(violations).isEmpty();
        assertThat(member.getName()).isEqualTo("이름최대길이테스트열다섯자");
    }

    @Test
    @DisplayName("이름이 15자를 초과하는 경우 검증 실패")
    void createMember_TooLongName_ValidationFails() {
        // Given
        Member member = Member.builder()
                .loginId("user@example.com")
                .password("password123")
                .name("이름최대길이테스트열다섯자초과길어짐")
                .nickName("길동이")
                .phoneNumber("0101234567")
                .birth(LocalDate.of(1990, 1, 1))
                .createdAt(LocalDateTime.now())
                .build();

        // When
        Set<ConstraintViolation<Member>> violations = validator.validate(member);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("사용자 이름은 최대 15자까지 입력할 수 있습니다.");
    }

    @Test
    @DisplayName("비밀번호가 30자인 경우 검증 성공")
    void createMember_ValidMaxLengthPassword_Success() {
        // Given
        Member member = Member.builder()
                .loginId("user@example.com")
                .password("password1234567890abcdefghij30")
                .name("홍길동")
                .nickName("길동이")
                .phoneNumber("0101234567")
                .birth(LocalDate.of(1990, 1, 1))
                .createdAt(LocalDateTime.now())
                .build();

        // When
        Set<ConstraintViolation<Member>> violations = validator.validate(member);

        // Then
        assertThat(violations).isEmpty();
        assertThat(member.getPassword()).isEqualTo("password1234567890abcdefghij30");
    }

    @Test
    @DisplayName("암호화된 비밀번호(BCrypt 해시)로 Member 생성 성공")
    void createMember_EncryptedPassword_Success() {
        // Given - BCrypt hash of "testpassword" (60 characters)
        String bcryptHash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        Member member = Member.builder()
                .loginId("user@example.com")
                .password(bcryptHash)
                .name("홍길동")
                .nickName("길동이")
                .phoneNumber("0101234567")
                .birth(LocalDate.of(1990, 1, 1))
                .createdAt(LocalDateTime.now())
                .build();

        // When
        Set<ConstraintViolation<Member>> violations = validator.validate(member);

        // Then
        assertThat(violations).isEmpty();
        assertThat(member.getPassword()).isEqualTo(bcryptHash);
        assertThat(member.getPassword().length()).isEqualTo(60);
    }
}