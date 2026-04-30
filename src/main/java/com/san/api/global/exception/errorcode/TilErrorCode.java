package com.san.api.global.exception.errorcode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * TIL 도메인 에러 코드 (T 계열)
 */
@Getter
@AllArgsConstructor
public enum TilErrorCode implements ErrorCode {

    SUMMARY_NOT_FOUND(HttpStatus.NOT_FOUND, "T001", "존재하지 않는 TIL입니다."),
    SUMMARY_ACCESS_DENIED(HttpStatus.FORBIDDEN, "T002", "해당 TIL에 대한 접근 권한이 없습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
