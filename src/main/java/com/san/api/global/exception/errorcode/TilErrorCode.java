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
    EMPTY_TIL_SOURCE(HttpStatus.BAD_REQUEST, "T003", "TIL 생성에 사용할 지식 원본이 없습니다."),
    INVALID_TIL_SOURCE_CONTENT(HttpStatus.BAD_REQUEST, "T004", "TIL 생성에 사용할 지식 원본이 유효하지 않습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
