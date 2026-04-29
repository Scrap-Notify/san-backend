package com.san.api.domain.user.entity;

/** 인증 제공자 유형. LOCAL은 자체 회원가입, GITHUB은 OAuth2 소셜 로그인. */
public enum AuthProvider {
    LOCAL,
    GITHUB
}
