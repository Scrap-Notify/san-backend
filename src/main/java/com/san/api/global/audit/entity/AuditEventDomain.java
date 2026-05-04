package com.san.api.global.audit.entity;

/**
 * 감사 로그 이벤트가 발생한 업무 도메인.
 *
 * 도메인 단위 필터링과 통계 집계를 위해 사용
 */
public enum AuditEventDomain {
    AUTH,
    USER,
    GITHUB,
    TIL,
    NOTIFICATION,
    EXTERNAL_API
}
