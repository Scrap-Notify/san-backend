package com.san.api.domain.auth.service;

import com.san.api.domain.auth.dto.response.TokenResponse;
import com.san.api.global.security.jwt.JwtProvider;
import com.san.api.global.security.redis.AuthRedisKeyPrefix;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenIssueService {

    private final JwtProvider jwtProvider;
    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.access-expiration}")
    private long accessExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    /**
     * 사용자 ID를 subject로 하는 서비스 JWT 토큰 쌍을 발급합니다.
     *
     * Refresh token은 사용자별 최신 토큰만 유효하도록 Redis에 TTL과 함께 저장합니다.
     */
    public TokenResponse issueTokenPair(String userId) {
        String accessToken = jwtProvider.generateAccessToken(userId);
        String refreshToken = jwtProvider.generateRefreshToken(userId);

        redisTemplate.opsForValue().set(
                AuthRedisKeyPrefix.REFRESH + userId,
                refreshToken,
                Duration.ofMillis(refreshExpiration)
        );

        return TokenResponse.of(accessToken, refreshToken, accessExpiration / 1000);
    }
}
