package com.san.api.global.security.redis;

/**
 * 인증 도메인에서 사용하는 Redis key prefix를 한 곳에서 관리합니다.
 */
public final class AuthRedisKeyPrefix {

    /** 사용자별 최신 refresh token 저장 key prefix. */
    public static final String REFRESH = "refresh:";

    /** 로그아웃된 access token blacklist 저장 key prefix. */
    public static final String BLACKLIST = "blacklist:";

    /** 사용자별 로그인 실패 횟수 저장 key prefix. */
    public static final String LOGIN_FAIL = "fail:";

    private AuthRedisKeyPrefix() {
    }
}
