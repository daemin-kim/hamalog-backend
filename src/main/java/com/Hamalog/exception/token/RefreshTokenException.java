package com.Hamalog.exception.token;

import com.Hamalog.exception.ErrorCode;

/**
 * Refresh Token 관련 예외
 */
public class RefreshTokenException extends TokenException {

    public RefreshTokenException(ErrorCode errorCode) {
        super(errorCode);
    }
}

