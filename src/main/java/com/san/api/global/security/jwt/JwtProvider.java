package com.san.api.global.security.jwt;

import com.san.api.domain.auth.entity.ClientType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/** JWT 토큰 생성, 파싱, 검증. subject는 userId(UUID 문자열). */
@Component
public class JwtProvider {

    private static final String TOKEN_TYPE_CLAIM = "typ";
    private static final String CLIENT_TYPE_CLAIM = "clientType";
    private static final String SESSION_ID_CLAIM = "sessionId";
    private static final String FAMILY_ID_CLAIM = "familyId";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final SecretKey secretKey;
    private final long accessExpiration;
    private final long refreshExpiration;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration}") long accessExpiration,
            @Value("${jwt.refresh-expiration}") long refreshExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
    }

    public String generateAccessToken(String userId) {
        return buildToken(userId, accessExpiration, ACCESS_TOKEN_TYPE);
    }

    public String generateAccessToken(String userId, ClientType clientType, String sessionId) {
        return buildToken(userId, accessExpiration, ACCESS_TOKEN_TYPE, clientType, sessionId);
    }

    public String generateRefreshToken(String userId) {
        return buildToken(userId, refreshExpiration, REFRESH_TOKEN_TYPE);
    }

    public String generateRefreshToken(String userId, ClientType clientType, String sessionId) {
        return generateRefreshToken(userId, clientType, sessionId, UUID.randomUUID().toString());
    }

    public String generateRefreshToken(String userId, ClientType clientType, String sessionId, String familyId) {
        // familyId는 refresh token rotation 과정에서 같은 로그인 세션 묶음을 추적하는 값입니다.
        return buildToken(userId, refreshExpiration, REFRESH_TOKEN_TYPE, clientType, sessionId, familyId);
    }

    /** 토큰에서 Authentication 객체 추출. principal은 userId(String). */
    public Authentication getAuthentication(String token) {
        String userId = getUserId(token);
        return new UsernamePasswordAuthenticationToken(userId, null, List.of());
    }

    public String getUserId(String token) {
        return getClaims(token).getSubject();
    }

    public JwtSessionClaims getSessionClaims(String token) {
        Claims claims = getClaims(token);
        ClientType clientType = ClientType.from(claims.get(CLIENT_TYPE_CLAIM, String.class));
        String sessionId = claims.get(SESSION_ID_CLAIM, String.class);
        String familyId = claims.get(FAMILY_ID_CLAIM, String.class);
        String jti = claims.getId();
        return new JwtSessionClaims(clientType, sessionId, familyId, jti);
    }

    /** 토큰 남은 유효시간(ms). 블랙리스트 TTL 계산에 사용. */
    public long getRemainingExpiration(String token) {
        Date expiration = getClaims(token).getExpiration();
        return Math.max(0, expiration.getTime() - System.currentTimeMillis());
    }

    public boolean isAccessToken(String token) {
        return ACCESS_TOKEN_TYPE.equals(getTokenType(token));
    }

    public boolean isRefreshToken(String token) {
        return REFRESH_TOKEN_TYPE.equals(getTokenType(token));
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private String getTokenType(String token) {
        return getClaims(token).get(TOKEN_TYPE_CLAIM, String.class);
    }

    private String buildToken(String userId, long expiration, String tokenType) {
        return buildToken(userId, expiration, tokenType, null, null);
    }

    private String buildToken(String userId, long expiration, String tokenType, ClientType clientType, String sessionId) {
        return buildToken(userId, expiration, tokenType, clientType, sessionId, null);
    }

    private String buildToken(String userId, long expiration, String tokenType, ClientType clientType, String sessionId, String familyId) {
        Date now = new Date();
        var builder = Jwts.builder()
                .subject(userId)
                .id(UUID.randomUUID().toString())
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(secretKey);

        if (clientType != null) {
            builder.claim(CLIENT_TYPE_CLAIM, clientType.name());
        }
        if (sessionId != null && !sessionId.isBlank()) {
            builder.claim(SESSION_ID_CLAIM, sessionId);
        }
        if (familyId != null && !familyId.isBlank()) {
            builder.claim(FAMILY_ID_CLAIM, familyId);
        }

        return builder.compact();
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
