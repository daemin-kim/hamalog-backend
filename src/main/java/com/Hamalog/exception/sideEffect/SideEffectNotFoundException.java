package com.Hamalog.exception.sideEffect;

import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;

/**
 * 부작용 정보를 찾을 수 없는 경우의 예외
 */
public class SideEffectNotFoundException extends CustomException {

    public SideEffectNotFoundException() {
        super(ErrorCode.SIDE_EFFECT_NOT_FOUND);
    }

    public SideEffectNotFoundException(Long sideEffectId) {
        super(ErrorCode.SIDE_EFFECT_NOT_FOUND);
    }
}

