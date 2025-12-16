package com.Hamalog.exception.file;

import static org.assertj.core.api.Assertions.assertThat;

import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FileSaveFailException Tests")
class FileSaveFailExceptionTest {

    @Test
    @DisplayName("파일 저장 실패 예외 생성 시 올바른 에러 코드와 메시지를 가져야 함")
    void constructor_ShouldCreateExceptionWithCorrectErrorCodeAndMessage() {
        // given
        // when
        FileSaveFailException exception = new FileSaveFailException();

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FILE_SAVE_FAIL);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.FILE_SAVE_FAIL.getMessage());
        assertThat(exception.getMessage()).isEqualTo("파일 저장에 실패했습니다.");
    }

    @Test
    @DisplayName("CustomException을 상속해야 함")
    void fileSaveFailException_ShouldExtendCustomException() {
        // given
        FileSaveFailException exception = new FileSaveFailException();

        // when & then
        assertThat(exception).isInstanceOf(CustomException.class);
        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("예외 정보를 올바르게 표시해야 함")
    void toString_ShouldContainProperInformation() {
        // given
        FileSaveFailException exception = new FileSaveFailException();

        // when
        String result = exception.toString();

        // then
        assertThat(result).contains("FileSaveFailException");
        assertThat(result).contains("파일 저장에 실패했습니다.");
    }

    @Test
    @DisplayName("원인 예외를 설정할 수 있어야 함")
    void exception_ShouldSupportCauseChaining() {
        // given
        Throwable cause = new RuntimeException("파일 시스템 오류");
        FileSaveFailException exception = new FileSaveFailException();

        // when
        exception.initCause(cause);

        // then
        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getCause().getMessage()).isEqualTo("파일 시스템 오류");
    }

    @Test
    @DisplayName("에러 코드가 불변이어야 함")
    void errorCode_ShouldBeImmutable() {
        // given
        FileSaveFailException exception = new FileSaveFailException();
        ErrorCode originalErrorCode = exception.getErrorCode();

        // when & then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FILE_SAVE_FAIL);
        assertThat(exception.getErrorCode()).isSameAs(originalErrorCode);
    }
}