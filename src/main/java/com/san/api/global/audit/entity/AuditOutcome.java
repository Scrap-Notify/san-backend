package com.san.api.global.audit.entity;

/**
 * 감사 이벤트 처리 결과.
 * 성공/실패 기준 조회, 실패 이벤트 대시보드, 장애 분석에 사용
 */
public enum AuditOutcome {
    SUCCESS,
    FAILURE
}
