package com.cosx.knowengine.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {

    private final int code;
    private final HttpStatus status;
    private final Object data;

    public BusinessException(int code, String message) {
        this(code, message, HttpStatus.BAD_REQUEST, null);
    }

    public BusinessException(int code, String message, HttpStatus status) {
        this(code, message, status, null);
    }

    public BusinessException(int code, String message, HttpStatus status, Object data) {
        super(message);
        this.code = code;
        this.status = status;
        this.data = data;
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(40400, message, HttpStatus.NOT_FOUND);
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(40900, message, HttpStatus.CONFLICT);
    }

    public static BusinessException conflict(int code, String message, Object data) {
        return new BusinessException(code, message, HttpStatus.CONFLICT, data);
    }

    public static BusinessException unauthorized(String message) {
        return new BusinessException(40100, message, HttpStatus.UNAUTHORIZED);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(40300, message, HttpStatus.FORBIDDEN);
    }

    public static BusinessException upstream(int code, String message) {
        return new BusinessException(code, message, HttpStatus.BAD_GATEWAY);
    }

    public static BusinessException unavailable(int code, String message) {
        return new BusinessException(code, message, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
