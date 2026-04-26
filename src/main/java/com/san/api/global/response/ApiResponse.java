package com.san.api.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.san.api.global.exception.ErrorCode;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean ok,
        T data,
        String error,
        String message,
        Instant timestamp) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, null, Instant.now());
    }

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(true, null, null, null, Instant.now());
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(false, null, errorCode.name(), message, Instant.now());
    }
}
