package com.Hamalog.exception.oauth2;

import com.Hamalog.exception.ErrorCode;

/**
 * OAuth2 토큰 교환 실패 예외
 */
public class OAuth2TokenExchangeException extends OAuth2Exception {

    public OAuth2TokenExchangeException() {
        super(ErrorCode.OAUTH2_TOKEN_EXCHANGE_FAILED);
    }

    public OAuth2TokenExchangeException(String details) {
        super(ErrorCode.OAUTH2_TOKEN_EXCHANGE_FAILED, details);
    }
}
