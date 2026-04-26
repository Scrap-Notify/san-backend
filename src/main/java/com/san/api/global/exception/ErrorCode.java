package com.san.api.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // common
    BAD_REQUEST(HttpStatus.BAD_REQUEST,                  "Bad request"),
    INVALID_INPUT(HttpStatus.BAD_REQUEST,                "Validation failed"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED,    "Method not allowed"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND,             "Resource not found"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error"),

    // auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED,                "Authentication required"),
    FORBIDDEN(HttpStatus.FORBIDDEN,                      "Access denied"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED,               "Invalid token"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED,               "Token expired"),

    // user
    USER_NOT_FOUND(HttpStatus.NOT_FOUND,                 "User not found"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT,                 "이미 사용 중인 이메일입니다."),
    ;

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
