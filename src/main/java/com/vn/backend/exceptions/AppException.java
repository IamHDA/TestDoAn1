package com.vn.backend.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.io.Serial;

@Getter
public class AppException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String code;

    private final HttpStatus httpStatus;

    public AppException(String code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }
}