package com.Hamalog.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CustomException Tests")
class CustomExceptionTest {

    @Test
    @DisplayName("Should create exception with error code and message")
    void constructor_WithErrorCode_ShouldCreateExceptionWithCodeAndMessage() {
        // given
        ErrorCode errorCode = ErrorCode.MEMBER_NOT_FOUND;

        // when
        CustomException exception = new CustomException(errorCode);

        // then
        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        assertThat(exception.getMessage()).isEqualTo(errorCode.getMessage());
        assertThat(exception.getMessage()).isEqualTo("회원을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("Should handle different error codes correctly")
    void constructor_WithDifferentErrorCodes_ShouldCreateCorrespondingExceptions() {
        // given & when
        CustomException memberException = new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        CustomException scheduleException = new CustomException(ErrorCode.MEDICATION_SCHEDULE_NOT_FOUND);
        CustomException unauthorizedException = new CustomException(ErrorCode.UNAUTHORIZED);

        // then
        assertThat(memberException.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
        assertThat(memberException.getMessage()).isEqualTo("회원을 찾을 수 없습니다.");

        assertThat(scheduleException.getErrorCode()).isEqualTo(ErrorCode.MEDICATION_SCHEDULE_NOT_FOUND);
        assertThat(scheduleException.getMessage()).isEqualTo("복약 스케줄을 찾을 수 없습니다.");

        assertThat(unauthorizedException.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
        assertThat(unauthorizedException.getMessage()).isEqualTo("인증이 필요합니다.");
    }

    @Test
    @DisplayName("Should be instance of RuntimeException")
    void customException_ShouldBeRuntimeException() {
        // given
        CustomException exception = new CustomException(ErrorCode.BAD_REQUEST);

        // when & then
        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should handle business logic error codes")
    void constructor_WithBusinessLogicErrors_ShouldCreateCorrectExceptions() {
        // given & when
        CustomException duplicateException = new CustomException(ErrorCode.DUPLICATE_MEMBER);
        CustomException recordException = new CustomException(ErrorCode.MEDICATION_RECORD_NOT_FOUND);
        CustomException timeException = new CustomException(ErrorCode.MEDICATION_TIME_NOT_FOUND);

        // then
        assertThat(duplicateException.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_MEMBER);
        assertThat(duplicateException.getMessage()).isEqualTo("이미 존재하는 회원입니다.");

        assertThat(recordException.getErrorCode()).isEqualTo(ErrorCode.MEDICATION_RECORD_NOT_FOUND);
        assertThat(recordException.getMessage()).isEqualTo("복약 기록을 찾을 수 없습니다.");

        assertThat(timeException.getErrorCode()).isEqualTo(ErrorCode.MEDICATION_TIME_NOT_FOUND);
        assertThat(timeException.getMessage()).isEqualTo("복약 시간 정보를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("Should handle security error codes")
    void constructor_WithSecurityErrors_ShouldCreateCorrectExceptions() {
        // given & when
        CustomException forbiddenException = new CustomException(ErrorCode.FORBIDDEN);
        CustomException badRequestException = new CustomException(ErrorCode.BAD_REQUEST);

        // then
        assertThat(forbiddenException.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
        assertThat(forbiddenException.getMessage()).isEqualTo("접근 권한이 없습니다.");

        assertThat(badRequestException.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
        assertThat(badRequestException.getMessage()).isEqualTo("잘못된 요청입니다.");
    }

    @Test
    @DisplayName("Should handle system error codes")
    void constructor_WithSystemErrors_ShouldCreateCorrectExceptions() {
        // given & when
        CustomException serverException = new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        CustomException fileException = new CustomException(ErrorCode.FILE_SAVE_FAIL);

        // then
        assertThat(serverException.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);
        assertThat(serverException.getMessage()).isEqualTo("서버 내부 오류가 발생했습니다.");

        assertThat(fileException.getErrorCode()).isEqualTo(ErrorCode.FILE_SAVE_FAIL);
        assertThat(fileException.getMessage()).isEqualTo("파일 저장에 실패했습니다.");
    }

    @Test
    @DisplayName("Should maintain immutable error code after creation")
    void errorCode_ShouldBeImmutableAfterCreation() {
        // given
        ErrorCode originalCode = ErrorCode.MEMBER_NOT_FOUND;
        CustomException exception = new CustomException(originalCode);

        // when - error code is final, so it can't be modified

        // then
        assertThat(exception.getErrorCode()).isEqualTo(originalCode);
        assertThat(exception.getErrorCode()).isSameAs(originalCode);
    }

    @Test
    @DisplayName("Should have proper toString representation")
    void toString_ShouldContainErrorCodeInformation() {
        // given
        CustomException exception = new CustomException(ErrorCode.MEMBER_NOT_FOUND);

        // when
        String result = exception.toString();

        // then
        assertThat(result).contains("CustomException");
        assertThat(result).contains("회원을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("Should support exception chaining with cause")
    void exception_ShouldSupportCauseChaining() {
        // given
        Throwable cause = new IllegalArgumentException("Original cause");
        CustomException exception = new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);

        // when
        exception.initCause(cause);

        // then
        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getCause().getMessage()).isEqualTo("Original cause");
    }
}