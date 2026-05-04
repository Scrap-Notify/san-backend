package com.san.api.global.audit.enums;

/**
 * 감사 로그에 기록할 세부 이벤트 타입.
 *
 * 사용자 주요 행위와 외부 연동 처리 결과를 코드화해 저장
 * 실패 이벤트는 {@code failureReasonCode}, {@code metadata}, {@code traceId}와 함께 장애 원인 분석에 활용
 */
public enum AuditEventType {
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    SIGNUP,
    WITHDRAW,

    GITHUB_TOKEN_LINKED,
    GITHUB_TOKEN_REFRESH_FAILED,
    GITHUB_API_REQUESTED,
    GITHUB_API_SUCCEEDED,
    GITHUB_API_FAILED,

    TIL_COMMIT_REQUESTED,
    TIL_COMMIT_PROCESSING,
    TIL_COMMIT_SUCCEEDED,
    TIL_COMMIT_FAILED,
    TIL_COMMIT_RETRY_REQUESTED,
    TIL_COMMIT_DUPLICATE_BLOCKED,

    NOTIFICATION_SENT,
    NOTIFICATION_READ,
    NOTIFICATION_STATUS_CHANGED,

    EXTERNAL_API_REQUESTED,
    EXTERNAL_API_SUCCEEDED,
    EXTERNAL_API_FAILED
}
