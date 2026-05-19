package com.san.api.global.exception.errorcode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/** 리콜 에러 코드 */
@Getter
@AllArgsConstructor
public enum RecallErrorCode implements ErrorCode {

    RECALL_TIL_NOT_FOUND(HttpStatus.NOT_FOUND, "R001", "리콜 퀴즈를 생성할 TIL이 없습니다."),
    EMPTY_RECALL_SOURCE(HttpStatus.BAD_REQUEST, "R002", "리콜 퀴즈를 생성할 원본이 없습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
