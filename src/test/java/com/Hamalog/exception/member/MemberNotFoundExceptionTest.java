package com.Hamalog.exception.member;

import static org.assertj.core.api.Assertions.assertThat;

import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MemberNotFoundException Tests")
class MemberNotFoundExceptionTest {

    @Test
    @DisplayName("회원 미발견 예외 생성 시 올바른 에러 코드와 메시지를 가져야 함")
    void constructor_ShouldCreateExceptionWithCorrectErrorCodeAndMessage() {
        // given
        // when
        MemberNotFoundException exception = new MemberNotFoundException();

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND.getMessage());
        assertThat(exception.getMessage()).isEqualTo("회원을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("CustomException을 상속해야 함")
    void memberNotFoundException_ShouldExtendCustomException() {
        // given
        MemberNotFoundException exception = new MemberNotFoundException();

        // when & then
        assertThat(exception).isInstanceOf(CustomException.class);
        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("예외 정보를 올바르게 표시해야 함")
    void toString_ShouldContainProperInformation() {
        // given
        MemberNotFoundException exception = new MemberNotFoundException();

        // when
        String result = exception.toString();

        // then
        assertThat(result).contains("MemberNotFoundException");
        assertThat(result).contains("회원을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("원인 예외를 설정할 수 있어야 함")
    void exception_ShouldSupportCauseChaining() {
        // given
        Throwable cause = new RuntimeException("사용자 인증 실패");
        MemberNotFoundException exception = new MemberNotFoundException();

        // when
        exception.initCause(cause);

        // then
        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getCause().getMessage()).isEqualTo("사용자 인증 실패");
    }

    @Test
    @DisplayName("에러 코드가 불변이어야 함")
    void errorCode_ShouldBeImmutable() {
        // given
        MemberNotFoundException exception = new MemberNotFoundException();
        ErrorCode originalErrorCode = exception.getErrorCode();

        // when & then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
        assertThat(exception.getErrorCode()).isSameAs(originalErrorCode);
    }
}