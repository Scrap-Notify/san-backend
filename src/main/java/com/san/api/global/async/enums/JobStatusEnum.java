package com.san.api.global.async.enums;

public enum JobStatusEnum {
    /** 작업 생성 직후 대기 상태 */
    PENDING,
    /** 비동기 워커가 처리 중인 상태 */
    PROCESSING,
    /** AI 응답 수신 및 DB 저장 완료 */
    COMPLETED,
    /** 처리 중 오류 발생: error_message 참고 */
    FAILED
}
