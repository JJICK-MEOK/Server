package com.jjikmeok.app.global.security.exception;

import com.jjikmeok.app.global.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class JwtTokenException extends RuntimeException {

    private final ErrorCode errorCode;

    public JwtTokenException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public JwtTokenException(ErrorCode errorCode) {
        this(errorCode, null);
    }
}
