package com.Hamalog.exception.token;

import com.Hamalog.exception.ErrorCode;

/**
 * 토큰 만료 예외
 */
public class TokenExpiredException extends TokenException {

    public TokenExpiredException() {
        super(ErrorCode.TOKEN_EXPIRED);
    }
}

