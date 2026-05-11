package com.san.api.global.security.jwt;

import com.san.api.domain.auth.entity.ClientType;

/** JWT에 담긴 인증 세션 식별 정보를 표현하는 내부 값 객체입니다. */
public record JwtSessionClaims(
        ClientType clientType,
        String sessionId,
        String familyId,
        String jti
) {
}
