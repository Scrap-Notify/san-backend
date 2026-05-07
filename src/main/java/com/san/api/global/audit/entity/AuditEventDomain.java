package com.san.api.global.audit.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 감사 로그 이벤트가 발생한 업무 도메인.
 *
 * 도메인 단위 필터링과 통계 집계를 위해 사용
 */
@Getter
@RequiredArgsConstructor
public enum AuditEventDomain {
    AUTH("인증"),
    USER("사용자"),
    GITHUB("GitHub 연동"),
    TIL("TIL"),
    NOTIFICATION("알림"),
    EXTERNAL_API("외부 API");

    private final String description;
}