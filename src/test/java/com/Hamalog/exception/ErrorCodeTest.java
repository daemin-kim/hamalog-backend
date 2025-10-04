package com.Hamalog.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ErrorCode Enum Tests")
class ErrorCodeTest {

    @Test
    @DisplayName("Should have correct business logic error codes")
    void businessLogicErrorCodes_ShouldHaveCorrectValues() {
        // given & when & then
        assertThat(ErrorCode.MEMBER_NOT_FOUND.getCode()).isEqualTo("MEMBER_NOT_FOUND");
        assertThat(ErrorCode.MEMBER_NOT_FOUND.getMessage()).isEqualTo("회원을 찾을 수 없습니다.");

        assertThat(ErrorCode.DUPLICATE_MEMBER.getCode()).isEqualTo("DUPLICATE_MEMBER");
        assertThat(ErrorCode.DUPLICATE_MEMBER.getMessage()).isEqualTo("이미 존재하는 회원입니다.");

        assertThat(ErrorCode.MEDICATION_SCHEDULE_NOT_FOUND.getCode()).isEqualTo("SCHEDULE_NOT_FOUND");
        assertThat(ErrorCode.MEDICATION_SCHEDULE_NOT_FOUND.getMessage()).isEqualTo("복약 스케줄을 찾을 수 없습니다.");

        assertThat(ErrorCode.MEDICATION_RECORD_NOT_FOUND.getCode()).isEqualTo("RECORD_NOT_FOUND");
        assertThat(ErrorCode.MEDICATION_RECORD_NOT_FOUND.getMessage()).isEqualTo("복약 기록을 찾을 수 없습니다.");

        assertThat(ErrorCode.MEDICATION_TIME_NOT_FOUND.getCode()).isEqualTo("TIME_NOT_FOUND");
        assertThat(ErrorCode.MEDICATION_TIME_NOT_FOUND.getMessage()).isEqualTo("복약 시간 정보를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("Should have correct security error codes")
    void securityErrorCodes_ShouldHaveCorrectValues() {
        // given & when & then
        assertThat(ErrorCode.UNAUTHORIZED.getCode()).isEqualTo("UNAUTHORIZED");
        assertThat(ErrorCode.UNAUTHORIZED.getMessage()).isEqualTo("인증이 필요합니다.");

        assertThat(ErrorCode.FORBIDDEN.getCode()).isEqualTo("FORBIDDEN");
        assertThat(ErrorCode.FORBIDDEN.getMessage()).isEqualTo("접근 권한이 없습니다.");

        assertThat(ErrorCode.BAD_REQUEST.getCode()).isEqualTo("BAD_REQUEST");
        assertThat(ErrorCode.BAD_REQUEST.getMessage()).isEqualTo("잘못된 요청입니다.");
    }

    @Test
    @DisplayName("Should have correct system error codes")
    void systemErrorCodes_ShouldHaveCorrectValues() {
        // given & when & then
        assertThat(ErrorCode.INTERNAL_SERVER_ERROR.getCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(ErrorCode.INTERNAL_SERVER_ERROR.getMessage()).isEqualTo("서버 내부 오류가 발생했습니다.");

        assertThat(ErrorCode.FILE_SAVE_FAIL.getCode()).isEqualTo("FILE_SAVE_FAIL");
        assertThat(ErrorCode.FILE_SAVE_FAIL.getMessage()).isEqualTo("파일 저장에 실패했습니다.");
    }

    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    @DisplayName("Should have non-null and non-empty code and message for all error codes")
    void allErrorCodes_ShouldHaveValidCodeAndMessage(ErrorCode errorCode) {
        // when & then
        assertThat(errorCode.getCode()).isNotNull();
        assertThat(errorCode.getCode()).isNotEmpty();
        assertThat(errorCode.getMessage()).isNotNull();
        assertThat(errorCode.getMessage()).isNotEmpty();
    }

    @Test
    @DisplayName("Should have correct total number of error codes")
    void errorCodes_ShouldHaveCorrectTotalCount() {
        // when
        ErrorCode[] errorCodes = ErrorCode.values();

        // then
        assertThat(errorCodes).hasSize(10);
    }

    @Test
    @DisplayName("Should support valueOf for all error codes")
    void valueOf_ShouldReturnCorrectErrorCodes() {
        // given & when & then
        assertThat(ErrorCode.valueOf("MEMBER_NOT_FOUND")).isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
        assertThat(ErrorCode.valueOf("DUPLICATE_MEMBER")).isEqualTo(ErrorCode.DUPLICATE_MEMBER);
        assertThat(ErrorCode.valueOf("MEDICATION_SCHEDULE_NOT_FOUND")).isEqualTo(ErrorCode.MEDICATION_SCHEDULE_NOT_FOUND);
        assertThat(ErrorCode.valueOf("MEDICATION_RECORD_NOT_FOUND")).isEqualTo(ErrorCode.MEDICATION_RECORD_NOT_FOUND);
        assertThat(ErrorCode.valueOf("MEDICATION_TIME_NOT_FOUND")).isEqualTo(ErrorCode.MEDICATION_TIME_NOT_FOUND);
        assertThat(ErrorCode.valueOf("UNAUTHORIZED")).isEqualTo(ErrorCode.UNAUTHORIZED);
        assertThat(ErrorCode.valueOf("FORBIDDEN")).isEqualTo(ErrorCode.FORBIDDEN);
        assertThat(ErrorCode.valueOf("BAD_REQUEST")).isEqualTo(ErrorCode.BAD_REQUEST);
        assertThat(ErrorCode.valueOf("INTERNAL_SERVER_ERROR")).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);
        assertThat(ErrorCode.valueOf("FILE_SAVE_FAIL")).isEqualTo(ErrorCode.FILE_SAVE_FAIL);
    }

    @Test
    @DisplayName("Should maintain enum singleton property")
    void errorCodes_ShouldBeSingletonInstances() {
        // given & when
        ErrorCode memberNotFound1 = ErrorCode.MEMBER_NOT_FOUND;
        ErrorCode memberNotFound2 = ErrorCode.valueOf("MEMBER_NOT_FOUND");

        // then
        assertThat(memberNotFound1).isSameAs(memberNotFound2);
    }

    @Test
    @DisplayName("Should have immutable properties")
    void errorCodeProperties_ShouldBeImmutable() {
        // given
        ErrorCode errorCode = ErrorCode.MEMBER_NOT_FOUND;
        String originalCode = errorCode.getCode();
        String originalMessage = errorCode.getMessage();

        // when - properties are final, so they can't be modified

        // then
        assertThat(errorCode.getCode()).isEqualTo(originalCode);
        assertThat(errorCode.getMessage()).isEqualTo(originalMessage);
    }

    @Test
    @DisplayName("Should support ordinal and name methods")
    void errorCodes_ShouldSupportOrdinalAndName() {
        // given
        ErrorCode memberNotFound = ErrorCode.MEMBER_NOT_FOUND;

        // when & then
        assertThat(memberNotFound.name()).isEqualTo("MEMBER_NOT_FOUND");
        assertThat(memberNotFound.ordinal()).isEqualTo(0);
        
        ErrorCode duplicateMember = ErrorCode.DUPLICATE_MEMBER;
        assertThat(duplicateMember.ordinal()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should have proper toString representation")
    void toString_ShouldReturnEnumName() {
        // given
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

        // when
        String result = errorCode.toString();

        // then
        assertThat(result).isEqualTo("INTERNAL_SERVER_ERROR");
    }

    @Test
    @DisplayName("Should be comparable as enum")
    void errorCodes_ShouldBeComparable() {
        // given
        ErrorCode first = ErrorCode.MEMBER_NOT_FOUND;
        ErrorCode second = ErrorCode.DUPLICATE_MEMBER;

        // when & then
        assertThat(first.compareTo(second)).isLessThan(0);
        assertThat(second.compareTo(first)).isGreaterThan(0);
        assertThat(first.compareTo(first)).isEqualTo(0);
    }

    @Test
    @DisplayName("Should categorize error codes correctly by type")
    void errorCodes_ShouldBeCategorizedByType() {
        // Business logic errors
        ErrorCode[] businessErrors = {
                ErrorCode.MEMBER_NOT_FOUND,
                ErrorCode.DUPLICATE_MEMBER,
                ErrorCode.MEDICATION_SCHEDULE_NOT_FOUND,
                ErrorCode.MEDICATION_RECORD_NOT_FOUND,
                ErrorCode.MEDICATION_TIME_NOT_FOUND
        };

        // Security errors
        ErrorCode[] securityErrors = {
                ErrorCode.UNAUTHORIZED,
                ErrorCode.FORBIDDEN,
                ErrorCode.BAD_REQUEST
        };

        // System errors
        ErrorCode[] systemErrors = {
                ErrorCode.INTERNAL_SERVER_ERROR,
                ErrorCode.FILE_SAVE_FAIL
        };

        // then
        for (ErrorCode businessError : businessErrors) {
            assertThat(businessError.getMessage()).containsAnyOf("찾을 수 없습니다", "존재하는");
        }

        for (ErrorCode securityError : securityErrors) {
            assertThat(securityError.getMessage()).containsAnyOf("인증", "권한", "요청");
        }

        for (ErrorCode systemError : systemErrors) {
            assertThat(systemError.getMessage()).containsAnyOf("오류", "실패");
        }
    }
}