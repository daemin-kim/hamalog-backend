package com.Hamalog.exception.file;

import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;

public class FileSaveFailException extends CustomException {
    public FileSaveFailException() {
        super(ErrorCode.FILE_SAVE_FAIL);
    }
}
