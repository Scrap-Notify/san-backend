package com.san.api.domain.auth.service;

import com.san.api.domain.auth.dto.response.TokenResponse;
import com.san.api.domain.auth.entity.ClientType;
import com.san.api.global.security.jwt.JwtProvider;
import com.san.api.global.security.redis.AuthRedisKeyPrefix;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 서비스 JWT 토큰 발급과 refresh token Redis 저장을 검증하는 테스트. */
@ExtendWith(MockitoExtension.class)
class TokenIssueServiceTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    private TokenIssueService tokenIssueService;
    private AuthSessionKeyService authSessionKeyService;

    @BeforeEach
    void setUp() {
        authSessionKeyService = new AuthSessionKeyService();
        tokenIssueService = new TokenIssueService(jwtProvider, redisTemplate, authSessionKeyService);
        ReflectionTestUtils.setField(tokenIssueService, "accessExpiration", 1800000L);
        ReflectionTestUtils.setField(tokenIssueService, "refreshExpiration", 604800000L);
    }

    @Test
    void issueTokenPairStoresRefreshTokenWithTtl() {
        String userId = "user-id";
        String sessionId = "session-id";
        when(jwtProvider.generateAccessToken(userId, ClientType.DASHBOARD, sessionId)).thenReturn("access-token");
        when(jwtProvider.generateRefreshToken(userId, ClientType.DASHBOARD, sessionId)).thenReturn("refresh-token");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        TokenResponse response = tokenIssueService.issueTokenPair(userId, ClientType.DASHBOARD, sessionId);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(1800L);
        assertThat(response.sessionId()).isEqualTo(sessionId);
        verify(valueOperations).set(
                AuthRedisKeyPrefix.REFRESH + userId + ":DASHBOARD:" + sessionId,
                "refresh-token",
                Duration.ofMillis(604800000L)
        );
        verify(setOperations).add(
                AuthRedisKeyPrefix.REFRESH + "index:user:" + userId,
                AuthRedisKeyPrefix.REFRESH + userId + ":DASHBOARD:" + sessionId
        );
        verify(redisTemplate).expire(
                AuthRedisKeyPrefix.REFRESH + "index:user:" + userId,
                Duration.ofMillis(604800000L)
        );
    }
}
