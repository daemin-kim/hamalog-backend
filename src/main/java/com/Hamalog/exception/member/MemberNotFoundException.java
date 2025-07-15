package com.Hamalog.exception.member;

import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;

public class MemberNotFoundException extends CustomException {

    public MemberNotFoundException() {
        super(ErrorCode.MEMBER_NOT_FOUND);
    }
}
