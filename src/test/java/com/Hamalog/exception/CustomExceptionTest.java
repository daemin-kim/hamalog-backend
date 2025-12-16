package com.Hamalog.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CustomExceptionTest {

    @Test
    @DisplayName("CustomException 생성 시 ErrorCode의 메시지가 정상적으로 저장되는지 확인")
    void createCustomException() {
        // given
        ErrorCode errorCode = ErrorCode.MEMBER_NOT_FOUND;

        // when
        CustomException exception = new CustomException(errorCode);

        // then
        assertThat(exception.getMessage()).isEqualTo("회원을 찾을 수 없습니다.");
        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
    }

    @Test
    @DisplayName("여러 ErrorCode에 대한 CustomException 생성 테스트")
    void createCustomExceptionWithDifferentErrorCodes() {
        // given
        ErrorCode[] errorCodes = {
            ErrorCode.UNAUTHORIZED,
            ErrorCode.BAD_REQUEST,
            ErrorCode.INTERNAL_SERVER_ERROR
        };

        for (ErrorCode errorCode : errorCodes) {
            // when
            CustomException exception = new CustomException(errorCode);

            // then
            assertThat(exception.getMessage()).isEqualTo(errorCode.getMessage());
            assertThat(exception.getErrorCode().getCode()).isEqualTo(errorCode.getCode());
            assertThat(exception).isInstanceOf(RuntimeException.class);
        }
    }
}
