package com.Hamalog.exception.medication;

import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MedicationTimeNotFoundException Tests")
class MedicationTimeNotFoundExceptionTest {

    @Test
    @DisplayName("복약 시간 미발견 예외 생성 시 올바른 에러 코드와 메시지를 가져야 함")
    void constructor_ShouldCreateExceptionWithCorrectErrorCodeAndMessage() {
        // given
        // when
        MedicationTimeNotFoundException exception = new MedicationTimeNotFoundException();

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEDICATION_TIME_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.MEDICATION_TIME_NOT_FOUND.getMessage());
        assertThat(exception.getMessage()).isEqualTo("복약 시간 정보를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("CustomException을 상속해야 함")
    void medicationTimeNotFoundException_ShouldExtendCustomException() {
        // given
        MedicationTimeNotFoundException exception = new MedicationTimeNotFoundException();

        // when & then
        assertThat(exception).isInstanceOf(CustomException.class);
        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("예외 정보를 올바르게 표시해야 함")
    void toString_ShouldContainProperInformation() {
        // given
        MedicationTimeNotFoundException exception = new MedicationTimeNotFoundException();

        // when
        String result = exception.toString();

        // then
        assertThat(result).contains("MedicationTimeNotFoundException");
        assertThat(result).contains("복약 시간 정보를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("원인 예외를 설정할 수 있어야 함")
    void exception_ShouldSupportCauseChaining() {
        // given
        Throwable cause = new RuntimeException("시간 데이터 파싱 오류");
        MedicationTimeNotFoundException exception = new MedicationTimeNotFoundException();

        // when
        exception.initCause(cause);

        // then
        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getCause().getMessage()).isEqualTo("시간 데이터 파싱 오류");
    }

    @Test
    @DisplayName("에러 코드가 불변이어야 함")
    void errorCode_ShouldBeImmutable() {
        // given
        MedicationTimeNotFoundException exception = new MedicationTimeNotFoundException();
        ErrorCode originalErrorCode = exception.getErrorCode();

        // when & then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEDICATION_TIME_NOT_FOUND);
        assertThat(exception.getErrorCode()).isSameAs(originalErrorCode);
    }
}