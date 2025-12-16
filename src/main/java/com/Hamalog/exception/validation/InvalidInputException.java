package com.Hamalog.exception.validation;

import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;

/**
 * 입력값 검증 실패 예외
 */
public class InvalidInputException extends CustomException {

    public InvalidInputException(ErrorCode errorCode) {
        super(errorCode);
    }
}
