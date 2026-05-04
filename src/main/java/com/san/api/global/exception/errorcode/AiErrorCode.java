package com.san.api.global.exception.errorcode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/** AI 외부 연동 에러 코드 */
@Getter
@AllArgsConstructor
public enum AiErrorCode implements ErrorCode {

    AI_ANALYSIS_FAILED(HttpStatus.BAD_GATEWAY, "AI001", "AI 분석 요청에 실패했습니다."),
    AI_ANALYSIS_INVALID_RESPONSE(HttpStatus.BAD_GATEWAY, "AI002", "AI 분석 응답이 올바르지 않습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
