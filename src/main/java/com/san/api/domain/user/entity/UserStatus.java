package com.san.api.domain.user.entity;

/** 계정 상태. LOCKED는 일시적 잠금(lockedUntil까지), WITHDRAWN은 탈퇴(논리 삭제). */
public enum UserStatus {
    ACTIVE,
    LOCKED,
    WITHDRAWN
}
