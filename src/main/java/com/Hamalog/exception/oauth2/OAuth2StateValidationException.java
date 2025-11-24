package com.Hamalog.exception.oauth2;

import com.Hamalog.exception.ErrorCode;

/**
 * OAuth2 State 검증 실패 예외 (CSRF 공격 방지)
 */
public class OAuth2StateValidationException extends OAuth2Exception {

    public OAuth2StateValidationException() {
        super(ErrorCode.OAUTH2_STATE_VALIDATION_FAILED);
    }
}

