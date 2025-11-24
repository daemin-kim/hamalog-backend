package com.Hamalog.exception.oauth2;
import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
/**
 * OAuth2 관련 예외
 */
public class OAuth2Exception extends CustomException {
    public OAuth2Exception(ErrorCode errorCode) {
        super(errorCode);
    }
    public OAuth2Exception(ErrorCode errorCode, String details) {
        super(errorCode);
    }
}
