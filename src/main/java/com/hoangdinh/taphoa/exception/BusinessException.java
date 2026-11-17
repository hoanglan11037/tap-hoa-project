package com.hoangdinh.taphoa.exception;

import lombok.Getter;

/** Lỗi nghiệp vụ do tầng service ném ra; giao diện hiển thị message (tiếng Việt) cho người dùng. */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
