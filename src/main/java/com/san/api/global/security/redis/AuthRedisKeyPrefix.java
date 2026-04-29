package com.san.api.global.security.redis;

public final class AuthRedisKeyPrefix {

    public static final String REFRESH = "refresh:";
    public static final String BLACKLIST = "blacklist:";
    public static final String LOGIN_FAIL = "fail:";

    private AuthRedisKeyPrefix() {
    }
}
