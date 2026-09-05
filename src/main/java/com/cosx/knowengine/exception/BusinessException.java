package com.cosx.knowengine.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {

    private final int code;
    private final HttpStatus status;

    public BusinessException(int code, String message) {
        this(code, message, HttpStatus.BAD_REQUEST);
    }

    public BusinessException(int code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(40400, message, HttpStatus.NOT_FOUND);
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(40900, message, HttpStatus.CONFLICT);
    }

    public static BusinessException unauthorized(String message) {
        return new BusinessException(40100, message, HttpStatus.UNAUTHORIZED);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(40300, message, HttpStatus.FORBIDDEN);
    }
}
