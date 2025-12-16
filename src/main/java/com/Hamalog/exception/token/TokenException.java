package com.Hamalog.exception.token;

import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;

/**
 * 토큰 관련 예외
 */
public class TokenException extends CustomException {

    public TokenException(ErrorCode errorCode) {
        super(errorCode);
    }
}
